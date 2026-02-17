package org.amalitech.bloggingplatformspring.repository;

import org.amalitech.bloggingplatformspring.entity.PerformanceMetricsSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * MongoDB Repository for PerformanceMetricsSnapshot entities.
 */
@Repository
public interface PerformanceMetricsRepository extends MongoRepository<PerformanceMetricsSnapshot, String> {

  /**
   * Find all snapshots by type
   */
  List<PerformanceMetricsSnapshot> findBySnapshotType(String snapshotType);

  /**
   * Find snapshots within a time range
   */
  List<PerformanceMetricsSnapshot> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

  /**
   * Find latest snapshot
   */
  Optional<PerformanceMetricsSnapshot> findTopByOrderByTimestampDesc();

  /**
   * Find latest snapshot by type
   */
  Optional<PerformanceMetricsSnapshot> findTopBySnapshotTypeOrderByTimestampDesc(String snapshotType);

  /**
   * Find snapshots by type with pagination
   */
  Page<PerformanceMetricsSnapshot> findBySnapshotTypeOrderByTimestampDesc(String snapshotType, Pageable pageable);

  /**
   * Find all snapshots ordered by timestamp
   */
  Page<PerformanceMetricsSnapshot> findAllByOrderByTimestampDesc(Pageable pageable);

  /**
   * Count snapshots by type
   */
  long countBySnapshotType(String snapshotType);

  /**
   * Delete old snapshots before a given date
   */
  void deleteByTimestampBefore(LocalDateTime timestamp);
}
