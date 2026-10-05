package com.debugathon.problem4;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
@Repository
public class SettlementRepository {
    private final JdbcTemplate db;
    private final TransactionTemplate tx;
    public SettlementRepository(JdbcTemplate db,TransactionTemplate tx) { this.db=db; this.tx=tx; }
    public UUID post(BookingEvent event,int partition,long offset,String instance) {
        UUID execution=UUID.randomUUID();
        tx.executeWithoutResult(status->db.update("""
            INSERT INTO settlements(execution_id,event_id,booking_id,amount_paise,currency,partition_id,record_offset,instance_id,run_id)
            VALUES (?,?,?,?,?,?,?,?,?)
            """,execution,event.eventId(),event.bookingId(),event.amountPaise(),event.currency(),partition,offset,instance,event.runId()));
        return execution;
    }
}
