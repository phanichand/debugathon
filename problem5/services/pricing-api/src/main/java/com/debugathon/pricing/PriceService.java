package com.debugathon.pricing;

import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class PriceService {
    private final PriceRepository repository;
    private final LocalPrices local;
    private final SharedPrices shared;
    private final QuoteAssembler quotes;
    private final MeterRegistry meters;
    public PriceService(PriceRepository repository, LocalPrices local, SharedPrices shared,
                        QuoteAssembler quotes, MeterRegistry meters) {
        this.repository = repository; this.local = local; this.shared = shared;
        this.quotes = quotes; this.meters = meters;
    }
    public Price get(String id) {
        Price value = local.get(id);
        if (value != null) return served(value, "local");
        value = shared.get(id);
        if (value != null) {
            local.put(value);
            return served(value, "redis");
        }
        Price snapshot = repository.find(id);
        Quote quote = quotes.prepare(snapshot);
        shared.offer(quote.price());
        local.put(quote.price());
        return served(quote.price(), "postgres");
    }
    public Price update(String id, BigDecimal amount, long expectedVersion) {
        Price value = repository.update(id, amount, expectedVersion);
        local.invalidate(value);
        shared.publish(value);
        local.put(value);
        meters.counter("pricing_updates", "outcome", "success").increment();
        Events.emit("update.completed", value);
        return value;
    }
    private Price served(Price value, String source) {
        meters.counter("pricing_reads", "source", source).increment();
        Events.emit("price.served", value, "source", source);
        return value;
    }
}
