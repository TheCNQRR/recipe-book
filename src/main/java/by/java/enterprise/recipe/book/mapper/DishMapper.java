package by.java.enterprise.recipe.book.mapper;

import by.java.enterprise.recipe.book.dto.request.DishCreateRequest;
import by.java.enterprise.recipe.book.dto.request.DishUpdateRequest;
import by.java.enterprise.recipe.book.dto.response.DishResponse;
import by.java.enterprise.recipe.book.model.Dish;
import by.java.enterprise.recipe.book.model.DishProduct;
import org.mapstruct.*;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DishMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "ingredients", ignore = true)
    @Mapping(target = "caloricity", ignore = true)
    @Mapping(target = "proteins", ignore = true)
    @Mapping(target = "fats", ignore = true)
    @Mapping(target = "carbs", ignore = true)
    Dish toEntity(DishCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "ingredients", ignore = true)
    @Mapping(target = "caloricity", ignore = true)
    @Mapping(target = "proteins", ignore = true)
    @Mapping(target = "fats", ignore = true)
    @Mapping(target = "carbs", ignore = true)
    void updateEntity(DishUpdateRequest request, @MappingTarget Dish dish);

    @Mapping(target = "ingredients", expression = "java(mapIngredients(dish.getIngredients()))")
    DishResponse toResponse(Dish dish);

    default List<DishResponse.IngredientDto> mapIngredients(List<DishProduct> ingredients) {
        if (ingredients == null) return List.of();
        return ingredients.stream()
                .map(dp -> new DishResponse.IngredientDto(
                        dp.getProduct().getId(),
                        dp.getProduct().getName(),
                        dp.getQuantity()))
                .collect(Collectors.toList());
    }
}
