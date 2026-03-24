package com.datastd.ingestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Map;

public class JsonDataRequest {

    @NotBlank(message = "Dataset name is required")
    private String name;

    @NotEmpty(message = "Records list cannot be empty")
    private List<Map<String, Object>> records;

    public JsonDataRequest() {}

    public JsonDataRequest(String name, List<Map<String, Object>> records) {
        this.name = name;
        this.records = records;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Map<String, Object>> getRecords() {
        return records;
    }

    public void setRecords(List<Map<String, Object>> records) {
        this.records = records;
    }
}

