package br.car.dsp_geo_file.batch.tasklet;

import br.car.dsp_geo_file.batch.config.GeoFileGenerationContextKeys;
import br.car.dsp_geo_file.storage.ObjectStorageClient;
import br.car.dsp_geo_file.storage.ObjectStorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

/**
 * Checks the bucket before anything is generated.
 *
 * <p>An absent or unreachable bucket is reported and the run ends without work — it must not
 * fail the container, because the job shares its Compose lifecycle with the rest of the stack
 * and the flags stay on, so the next cycle is a retry rather than lost data.
 */
@Slf4j
public class ObjectStorageReadinessTasklet implements Tasklet {

    private final ObjectStorageClient objectStorageClient;
    private final String bucket;

    public ObjectStorageReadinessTasklet(ObjectStorageClient objectStorageClient, String bucket) {
        this.objectStorageClient = objectStorageClient;
        this.bucket = bucket;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        boolean ready;
        try {
            ready = objectStorageClient.bucketExists();
            if (!ready) {
                log.error("Bucket '{}' does not exist — no file will be published. "
                        + "Create the bucket; the territorial flags stay on for the next run.", bucket);
            }
        } catch (ObjectStorageException ex) {
            ready = false;
            log.error("Object storage unreachable — no file will be published: {}", ex.getMessage(), ex);
        }

        chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext()
                .put(GeoFileGenerationContextKeys.STORAGE_READY, ready);

        if (ready) {
            log.info("Bucket '{}' is reachable", bucket);
        }
        return RepeatStatus.FINISHED;
    }
}
