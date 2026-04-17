package by.java.enterprise.recipe.book.dto.filter;

import by.java.enterprise.recipe.book.model.enums.DietaryFlags;
import by.java.enterprise.recipe.book.model.enums.ProductCategory;
import by.java.enterprise.recipe.book.model.enums.ReadinessLevel;

import java.util.List;

public record ProductFilter(
        String nameSubstring,
        ProductCategory category,
        ReadinessLevel readiness,
        List<DietaryFlags> additionalFlags,
        String sortBy,
        String sortDirection
) {
    public ProductFilter {
        if (sortBy == null) sortBy = "name";
        if (sortDirection == null) sortDirection = "asc";
    }
}
