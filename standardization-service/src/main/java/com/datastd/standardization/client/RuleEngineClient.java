package com.datastd.standardization.client;

import com.datastd.common.dto.RuleResponse;
import com.datastd.common.dto.RuleSetResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "rule-engine-service")
public interface RuleEngineClient {

    @PostMapping("/api/rules/by-ids")
    List<RuleResponse> getRulesByIds(@RequestBody List<UUID> ids);

    @GetMapping("/api/rules/rulesets/{id}")
    RuleSetResponse getRuleSetById(@PathVariable("id") UUID id);
}

