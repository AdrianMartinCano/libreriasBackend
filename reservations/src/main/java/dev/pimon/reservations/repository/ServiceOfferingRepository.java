package dev.pimon.reservations.repository;

import dev.pimon.reservations.entity.ServiceOffering;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, String> {
    List<ServiceOffering> findByActiveTrueOrderByName();
}
