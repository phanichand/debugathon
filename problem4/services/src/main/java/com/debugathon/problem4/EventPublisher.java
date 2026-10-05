package com.debugathon.problem4;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
@Service
@ConditionalOnProperty(name="platform.role",havingValue="producer")
public class EventPublisher {
    private final KafkaProducer<String,String> producer;
    private final KafkaSettings settings;
    private final JdbcTemplate db;
    private final ObjectMapper json;
    public EventPublisher(KafkaSettings settings, JdbcTemplate db, ObjectMapper json) {
        this.settings=settings; this.db=db; this.json=json;
        Properties p=settings.base(); p.put("key.serializer",StringSerializer.class.getName());
        p.put("value.serializer",StringSerializer.class.getName()); p.put("acks","all");
        p.put("enable.idempotence","true"); p.put("delivery.timeout.ms",30000);
        producer=new KafkaProducer<>(p);
    }
    public Map<String,Object> publish(BookingEvent event) throws Exception {
        event.validate();
        String payload=json.writeValueAsString(event);
        db.update("INSERT INTO source_events(event_id,run_id,payload) VALUES (?,?,?::jsonb) ON CONFLICT DO NOTHING",
                  event.eventId(),event.runId(),payload);
        String stored=db.queryForObject("SELECT payload::text FROM source_events WHERE event_id=?",String.class,event.eventId());
        if (!json.readTree(payload).equals(json.readTree(stored))) throw new IllegalArgumentException("Event ID payload conflict");
        RecordMetadata m=producer.send(new ProducerRecord<>(settings.topic,event.bookingId(),payload)).get(35,TimeUnit.SECONDS);
        db.update("INSERT INTO delivery_receipts(event_id,topic,partition_id,record_offset) VALUES (?,?,?,?)",
                  event.eventId(),settings.topic,m.partition(),m.offset());
        return Map.of("eventId",event.eventId(),"partition",m.partition(),"offset",m.offset());
    }
    @PreDestroy public void close() { producer.close(); }
}
