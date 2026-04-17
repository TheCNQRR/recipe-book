package by.java.enterprise.recipe.book.repository;

import by.java.enterprise.recipe.book.model.Dish;
import by.java.enterprise.recipe.book.model.enums.DietaryFlags;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface DishRepository extends JpaRepository<Dish, UUID>, JpaSpecificationExecutor<Dish> {
    @Query("SELECT DISTINCT d FROM Dish d JOIN d.ingredients i WHERE i.product.id = :productId")
    List<Dish> findAllByProductId(@Param("productId") UUID productId);
}
