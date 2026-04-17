package by.java.enterprise.recipe.book.dto.filter;

import by.java.enterprise.recipe.book.model.enums.DietaryFlags;
import by.java.enterprise.recipe.book.model.enums.ReadinessLevel;
import by.java.enterprise.recipe.book.model.enums.Type;

import java.util.List;

public record DishFilter(
        String nameSubstring,
        Type type,
        List<DietaryFlags> dietaryFlags,
        String sortBy,
        String sortDirection
) {
    public DishFilter {
        if (sortBy == null) sortBy = "name";
        if (sortDirection == null) sortDirection = "asc";
    }
}
