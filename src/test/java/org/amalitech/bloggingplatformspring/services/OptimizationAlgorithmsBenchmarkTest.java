package org.amalitech.bloggingplatformspring.services;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Random;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OptimizationAlgorithmsBenchmarkTest {

  private static final int DATASET_SIZE = 20_000;
  private static final int ITERATIONS = 60;
  private static final int TOP_LIMIT = 20;

  @Test
  void benchmarkPopularRetrievalBeforeAndAfterOptimization() {
    List<SyntheticPost> posts = buildSyntheticPosts(DATASET_SIZE);

    long beforeLatencyMs = benchmarkNaivePopularRetrieval(posts, TOP_LIMIT);
    long afterLatencyMs = benchmarkIndexedPopularRetrieval(posts, TOP_LIMIT);

    System.out.printf("popular-retrieval before=%dms after=%dms improvement=%.2f%%%n",
        beforeLatencyMs,
        afterLatencyMs,
        percentageImprovement(beforeLatencyMs, afterLatencyMs));

    assertTrue(afterLatencyMs < beforeLatencyMs,
        "Indexed retrieval should be faster than naive sorting/search retrieval");
  }

  @Test
  void benchmarkMethodMatchingLinearSearchVsIndexedLookup() {
    List<String> metricNames = buildMethodNames(15_000);
    List<String> lookupKeys = buildLookupKeys(metricNames);

    long beforeLatencyMs = benchmarkLinearMethodMatching(metricNames, lookupKeys);
    long afterLatencyMs = benchmarkIndexedMethodMatching(metricNames, lookupKeys);

    System.out.printf("method-matching before=%dms after=%dms improvement=%.2f%%%n",
        beforeLatencyMs,
        afterLatencyMs,
        percentageImprovement(beforeLatencyMs, afterLatencyMs));

    assertTrue(afterLatencyMs < beforeLatencyMs,
        "Indexed method lookup should be faster than linear scanning");
  }

  private long benchmarkNaivePopularRetrieval(List<SyntheticPost> posts, int topLimit) {
    long startedAt = System.nanoTime();

    for (int run = 0; run < ITERATIONS; run++) {
      List<SyntheticPost> sorted = new ArrayList<>(posts);
      sorted.sort(Comparator.comparingLong(SyntheticPost::commentsCount).reversed());

      List<Long> topIds = sorted.stream().limit(topLimit).map(SyntheticPost::id).toList();
      for (Long topId : topIds) {
        findByIdLinear(posts, topId);
      }
    }

    return toMilliseconds(System.nanoTime() - startedAt);
  }

  private long benchmarkIndexedPopularRetrieval(List<SyntheticPost> posts, int topLimit) {
    long startedAt = System.nanoTime();

    Map<Long, SyntheticPost> byId = new HashMap<>(posts.size());
    NavigableSet<SyntheticPost> ranking = new TreeSet<>(
        Comparator.comparingLong(SyntheticPost::commentsCount)
            .reversed()
            .thenComparing(SyntheticPost::id, Comparator.reverseOrder()));

    for (SyntheticPost post : posts) {
      byId.put(post.id(), post);
      ranking.add(post);
    }

    for (int run = 0; run < ITERATIONS; run++) {
      int collected = 0;
      for (SyntheticPost post : ranking) {
        byId.get(post.id());
        collected++;
        if (collected == topLimit) {
          break;
        }
      }
    }

    return toMilliseconds(System.nanoTime() - startedAt);
  }

  private long benchmarkLinearMethodMatching(List<String> metricNames, List<String> lookupKeys) {
    long startedAt = System.nanoTime();

    for (int run = 0; run < ITERATIONS; run++) {
      for (String lookupKey : lookupKeys) {
        findByPartialLinear(metricNames, lookupKey);
      }
    }

    return toMilliseconds(System.nanoTime() - startedAt);
  }

  private long benchmarkIndexedMethodMatching(List<String> metricNames, List<String> lookupKeys) {
    long startedAt = System.nanoTime();

    Map<String, String> lookup = new HashMap<>(metricNames.size() * 2);
    for (String metricName : metricNames) {
      lookup.putIfAbsent(normalize(metricName), metricName);
      lookup.putIfAbsent(normalize(simplify(metricName)), metricName);
    }

    for (int run = 0; run < ITERATIONS; run++) {
      for (String lookupKey : lookupKeys) {
        lookup.get(normalize(lookupKey));
        lookup.get(normalize(simplify(lookupKey)));
      }
    }

    return toMilliseconds(System.nanoTime() - startedAt);
  }

  private SyntheticPost findByIdLinear(List<SyntheticPost> posts, Long id) {
    for (SyntheticPost post : posts) {
      if (post.id().equals(id)) {
        return post;
      }
    }
    return null;
  }

  private String findByPartialLinear(List<String> metricNames, String key) {
    for (String metricName : metricNames) {
      if (metricName.contains(key) || key.contains(metricName)) {
        return metricName;
      }
    }
    return null;
  }

  private List<SyntheticPost> buildSyntheticPosts(int size) {
    Random random = new Random(42);
    List<SyntheticPost> posts = new ArrayList<>(size);

    for (int index = 1; index <= size; index++) {
      long commentsCount = random.nextInt(500);
      LocalDateTime updatedAt = LocalDateTime.now().minusHours(random.nextInt(96));
      posts.add(new SyntheticPost((long) index, commentsCount, updatedAt));
    }

    return posts;
  }

  private List<String> buildMethodNames(int size) {
    List<String> names = new ArrayList<>(size);
    for (int index = 0; index < size; index++) {
      names.add("org.amalitech.bloggingplatformspring.services.Service" + (index % 200)
          + ".getMethod" + index + "(Long)");
    }
    return names;
  }

  private List<String> buildLookupKeys(List<String> names) {
    List<String> keys = new ArrayList<>();
    for (int index = 0; index < names.size(); index += 50) {
      keys.add(simplify(names.get(index)));
    }
    return keys;
  }

  private String normalize(String value) {
    return value.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
  }

  private String simplify(String methodName) {
    String withoutParams = methodName.contains("(")
        ? methodName.substring(0, methodName.indexOf('('))
        : methodName;
    int lastDot = withoutParams.lastIndexOf('.');
    return lastDot >= 0 ? withoutParams.substring(lastDot + 1) : withoutParams;
  }

  private double percentageImprovement(long beforeMs, long afterMs) {
    if (beforeMs <= 0) {
      return 0.0;
    }
    return ((double) (beforeMs - afterMs) / beforeMs) * 100;
  }

  private long toMilliseconds(long nanoseconds) {
    return nanoseconds / 1_000_000;
  }

  private record SyntheticPost(Long id, long commentsCount, LocalDateTime updatedAt) {
  }
}
