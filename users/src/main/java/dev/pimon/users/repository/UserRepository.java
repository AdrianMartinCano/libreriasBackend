package dev.pimon.users.repository;

import dev.pimon.users.entity.AppUser;
import dev.pimon.users.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, String> {
    Optional<AppUser> findByEmail(String email);
    boolean existsByEmail(String email);

    /** ¿Hay OTRO usuario (distinto de {@code id}) con ese nombre? Para nombres/nicks únicos. */
    boolean existsByNameIgnoreCaseAndIdNot(String name, String id);

    /** ¿Hay ya alguien con ese nick? Sin distinguir mayúsculas: «Marta» y «marta» son el mismo. */
    boolean existsByNameIgnoreCase(String name);

    /** Nº de usuarios ACTIVOS que tienen el rol indicado — para no quedarse sin administradores. */
    @Query("SELECT COUNT(u) FROM AppUser u JOIN u.roles r WHERE r = :role AND u.active = true")
    long countActiveWithRole(@Param("role") Role role);
}
