package com.example.project.domain.dispatch.service.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class DispatchSchedulerService {

    private final DispatchOrchestratorService dispatchOrchestratorService;

    @Value("${dispatch.scheduler.enabled:true}")
    private boolean schedulerEnabled;

    @Scheduled(cron = "${dispatch.wave.sync-cron:0/15 * * * * *}")
    public void syncExpiredWaitingJobs() {
        if (!schedulerEnabled) {
            return;
        }
        try {
            dispatchOrchestratorService.processExpiredWaitingJobs();
        } catch (Exception e) {
            log.warn("dispatch scheduler sync failed: {}", e.getMessage());
        }
    }
}
