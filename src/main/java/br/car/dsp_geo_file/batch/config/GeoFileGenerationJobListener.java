package br.car.dsp_geo_file.batch.config;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

/**
 * Sets the job {@link org.springframework.batch.core.ExitStatus} from storage readiness and
 * publish counters so {@code BATCH_JOB_EXECUTION.EXIT_CODE} reflects business outcome.
 */
@Component
public class GeoFileGenerationJobListener implements JobExecutionListener {

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.FAILED) {
            return;
        }
        jobExecution.setExitStatus(GeoFileGenerationExitStatusResolver.forJob(jobExecution));
    }
}
