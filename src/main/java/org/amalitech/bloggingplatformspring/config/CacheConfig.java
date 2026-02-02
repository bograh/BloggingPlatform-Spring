package org.amalitech.bloggingplatformspring.config;

import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Configuration for caching with statistics tracking
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private static final ConcurrentHashMap<String, CacheStatistics> cacheStats = new ConcurrentHashMap<>();

    /**
     * Get statistics for all caches
     */
    public static ConcurrentHashMap<String, CacheStatistics> getAllCacheStatistics() {
        return cacheStats;
    }

    /**
     * Reset statistics for all caches
     */
    public static void resetAllStatistics() {
        cacheStats.values().forEach(CacheStatistics::reset);
    }

    /**
     * Configure cache manager with monitoring capabilities
     */
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        String[] cacheNames = {"users", "posts", "allPosts", "comments", "tags"};

        Arrays.stream(cacheNames).forEach(name ->
                cacheStats.putIfAbsent(name, new CacheStatistics(name))
        );

        cacheManager.setCaches(Arrays.stream(cacheNames)
                .map(name -> new MonitoredCache(name, cacheStats.get(name)))
                .toList());

        cacheManager.afterPropertiesSet();

        return cacheManager;
    }

    /**
     * Custom cache implementation with hit/miss tracking
     */
    static class MonitoredCache extends ConcurrentMapCache {
        private final CacheStatistics statistics;

        public MonitoredCache(String name, CacheStatistics statistics) {
            super(name);
            this.statistics = statistics;
        }

        @Override
        public ValueWrapper get(@NonNull Object key) {
            ValueWrapper value = super.get(key);
            if (value != null) {
                statistics.recordHit();
            } else {
                statistics.recordMiss();
            }
            return value;
        }

        @Override
        public <T> T get(@NonNull Object key, Class<T> type) {
            T value = super.get(key, type);
            if (value != null) {
                statistics.recordHit();
            } else {
                statistics.recordMiss();
            }
            return value;
        }

        @Override
        public void put(@NonNull Object key, Object value) {
            super.put(key, value);
            statistics.recordPut();
        }

        @Override
        public void evict(@NonNull Object key) {
            super.evict(key);
            statistics.recordEviction();
        }

        @Override
        public void clear() {
            super.clear();
            statistics.recordClear();
        }
    }

    /**
     * Statistics holder for a cache
     */
    public static class CacheStatistics {
        @Getter
        private final String cacheName;
        private final AtomicLong hits = new AtomicLong(0);
        private final AtomicLong misses = new AtomicLong(0);
        private final AtomicLong puts = new AtomicLong(0);
        private final AtomicLong evictions = new AtomicLong(0);
        private final AtomicLong clears = new AtomicLong(0);

        public CacheStatistics(String cacheName) {
            this.cacheName = cacheName;
        }

        public void recordHit() {
            hits.incrementAndGet();
        }

        public void recordMiss() {
            misses.incrementAndGet();
        }

        public void recordPut() {
            puts.incrementAndGet();
        }

        public void recordEviction() {
            evictions.incrementAndGet();
        }

        public void recordClear() {
            clears.incrementAndGet();
        }

        public void reset() {
            hits.set(0);
            misses.set(0);
            puts.set(0);
            evictions.set(0);
            clears.set(0);
        }

        public long getHits() {
            return hits.get();
        }

        public long getMisses() {
            return misses.get();
        }

        public long getPuts() {
            return puts.get();
        }

        public long getEvictions() {
            return evictions.get();
        }

        public long getClears() {
            return clears.get();
        }

        public long getTotalRequests() {
            return hits.get() + misses.get();
        }

        public double getHitRate() {
            long total = getTotalRequests();
            return total == 0 ? 0.0 : (double) hits.get() / total * 100;
        }

        public double getMissRate() {
            long total = getTotalRequests();
            return total == 0 ? 0.0 : (double) misses.get() / total * 100;
        }
    }
}