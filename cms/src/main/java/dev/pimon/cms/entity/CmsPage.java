package dev.pimon.cms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import dev.pimon.common.entity.BaseEntity;

@Entity
@Table(name = "cms_pages")
@Getter @Setter @NoArgsConstructor
public class CmsPage extends BaseEntity {

    /** URL-friendly identifier — ej. "home", "sobre-nosotros", "politica-privacidad" */
    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String title;

    private String description;     // para SEO (meta description)

    /**
     * Array de bloques JSON serializado como texto.
     * Formato: [{ "type": "hero", "data": {...} }, { "type": "features", "data": {...} }]
     * El frontend lo deserializa con lib-cms-page de @org/cms.
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String blocksJson = "[]";

    private boolean published = false;  // false = borrador, true = visible públicamente
}
