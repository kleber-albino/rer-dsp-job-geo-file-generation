package br.car.dsp_geo_file.export;

import java.util.List;

/**
 * A theme's table on {@code dsp-geoserver-db}, as introspected.
 */
public record FeatureTable(
        String qualifiedName,
        String primaryKeyColumn,
        List<FeatureColumn> columns
) {

    public boolean hasColumn(String name) {
        return columns.stream().anyMatch(column -> column.name().equalsIgnoreCase(name));
    }

    /** One column, with the geometry flag the CSV needs to render WKT instead of the raw value. */
    public record FeatureColumn(String name, boolean geometry) {
    }
}
