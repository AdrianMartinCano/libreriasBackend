package dev.pimon.menu.dto;

import dev.pimon.menu.entity.Allergen;
import dev.pimon.menu.entity.DailyMenu;
import dev.pimon.menu.entity.DailyMenuCourse;
import dev.pimon.menu.entity.DailyMenuOption;
import dev.pimon.menu.entity.MenuItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Vista del menú del día. Cada opción se sirve <b>ya resuelta</b>: si referencia un plato de la
 * carta, hereda su nombre/descripción/alérgenos; si es línea libre, usa sus propios campos. Así el
 * front no necesita conocer la carta para pintarlo.
 */
public record DailyMenuDto(
        String                id,
        String                date,        // ISO 'YYYY-MM-DD'
        String                name,
        BigDecimal            price,
        String                description,
        boolean               published,
        String                publishedAt, // ISO o null
        List<CourseDto>       courses
) {
    public record CourseDto(String id, String name, int choose, List<OptionDto> options) {}

    public record OptionDto(
            String        id,
            String        itemId,      // null = línea libre
            String        name,        // efectivo (del plato si itemId != null)
            String        description,
            BigDecimal    supplement,
            Set<Allergen> allergens
    ) {}

    public static DailyMenuDto from(DailyMenu d) {
        List<CourseDto> courses = d.getCourses().stream()
                .map(DailyMenuDto::courseFrom)
                .toList();
        return new DailyMenuDto(
                d.getId(), d.getDate().toString(), d.getName(), d.getPrice(), d.getDescription(),
                d.isPublished(), d.getPublishedAt() != null ? d.getPublishedAt().toString() : null,
                courses);
    }

    private static CourseDto courseFrom(DailyMenuCourse c) {
        return new CourseDto(c.getId(), c.getName(), c.getChoose(),
                c.getOptions().stream().map(DailyMenuDto::optionFrom).toList());
    }

    private static OptionDto optionFrom(DailyMenuOption o) {
        MenuItem item = o.getItem();
        if (item != null) {
            return new OptionDto(o.getId(), item.getId(), item.getName(), item.getDescription(),
                    o.getSupplement(), item.getAllergens());
        }
        return new OptionDto(o.getId(), null, o.getName(), o.getDescription(),
                o.getSupplement(), o.getAllergens());
    }
}
