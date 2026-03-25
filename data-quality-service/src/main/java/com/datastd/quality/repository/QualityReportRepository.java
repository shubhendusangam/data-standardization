package com.datastd.quality.repository;

import com.datastd.quality.entity.QualityReportEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QualityReportRepository extends JpaRepository<QualityReportEntity, UUID> {

    Optional<QualityReportEntity> findTopByDatasetIdOrderByEvaluatedAtDesc(UUID datasetId);

    List<QualityReportEntity> findByDatasetIdOrderByEvaluatedAtDesc(UUID datasetId);

    List<QualityReportEntity> findByOverallStatusOrderByEvaluatedAtDesc(String overallStatus);

    List<QualityReportEntity> findAllByOrderByEvaluatedAtDesc();

    // ── Paginated history ────────────────────────────────────────────
    Page<QualityReportEntity> findByDatasetIdOrderByEvaluatedAtDesc(UUID datasetId, Pageable pageable);

    // ── Trend: reports for a dataset within a time window ────────────
    List<QualityReportEntity> findByDatasetIdAndEvaluatedAtAfterOrderByEvaluatedAtAsc(UUID datasetId, Instant after);

    // ── Distinct dataset IDs that have reports ───────────────────────
    @Query("SELECT DISTINCT r.datasetId FROM QualityReportEntity r")
    List<UUID> findDistinctDatasetIds();

    // ── Retention: delete old reports ────────────────────────────────
    @Modifying
    @Query("DELETE FROM QualityReportEntity r WHERE r.evaluatedAt < :cutoff")
    int deleteByEvaluatedAtBefore(@Param("cutoff") Instant cutoff);
}
