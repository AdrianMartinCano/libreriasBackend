package dev.pimon.dining.repository;

import dev.pimon.dining.entity.Seating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatingRepository extends JpaRepository<Seating, String> {

    List<Seating> findByActiveTrueOrderBySortOrderAscStartAsc();

    List<Seating> findAllByOrderBySortOrderAscStartAsc();
}
