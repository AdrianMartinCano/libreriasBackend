package dev.pimon.users.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import dev.pimon.common.dto.ApiResponse;
import dev.pimon.security.annotation.CurrentUser;
import dev.pimon.security.model.AuthUser;
import dev.pimon.users.dto.ChangePasswordRequest;
import dev.pimon.users.dto.UpdateProfileRequest;
import dev.pimon.users.dto.UserDto;
import dev.pimon.users.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints para que el usuario gestione su propio perfil.
 * Requieren JWT válido.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** GET /api/users/me — perfil del usuario autenticado */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> me(@CurrentUser AuthUser authUser) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getById(authUser.id())));
    }

    /** PUT /api/users/me — actualizar nombre */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> updateProfile(
            @CurrentUser AuthUser authUser,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok(userService.updateProfile(authUser.id(), request),
                        "Perfil actualizado correctamente")
        );
    }

    /** PUT /api/users/me/password — cambiar contraseña */
    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @CurrentUser AuthUser authUser,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(authUser.id(), request);
        return ResponseEntity.ok(ApiResponse.noContent("Contraseña cambiada correctamente"));
    }
}
