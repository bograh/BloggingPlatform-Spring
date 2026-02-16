package org.amalitech.bloggingplatformspring.repository;

import org.amalitech.bloggingplatformspring.entity.CacheMetricsSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * MongoDB Repository for CacheMetricsSnapshot entities.
 */
@Repository
public interface CacheMetricsRepository extends MongoRepository<CacheMetricsSnapshot, String> {

  /**
   * Find all snapshots by type
   */
  List<CacheMetricsSnapshot> findBySnapshotType(String snapshotType);

  /**
   * Find snapshots within a time range
   */
  List<CacheMetricsSnapshot> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

  /**
   * Find latest snapshot
   */
  Optional<CacheMetricsSnapshot> findTopByOrderByTimestampDesc();

  /**
   * Find latest snapshot by type
   */
  Optional<CacheMetricsSnapshot> findTopBySnapshotTypeOrderByTimestampDesc(String snapshotType);

  /**
   * Find snapshots by type with pagination
   */
  Page<CacheMetricsSnapshot> findBySnapshotTypeOrderByTimestampDesc(String snapshotType, Pageable pageable);

  /**
   * Find all snapshots ordered by timestamp
   */
  Page<CacheMetricsSnapshot> findAllByOrderByTimestampDesc(Pageable pageable);

  /**
   * Count snapshots by type
   */
  long countBySnapshotType(String snapshotType);

  /**
   * Delete old snapshots before a given date
   */
  void deleteByTimestampBefore(LocalDateTime timestamp);
}
