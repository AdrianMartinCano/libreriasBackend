package dev.pimon.google.repository;

import dev.pimon.google.entity.CuentaGoogle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CuentaGoogleRepository extends JpaRepository<CuentaGoogle, String> {

    /** Por el identificador de Google, que es el estable. Nunca por el correo. */
    Optional<CuentaGoogle> findBySub(String sub);

    /** Si esta cuenta ya está vinculada a Google: lo usa el front para saber qué ofrecer. */
    boolean existsByUserId(String userId);
}
