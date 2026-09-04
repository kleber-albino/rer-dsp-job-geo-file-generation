package br.car.dsp_geo_file.batch.reader;

import br.car.dsp_geo_file.territory.Territory;
import br.car.dsp_geo_file.territory.TerritoryFileStateRepository;
import br.car.dsp_geo_file.territory.TerritoryLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Reads the territories with {@code requires_s3_file_regeneration = true}, level 2 first.
 *
 * <p>Loads on the first read rather than at startup, so the object storage check runs before
 * the database is queried and a missing bucket costs nothing.
 */
@Slf4j
@Component
public class PendingTerritoryReader implements ItemReader<Territory> {

    private final TerritoryFileStateRepository territoryRepository;

    private Iterator<Territory> pending;

    public PendingTerritoryReader(TerritoryFileStateRepository territoryRepository) {
        this.territoryRepository = territoryRepository;
    }

    @Override
    public Territory read() {
        if (pending == null) {
            pending = loadPending().iterator();
        }
        return pending.hasNext() ? pending.next() : null;
    }

    private List<Territory> loadPending() {
        List<Territory> territories = new ArrayList<>();
        for (TerritoryLevel level : TerritoryLevel.values()) {
            List<Territory> ofLevel = territoryRepository.findPending(level);
            log.info("{} territory(ies) pending file generation on {}", ofLevel.size(), level.table());
            territories.addAll(ofLevel);
        }
        return territories;
    }
}
