package br.car.dsp_geo_file.generation;

import br.car.dsp_geo_file.export.GeoFileExportContext;
import br.car.dsp_geo_file.export.GeoFileExporter;
import br.car.dsp_geo_file.export.GeoFileExporterRegistry;
import br.car.dsp_geo_file.export.FeatureTableResolver;
import br.car.dsp_geo_file.export.GeneratedGeoFile;
import br.car.dsp_geo_file.export.TerritoryFeatureFilterBuilder;
import br.car.dsp_geo_file.storage.ObjectStorageClient;
import br.car.dsp_geo_file.storage.S3ObjectKeyBuilder;
import br.car.dsp_geo_file.territory.Territory;
import br.car.dsp_geo_file.theme.DownloadThemeConfig;
import br.car.dsp_geo_file.theme.DownloadThemesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;

/**
 * For one territory, walks the enabled themes and the formats each theme declares, exports and
 * publishes every file.
 *
 * <p>A failure is scoped to the pair (theme, format) so one broken theme does not cost the
 * others their file — but any failure keeps the territory pending, which is what makes the next
 * run a retry instead of a no-op.
 */
@Slf4j
@Service
public class GeoFileGenerationOrchestrator {

    /** User-metadata with the PutObject instant; the backend shows it as last file generate. */
    public static final String GENERATED_AT_METADATA = "generated-at";

    private final DownloadThemesService downloadThemesService;
    private final GeoFileExporterRegistry exporterRegistry;
    private final FeatureTableResolver featureTableResolver;
    private final TerritoryFeatureFilterBuilder filterBuilder;
    private final S3ObjectKeyBuilder keyBuilder;
    private final ObjectStorageClient objectStorageClient;
    private final JdbcTemplate geoTargetJdbcTemplate;

    public GeoFileGenerationOrchestrator(
            DownloadThemesService downloadThemesService,
            GeoFileExporterRegistry exporterRegistry,
            FeatureTableResolver featureTableResolver,
            TerritoryFeatureFilterBuilder filterBuilder,
            S3ObjectKeyBuilder keyBuilder,
            ObjectStorageClient objectStorageClient,
            @Qualifier("geoTargetJdbcTemplate") JdbcTemplate geoTargetJdbcTemplate) {
        this.downloadThemesService = downloadThemesService;
        this.exporterRegistry = exporterRegistry;
        this.featureTableResolver = featureTableResolver;
        this.filterBuilder = filterBuilder;
        this.keyBuilder = keyBuilder;
        this.objectStorageClient = objectStorageClient;
        this.geoTargetJdbcTemplate = geoTargetJdbcTemplate;
    }

    public TerritoryPublishResult publish(Territory territory) {
        int published = 0;
        int emptied = 0;
        int failed = 0;

        for (DownloadThemeConfig theme : downloadThemesService.getEnabledThemes()) {
            if (theme.formats() == null) {
                continue;
            }
            for (String rawFormat : theme.formats()) {
                String format = normalize(rawFormat);
                var exporter = exporterRegistry.find(format);
                if (exporter.isEmpty()) {
                    log.debug("No exporter for format={} (theme={}) — served by WFS", format, theme.code());
                    continue;
                }
                try {
                    if (publishOne(territory, theme, format, exporter.get())) {
                        published++;
                    } else {
                        emptied++;
                    }
                } catch (RuntimeException ex) {
                    failed++;
                    log.error("Failed to publish territory={} theme={} format={}: {}",
                            territory.id(), theme.code(), format, ex.getMessage(), ex);
                }
            }
        }
        return new TerritoryPublishResult(published, emptied, failed);
    }

    /** True when an object was written, false when the cut is empty and the object was removed. */
    private boolean publishOne(Territory territory,
                               DownloadThemeConfig theme,
                               String format,
                               GeoFileExporter exporter) {
        String key = keyBuilder.build(format, territory, theme.code(), exporter.fileExtension());
        GeneratedGeoFile file = exporter.generate(new GeoFileExportContext(
                territory,
                theme,
                featureTableResolver.resolve(theme),
                filterBuilder.build(theme, territory),
                geoTargetJdbcTemplate
        ));

        if (file.isEmpty()) {
            // No features left in the cut: a stale object would keep answering downloads
            // the WFS itself would refuse.
            if (objectStorageClient.head(key).isPresent()) {
                objectStorageClient.delete(key);
            }
            return false;
        }

        objectStorageClient.put(key, file.content(), exporter.contentType(), generationMetadata());
        return true;
    }

    private static Map<String, String> generationMetadata() {
        return Map.of(GENERATED_AT_METADATA, Instant.now().toString());
    }

    private static String normalize(String format) {
        return format == null ? null : format.trim().toLowerCase(Locale.ROOT);
    }

    /** What happened for one territory; {@code failed} zero is what allows clearing the flag. */
    public record TerritoryPublishResult(int published, int emptied, int failed) {

        public boolean complete() {
            return failed == 0;
        }
    }
}
