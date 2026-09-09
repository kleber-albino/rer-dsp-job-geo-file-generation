package br.car.dsp_geo_file.export;

import java.time.Instant;

/**
 * The result of one export. {@code featureCount} zero means the cut is empty and no object
 * should exist for it; {@code lastUpdate} is the newest feature timestamp, published as
 * user-metadata so the search endpoint can answer without the WFS.
 */
public record GeneratedGeoFile(
        byte[] content,
        long featureCount,
        Instant lastUpdate
) {

    public static GeneratedGeoFile empty() {
        return new GeneratedGeoFile(new byte[0], 0L, null);
    }

    public boolean isEmpty() {
        return featureCount == 0L;
    }
}
