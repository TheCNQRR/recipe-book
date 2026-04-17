package by.java.enterprise.recipe.book.service;

import by.java.enterprise.recipe.book.dto.filter.DishFilter;
import by.java.enterprise.recipe.book.dto.request.DishCreateRequest;
import by.java.enterprise.recipe.book.dto.request.DishUpdateRequest;
import by.java.enterprise.recipe.book.exception.ValidationException;
import by.java.enterprise.recipe.book.model.Dish;
import by.java.enterprise.recipe.book.model.DishProduct;
import by.java.enterprise.recipe.book.model.Product;
import by.java.enterprise.recipe.book.model.enums.DietaryFlags;
import by.java.enterprise.recipe.book.model.enums.Type;
import by.java.enterprise.recipe.book.repository.DishProductRepository;
import by.java.enterprise.recipe.book.repository.DishRepository;
import by.java.enterprise.recipe.book.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DishService {

    private final DishRepository dishRepository;
    private final ProductRepository productRepository;
    private final DishProductRepository dishProductRepository;

    @Transactional
    public Dish create(DishCreateRequest request) {
        if (request.name().length() < 2) {
            throw new ValidationException("Минимальная длина названия 2 символа");
        }

        if (request.photos().size() > 5) {
            throw new ValidationException("Максимум 5 фотографий");
        }

        String name = request.name();
        Type macroType = extractTypeFromMacro(name);
        String cleanedName = removeMacroFromName(name);
        Type finalType = request.type() != null ? request.type() : macroType;
        if (finalType == null) {
            throw new ValidationException("Не указана категория блюда");
        }

        Dish dish = Dish.builder()
                .name(cleanedName)
                .photos(request.photos())
                .portionSize(request.portionSize())
                .type(finalType)
                .build();

        List<DishProduct> ingredients = new ArrayList<>();
        for (var ingDto : request.ingredients()) {
            Product product = productRepository.findById(ingDto.productId())
                    .orElseThrow(() -> new EntityNotFoundException("Продукт не найден: " + ingDto.productId()));
            DishProduct dp = DishProduct.builder()
                    .dish(dish)
                    .product(product)
                    .quantity(ingDto.quantity())
                    .build();
            ingredients.add(dp);
        }
        dish.setIngredients(ingredients);

        calculateNutrition(dish);

        if (request.caloricity() != null) dish.setCaloricity(request.caloricity());
        if (request.proteins() != null) dish.setProteins(request.proteins());
        if (request.fats() != null) dish.setFats(request.fats());
        if (request.carbs() != null) dish.setCarbs(request.carbs());

        validateNutritionPer100g(dish);

        Set<DietaryFlags> allowedFlags = computeAllowedFlags(dish);
        if (request.dietaryFlags() != null) {
            List<DietaryFlags> validFlags = request.dietaryFlags().stream()
                    .filter(allowedFlags::contains)
                    .collect(Collectors.toList());
            dish.setDietaryFlags(validFlags);
        }

        return dishRepository.save(dish);
    }

    @Transactional
    public Dish update(UUID id, DishUpdateRequest request) {
        Dish dish = findById(id);

        if (request.photos().size() > 5) {
            throw new ValidationException("Максимум 5 фотографий");
        }

        String name = request.name();
        Type macroType = extractTypeFromMacro(name);
        String cleanedName = removeMacroFromName(name);
        Type finalType = request.type() != null ? request.type() : macroType;
        if (finalType == null) {
            throw new ValidationException("Не указана категория блюда");
        }

        dish.setName(cleanedName);
        dish.setPhotos(request.photos());
        dish.setPortionSize(request.portionSize());
        dish.setType(finalType);

        dishProductRepository.deleteAllByDishId(id);
        dish.getIngredients().clear();
        List<DishProduct> newIngredients = new ArrayList<>();
        for (var ingDto : request.ingredients()) {
            Product product = productRepository.findById(ingDto.productId())
                    .orElseThrow(() -> new EntityNotFoundException("Продукт не найден"));
            DishProduct dp = DishProduct.builder()
                    .dish(dish)
                    .product(product)
                    .quantity(ingDto.quantity())
                    .build();
            newIngredients.add(dp);
        }
        dish.getIngredients().addAll(newIngredients);

        calculateNutrition(dish);

        if (request.caloricity() != null) dish.setCaloricity(request.caloricity());
        if (request.proteins() != null) dish.setProteins(request.proteins());
        if (request.fats() != null) dish.setFats(request.fats());
        if (request.carbs() != null) dish.setCarbs(request.carbs());

        validateNutritionPer100g(dish);

        Set<DietaryFlags> allowedFlags = computeAllowedFlags(dish);
        List<DietaryFlags> validFlags = request.dietaryFlags().stream()
                .filter(allowedFlags::contains)
                .collect(Collectors.toList());
        dish.setDietaryFlags(validFlags);

        return dishRepository.save(dish);
    }

    public Dish findById(UUID id) {
        return dishRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Блюдо не найдено"));
    }

    public List<Dish> findAll(DishFilter filter) {
        Specification<Dish> spec = buildSpecification(filter);
        return dishRepository.findAll(spec);
    }

    private Specification<Dish> buildSpecification(DishFilter filter) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();

            if (filter.nameSubstring() != null && !filter.nameSubstring().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")),
                        "%" + filter.nameSubstring().toLowerCase() + "%"));
            }

            if (filter.type() != null) {
                predicates.add(cb.equal(root.get("type"), filter.type()));
            }

            if (filter.sortBy() != null) {
                var path = root.get(filter.sortBy());
                query.orderBy(filter.sortDirection().equalsIgnoreCase("desc")
                        ? cb.desc(path) : cb.asc(path));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    @Transactional
    public void delete(UUID id) {
        Dish dish = findById(id);
        dishRepository.delete(dish);
    }

    private void calculateNutrition(Dish dish) {
        double totalCal = 0.0, totalProt = 0.0, totalFats = 0.0, totalCarbs = 0.0;
        for (DishProduct dp : dish.getIngredients()) {
            Product p = dp.getProduct();
            double factor = dp.getQuantity() / 100.0;
            totalCal += p.getCaloricity() * factor;
            totalProt += p.getProteins() * factor;
            totalFats += p.getFats() * factor;
            totalCarbs += p.getCarbs() * factor;
        }
        dish.setCaloricity(totalCal);
        dish.setProteins(totalProt);
        dish.setFats(totalFats);
        dish.setCarbs(totalCarbs);
    }

    private void validateNutritionPer100g(Dish dish) {
        double portion = dish.getPortionSize();
        if (portion <= 0) return;
        double prot100 = (dish.getProteins() / portion) * 100;
        double fats100 = (dish.getFats() / portion) * 100;
        double carbs100 = (dish.getCarbs() / portion) * 100;
        if (prot100 + fats100 + carbs100 > 100.0) {
            throw new ValidationException("Сумма БЖУ на 100 г блюда не может превышать 100");
        }
    }

    private Set<DietaryFlags> computeAllowedFlags(Dish dish) {
        Set<DietaryFlags> allowed = new HashSet<>(Arrays.asList(DietaryFlags.values()));
        for (DishProduct dp : dish.getIngredients()) {
            List<DietaryFlags> productFlags = dp.getProduct().getAdditionalFlags();
            allowed.retainAll(productFlags);
        }
        return allowed;
    }

    private Type extractTypeFromMacro(String name) {
        if (name.contains("!десерт")) return Type.DESSERT;
        if (name.contains("!первое")) return Type.FIRST_COURSE;
        if (name.contains("!второе")) return Type.MAIN_COURSE;
        if (name.contains("!напиток")) return Type.DRINK;
        if (name.contains("!салат")) return Type.SALAD;
        if (name.contains("!суп")) return Type.SOUP;
        if (name.contains("!перекус")) return Type.SNACK;
        return null;
    }

    private String removeMacroFromName(String name) {
        String result = name;
        String[] macros = {"!десерт", "!первое", "!второе", "!напиток", "!салат", "!суп", "!перекус"};
        for (String macro : macros) {
            result = result.replaceAll("(?i)" + Pattern.quote(macro), "");
        }
        return result.trim().replaceAll("\\s+", " ");
    }
}
