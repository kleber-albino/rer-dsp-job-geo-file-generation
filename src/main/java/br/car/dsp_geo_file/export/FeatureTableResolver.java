package br.car.dsp_geo_file.export;

import br.car.dsp_geo_file.theme.DownloadThemeConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves a theme's GeoServer {@code typeName} to the table the migration wrote on
 * {@code dsp-geoserver-db}, and introspects its columns.
 *
 * <p>The layer name is the table name with hyphens ({@code dsp:area-of-interest} →
 * {@code dsp.area_of_interest}) — that is how the migration publishes layers. The mapping is
 * verified against the catalogue instead of assumed, so a theme pointing at a layer that does
 * not exist fails with the table name in the message rather than as an empty file.
 */
@Slf4j
@Component
public class FeatureTableResolver {

    private static final String SCHEMA = "dsp";

    private final JdbcTemplate geoTargetJdbcTemplate;
    private final Map<String, FeatureTable> cache = new ConcurrentHashMap<>();

    public FeatureTableResolver(
            @Qualifier("geoTargetJdbcTemplate") JdbcTemplate geoTargetJdbcTemplate) {
        this.geoTargetJdbcTemplate = geoTargetJdbcTemplate;
    }

    public FeatureTable resolve(DownloadThemeConfig theme) {
        return cache.computeIfAbsent(theme.typeName(), typeName -> introspect(tableName(typeName)));
    }

    /** Local part of the {@code typeName}, the prefix GeoServer puts on every feature id. */
    public static String featureIdPrefix(String typeName) {
        int separator = typeName.indexOf(':');
        return separator < 0 ? typeName : typeName.substring(separator + 1);
    }

    private static String tableName(String typeName) {
        return featureIdPrefix(typeName).toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private FeatureTable introspect(String table) {
        List<FeatureTable.FeatureColumn> columns = new ArrayList<>();
        geoTargetJdbcTemplate.query(
                """
                SELECT column_name, udt_name
                FROM information_schema.columns
                WHERE table_schema = ? AND table_name = ?
                ORDER BY ordinal_position
                """,
                rs -> {
                    columns.add(new FeatureTable.FeatureColumn(
                            rs.getString("column_name"),
                            "geometry".equalsIgnoreCase(rs.getString("udt_name"))
                    ));
                },
                SCHEMA,
                table);

        if (columns.isEmpty()) {
            throw new IllegalStateException(
                    "Theme table " + SCHEMA + "." + table + " not found on geo-target. "
                            + "Run the migration job for this layer before generating files.");
        }

        String primaryKey = resolvePrimaryKey(table);
        log.info("Theme table {}.{} resolved with {} column(s), pk={}",
                SCHEMA, table, columns.size(), primaryKey);
        return new FeatureTable(SCHEMA + "." + table, primaryKey, List.copyOf(columns));
    }

    private String resolvePrimaryKey(String table) {
        List<String> keys = geoTargetJdbcTemplate.queryForList(
                """
                SELECT kcu.column_name
                FROM information_schema.table_constraints tc
                JOIN information_schema.key_column_usage kcu
                  ON kcu.constraint_name = tc.constraint_name
                 AND kcu.table_schema = tc.table_schema
                WHERE tc.table_schema = ? AND tc.table_name = ? AND tc.constraint_type = 'PRIMARY KEY'
                ORDER BY kcu.ordinal_position
                """,
                String.class,
                SCHEMA,
                table);
        if (keys.isEmpty()) {
            throw new IllegalStateException(
                    "Theme table " + SCHEMA + "." + table + " has no primary key — "
                            + "the feature id column cannot be resolved");
        }
        return keys.getFirst();
    }
}
