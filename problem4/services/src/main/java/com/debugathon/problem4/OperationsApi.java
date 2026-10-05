package com.debugathon.problem4;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
@RestController
@ConditionalOnProperty(name="platform.role",havingValue="producer")
public class OperationsApi {
    private final EventPublisher publisher;
    private final LoadGenerator load;
    private final JdbcTemplate db;
    private final KafkaSettings settings;
    public OperationsApi(EventPublisher publisher,LoadGenerator load,JdbcTemplate db,KafkaSettings settings) {
        this.publisher=publisher; this.load=load; this.db=db; this.settings=settings;
    }
    @PostMapping("/events") public Map<String,Object> event(@RequestBody BookingEvent event) throws Exception { return publisher.publish(event); }
    @PostMapping("/traffic") public Map<String,Object> traffic(@RequestParam(defaultValue="200") int rate,
         @RequestParam(defaultValue="180") int seconds,@RequestParam(defaultValue="42") long seed,
         @RequestParam(defaultValue="0.01") double reviews,@RequestParam(defaultValue="0.0005") double retries,
         @RequestParam String runId) { return load.start(runId,rate,seconds,seed,reviews,retries); }
    @GetMapping("/traffic") public Map<String,Object> traffic() { return load.status(); }
    @GetMapping("/operations/reconciliation") public Map<String,Object> reconcile(@RequestParam String runId) {
        Long expected=db.queryForObject("SELECT count(*) FROM source_events e WHERE run_id=? AND EXISTS(SELECT 1 FROM delivery_receipts d WHERE d.event_id=e.event_id)",Long.class,runId);
        var missing=db.queryForList("SELECT e.event_id FROM source_events e WHERE run_id=? AND EXISTS(SELECT 1 FROM delivery_receipts d WHERE d.event_id=e.event_id) AND NOT EXISTS(SELECT 1 FROM settlements s WHERE s.event_id=e.event_id) ORDER BY e.event_id",runId);
        var duplicates=db.queryForList("SELECT event_id,count(*) AS copies,sum(amount_paise) AS total_paise FROM settlements WHERE run_id=? GROUP BY event_id HAVING count(*)>1 ORDER BY event_id",runId);
        Long rows=db.queryForObject("SELECT count(*) FROM settlements WHERE run_id=?",Long.class,runId);
        return Map.of("runId",runId,"expected",expected,"settlementRows",rows,"missing",missing,"duplicates",duplicates);
    }
    @GetMapping("/operations/events/{id}") public Map<String,Object> evidence(@PathVariable String id) {
        return Map.of("source",db.queryForList("SELECT * FROM source_events WHERE event_id=?",id),
           "deliveries",db.queryForList("SELECT * FROM delivery_receipts WHERE event_id=? ORDER BY created_at",id),
           "settlements",db.queryForList("SELECT * FROM settlements WHERE event_id=? ORDER BY created_at",id));
    }
    @GetMapping("/operations/offsets") public List<Map<String,Object>> offsets() throws Exception {
        try(Admin admin=Admin.create(settings.base())) {
            var partitions=admin.describeTopics(List.of(settings.topic)).allTopicNames().get(5,TimeUnit.SECONDS).get(settings.topic).partitions();
            Map<TopicPartition,OffsetSpec> requests=new HashMap<>();
            partitions.forEach(p->requests.put(new TopicPartition(settings.topic,p.partition()),OffsetSpec.latest()));
            var end=admin.listOffsets(requests).all().get(5,TimeUnit.SECONDS);
            var committed=admin.listConsumerGroupOffsets(settings.group).partitionsToOffsetAndMetadata().get(5,TimeUnit.SECONDS);
            List<Map<String,Object>> result=new ArrayList<>();
            for(var p:requests.keySet()) {
                OffsetAndMetadata c=committed.get(p); long next=c==null?0:c.offset();
                result.add(Map.of("partition",p.partition(),"committed",next,"end",end.get(p).offset(),"lag",end.get(p).offset()-next));
            }
            result.sort(Comparator.comparingInt(x->(Integer)x.get("partition"))); return result;
        }
    }
    @ExceptionHandler(IllegalArgumentException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String,String> bad(IllegalArgumentException e) { return Map.of("error",e.getMessage()); }
    @ExceptionHandler(IllegalStateException.class) @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String,String> conflict(IllegalStateException e) { return Map.of("error",e.getMessage()); }
}
