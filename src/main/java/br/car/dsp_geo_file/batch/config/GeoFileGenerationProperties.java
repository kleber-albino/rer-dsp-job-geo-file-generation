package br.car.dsp_geo_file.batch.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "geo-file-generation")
public class GeoFileGenerationProperties {

    /**
     * Territories per chunk. One by default: a chunk covers every theme and format of the
     * territory, and committing per territory is what keeps a crash from re-publishing work.
     */
    private int chunkSize = 1;

    /** Orphan collection can be turned off while diagnosing the bucket by hand. */
    private boolean orphanCleanupEnabled = true;

    /**
     * Local directory where files are written before S3 upload.
     * Subfolders mirror the object key.
     */
    private String stagingDir = System.getProperty("java.io.tmpdir") + "/dsp-geo-files";
}
