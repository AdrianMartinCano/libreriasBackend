package dev.pimon.reservations.repository;

import dev.pimon.reservations.entity.StaffMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StaffMemberRepository extends JpaRepository<StaffMember, String> {

    /** Trabajadores activos ordenados para el flujo público y las columnas del timeline. */
    List<StaffMember> findByActiveTrueOrderBySortOrderAscNameAsc();

    /** Todos (activos e inactivos) para el CRUD del panel admin. */
    List<StaffMember> findAllByOrderBySortOrderAscNameAsc();

    /** Resuelve el trabajador vinculado a una cuenta (scoping de ROLE_STAFF y atajo "incluirme"). */
    Optional<StaffMember> findByUserId(String userId);

    boolean existsByUserId(String userId);
}
