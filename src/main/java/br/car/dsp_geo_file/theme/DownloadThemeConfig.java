package br.car.dsp_geo_file.theme;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * One theme of {@code downloadThemesConfig.json}, the catalogue shared with the backend.
 * {@code formats} is the list this job pre-generates — no separate catalogue to keep in sync.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DownloadThemeConfig(
        String code,
        String name,
        String typeName,
        List<String> formats,
        boolean enabled,
        DownloadTerritoryFilterConfig territoryFilter
) {
}
