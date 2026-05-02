package com.resumeai.export.repository;

import com.resumeai.export.entity.ExportJobRecord;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExportJobRepository extends JpaRepository<ExportJobRecord, UUID> {

    List<ExportJobRecord> findByUserIdOrderByRequestedAtDesc(Long userId);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, String status);
}
