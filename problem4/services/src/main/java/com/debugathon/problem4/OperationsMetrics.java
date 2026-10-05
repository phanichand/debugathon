package com.debugathon.problem4;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component
@ConditionalOnProperty(name="platform.role",havingValue="producer")
public class OperationsMetrics {
    private final OperationsApi api;
    private final org.springframework.jdbc.core.JdbcTemplate db;
    private final AtomicLong databaseHealthy=new AtomicLong();
    private final AtomicLong lag=new AtomicLong(-1),available=new AtomicLong();
    public OperationsMetrics(OperationsApi api,MeterRegistry registry,org.springframework.jdbc.core.JdbcTemplate db) {
        this.db=db; registry.gauge("settlement_database_healthy",databaseHealthy);
        this.api=api; registry.gauge("settlement_group_lag",lag); registry.gauge("settlement_broker_available",available);
    }
    @Scheduled(fixedDelay=3000) public void sample() {
        try { databaseHealthy.set(db.queryForObject("SELECT 1",Integer.class)); } catch(Exception ex) { databaseHealthy.set(0); }
        try { long value=0; for(var p:api.offsets()) value+=((Number)p.get("lag")).longValue(); lag.set(value); available.set(1); }
        catch(Exception ex) { available.set(0); lag.set(-1); }
    }
}
