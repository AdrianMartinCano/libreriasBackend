package dev.pimon.menu.repository;

import dev.pimon.menu.entity.DailyMenu;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DailyMenuRepository extends JpaRepository<DailyMenu, String> {

    /** Menús (cualquier estado) de una fecha — vista admin. */
    List<DailyMenu> findByDateOrderByName(LocalDate date);

    /** Menús PUBLICADOS de una fecha — vista pública. */
    List<DailyMenu> findByDateAndPublishedTrueOrderByName(LocalDate date);

    /** Listado admin por rango de fechas (calendario/histórico), más recientes primero. */
    List<DailyMenu> findByDateBetweenOrderByDateDescName(LocalDate from, LocalDate to);

    boolean existsByDateAndName(LocalDate date, String name);

    /** Histórico paginado de todos los menús, los más recientes primero. */
    @Query("SELECT d FROM DailyMenu d ORDER BY d.date DESC, d.name ASC")
    Page<DailyMenu> findPage(Pageable pageable);

    /**
     * Menú con sus grupos y opciones cargados — para clonar o servir el detalle sin N+1.
     * Dos consultas separadas evitan el {@code MultipleBagFetchException} de dos listas a la vez;
     * aquí traemos los grupos y dejamos que {@code @BatchSize}/segunda query resuelva opciones.
     */
    @Query("SELECT DISTINCT d FROM DailyMenu d LEFT JOIN FETCH d.courses WHERE d.id = :id")
    DailyMenu findByIdWithCourses(@Param("id") String id);
}
