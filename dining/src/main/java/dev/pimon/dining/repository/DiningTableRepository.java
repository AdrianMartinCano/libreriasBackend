package dev.pimon.dining.repository;

import dev.pimon.dining.entity.DiningTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DiningTableRepository extends JpaRepository<DiningTable, String> {

    /** Mesas activas con su zona cargada, ordenadas — base del cálculo de disponibilidad. */
    @Query("SELECT t FROM DiningTable t JOIN FETCH t.zone z " +
           "WHERE t.active = true AND z.active = true " +
           "ORDER BY z.sortOrder ASC, t.sortOrder ASC, t.label ASC")
    List<DiningTable> findActiveWithZone();

    List<DiningTable> findByZone_IdOrderBySortOrderAscLabelAsc(String zoneId);

    boolean existsByZone_Id(String zoneId);
}
