package com.debugathon.problem4;
import jakarta.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
@Service
@ConditionalOnProperty(name="platform.role",havingValue="producer")
public class LoadGenerator {
    private final EventPublisher publisher;
    private final ExecutorService pool=Executors.newFixedThreadPool(8);
    private final AtomicBoolean running=new AtomicBoolean();
    private final AtomicLong sent=new AtomicLong(),errors=new AtomicLong();
    private volatile String runId="";
    public LoadGenerator(EventPublisher publisher) { this.publisher=publisher; }
    public synchronized Map<String,Object> start(String id, int rate, int seconds, long seed, double reviews, double retries) {
        if (rate<1 || rate>500 || seconds<1 || seconds>3600 || reviews<0 || reviews>0.1 || retries<0 || retries>0.1)
            throw new IllegalArgumentException("Invalid workload bounds");
        if (!running.compareAndSet(false,true)) throw new IllegalStateException("A workload is active");
        runId=id; sent.set(0); errors.set(0);
        AtomicInteger remaining=new AtomicInteger(8);
        for(int lane=0;lane<8;lane++) {
            final int n=lane;
            pool.submit(()->{
                Random random=new Random(seed+n); long start=System.nanoTime();
                try {
                    for(int i=n;i<rate*seconds;i+=8) {
                        long wait=start+(long)(i*1_000_000_000.0/rate)-System.nanoTime();
                        if(wait>0) TimeUnit.NANOSECONDS.sleep(wait);
                        BookingEvent event=new BookingEvent(id+"-"+i,id+"-booking-"+i,
                            random.nextDouble()<reviews?"review":"standard",82000,"INR",id);
                        try { publisher.publish(event); sent.incrementAndGet();
                            if(random.nextDouble()<retries) publisher.publish(event);
                        } catch(Exception ex) { errors.incrementAndGet(); }
                    }
                } catch(InterruptedException ex) { Thread.currentThread().interrupt(); }
                finally { if(remaining.decrementAndGet()==0) running.set(false); }
            });
        }
        return status();
    }
    public Map<String,Object> status() { return Map.of("runId",runId,"running",running.get(),"published",sent.get(),"errors",errors.get()); }
    @PreDestroy public void close() { pool.shutdownNow(); }
}
