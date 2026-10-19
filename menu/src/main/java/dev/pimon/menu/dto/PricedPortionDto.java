package dev.pimon.menu.dto;

import dev.pimon.menu.entity.PricedPortion;

import java.math.BigDecimal;

public record PricedPortionDto(String label, BigDecimal amount) {
    public static PricedPortionDto from(PricedPortion p) {
        return new PricedPortionDto(p.getLabel(), p.getAmount());
    }
}
