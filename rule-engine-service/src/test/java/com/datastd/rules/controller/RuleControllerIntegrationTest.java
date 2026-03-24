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
    void getRuleById_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/rules/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
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
}

