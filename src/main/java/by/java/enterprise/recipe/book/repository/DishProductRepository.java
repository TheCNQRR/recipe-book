package by.java.enterprise.recipe.book.repository;

import by.java.enterprise.recipe.book.model.DishProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DishProductRepository extends JpaRepository<DishProduct, UUID> {

    boolean existsByProductId(UUID productId);

    @Query("""
        SELECT DISTINCT d.name
        FROM DishProduct dp
        JOIN dp.dish d
        WHERE dp.product.id = :productId
    """)
    List<String> findDishNamesByProductId(@Param("productId") UUID productId);

    @Modifying
    @Query("DELETE FROM DishProduct dp WHERE dp.dish.id = :dishId")
    void deleteAllByDishId(@Param("dishId") UUID dishId);
}
