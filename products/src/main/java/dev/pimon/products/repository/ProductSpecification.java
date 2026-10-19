package dev.pimon.products.repository;

import dev.pimon.products.dto.ProductFilter;
import dev.pimon.products.entity.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {

    private ProductSpecification() {}

    /** Lo que ve el publico: solo lo publicado. */
    public static Specification<Product> withFilter(ProductFilter f) {
        return withFilter(f, true);
    }

    /**
     * @param soloActivos {@code false} incluye tambien las ocultas.
     *
     * <b>El panel las necesita.</b> Sin esto, ocultar un producto lo hace desaparecer
     * tambien de la administracion, y entonces no hay forma de volver a publicarlo: la
     * unica salida seria tocar la base de datos a mano. Un estado del que no se puede
     * volver no es un estado, es una trampa.
     */
    public static Specification<Product> withFilter(ProductFilter f, boolean soloActivos) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (soloActivos) {
                predicates.add(cb.isTrue(root.get("active")));
            }

            if (f.search() != null && !f.search().isBlank()) {
                String pattern = "%" + f.search().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")),        pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }

            if (f.category() != null && !f.category().isBlank()) {
                /*
                 * OJO CON EL DISTINCT, QUE NO ES OPCIONAL.
                 *
                 * Desde la 0.10.0 las categorías son una colección, así que este join
                 * produce UNA FILA POR CATEGORÍA del producto. Sin distinct, un producto
                 * que esté en tres categorías sale tres veces en el listado — y con
                 * paginación es peor que un duplicado feo: descuadra el total y deja
                 * páginas con menos elementos de los que dicen tener.
                 *
                 * Con un @ManyToOne esto no hacía falta, y por eso no estaba.
                 */
                if (query != null) {
                    query.distinct(true);
                }
                predicates.add(cb.equal(
                        root.join("categories").get("slug"), f.category()
                ));
            }

            if (f.minPrice() != null) {
                predicates.add(cb.ge(root.get("price"), f.minPrice()));
            }

            if (f.maxPrice() != null) {
                predicates.add(cb.le(root.get("price"), f.maxPrice()));
            }

            if (Boolean.TRUE.equals(f.inStock())) {
                predicates.add(cb.gt(root.get("stock"), 0));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
