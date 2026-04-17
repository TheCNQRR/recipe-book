package by.java.enterprise.recipe.book.dto.response;

import by.java.enterprise.recipe.book.model.enums.DietaryFlags;
import by.java.enterprise.recipe.book.model.enums.Type;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DishResponse(
        UUID id,
        String name,
        List<String> photos,
        Double caloricity,
        Double proteins,
        Double fats,
        Double carbs,
        Double portionSize,
        Type type,
        List<DietaryFlags> dietaryFlags,
        List<IngredientDto> ingredients,
        Instant createdAt,
        Instant updatedAt
) {
    public record IngredientDto(
            UUID productId,
            String productName,
            Double quantity
    ) {}
}
