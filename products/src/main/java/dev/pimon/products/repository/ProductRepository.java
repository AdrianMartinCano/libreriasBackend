package dev.pimon.products.repository;

import dev.pimon.products.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository
        extends JpaRepository<Product, String>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySlugAndActiveTrue(String slug);
    boolean existsBySlug(String slug);

    /**
     * Descuenta stock de forma <b>atómica y segura ante concurrencia</b>: la condición
     * {@code stock >= :qty} y el descuento ocurren en la misma sentencia SQL, por lo que dos
     * compras simultáneas del último ítem no pueden sobrevender (la BD serializa el UPDATE).
     *
     * @return número de filas afectadas: {@code 1} si había stock suficiente, {@code 0} si no.
     */
    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock - :qty WHERE p.id = :id AND p.stock >= :qty")
    int decrementStock(@Param("id") String id, @Param("qty") int qty);

    /**
     * Devuelve al stock lo que reservo un pedido que no llego a pagarse.
     *
     * NO lleva guarda como el descuento, y es a proposito: sumar siempre puede. La guarda
     * de aquel existe para no vender lo que no hay; aqui no hay nada que impedir.
     *
     * Si el producto ya no existe devuelve 0 y no pasa nada: alguien lo borro del catalogo
     * mientras el pedido esperaba, y ese stock ya no le importa a nadie.
     *
     * @return filas afectadas: 1 si el producto sigue existiendo, 0 si no.
     */
    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock + :qty WHERE p.id = :id")
    int incrementStock(@Param("id") String id, @Param("qty") int qty);

    /**
     * Cuantos productos hay en una categoria, INCLUIDOS los de la papelera.
     *
     * Cuentan los inactivos porque se pueden restaurar: borrar la categoria dejaria un
     * producto restaurado apuntando a una fila que ya no existe.
     *
     * <b>Se llamaba {@code countByCategoryId} hasta la 0.10.0.</b> El nombre cambia porque
     * la propiedad cambio: {@code categories} es ahora una coleccion, y Spring Data deriva
     * la consulta del nombre del campo. Con el nombre viejo no arranca la aplicacion.
     */
    long countByCategoriesId(String categoryId);

    /** Solo los que estan a la venta. Sirve para explicar mejor por que no se puede borrar. */
    long countByCategoriesIdAndActiveTrue(String categoryId);
}
