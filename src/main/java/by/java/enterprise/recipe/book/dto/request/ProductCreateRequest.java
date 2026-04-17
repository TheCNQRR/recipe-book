package by.java.enterprise.recipe.book.dto.request;

import by.java.enterprise.recipe.book.model.enums.DietaryFlags;
import by.java.enterprise.recipe.book.model.enums.ProductCategory;
import by.java.enterprise.recipe.book.model.enums.ReadinessLevel;
import jakarta.validation.constraints.*;

import java.util.ArrayList;
import java.util.List;

public record ProductCreateRequest(
        @NotBlank
        @Size(min = 2)
        String name,

        @Size(max = 5, message = "Максимум 5 фотографий")
        List<String> photos,

        @NotNull
        @DecimalMin("0.0")
        Double caloricity,

        @NotNull
        @DecimalMin("0.0")
        @DecimalMax("100.0")
        Double proteins,

        @NotNull
        @DecimalMin("0.0")
        @DecimalMax("100.0")
        Double fats,

        @NotNull
        @DecimalMin("0.0")
        @DecimalMax("100.0")
        Double carbs,

        String composition,

        @NotNull
        ProductCategory category,

        @NotNull
        ReadinessLevel readiness,

        List<DietaryFlags> additionalFlags
) {
    public ProductCreateRequest {
        if (photos == null) photos = new ArrayList<>();
        if (additionalFlags == null) additionalFlags = new ArrayList<>();
    }
}
