package org.amalitech.bloggingplatformspring.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import org.amalitech.bloggingplatformspring.utils.Constants;
import org.jspecify.annotations.NonNull;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
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

        String[] cacheNames = {
                Constants.USERS_CACHE_NAME,
                Constants.POSTS_CACHE_NAME,
                Constants.POST_LIST_CACHE_NAME,
                Constants.TAGS_CACHE_NAME,
                Constants.COMMENTS_CACHE_NAME
        };

        Arrays.stream(cacheNames).forEach(name ->
                cacheStats.putIfAbsent(name, new CacheStatistics(name))
        );

        cacheManager.setCaches(List.of(
                new MonitoredCaffeineCache(Constants.USERS_CACHE_NAME,
                        Caffeine.newBuilder()
                                .expireAfterWrite(10, TimeUnit.MINUTES)
                                .recordStats()
                                .build(),
                        cacheStats.get(Constants.USERS_CACHE_NAME)),

                new MonitoredCaffeineCache(Constants.POSTS_CACHE_NAME,
                        Caffeine.newBuilder()
                                .expireAfterWrite(15, TimeUnit.MINUTES)
                                .recordStats()
                                .build(),
                        cacheStats.get(Constants.POSTS_CACHE_NAME)),

                new MonitoredCaffeineCache(Constants.POST_LIST_CACHE_NAME,
                        Caffeine.newBuilder()
                                .expireAfterWrite(5, TimeUnit.MINUTES)
                                .recordStats()
                                .build(),
                        cacheStats.get(Constants.POST_LIST_CACHE_NAME)),

                new MonitoredCaffeineCache(Constants.TAGS_CACHE_NAME,
                        Caffeine.newBuilder()
                                .expireAfterWrite(15, TimeUnit.MINUTES)
                                .recordStats()
                                .build(),
                        cacheStats.get(Constants.TAGS_CACHE_NAME)),

                new MonitoredCaffeineCache(Constants.COMMENTS_CACHE_NAME,
                        Caffeine.newBuilder()
                                .expireAfterWrite(5, TimeUnit.MINUTES)
                                .recordStats()
                                .build(),
                        cacheStats.get(Constants.COMMENTS_CACHE_NAME))
        ));

        return cacheManager;
    }

    /**
     * Custom Caffeine cache implementation with hit/miss tracking
     */
    static class MonitoredCaffeineCache extends CaffeineCache {
        private final CacheStatistics statistics;

        public MonitoredCaffeineCache(String name, com.github.benmanes.caffeine.cache.Cache<Object, Object> cache,
                                      CacheStatistics statistics) {
            super(name, cache);
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