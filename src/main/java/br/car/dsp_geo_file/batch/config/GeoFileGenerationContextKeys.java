package br.car.dsp_geo_file.batch.config;

/**
 * Job execution context keys shared between the readiness check and the decider.
 */
public final class GeoFileGenerationContextKeys {

    public static final String STORAGE_READY = "storageReady";

    public static final String PUBLISH_CONFIG_FAILURES = "publishConfigFailures";
    public static final String PUBLISH_TRANSIENT_FAILURES = "publishTransientFailures";
    public static final String TERRITORIES_WITH_FAILURES = "territoriesWithFailures";

    private GeoFileGenerationContextKeys() {
    }
}
