package dev.pimon.menu.entity;

import dev.pimon.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Un grupo de elección dentro de un {@link DailyMenu}: "Primeros", "Segundos", "Postres".
 * El comensal elige {@code choose} opción(es) de las disponibles.
 */
@Entity
@Table(name = "menu_daily_course")
@Getter
@Setter
@NoArgsConstructor
public class DailyMenuCourse extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "daily_menu_id", nullable = false)
    private DailyMenu dailyMenu;

    @Column(nullable = false)
    private String name;   // "Primeros"

    /** Nº de opciones que el comensal elige de este grupo (normalmente 1). */
    @Column(nullable = false)
    private int choose = 1;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "sort_order")
    private List<DailyMenuOption> options = new ArrayList<>();

    public void addOption(DailyMenuOption option) {
        option.setCourse(this);
        this.options.add(option);
    }
}
