package com.datastd.standardization.repository;

import com.datastd.standardization.entity.ProcessingJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProcessingJobRepository extends JpaRepository<ProcessingJob, UUID> {
    List<ProcessingJob> findByDatasetId(UUID datasetId);
    List<ProcessingJob> findByStatus(ProcessingJob.JobStatus status);
    List<ProcessingJob> findAllByOrderByCreatedAtDesc();
}

