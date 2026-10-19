package dev.pimon.users.service;

import lombok.RequiredArgsConstructor;
import dev.pimon.common.dto.PageResponse;
import dev.pimon.common.exception.AppException;
import dev.pimon.users.dto.*;
import dev.pimon.users.entity.AppUser;
import dev.pimon.users.entity.Role;
import dev.pimon.users.mapper.UserMapper;
import dev.pimon.users.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Gestiona usuarios y actúa como UserDetailsService para Spring Security.
 *
 * loadUserByUsername(email) devuelve UserDetails con:
 *   username = userId (UUID)  → se usa como subject del JWT
 *   password = BCrypt hash
 *   authorities = roles del usuario
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserService implements UserDetailsService {

    private final UserRepository  repo;
    private final PasswordEncoder encoder;

    // ── UserDetailsService (Spring Security) ────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AppUser user = repo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));

        var authorities = user.getRoles().stream()
                .map(r -> new SimpleGrantedAuthority(r.name()))
                .toList();

        return User.builder()
                .username(user.getId())       // UUID como subject del JWT
                .password(user.getPassword())
                .authorities(authorities)
                .disabled(!user.isActive())
                .build();
    }

    // ── Registro ─────────────────────────────────────────────────────────

    public UserDto register(RegisterRequest req) {
        if (repo.existsByEmail(req.email())) {
            throw AppException.conflict("Ya existe una cuenta con el email: " + req.email());
        }
        String name = exigirNickLibre(req.name(), null);
        AppUser user = new AppUser(
                req.email(),
                encoder.encode(req.password()),
                name,
                Set.of(Role.ROLE_USER)
        );
        return UserMapper.toDto(repo.save(user));
    }

    // ── Perfil propio ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public UserDto getById(String id) {
        return UserMapper.toDto(findById(id));
    }

    public UserDto updateProfile(String id, UpdateProfileRequest req) {
        AppUser user = findById(id);
        String name = req.name() != null ? req.name().trim() : null;
        // Nombre/nick único: no permitir el de otro usuario.
        if (name != null && !name.isBlank()
                && repo.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw AppException.conflict("Ese nombre ya está en uso, elige otro");
        }
        user.setName(name);
        return UserMapper.toDto(repo.save(user));
    }

    public void changePassword(String id, ChangePasswordRequest req) {
        AppUser user = findById(id);
        if (!encoder.matches(req.currentPassword(), user.getPassword())) {
            throw AppException.badRequest("La contraseña actual no es correcta");
        }
        user.setPassword(encoder.encode(req.newPassword()));
        repo.save(user);
    }

    // ── Admin ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<UserDto> listAll(Pageable pageable) {
        return PageResponse.from(repo.findAll(pageable).map(UserMapper::toDto));
    }

    /** Alta de usuario por un admin, con roles indicados (p. ej. ROLE_STAFF para un trabajador). */
    public UserDto adminCreate(AdminCreateUserRequest req) {
        if (repo.existsByEmail(req.email())) {
            throw AppException.conflict("Ya existe una cuenta con el email: " + req.email());
        }
        Set<Role> roles = req.roles() != null && !req.roles().isEmpty()
                ? req.roles()
                : Set.of(Role.ROLE_USER);
        String name = exigirNickLibre(req.name(), null);
        AppUser user = new AppUser(req.email(), encoder.encode(req.password()), name, roles);
        if (req.active() != null) user.setActive(req.active());
        return UserMapper.toDto(repo.save(user));
    }

    public UserDto adminUpdate(String id, AdminUpdateUserRequest req) {
        AppUser user = findById(id);

        Set<Role> nextRoles = req.roles()  != null ? req.roles()  : user.getRoles();
        boolean   nextActive = req.active() != null ? req.active() : user.isActive();
        guardLastActiveAdmin(user, nextActive, nextRoles);

        // Si no viene nombre, se deja el que tenía. Antes se asignaba tal cual, así que un
        // PUT que solo cambiaba el rol dejaba al usuario SIN nombre.
        if (req.name() != null) {
            user.setName(exigirNickLibre(req.name(), id));
        }
        if (req.roles() != null)  user.setRoles(req.roles());
        if (req.active() != null) user.setActive(req.active());
        return UserMapper.toDto(repo.save(user));
    }

    /**
     * Comprueba que el nick esté libre y lo devuelve limpio.
     *
     * <h2>Por qué el nick es único</h2>
     *
     * Porque es <b>con lo que se firma</b> en el foro y en los comentarios del blog, y ahí
     * el nombre es la identidad: si dos personas pueden llamarse igual, cualquiera puede
     * ponerse el nick de otra y escribir en su nombre. No hace falta mala fe — con dos
     * «Marta» ya no se sabe quién dijo qué.
     *
     * <h2>Sin distinguir mayúsculas</h2>
     *
     * «Marta» y «marta» se leen igual de un vistazo, así que valen como suplantación.
     * Comparar exacto sería dejar la puerta abierta con el cerrojo puesto.
     *
     * <h2>Se comprueba aquí y no con un índice en la base</h2>
     *
     * Un {@code unique} en la columna no distinguiría mayúsculas (haría falta un índice
     * funcional que Hibernate no sabe crear con {@code ddl-auto}), así que no cubriría el
     * caso que importa. Y esta librería la usan varias aplicaciones: añadir la restricción
     * rompería el arranque de cualquiera que ya tenga nombres repetidos, sin avisar. La
     * carrera de dos registros simultáneos con el mismo nick queda teóricamente abierta;
     * a esta escala no pasa, y el día que importe se resuelve con el índice y su migración.
     *
     * @param name  el nick pedido
     * @param idPropio id del usuario que lo pide, o {@code null} si aún no existe
     * @return el nick sin espacios alrededor
     */
    private String exigirNickLibre(String name, String idPropio) {
        String limpio = name == null ? "" : name.trim();
        if (limpio.isBlank()) {
            throw AppException.badRequest("El nombre es obligatorio");
        }
        boolean cogido = idPropio == null
                ? repo.existsByNameIgnoreCase(limpio)
                : repo.existsByNameIgnoreCaseAndIdNot(limpio, idPropio);
        if (cogido) {
            throw AppException.conflict("Ese nombre ya está en uso, elige otro");
        }
        return limpio;
    }

    public void delete(String id) {
        AppUser user = findById(id);
        // Tras borrar no queda ni activo ni con rol admin → comprobamos que no era el último.
        guardLastActiveAdmin(user, false, Set.of());
        repo.delete(user);
    }

    /**
     * Impide dejar el sistema sin administradores activos: si {@code current} es ahora mismo un
     * admin activo y la operación lo dejaría sin ese estado (desactivado, sin rol admin, o borrado),
     * y además es el <b>último</b> admin activo, se rechaza.
     */
    private void guardLastActiveAdmin(AppUser current, boolean nextActive, Set<Role> nextRoles) {
        boolean wasActiveAdmin  = current.isActive() && current.getRoles().contains(Role.ROLE_ADMIN);
        boolean willBeActiveAdmin = nextActive && nextRoles.contains(Role.ROLE_ADMIN);
        if (wasActiveAdmin && !willBeActiveAdmin
                && repo.countActiveWithRole(Role.ROLE_ADMIN) <= 1) {
            throw AppException.conflict(
                    "No puedes dejar el sistema sin administradores activos: este es el último.");
        }
    }

    // ── Inicialización ────────────────────────────────────────────────────

    /** Crea un usuario admin si no existen usuarios en la BD. */
    public void createDefaultAdmin(String email, String password, String name) {
        if (repo.count() == 0) {
            AppUser admin = new AppUser(email, encoder.encode(password), name,
                    Set.of(Role.ROLE_USER, Role.ROLE_ADMIN));
            repo.save(admin);
        }
    }

    // ── Privado ──────────────────────────────────────────────────────────

    private AppUser findById(String id) {
        return repo.findById(id)
                .orElseThrow(() -> AppException.notFound("Usuario no encontrado con id: " + id));
    }
}
