package dev.pimon.users.mapper;

import dev.pimon.users.dto.UserDto;
import dev.pimon.users.entity.AppUser;
import dev.pimon.users.entity.Role;

import java.util.stream.Collectors;

public class UserMapper {

    private UserMapper() {}

    public static UserDto toDto(AppUser user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRoles().stream().map(Role::name).collect(Collectors.toSet()),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
