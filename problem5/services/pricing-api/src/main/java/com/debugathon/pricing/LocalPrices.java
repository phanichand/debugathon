package com.debugathon.pricing;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalPrices {
    private final Cache<String, Price> entries;

    public LocalPrices(@Value("${pricing.local-ttl}") Duration ttl, MeterRegistry meters) {
        entries = Caffeine.newBuilder().maximumSize(10000).expireAfterWrite(ttl).recordStats().build();
        CaffeineCacheMetrics.monitor(meters, entries, "prices");
    }
    public Price get(String id) { return entries.getIfPresent(id); }
    public void put(Price value) {
        entries.put(value.productId(), value);
        Events.emit("cache.stored", value, "tier", "local");
    }
    public void invalidate(Price value) {
        entries.invalidate(value.productId());
        Events.emit("cache.invalidated", value, "tier", "local", "result", "completed");
    }
}
