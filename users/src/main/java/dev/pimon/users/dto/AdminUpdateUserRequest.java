package dev.pimon.users.dto;

import dev.pimon.users.entity.Role;

import java.util.Set;

/**
 * Lo que un administrador cambia de una cuenta. <b>Todo es opcional.</b>
 *
 * <h2>Nulo significa «no lo toques»</h2>
 *
 * Es una modificación parcial: quien quiere desactivar una cuenta manda
 * {@code {"active": false}} y nada más. El servicio ya lo entiende así —solo escribe los
 * campos que llegan—, pero el DTO exigía el nombre con {@code @NotBlank} y tumbaba la
 * petición antes de que el servicio llegara a verla.
 *
 * El efecto era que <b>desactivar una cuenta desde el panel devolvía «el nombre no debe
 * estar vacío»</b>, un error que no tiene nada que ver con lo que se estaba haciendo y que
 * no hay forma de entender desde fuera.
 *
 * <h2>Vacío no es lo mismo que nulo</h2>
 *
 * Que el nombre sea opcional no quiere decir que se pueda borrar: mandar una cadena vacía
 * es pedir explícitamente un nombre en blanco, y eso el servicio lo rechaza con su propio
 * mensaje. Lo que se permite es <b>no mencionarlo</b>.
 *
 * @param name   nuevo nick, o {@code null} para dejar el que tiene
 * @param roles  nuevos roles, o {@code null} para dejar los que tiene
 * @param active si la cuenta puede entrar, o {@code null} para dejarlo como está
 */
public record AdminUpdateUserRequest(
        String     name,
        Set<Role>  roles,
        Boolean    active
) {}
