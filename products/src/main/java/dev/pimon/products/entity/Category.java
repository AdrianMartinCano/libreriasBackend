package dev.pimon.products.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import dev.pimon.common.entity.BaseEntity;

@Entity
@Table(name = "categories")
@Getter @Setter @NoArgsConstructor
public class Category extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    private String description;
    private String imageUrl;
    private boolean active = true;

    public Category(String name, String slug, String description, String imageUrl) {
        this.name        = name;
        this.slug        = slug;
        this.description = description;
        this.imageUrl    = imageUrl;
    }
}
