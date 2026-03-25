package com.datastd.quality.controller;

import com.datastd.quality.dto.ValidationRuleRequest;
import com.datastd.quality.entity.Severity;
import com.datastd.quality.entity.ValidationType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.annotation.DirtiesContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class QualityControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("CRUD: create, list, toggle, delete a validation rule")
    void testRuleCrud() throws Exception {
        ValidationRuleRequest request = new ValidationRuleRequest();
        request.setName("email-not-null");
        request.setColumnName("email");
        request.setValidationType(ValidationType.NOT_NULL);
        request.setSeverity(Severity.ERROR);
        request.setParams("{\"maxNullRatePct\": 5.0}");
        request.setActive(true);

        // Create
        String createResponse = mockMvc.perform(post("/api/quality/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("email-not-null"))
                .andExpect(jsonPath("$.validationType").value("NOT_NULL"))
                .andExpect(jsonPath("$.severity").value("ERROR"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn().getResponse().getContentAsString();

        String ruleId = objectMapper.readTree(createResponse).get("id").asText();

        // List (8 seeded templates + 1 newly created)
        mockMvc.perform(get("/api/quality/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(9));

        // Toggle
        mockMvc.perform(patch("/api/quality/rules/" + ruleId + "/toggle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // Delete
        mockMvc.perform(delete("/api/quality/rules/" + ruleId))
                .andExpect(status().isNoContent());

        // Verify deleted (8 seeded templates remain)
        mockMvc.perform(get("/api/quality/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8));
    }

    @Test
    @DisplayName("Create and retrieve a rule set")
    void testRuleSetCrud() throws Exception {
        // Create a rule first
        ValidationRuleRequest ruleReq = new ValidationRuleRequest();
        ruleReq.setName("name-not-empty");
        ruleReq.setColumnName("name");
        ruleReq.setValidationType(ValidationType.NOT_EMPTY);
        ruleReq.setSeverity(Severity.WARNING);
        ruleReq.setParams("{\"maxNullRatePct\": 0.0}");

        String ruleResp = mockMvc.perform(post("/api/quality/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ruleReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String ruleId = objectMapper.readTree(ruleResp).get("id").asText();

        // Create rule set
        String ruleSetBody = String.format(
                "{\"name\": \"basic-checks\", \"description\": \"Basic quality checks\", \"ruleIds\": [\"%s\"]}",
                ruleId);

        String ruleSetResp = mockMvc.perform(post("/api/quality/rulesets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ruleSetBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("basic-checks"))
                .andExpect(jsonPath("$.rules.length()").value(1))
                .andReturn().getResponse().getContentAsString();

        String ruleSetId = objectMapper.readTree(ruleSetResp).get("id").asText();

        // Get by ID
        mockMvc.perform(get("/api/quality/rulesets/" + ruleSetId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("basic-checks"))
                .andExpect(jsonPath("$.rules.length()").value(1));
    }

    @Test
    @DisplayName("Validation returns 400 when required fields missing")
    void testValidationMissingFields() throws Exception {
        String body = "{\"columnName\": \"email\"}"; // missing name, validationType, severity

        mockMvc.perform(post("/api/quality/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").exists());
    }
}

