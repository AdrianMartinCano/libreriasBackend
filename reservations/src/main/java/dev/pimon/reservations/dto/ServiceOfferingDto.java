package dev.pimon.reservations.dto;

import dev.pimon.reservations.entity.ServiceOffering;

import java.math.BigDecimal;

public record ServiceOfferingDto(
        String     id,
        String     name,
        String     description,
        int        durationMinutes,
        BigDecimal price,
        String     imageUrl,
        String     color
) {
    public static ServiceOfferingDto from(ServiceOffering s) {
        return new ServiceOfferingDto(
                s.getId(), s.getName(), s.getDescription(),
                s.getDurationMinutes(), s.getPrice(), s.getImageUrl(), s.getColor()
        );
    }
}
