package com.datastd.quality.repository;

import com.datastd.quality.entity.ValidationRule;
import com.datastd.quality.entity.ValidationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ValidationRuleRepository extends JpaRepository<ValidationRule, UUID> {

    List<ValidationRule> findByColumnName(String columnName);

    List<ValidationRule> findByValidationType(ValidationType validationType);

    List<ValidationRule> findByActive(boolean active);

    List<ValidationRule> findByIdIn(List<UUID> ids);
}

