package com.debugathon.pricing;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.spi.LoggingEventBuilder;

final class Events {
    private static final Logger LOG = LoggerFactory.getLogger("pricing.events");
    private static final AtomicLong SEQUENCE = new AtomicLong();
    private static final String BOOT_ID = UUID.randomUUID().toString();
    private Events() {}

    static void emit(String event, Price price, Object... fields) {
        LoggingEventBuilder entry = LOG.atInfo().addKeyValue("event", event)
            .addKeyValue("eventSequence", SEQUENCE.incrementAndGet())
            .addKeyValue("bootId", BOOT_ID).addKeyValue("monotonicNanos", System.nanoTime())
            .addKeyValue("requestId", MDC.get("requestId"));
        if (price != null) {
            entry.addKeyValue("productId", price.productId()).addKeyValue("version", price.version())
                .addKeyValue("amount", price.amount()).addKeyValue("updatedAt", price.updatedAt());
        }
        for (int i = 0; i < fields.length; i += 2) entry.addKeyValue(fields[i].toString(), fields[i + 1]);
        entry.log(event);
    }
}
