package dev.pimon.menu.repository;

import dev.pimon.menu.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, String> {

    /** Todos los platos de una categoría (vista admin), ordenados. */
    List<MenuItem> findByCategory_IdOrderBySortOrderAscNameAsc(String categoryId);

    /** ¿Tiene la categoría algún plato? — impide borrarla (debe vaciarse o desactivarse). */
    boolean existsByCategory_Id(String categoryId);

    /**
     * Carta pública: solo platos disponibles de categorías activas, con la categoría cargada
     * (JOIN FETCH evita el N+1 al agrupar por categoría). Ordenado por categoría y luego por plato.
     */
    @Query("SELECT i FROM MenuItem i JOIN FETCH i.category c " +
           "WHERE i.available = true AND c.active = true " +
           "ORDER BY c.sortOrder ASC, c.name ASC, i.sortOrder ASC, i.name ASC")
    List<MenuItem> findPublicCarta();

    /** Platos de la carta referenciados por un conjunto de ids (para resolver opciones del menú del día). */
    @Query("SELECT i FROM MenuItem i WHERE i.id IN :ids")
    List<MenuItem> findAllByIdIn(@Param("ids") List<String> ids);
}
