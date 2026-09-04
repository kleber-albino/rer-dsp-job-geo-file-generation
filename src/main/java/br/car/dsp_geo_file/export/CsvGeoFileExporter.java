package br.car.dsp_geo_file.export;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * CSV in the same shape the GeoServer WFS returns: an {@code FID} column built from the layer
 * name and the primary key, then every attribute in table order, geometry rendered as WKT.
 *
 * <p>That shape is the contract: the endpoint falls back to the WFS when the object is missing,
 * and a citizen must not be able to tell which of the two answered.
 */
@Slf4j
@Component
public class CsvGeoFileExporter implements GeoFileExporter {

    public static final String FORMAT = "csv";

    private static final String FID_HEADER = "FID";
    private static final String LAST_UPDATE_COLUMN = "updated_at";
    private static final String LINE_SEPARATOR = "\r\n";

    @Override
    public boolean supports(String format) {
        return FORMAT.equalsIgnoreCase(format == null ? null : format.trim());
    }

    @Override
    public String contentType() {
        return "text/csv;charset=UTF-8";
    }

    @Override
    public String fileExtension() {
        return FORMAT;
    }

    @Override
    public GeneratedGeoFile generate(GeoFileExportContext context) {
        FeatureTable table = context.featureTable();
        String featureIdPrefix = FeatureTableResolver.featureIdPrefix(context.theme().typeName());
        boolean tracksLastUpdate = table.hasColumn(LAST_UPDATE_COLUMN);

        StringBuilder csv = new StringBuilder();
        csv.append(header(table)).append(LINE_SEPARATOR);

        AtomicLong featureCount = new AtomicLong();
        AtomicReference<Instant> lastUpdate = new AtomicReference<>();

        context.geoTargetJdbcTemplate().query(
                selectSql(table, context.filter()),
                resultSet -> {
                    StringJoiner line = new StringJoiner(",");
                    line.add(escape(featureIdPrefix + "." + resultSet.getString(table.primaryKeyColumn())));
                    for (FeatureTable.FeatureColumn column : table.columns()) {
                        line.add(escape(resultSet.getString(column.name())));
                    }
                    csv.append(line).append(LINE_SEPARATOR);
                    featureCount.incrementAndGet();
                    if (tracksLastUpdate) {
                        trackLastUpdate(lastUpdate, resultSet.getTimestamp(LAST_UPDATE_COLUMN));
                    }
                },
                context.filter().argsArray());

        if (featureCount.get() == 0L) {
            return GeneratedGeoFile.empty();
        }

        log.debug("Generated CSV for territory={} theme={} features={}",
                context.territory().id(), context.theme().code(), featureCount.get());
        return new GeneratedGeoFile(
                csv.toString().getBytes(StandardCharsets.UTF_8),
                featureCount.get(),
                lastUpdate.get());
    }

    private static String header(FeatureTable table) {
        StringJoiner header = new StringJoiner(",");
        header.add(FID_HEADER);
        for (FeatureTable.FeatureColumn column : table.columns()) {
            header.add(escape(column.name()));
        }
        return header.toString();
    }

    private static String selectSql(FeatureTable table, FeatureFilter filter) {
        StringJoiner columns = new StringJoiner(", ");
        for (FeatureTable.FeatureColumn column : table.columns()) {
            String quoted = TerritoryFeatureFilterBuilder.quoteIdentifier(column.name());
            columns.add(column.geometry() ? "ST_AsText(" + quoted + ") AS " + quoted : quoted);
        }
        return "SELECT " + columns + " FROM " + table.qualifiedName() + " WHERE " + filter.sql();
    }

    private static void trackLastUpdate(AtomicReference<Instant> lastUpdate, Timestamp value) {
        if (value == null) {
            return;
        }
        Instant candidate = value.toInstant();
        lastUpdate.updateAndGet(current ->
                current == null || candidate.isAfter(current) ? candidate : current);
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.indexOf(',') < 0 && value.indexOf('"') < 0
                && value.indexOf('\n') < 0 && value.indexOf('\r') < 0) {
            return value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
