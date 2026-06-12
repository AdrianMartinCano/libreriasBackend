package dev.pimon.newsletter.campaign;

import java.util.Arrays;
import java.util.Optional;

/**
 * Plantillas de email genéricas y reutilizables disponibles para campañas de newsletter.
 * Cada plantilla incrusta el asunto/contenido escrito por el administrador y se adapta
 * a la marca del proyecto vía {@code pimon.email.brand-name/primary-color/accent-color/logo-url}.
 */
public enum CampaignTemplate {

    MINIMAL("minimal", "Minimalista", "Solo el contenido y el pie de baja, sin cabecera.", "newsletter-tpl-minimal"),
    BRANDED("branded", "Con cabecera de marca", "Cabecera con el nombre/logo y color de acento de la marca.", "newsletter-tpl-branded"),
    ANNOUNCEMENT("announcement", "Anuncio destacado", "Título grande centrado con el asunto y acento de marca.", "newsletter-tpl-announcement");

    private final String id;
    private final String displayName;
    private final String description;
    private final String templateFile;

    CampaignTemplate(String id, String displayName, String description, String templateFile) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.templateFile = templateFile;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getTemplateFile() {
        return templateFile;
    }

    /** Busca una plantilla por su id. Vacío o null = sin plantilla (envío "a pelo"). */
    public static Optional<CampaignTemplate> fromId(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(t -> t.id.equalsIgnoreCase(id.trim()))
                .findFirst();
    }
}
