package com.datastd.standardization.client;

import com.datastd.common.dto.QualityReport;
import com.datastd.standardization.dto.QualityValidateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "data-quality-service")
public interface DataQualityClient {

    @PostMapping("/api/quality/validate")
    QualityReport validate(@RequestBody QualityValidateRequest request);

    @GetMapping("/api/quality/reports/{datasetId}")
    QualityReport getLatestReport(@PathVariable("datasetId") UUID datasetId);
}

