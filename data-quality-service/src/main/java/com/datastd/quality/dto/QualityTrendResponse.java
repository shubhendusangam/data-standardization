package com.datastd.quality.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Quality trend data for a dataset over a time window.
 */
public class QualityTrendResponse {

    private UUID datasetId;
    private List<TrendPoint> trendPoints;
    private double avgScore;
    private double minScore;
    private String trend; // IMPROVING, STABLE, DEGRADING

    public QualityTrendResponse() {}

    public static class TrendPoint {
        private Instant date;
        private int qualityScore;
        private String overallStatus;
        private int totalRecords;

        public TrendPoint() {}

        public TrendPoint(Instant date, int qualityScore, String overallStatus, int totalRecords) {
            this.date = date;
            this.qualityScore = qualityScore;
            this.overallStatus = overallStatus;
            this.totalRecords = totalRecords;
        }

        public Instant getDate() { return date; }
        public void setDate(Instant date) { this.date = date; }
        public int getQualityScore() { return qualityScore; }
        public void setQualityScore(int qualityScore) { this.qualityScore = qualityScore; }
        public String getOverallStatus() { return overallStatus; }
        public void setOverallStatus(String overallStatus) { this.overallStatus = overallStatus; }
        public int getTotalRecords() { return totalRecords; }
        public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }
    }

    public UUID getDatasetId() { return datasetId; }
    public void setDatasetId(UUID datasetId) { this.datasetId = datasetId; }
    public List<TrendPoint> getTrendPoints() { return trendPoints; }
    public void setTrendPoints(List<TrendPoint> trendPoints) { this.trendPoints = trendPoints; }
    public double getAvgScore() { return avgScore; }
    public void setAvgScore(double avgScore) { this.avgScore = avgScore; }
    public double getMinScore() { return minScore; }
    public void setMinScore(double minScore) { this.minScore = minScore; }
    public String getTrend() { return trend; }
    public void setTrend(String trend) { this.trend = trend; }
}

