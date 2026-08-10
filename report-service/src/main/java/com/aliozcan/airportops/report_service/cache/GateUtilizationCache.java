package com.aliozcan.airportops.report_service.cache;

import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Caches the serialized gate-utilization report per org/date. Cache-aside: the
 * controller reads through this on a miss and repopulates it; the consumer
 * evicts the key whenever an event changes the underlying read model so the
 * next read is never stale for longer than the time it takes Kafka to deliver.
 */
@Component
public class GateUtilizationCache {

    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;

    public GateUtilizationCache(
            StringRedisTemplate redisTemplate,
            @Value("${app.report.gate-utilization-cache-ttl-seconds}") long ttlSeconds) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    public String get(UUID organizationId, LocalDate date) {
        return redisTemplate.opsForValue().get(keyOf(organizationId, date));
    }

    public void put(UUID organizationId, LocalDate date, String json) {
        redisTemplate.opsForValue().set(keyOf(organizationId, date), json, ttl);
    }

    public void evict(UUID organizationId, LocalDate date) {
        redisTemplate.delete(keyOf(organizationId, date));
    }

    private String keyOf(UUID organizationId, LocalDate date) {
        return "org:" + organizationId + ":report:gate-utilization:" + date;
    }
}
