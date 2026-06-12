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
        AppUser user = new AppUser(
                req.email(),
                encoder.encode(req.password()),
                req.name(),
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
        user.setName(req.name());
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

    public UserDto adminUpdate(String id, AdminUpdateUserRequest req) {
        AppUser user = findById(id);
        user.setName(req.name());
        if (req.roles() != null)  user.setRoles(req.roles());
        if (req.active() != null) user.setActive(req.active());
        return UserMapper.toDto(repo.save(user));
    }

    public void delete(String id) {
        if (!repo.existsById(id)) {
            throw AppException.notFound("Usuario no encontrado con id: " + id);
        }
        repo.deleteById(id);
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
