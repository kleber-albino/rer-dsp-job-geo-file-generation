package br.car.dsp_geo_file.batch.config;

/**
 * Job execution context keys shared between the readiness check and the decider.
 */
public final class GeoFileGenerationContextKeys {

    public static final String STORAGE_READY = "storageReady";

    private GeoFileGenerationContextKeys() {
    }
}
