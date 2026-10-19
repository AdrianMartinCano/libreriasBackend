package dev.pimon.google.entity;

import dev.pimon.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * El vínculo entre una cuenta de la aplicación y una cuenta de Google.
 *
 * Que exista esta fila significa dos cosas: que esa persona puede entrar con Google, y que
 * probablemente <b>no tiene una contraseña que conozca</b> — si la cuenta se creó al entrar
 * con Google, se guardó un hash de una cadena aleatoria que nadie llegó a ver.
 */
@Entity
@Table(name = "cuentas_google")
@Getter
@Setter
@NoArgsConstructor
public class CuentaGoogle extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true, length = 36)
    private String userId;

    /**
     * Identificador de la persona en Google, estable de por vida.
     *
     * Es por lo que se busca, <b>no</b> por el correo: alguien puede cambiar su dirección de
     * Gmail y seguir siendo la misma persona.
     */
    @Column(name = "sub", nullable = false, unique = true)
    private String sub;

    /** Con qué dirección se vinculó. Informativo; se refresca si cambia en Google. */
    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "vinculada_en", nullable = false)
    private Instant vinculadaEn;

    public CuentaGoogle(String userId, String sub, String email, Instant vinculadaEn) {
        this.userId      = userId;
        this.sub         = sub;
        this.email       = email;
        this.vinculadaEn = vinculadaEn;
    }
}
