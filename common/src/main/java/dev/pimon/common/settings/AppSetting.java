package dev.pimon.common.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Ajustes editables en caliente, una fila por módulo. El valor se guarda como un documento JSON
 * (`payload`) que cada módulo mapea a su propio POJO tipado vía {@link SettingsStore}.
 *
 * <p>A diferencia de la configuración de {@code application.yml} (secretos, arranque), esta tabla
 * contiene ajustes <b>de negocio</b> que el administrador modifica desde el panel sin reiniciar.</p>
 */
@Entity
@Table(name = "app_settings")
@Getter
@Setter
@NoArgsConstructor
public class AppSetting {

    /** Clave del módulo: "reservations", "branding", ... */
    @Id
    @Column(length = 64)
    private String moduleKey;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public AppSetting(String moduleKey) {
        this.moduleKey = moduleKey;
    }
}
