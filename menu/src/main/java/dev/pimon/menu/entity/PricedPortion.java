package dev.pimon.menu.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Una ración con precio dentro de un plato (p. ej. "Tapa" 3,50€ / "Media" 7€ / "Ración" 12€).
 * Si un plato no usa raciones, su lista queda vacía y se muestra el {@code price} base del plato.
 * Embebido en {@link MenuItem} vía {@code @ElementCollection}.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class PricedPortion {

    /** Etiqueta de la ración: "Tapa", "Media", "Ración"... */
    @Column(nullable = false, length = 40)
    private String label;

    // Dinero en BigDecimal (precision/scale fijos) — evita los errores de redondeo de Double.
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    public PricedPortion(String label, BigDecimal amount) {
        this.label  = label;
        this.amount = amount;
    }
}
