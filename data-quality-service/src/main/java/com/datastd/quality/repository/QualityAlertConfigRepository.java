package com.datastd.quality.repository;

import com.datastd.quality.entity.QualityAlertConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QualityAlertConfigRepository extends JpaRepository<QualityAlertConfig, UUID> {

    List<QualityAlertConfig> findByActive(boolean active);
}

