package com.datastd.quality.repository;

import com.datastd.quality.entity.ValidationRuleSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ValidationRuleSetRepository extends JpaRepository<ValidationRuleSet, UUID> {
}

