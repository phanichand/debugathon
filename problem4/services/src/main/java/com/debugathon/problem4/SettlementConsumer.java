package com.debugathon.problem4;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
@Component
@ConditionalOnProperty(name="platform.role",havingValue="consumer")
public class SettlementConsumer {
    private static final Logger log=LoggerFactory.getLogger(SettlementConsumer.class);
    private record Completion(TopicPartition partition,long offset,long epoch) {}
    private final KafkaConsumer<String,String> client;
    private final KafkaSettings settings;
    private final SettlementProcessor processor;
    private final ObjectMapper json;
    private final MeterRegistry metrics;
    private final ThreadPoolExecutor workers;
    private final BlockingQueue<Completion> completions=new LinkedBlockingQueue<>();
    private final CompletionTracker tracker=new CompletionTracker();
    private final Map<TopicPartition,Long> epochs=new HashMap<>();
    private final AtomicInteger inflight=new AtomicInteger();
    private final String instance=UUID.randomUUID().toString();
    private final long checkpointMs;
    private final int capacity;
    private volatile boolean running=true;
    private Thread loop;
    public SettlementConsumer(KafkaSettings settings,SettlementProcessor processor,ObjectMapper json,MeterRegistry metrics,
           @Value("${platform.workers}") int count,@Value("${platform.queue-capacity}") int queue,
           @Value("${platform.checkpoint-ms}") long checkpointMs) {
        this.settings=settings; this.processor=processor; this.json=json; this.metrics=metrics; this.checkpointMs=checkpointMs;
        capacity=count+queue;
        workers=new ThreadPoolExecutor(count,count,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(queue));
        Properties p=settings.base(); p.put("group.id",settings.group); p.put("enable.auto.commit","false");
        p.put("auto.offset.reset","earliest"); p.put("max.poll.records",64);
        p.put("key.deserializer",StringDeserializer.class.getName()); p.put("value.deserializer",StringDeserializer.class.getName());
        p.put("session.timeout.ms",10000); p.put("heartbeat.interval.ms",2000);
        client=new KafkaConsumer<>(p);
        metrics.gauge("settlement_inflight",inflight);
    }
    @PostConstruct public void start() { loop=new Thread(this::run,"kafka-poll"); loop.start(); }
    private void run() {
        log.info("stage=consumer_started instance={}",instance);
        client.subscribe(List.of(settings.topic),new ConsumerRebalanceListener() {
            public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                log.info("stage=partitions_revoked instance={} partitions={}",instance,partitions);
                partitions.forEach(p->epochs.merge(p,1L,Long::sum)); tracker.forget(partitions);
            }
            public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                partitions.forEach(p->epochs.merge(p,1L,Long::sum));
                log.info("stage=partitions_assigned instance={} partitions={}",instance,partitions);
            }
        });
        long last=System.nanoTime();
        try {
            while(running) {
                Completion done;
                while((done=completions.poll())!=null)
                    if(client.assignment().contains(done.partition()) && Objects.equals(epochs.get(done.partition()),done.epoch()))
                        tracker.record(done.partition(),done.offset());
                if(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-last)>=checkpointMs) {
                    var snapshot=tracker.snapshot();
                    if(!snapshot.isEmpty()) {
                        try {
                            client.commitSync(snapshot,Duration.ofSeconds(5));
                            snapshot.forEach((p,o)->log.info("stage=checkpoint_saved partition={} nextOffset={} instance={}",p.partition(),o.offset(),instance));
                        } catch(CommitFailedException ex) { log.warn("stage=checkpoint_deferred instance={} reason={}",instance,ex.getClass().getSimpleName()); }
                    }
                    last=System.nanoTime();
                }
                if(capacity-inflight.get()<64) client.pause(client.assignment()); else client.resume(client.assignment());
                for(ConsumerRecord<String,String> record:client.poll(Duration.ofMillis(50))) {
                    TopicPartition partition=new TopicPartition(record.topic(),record.partition());
                    long epoch=epochs.getOrDefault(partition,0L);
                    metrics.counter("settlement_received_total").increment();
                    log.info("stage=received partition={} offset={} instance={}",record.partition(),record.offset(),instance);
                    inflight.incrementAndGet();
                    workers.execute(()->{
                        try {
                            BookingEvent event=json.readValue(record.value(),BookingEvent.class); event.validate();
                            processor.process(event,record.partition(),record.offset(),instance);
                            completions.add(new Completion(partition,record.offset(),epoch));
                        } catch(InterruptedException ex) { Thread.currentThread().interrupt(); }
                          catch(Exception ex) { metrics.counter("settlement_errors_total").increment(); log.error("stage=processing_error partition={} offset={} instance={}",record.partition(),record.offset(),instance,ex); }
                        finally { inflight.decrementAndGet(); }
                    });
                }
            }
        } catch(WakeupException ex) { if(running) throw ex; }
          catch(Exception ex) { log.error("stage=consumer_failed instance={}",instance,ex); System.exit(1); }
        finally { workers.shutdownNow(); client.close(Duration.ofSeconds(5)); }
    }
    @PreDestroy public void stop() throws InterruptedException { running=false; client.wakeup(); if(loop!=null) loop.join(10000); }
}
