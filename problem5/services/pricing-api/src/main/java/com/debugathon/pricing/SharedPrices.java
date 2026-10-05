package com.debugathon.pricing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class SharedPrices {
    private final StringRedisTemplate redis;
    private final ObjectMapper json;
    private final Duration ttl;
    public SharedPrices(StringRedisTemplate redis, ObjectMapper json,
                        @Value("${pricing.redis-ttl}") Duration ttl) {
        this.redis = redis; this.json = json; this.ttl = ttl;
    }
    static String key(String id) { return "pricing:product:" + id; }
    public Price get(String id) {
        String value = redis.opsForValue().get(key(id));
        if (value == null) return null;
        try { return json.readValue(value, Price.class); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Invalid price document", e); }
    }
    public boolean offer(Price value) {
        boolean stored = Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key(value.productId()), encode(value), ttl));
        Events.emit("cache.offered", value, "tier", "redis", "stored", stored);
        return stored;
    }
    public void publish(Price value) {
        redis.opsForValue().set(key(value.productId()), encode(value), ttl);
        Events.emit("cache.stored", value, "tier", "redis");
    }
    private String encode(Price value) {
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Price serialization failed", e); }
    }
}
