package dev.pimon.users.entity;

import dev.pimon.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Un permiso de un solo uso para cambiar una contrasena sin saber la anterior.
 *
 * <h2>Aqui se guarda el HASH, nunca el token</h2>
 *
 * El token viaja al correo del usuario y no vuelve a escribirse en ninguna parte: en esta
 * tabla solo queda su SHA-256. La diferencia importa el dia que alguien lea la base de
 * datos —una copia de seguridad mal guardada, un volcado, un acceso de lectura de mas—:
 * con los tokens en claro, cualquiera de esas filas es una llave para entrar en la cuenta
 * de otro. Con el hash, no sirven de nada.
 *
 * Es el mismo motivo por el que las contrasenas tampoco se guardan en claro, y por eso
 * mismo <b>no se puede reenviar un token ya emitido</b>: nadie sabe cual era. Pedirlo otra
 * vez emite uno nuevo.
 *
 * <h2>Un solo uso y poca vida</h2>
 *
 * {@code usadoEn} lo marca gastado en cuanto se usa, y {@code expiraEn} le pone fecha. Un
 * enlace de recuperacion acaba en la bandeja de entrada del usuario y ahi se queda para
 * siempre: sin estas dos cosas, un correo de hace dos anos seguiria abriendo la cuenta.
 */
@Entity
@Table(name = "tokens_recuperacion", indexes = {
        @Index(name = "idx_token_recuperacion_hash", columnList = "hash", unique = true),
        @Index(name = "idx_token_recuperacion_usuario", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
public class TokenRecuperacion extends BaseEntity {

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    /** SHA-256 del token en hexadecimal. El token original no se guarda en ningun sitio. */
    @Column(nullable = false, length = 64, unique = true)
    private String hash;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    /** Cuando se uso. {@code null} mientras siga sirviendo. */
    @Column(name = "usado_en")
    private Instant usadoEn;

    public TokenRecuperacion(String userId, String hash, Instant expiraEn) {
        this.userId = userId;
        this.hash = hash;
        this.expiraEn = expiraEn;
    }

    /** Si todavia sirve: ni gastado ni caducado. */
    public boolean esValido(Instant ahora) {
        return usadoEn == null && expiraEn.isAfter(ahora);
    }
}
