package br.car.dsp_geo_file.export;

/**
 * One output format. Adding GPKG means adding an implementation and declaring the format in
 * the theme catalogue — the object key contract and the endpoints do not change.
 */
public interface GeoFileExporter {

    boolean supports(String format);

    /** Value written to {@code Content-Type} on the object. */
    String contentType();

    /** Extension of the object key, without the dot. */
    String fileExtension();

    GeneratedGeoFile generate(GeoFileExportContext context);
}
