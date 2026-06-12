package dev.pimon.users.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import dev.pimon.common.dto.ApiResponse;
import dev.pimon.common.dto.PageResponse;
import dev.pimon.users.dto.AdminUpdateUserRequest;
import dev.pimon.users.dto.UserDto;
import dev.pimon.users.service.UserService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Administración de usuarios — solo ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    /** GET /api/admin/users?page=0&size=20 — lista paginada */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserDto>>> list(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(userService.listAll(pageable)));
    }

    /** GET /api/admin/users/{id} — detalle de un usuario */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> get(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getById(id)));
    }

    /** PUT /api/admin/users/{id} — actualizar nombre, roles y estado */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> update(
            @PathVariable String id,
            @Valid @RequestBody AdminUpdateUserRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok(userService.adminUpdate(id, request),
                        "Usuario actualizado correctamente")
        );
    }

    /** DELETE /api/admin/users/{id} — eliminar usuario */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        userService.delete(id);
        return ResponseEntity.ok(ApiResponse.noContent("Usuario eliminado correctamente"));
    }
}
