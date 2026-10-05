#!/usr/bin/env python3
"""Public environment/symptom checks. Not the organizer's grading suite."""
import json, os, subprocess, time, urllib.request, uuid
from pathlib import Path
BASE=os.environ.get('BASE_URL','http://localhost:8084')
COMPOSE=['docker','compose','-f','docker/docker-compose.yml']
EVIDENCE=Path('evidence'); EVIDENCE.mkdir(exist_ok=True)
results=[]
def command(*args,env=None):
    return subprocess.check_output(COMPOSE+list(args),text=True,env=env)
def sql(query):
    return command('exec','-T','postgres','psql','-U','settlement','-d','settlements','-At','-v','ON_ERROR_STOP=1','-c',query).strip()
def api(path,body=None):
    data=None if body is None else json.dumps(body).encode()
    request=urllib.request.Request(BASE+path,data=data,headers={'Content-Type':'application/json'})
    with urllib.request.urlopen(request,timeout=40) as r: return json.load(r)
def wait(fn,timeout=90):
    end=time.monotonic()+timeout; last=None
    while time.monotonic()<end:
        try:
            last=fn()
            if last: return last
        except Exception as ex: last=str(ex)
        time.sleep(.2)
    raise AssertionError(f'Condition timed out; last result: {last}')
def reconcile(run): return api('/operations/reconciliation?runId='+run)
def publish(run,label,merchant='standard',booking=None):
    return api('/events',{'eventId':run+'-'+label,'bookingId':booking or run+'-booking-'+label,'merchantId':merchant,'amountPaise':82000,'currency':'INR','runId':run})
def settled(event): return api('/operations/events/'+event)['settlements']
def offsets(): return api('/operations/offsets')
def lag_zero(): return all(x['lag']==0 for x in offsets())
def save(name,data):
    results.append({'scenario':name,**data}); (EVIDENCE/'results.json').write_text(json.dumps(results,indent=2))
def recreate(checkpoint):
    env={**os.environ,'CHECKPOINT_MS':str(checkpoint),'RECEIPT_TIMEOUT_RATE':'0'}
    command('up','-d','--no-deps','--force-recreate','consumer',env=env)
    wait(lambda:'stage=partitions_assigned' in command('logs','--no-color','consumer'))
def kill():
    command('kill','-s','SIGKILL','consumer')
try:
    wait(lambda:api('/actuator/health')['status']=='UP',180)
    wait(lambda:len(offsets())==3)
    run='happy-'+uuid.uuid4().hex[:8]
    for i in range(20): publish(run,str(i))
    wait(lambda:reconcile(run)['settlementRows']==20)
    wait(lag_zero)
    report=reconcile(run)
    assert report['expected']==20 and not report['missing'] and not report['duplicates'],report
    save('ordinary processing',{'reconciliation':report,'offsets':offsets()})
    print('PASS ordinary processing',flush=True)

    run='incident-'+uuid.uuid4().hex[:8]; merchant=run+'-merchant'; key=run+'-booking'
    holder=subprocess.Popen(COMPOSE+['exec','-T','-e','PGAPPNAME=public-symptom-check','postgres','psql','-U','settlement','-d','settlements','-At','-c',f"SELECT pg_advisory_lock(hashtext('{merchant}')); SELECT pg_sleep(120);"],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
    try:
        wait(lambda:int(sql("SELECT count(*) FROM pg_locks l JOIN pg_stat_activity a ON a.pid=l.pid WHERE a.application_name='public-symptom-check' AND l.locktype='advisory' AND l.granted"))==1)
        first=publish(run,'a',merchant,key)
        wait(lambda:int(sql("SELECT count(*) FROM pg_locks WHERE locktype='advisory' AND NOT granted"))>=1)
        later=[publish(run,str(i),'standard',key) for i in range(3)]
        assert all(x['partition']==first['partition'] for x in later)
        wait(lambda:reconcile(run)['settlementRows']==3)
        wait(lag_zero)
        before=offsets()
        assert next(p['committed'] for p in before if p['partition']==first['partition'])>first['offset']
        assignments=command('logs','--no-color','consumer').count('stage=partitions_assigned')
        killed_at=time.time(); kill()
        (EVIDENCE/'incident-consumer.log').write_text(command('logs','--no-color','consumer'))
        sql("SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE application_name='public-symptom-check'")
        holder.wait(timeout=10)
        command('start','consumer')
        wait(lambda:command('logs','--no-color','consumer').count('stage=partitions_assigned')>assignments)
        wait(lag_zero)
        time.sleep(2)
        report=reconcile(run)
        assert len(report['missing'])==1 and report['missing'][0]['event_id']==first['eventId'],report
        save('business reconciliation after restart',{'event':first,'later':later,'beforeRestart':before,'killedAtEpochSeconds':killed_at,'afterRestart':offsets(),'reconciliation':report})
        print('PASS zero lag with missing settlement after restart',flush=True)
    finally:
        sql("SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE application_name='public-symptom-check'")
        holder.wait(timeout=10)

    recreate(60000)
    run='replay-'+uuid.uuid4().hex[:8]
    event=publish(run,'a')
    wait(lambda:len(settled(event['eventId']))==1)
    before=offsets()
    assert next(p['committed'] for p in before if p['partition']==event['partition'])<=event['offset']
    killed_at=time.time(); kill()
    (EVIDENCE/'replay-consumer.log').write_text(command('logs','--no-color','consumer'))
    recreate(1000)
    wait(lambda:len(settled(event['eventId']))==2)
    wait(lag_zero)
    report=reconcile(run)
    assert len(report['duplicates'])==1 and not report['missing'],report
    save('redelivery after restart',{'event':event,'beforeRestart':before,'killedAtEpochSeconds':killed_at,'afterRestart':offsets(),'reconciliation':report,'eventEvidence':api('/operations/events/'+event['eventId'])})
    print('PASS replay can create duplicate settlements',flush=True)

    run='retry-'+uuid.uuid4().hex[:8]
    first=publish(run,'a'); second=publish(run,'a')
    wait(lambda:len(settled(first['eventId']))==2)
    report=reconcile(run)
    assert report['expected']==1 and len(report['duplicates'])==1,report
    save('repeated business delivery',{'deliveries':[first,second],'reconciliation':report})
    print('PASS repeated delivery retains business identity',flush=True)
finally:
    try: (EVIDENCE/'compose.log').write_text(command('logs','--no-color'))
    except Exception: pass
