package com.datastd.quality.engine;

import com.datastd.quality.dto.QualityTrendResponse;
import com.datastd.quality.entity.QualityReportEntity;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Computes quality trend direction using simple linear regression on qualityScore over time.
 * slope > +2/week → IMPROVING, slope < -2/week → DEGRADING, else STABLE.
 */
@Component
public class TrendEngine {

    /**
     * Compute trend direction from a list of reports ordered by evaluatedAt ASC.
     */
    public String computeTrend(List<QualityReportEntity> reports) {
        if (reports.size() < 2) {
            return "UNKNOWN";
        }

        // Simple linear regression: score(y) vs time-index(x)
        // x = index (0, 1, 2, ...), y = qualityScore
        int n = reports.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < n; i++) {
            double x = i;
            double y = reports.get(i).getQualityScore();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        double denominator = n * sumX2 - sumX * sumX;
        if (denominator == 0) {
            return "STABLE";
        }

        double slope = (n * sumXY - sumX * sumY) / denominator;

        // Normalize slope: if reports span days, convert slope per index to slope per week
        // Approximate: assume average interval between reports
        long firstMs = reports.get(0).getEvaluatedAt().toEpochMilli();
        long lastMs = reports.get(n - 1).getEvaluatedAt().toEpochMilli();
        double spanWeeks = (double) (lastMs - firstMs) / (7.0 * 24 * 60 * 60 * 1000);

        double slopePerWeek;
        if (spanWeeks > 0 && n > 1) {
            // slope is per-index, there are (n-1) intervals over spanWeeks
            slopePerWeek = slope * (n - 1) / spanWeeks;
        } else {
            slopePerWeek = slope;
        }

        if (slopePerWeek > 2) return "IMPROVING";
        if (slopePerWeek < -2) return "DEGRADING";
        return "STABLE";
    }
}

