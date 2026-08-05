package org.amalitech.bloggingplatformspring.repository;

import org.amalitech.bloggingplatformspring.entity.ReportExport;
import org.amalitech.bloggingplatformspring.enums.ReportType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReportExportRepository extends JpaRepository<ReportExport, UUID> {

  List<ReportExport> findByRequestedByOrderByCreatedAtDesc(UUID requestedBy, Pageable pageable);

  List<ReportExport> findByReportTypeAndExportStatus(ReportType reportType, String status);

  @Query("SELECT r FROM ReportExport r WHERE r.exportStatus = 'COMPLETED' AND r.expiresAt < :now")
  List<ReportExport> findExpiredReports(@Param("now") LocalDateTime now);

  @Modifying
  @Query("DELETE FROM ReportExport r WHERE r.exportStatus = 'COMPLETED' AND r.expiresAt < :now")
  int deleteExpiredReports(@Param("now") LocalDateTime now);
}
