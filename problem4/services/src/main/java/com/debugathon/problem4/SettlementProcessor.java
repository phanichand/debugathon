package com.debugathon.problem4;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import io.micrometer.core.instrument.MeterRegistry;
@Service
public class SettlementProcessor {
    private static final Logger log=LoggerFactory.getLogger(SettlementProcessor.class);
    private final SettlementRepository repository;
    private final JdbcTemplate db;
    private final TransactionTemplate tx;
    private final MeterRegistry metrics;
    private final long reviewMs;
    private final double receiptTimeoutRate;
    public SettlementProcessor(SettlementRepository repository,JdbcTemplate db,TransactionTemplate tx,MeterRegistry metrics,
         @Value("${platform.review-ms}") long reviewMs,@Value("${platform.receipt-timeout-rate}") double receiptTimeoutRate) {
        this.repository=repository; this.db=db; this.tx=tx; this.metrics=metrics;
        this.reviewMs=reviewMs; this.receiptTimeoutRate=receiptTimeoutRate;
    }
    public void process(BookingEvent event,int partition,long offset,String instance) throws InterruptedException {
        long start=System.nanoTime();
        log.info("stage=worker_started eventId={} partition={} offset={} instance={} thread={}",event.eventId(),partition,offset,instance,Thread.currentThread().getName());
        tx.executeWithoutResult(status->db.execute((java.sql.Connection connection)->{
            try(var stmt=connection.prepareStatement("SELECT pg_advisory_xact_lock(hashtext(?))")) {
                stmt.setString(1,event.merchantId()); stmt.execute();
            }
            return null;
        }));
        Thread.sleep("review".equals(event.merchantId())?reviewMs:40+ThreadLocalRandom.current().nextInt(21));
        for(int attempt=1;attempt<=3;attempt++) {
            if(Thread.currentThread().isInterrupted()) throw new InterruptedException();
            UUID execution=repository.post(event,partition,offset,instance);
            metrics.counter("settlement_db_writes_total").increment();
            log.info("stage=settlement_committed eventId={} partition={} offset={} execution={} instance={} attempt={}",event.eventId(),partition,offset,execution,instance,attempt);
            if(attempt==1 && ThreadLocalRandom.current().nextDouble()<receiptTimeoutRate) {
                metrics.counter("settlement_retries_total").increment();
                log.warn("stage=receipt_timeout eventId={} execution={} attempt={}",event.eventId(),execution,attempt);
                Thread.sleep(100); continue;
            }
            metrics.counter("settlement_processed_total").increment();
            log.info("stage=worker_completed eventId={} partition={} offset={} instance={} durationMs={}",event.eventId(),partition,offset,instance,(System.nanoTime()-start)/1_000_000);
            return;
        }
    }
}
