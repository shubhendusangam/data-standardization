package com.datastd.ingestion.repository;

import com.datastd.ingestion.entity.IngestedDataset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IngestedDatasetRepository extends JpaRepository<IngestedDataset, UUID> {
    List<IngestedDataset> findAllByOrderByCreatedAtDesc();
    List<IngestedDataset> findBySourceType(IngestedDataset.SourceType sourceType);
    List<IngestedDataset> findByStatus(IngestedDataset.DatasetStatus status);
}

