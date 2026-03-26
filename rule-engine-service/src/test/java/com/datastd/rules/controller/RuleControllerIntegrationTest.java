package com.datastd.rules.controller;

import com.datastd.rules.entity.StandardizationRule.RuleType;
import com.datastd.rules.repository.RuleSetRepository;
import com.datastd.rules.repository.StandardizationRuleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RuleControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StandardizationRuleRepository ruleRepository;

    @Autowired
    private RuleSetRepository ruleSetRepository;

    @BeforeEach
    void setUp() {
        ruleSetRepository.deleteAll();
        ruleRepository.deleteAll();
    }

    // ─── POST /api/rules ──────────────────────────────────────────

    @Test
    void createRule_shouldReturn201WithRule() throws Exception {
        String body = ruleJson("Trim Name", "name", "TRIM", 1);

        mockMvc.perform(post("/api/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Trim Name"))
                .andExpect(jsonPath("$.ruleType").value("TRIM"))
                .andExpect(jsonPath("$.fieldName").value("name"))
                .andExpect(jsonPath("$.priority").value(1))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void createRule_missingName_shouldReturn400() throws Exception {
        String body = """
                {"fieldName":"x","ruleType":"TRIM"}
                """;

        mockMvc.perform(post("/api/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ─── GET /api/rules ───────────────────────────────────────────

    @Test
    void getAllRules_shouldReturnCreatedRules() throws Exception {
        createSampleRule("R1", "name", "TRIM", 1);
        createSampleRule("R2", "email", "LOWERCASE", 2);

        mockMvc.perform(get("/api/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getAllRules_filterByFieldName() throws Exception {
        createSampleRule("R1", "name", "TRIM", 1);
        createSampleRule("R2", "email", "LOWERCASE", 2);

        mockMvc.perform(get("/api/rules").param("fieldName", "email"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fieldName").value("email"));
    }

    // ─── GET /api/rules/{id} ──────────────────────────────────────

    @Test
    void getRuleById_shouldReturnRule() throws Exception {
        UUID id = createSampleRule("Lookup", "name", "UPPERCASE", 0);

        mockMvc.perform(get("/api/rules/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Lookup"));
    }

    @Test
    void getRuleById_notFound_shouldReturn404WithErrorResponse() throws Exception {
        mockMvc.perform(get("/api/rules/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RULE_NOT_FOUND"))
                .andExpect(jsonPath("$.httpStatus").value(404))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    // ─── PUT /api/rules/{id} ──────────────────────────────────────

    @Test
    void updateRule_shouldModifyFields() throws Exception {
        UUID id = createSampleRule("OldName", "name", "TRIM", 1);
        String updatedBody = ruleJson("NewName", "name", "UPPERCASE", 2);

        mockMvc.perform(put("/api/rules/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("NewName"))
                .andExpect(jsonPath("$.ruleType").value("UPPERCASE"))
                .andExpect(jsonPath("$.priority").value(2));
    }

    // ─── DELETE /api/rules/{id} ───────────────────────────────────

    @Test
    void deleteRule_shouldReturn204() throws Exception {
        UUID id = createSampleRule("DeleteMe", "name", "TRIM", 0);

        mockMvc.perform(delete("/api/rules/" + id))
                .andExpect(status().isNoContent());

        assertThat(ruleRepository.findById(id)).isEmpty();
    }

    // ─── PATCH /api/rules/{id}/toggle ─────────────────────────────

    @Test
    void toggleRule_shouldFlipActive() throws Exception {
        UUID id = createSampleRule("Toggle", "name", "TRIM", 0);

        // Initially active=true, toggle to false
        mockMvc.perform(patch("/api/rules/" + id + "/toggle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // Toggle back to true
        mockMvc.perform(patch("/api/rules/" + id + "/toggle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    // ─── POST /api/rules/by-ids ───────────────────────────────────

    @Test
    void getRulesByIds_shouldReturnMatchingRules() throws Exception {
        UUID id1 = createSampleRule("R1", "name", "TRIM", 1);
        UUID id2 = createSampleRule("R2", "email", "LOWERCASE", 2);
        createSampleRule("R3", "city", "UPPERCASE", 3); // not requested

        String body = objectMapper.writeValueAsString(List.of(id1, id2));

        mockMvc.perform(post("/api/rules/by-ids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // ─── Rule Sets ────────────────────────────────────────────────

    @Test
    void createAndGetRuleSet_fullLifecycle() throws Exception {
        UUID rId1 = createSampleRule("R1", "name", "TRIM", 1);
        UUID rId2 = createSampleRule("R2", "email", "LOWERCASE", 2);

        // Create rule set
        String rsBody = objectMapper.writeValueAsString(Map.of(
                "name", "My Rule Set",
                "description", "Test set",
                "ruleIds", List.of(rId1, rId2)
        ));

        MvcResult createResult = mockMvc.perform(post("/api/rules/rulesets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rsBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("My Rule Set"))
                .andExpect(jsonPath("$.rules", hasSize(2)))
                .andReturn();

        String rsId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        // Get rule set by ID
        mockMvc.perform(get("/api/rules/rulesets/" + rsId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("My Rule Set"))
                .andExpect(jsonPath("$.rules", hasSize(2)));

        // List all rule sets
        mockMvc.perform(get("/api/rules/rulesets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    // ─── Helpers ──────────────────────────────────────────────────

    private String ruleJson(String name, String fieldName, String ruleType, int priority) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "name", name,
                "fieldName", fieldName,
                "ruleType", ruleType,
                "priority", priority,
                "active", true
        ));
    }

    private String ruleJsonWithConfig(String name, String fieldName, String ruleType,
                                      int priority, String ruleConfig) throws Exception {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("name", name);
        map.put("fieldName", fieldName);
        map.put("ruleType", ruleType);
        map.put("priority", priority);
        map.put("active", true);
        if (ruleConfig != null) {
            map.put("ruleConfig", ruleConfig);
        }
        return objectMapper.writeValueAsString(map);
    }

    private UUID createSampleRule(String name, String fieldName, String ruleType, int priority) throws Exception {
        String body = ruleJson(name, fieldName, ruleType, priority);
        MvcResult result = mockMvc.perform(post("/api/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(node.get("id").asText());
    }

    // ─── Rule Config Validation ───────────────────────────────────

    @Test
    void createRule_replaceWithBadConfig_shouldReturn400WithErrorCode() throws Exception {
        // Typo: "fin" instead of "find"
        String body = ruleJsonWithConfig("Bad Replace", "name", "REPLACE", 1,
                "{\"fin\":\"old\",\"replace\":\"new\"}");

        mockMvc.perform(post("/api/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_RULE_CONFIG"))
                .andExpect(jsonPath("$.message").value(containsString("find")))
                .andExpect(jsonPath("$.httpStatus").value(400));
    }

    @Test
    void createRule_replaceWithValidConfig_shouldReturn201() throws Exception {
        String body = ruleJsonWithConfig("Good Replace", "name", "REPLACE", 1,
                "{\"find\":\"old\",\"replace\":\"new\"}");

        mockMvc.perform(post("/api/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Good Replace"));
    }

    @Test
    void updateRule_withBadConfig_shouldReturn400() throws Exception {
        UUID id = createSampleRule("Original", "name", "TRIM", 1);

        // Update to REPLACE with missing "find"
        String body = ruleJsonWithConfig("Updated", "name", "REPLACE", 1,
                "{\"replace\":\"new\"}");

        mockMvc.perform(put("/api/rules/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_RULE_CONFIG"));
    }

    @Test
    void createRule_trimWithEmptyConfig_shouldReturn201() throws Exception {
        String body = ruleJsonWithConfig("Trim Rule", "name", "TRIM", 1, "{}");

        mockMvc.perform(post("/api/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void createRule_mapValuesWithArrayMappings_shouldReturn400() throws Exception {
        String body = ruleJsonWithConfig("Bad MapValues", "status", "MAP_VALUES", 1,
                "{\"mappings\":[\"M\",\"Male\"]}");

        mockMvc.perform(post("/api/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_RULE_CONFIG"))
                .andExpect(jsonPath("$.message").value(containsString("must be a JSON object")));
    }

    @Test
    void createRule_defaultValueValid_shouldReturn201() throws Exception {
        String body = ruleJsonWithConfig("Default Rule", "city", "DEFAULT_VALUE", 1,
                "{\"defaultValue\":\"N/A\"}");

        mockMvc.perform(post("/api/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void createRule_dateFormatValid_shouldReturn201() throws Exception {
        String body = ruleJsonWithConfig("Date Rule", "date", "DATE_FORMAT", 1,
                "{\"sourceFormat\":\"MM/dd/yyyy\",\"targetFormat\":\"yyyy-MM-dd\"}");

        mockMvc.perform(post("/api/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void createRule_errorResponseContainsTraceIdField() throws Exception {
        String body = ruleJsonWithConfig("Bad Replace", "name", "REPLACE", 1,
                "{\"fin\":\"old\",\"replace\":\"new\"}");

        mockMvc.perform(post("/api/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(jsonPath("$.errorCode").value("INVALID_RULE_CONFIG"))
                .andExpect(jsonPath("$.httpStatus").value(400))
                .andExpect(jsonPath("$.path").value("/api/rules"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }
}

