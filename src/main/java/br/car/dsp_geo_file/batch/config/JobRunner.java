package br.car.dsp_geo_file.batch.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Launches the geo file generation job on startup.
 */
@Slf4j
@Component
public class JobRunner implements CommandLineRunner {

    private final JobLauncher jobLauncher;
    private final Job geoFileGenerationJob;

    @Value("${execution-jobs.geo-file-generation-job:true}")
    private boolean runGeoFileGeneration;

    public JobRunner(JobLauncher jobLauncher,
                     @Qualifier(GeoFileGenerationJobConfig.JOB_NAME) Job geoFileGenerationJob) {
        this.jobLauncher = jobLauncher;
        this.geoFileGenerationJob = geoFileGenerationJob;
    }

    @Override
    public void run(String... args) throws Exception {
        if (!runGeoFileGeneration) {
            log.info("Job {} is disabled", geoFileGenerationJob.getName());
            return;
        }

        log.info("Starting job {}", geoFileGenerationJob.getName());
        long startedAt = System.currentTimeMillis();
        JobExecution execution = jobLauncher.run(geoFileGenerationJob, new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters());
        long durationMs = System.currentTimeMillis() - startedAt;

        log.info("Job {} finished with status={} exitCode={} in {} ms",
                geoFileGenerationJob.getName(),
                execution.getStatus(),
                execution.getExitStatus().getExitCode(),
                durationMs);
        for (Throwable failure : execution.getAllFailureExceptions()) {
            log.error("Job {} failure: {}",
                    geoFileGenerationJob.getName(), failure.getMessage(), failure);
        }
        for (StepExecution step : execution.getStepExecutions()) {
            log.info("  Step {}: read={} write={} commit={}",
                    step.getStepName(),
                    step.getReadCount(),
                    step.getWriteCount(),
                    step.getCommitCount());
        }
        logPublishSummary(execution);
    }

    private void logPublishSummary(JobExecution execution) {
        StepExecution generationStep = execution.getStepExecutions().stream()
                .filter(step -> GeoFileGenerationJobConfig.GEO_FILE_GENERATION_STEP.equals(step.getStepName()))
                .findFirst()
                .orElse(null);
        if (generationStep == null) {
            return;
        }

        ExecutionContext context = generationStep.getExecutionContext();
        int configFailures = context.getInt(GeoFileGenerationContextKeys.PUBLISH_CONFIG_FAILURES, 0);
        int transientFailures = context.getInt(GeoFileGenerationContextKeys.PUBLISH_TRANSIENT_FAILURES, 0);
        int territoriesWithFailures = context.getInt(GeoFileGenerationContextKeys.TERRITORIES_WITH_FAILURES, 0);
        int totalFailures = configFailures + transientFailures;

        if (totalFailures > 0) {
            log.error("[GEO_PUBLISH_SUMMARY] territoriesWithFailures={} configFailures={} transientFailures={}",
                    territoriesWithFailures, configFailures, transientFailures);
        } else {
            log.info("[GEO_PUBLISH_SUMMARY] no publish failures");
        }
    }
}
