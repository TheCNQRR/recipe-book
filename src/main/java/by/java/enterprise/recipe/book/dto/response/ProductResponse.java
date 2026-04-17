package by.java.enterprise.recipe.book.dto.response;

import by.java.enterprise.recipe.book.model.enums.DietaryFlags;
import by.java.enterprise.recipe.book.model.enums.ProductCategory;
import by.java.enterprise.recipe.book.model.enums.ReadinessLevel;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        List<String> photos,
        Double caloricity,
        Double proteins,
        Double fats,
        Double carbs,
        String composition,
        ProductCategory category,
        ReadinessLevel readiness,
        List<DietaryFlags> additionalFlags,
        Instant createdAt,
        Instant updatedAt
) {
}
