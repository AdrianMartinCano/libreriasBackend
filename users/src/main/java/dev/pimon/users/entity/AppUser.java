package dev.pimon.users.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import dev.pimon.common.entity.BaseEntity;

import java.util.HashSet;
import java.util.Set;

/**
 * Usuario de la aplicación.
 * Se llama AppUser para evitar conflicto con org.springframework.security.core.userdetails.User.
 *
 * Tabla: app_users (evita la palabra reservada "user" en algunas BD)
 */
@Entity
@Table(name = "app_users")
@Getter
@Setter
@NoArgsConstructor
public class AppUser extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Set<Role> roles = new HashSet<>();

    private boolean active = true;

    public AppUser(String email, String password, String name, Set<Role> roles) {
        this.email    = email;
        this.password = password;
        this.name     = name;
        this.roles    = roles;
    }
}
