package br.car.dsp_geo_file.batch.writer;

import br.car.dsp_geo_file.generation.GeoFileGenerationOrchestrator;
import br.car.dsp_geo_file.territory.Territory;
import br.car.dsp_geo_file.territory.TerritoryFileStateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * Publishes every file of the territory and clears the flag only on a complete round.
 *
 * <p>A partial round (one format published, another failed) deliberately keeps
 * {@code requires_s3_file_regeneration = true}: republishing a file that is already correct
 * costs a cycle, serving a stale one costs the citizen wrong data.
 */
@Slf4j
@Component
public class TerritoryGeoFileWriter implements ItemWriter<Territory> {

    private final GeoFileGenerationOrchestrator orchestrator;
    private final TerritoryFileStateRepository territoryRepository;

    public TerritoryGeoFileWriter(GeoFileGenerationOrchestrator orchestrator,
                                  TerritoryFileStateRepository territoryRepository) {
        this.orchestrator = orchestrator;
        this.territoryRepository = territoryRepository;
    }

    @Override
    public void write(Chunk<? extends Territory> chunk) {
        for (Territory territory : chunk) {
            var result = orchestrator.publish(territory);
            if (result.complete()) {
                territoryRepository.markGenerated(territory.level(), territory.id());
                log.info("Territory {} ({}) done — {} file(s) published, {} empty cut(s)",
                        territory.id(), territory.level(), result.published(), result.emptied());
            } else {
                log.warn("Territory {} ({}) still pending — {} failure(s) out of {} attempt(s)",
                        territory.id(),
                        territory.level(),
                        result.failed(),
                        result.published() + result.emptied() + result.failed());
            }
        }
    }
}
