package by.java.enterprise.recipe.book.dto.request;

import by.java.enterprise.recipe.book.model.enums.DietaryFlags;
import by.java.enterprise.recipe.book.model.enums.Type;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record DishUpdateRequest(
        @NotBlank @Size(min = 2) String name,

        @Size(max = 5, message = "Максимум 5 фотографий")
        List<String> photos,

        @DecimalMin("0.0")
        Double caloricity,

        @DecimalMin("0.0")
        Double proteins,

        @DecimalMin("0.0")
        Double fats,

        @DecimalMin("0.0")
        Double carbs,

        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        Double portionSize,

        Type type,

        List<DietaryFlags> dietaryFlags,

        @NotEmpty(message = "Должен быть хотя бы один продукт")
        @Valid
        List<IngredientDto> ingredients
) {
    public DishUpdateRequest {
        if (photos == null) photos = new ArrayList<>();
        if (dietaryFlags == null) dietaryFlags = new ArrayList<>();
        if (ingredients == null) ingredients = new ArrayList<>();
    }

    public record IngredientDto(
            @NotNull UUID productId,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) Double quantity
    ) {}
}
