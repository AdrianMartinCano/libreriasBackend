package dev.pimon.users.repository;

import dev.pimon.users.entity.TokenRecuperacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, String> {

    /**
     * Busca por el hash, que es lo unico que se guarda.
     *
     * Se busca por hash y NO por usuario porque en este punto no se sabe quien es: lo unico
     * que llega del enlace es el token, y de el sale el hash.
     */
    Optional<TokenRecuperacion> findByHash(String hash);

    /**
     * Gasta de golpe todos los tokens vivos de un usuario.
     *
     * Se llama al terminar un cambio de contrasena. Sin esto, quien pidiera el enlace tres
     * veces —porque el correo tardaba— se quedaria con dos permisos vivos que siguen
     * abriendo la cuenta despues de haberla recuperado.
     *
     * @return cuantos se han invalidado
     */
    @Modifying
    @Query("""
            UPDATE TokenRecuperacion t SET t.usadoEn = :ahora
            WHERE t.userId = :userId AND t.usadoEn IS NULL
            """)
    int invalidarLosDelUsuario(@Param("userId") String userId, @Param("ahora") Instant ahora);

    /**
     * Borra los que ya no sirven.
     *
     * Esta tabla solo crece, y sus filas no valen para nada una vez usadas o caducadas. No
     * se guarda historico a proposito: saber que alguien pidio recuperar su contrasena en
     * marzo no le sirve a nadie y es un dato mas que custodiar.
     */
    @Modifying
    @Query("DELETE FROM TokenRecuperacion t WHERE t.expiraEn < :limite OR t.usadoEn IS NOT NULL")
    int limpiarCaducados(@Param("limite") Instant limite);
}
