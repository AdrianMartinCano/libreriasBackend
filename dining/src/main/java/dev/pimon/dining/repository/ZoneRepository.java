package dev.pimon.dining.repository;

import dev.pimon.dining.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ZoneRepository extends JpaRepository<Zone, String> {

    List<Zone> findByActiveTrueOrderBySortOrderAscNameAsc();

    List<Zone> findAllByOrderBySortOrderAscNameAsc();
}
