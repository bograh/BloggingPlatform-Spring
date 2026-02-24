package org.amalitech.bloggingplatformspring.repository;

import org.amalitech.bloggingplatformspring.entity.ModerationTask;
import org.amalitech.bloggingplatformspring.enums.ModerationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ModerationTaskRepository extends JpaRepository<ModerationTask, UUID> {

  List<ModerationTask> findByModeratorIdOrderByCreatedAtDesc(UUID moderatorId, Pageable pageable);

  List<ModerationTask> findByStatusOrderByCreatedAtAsc(ModerationStatus status, Pageable pageable);

  long countByModeratorIdAndStatus(UUID moderatorId, ModerationStatus status);
}
