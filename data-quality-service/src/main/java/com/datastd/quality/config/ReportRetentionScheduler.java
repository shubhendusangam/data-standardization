package com.datastd.quality.config;

import com.datastd.quality.repository.QualityReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Nightly cleanup of old quality reports.
 * Runs at 3 AM daily. Retention period is configurable via application.yml.
 */
@Component
public class ReportRetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReportRetentionScheduler.class);

    private final QualityReportRepository reportRepository;
    private final int retentionDays;

    public ReportRetentionScheduler(QualityReportRepository reportRepository,
                                    @Value("${quality.report.retention-days:90}") int retentionDays) {
        this.reportRepository = reportRepository;
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldReports() {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        log.info("Running report retention cleanup: deleting reports older than {} days (before {})",
                retentionDays, cutoff);
        int deleted = reportRepository.deleteByEvaluatedAtBefore(cutoff);
        log.info("Report retention cleanup complete: deleted {} old reports", deleted);
    }
}

