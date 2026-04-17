package by.java.enterprise.recipe.book.service;

import by.java.enterprise.recipe.book.dto.request.ProductCreateRequest;
import by.java.enterprise.recipe.book.dto.request.ProductUpdateRequest;
import by.java.enterprise.recipe.book.dto.filter.ProductFilter;
import by.java.enterprise.recipe.book.exception.ProductUsedInDishException;
import by.java.enterprise.recipe.book.exception.ValidationException;
import by.java.enterprise.recipe.book.mapper.ProductMapper;
import by.java.enterprise.recipe.book.model.Dish;
import by.java.enterprise.recipe.book.model.Product;
import by.java.enterprise.recipe.book.model.converter.DietaryFlagConverter;
import by.java.enterprise.recipe.book.model.enums.DietaryFlags;
import by.java.enterprise.recipe.book.repository.DishProductRepository;
import by.java.enterprise.recipe.book.repository.DishRepository;
import by.java.enterprise.recipe.book.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final DishProductRepository dishProductRepository;
    private final ProductMapper productMapper;
    private final DishRepository dishRepository;

    public List<Product> findAll(ProductFilter filter) {
        Specification<Product> spec = buildSpecification(filter);
        return productRepository.findAll(spec);
    }

    @Transactional
    public Product create(ProductCreateRequest request) {
        validateNutritionSum(request.proteins(), request.fats(), request.carbs());

        if (request.name().length() < 2) {
            throw new ValidationException("Минимальная длина названия 2 символа");
        }

        if (request.photos().size() > 5) {
            throw new ValidationException("Максимальное количество фотографий – 5");
        }
        Product product = productMapper.toEntity(request);
        return productRepository.save(product);
    }

    @Transactional
    public Product update(UUID id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Продукт не найден"));

        Set<DietaryFlags> flagsToRemove = new HashSet<>(product.getAdditionalFlags());
        flagsToRemove.removeAll(request.additionalFlags());

        if (!flagsToRemove.isEmpty()) {
            List<Dish> dishesWithProduct = dishRepository.findAllByProductId(id);
            List<Dish> affectedDishes = dishesWithProduct.stream()
                    .filter(dish -> !Collections.disjoint(dish.getDietaryFlags(), flagsToRemove))
                    .toList();

            if (!affectedDishes.isEmpty()) {
                String dishNames = affectedDishes.stream()
                        .map(Dish::getName)
                        .collect(Collectors.joining(", "));
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Невозможно убрать флаги " + flagsToRemove + ": они используются в блюдах: " + dishNames);
            }
        }

        product.setName(request.name());
        product.setPhotos(request.photos());
        product.setCaloricity(request.caloricity());
        product.setProteins(request.proteins());
        product.setFats(request.fats());
        product.setCarbs(request.carbs());
        product.setComposition(request.composition());
        product.setCategory(request.category());
        product.setReadiness(request.readiness());
        product.setAdditionalFlags(request.additionalFlags());

        return productRepository.save(product);
    }

    @Transactional
    public void delete(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Продукт не найден"));

        if (dishProductRepository.existsByProductId(id)) {
            List<String> dishNames = dishProductRepository.findDishNamesByProductId(id);
            throw new ProductUsedInDishException(
                    "Невозможно удалить продукт, он используется в блюдах: " + String.join(", ", dishNames));
        }
        productRepository.delete(product);
    }

    private void validateNutritionSum(Double proteins, Double fats, Double carbs) {
        if (proteins + fats + carbs > 100.0) {
            throw new ValidationException("Сумма белков, жиров и углеводов на 100 г не может превышать 100");
        }
    }

    private Specification<Product> buildSpecification(ProductFilter filter) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (filter.nameSubstring() != null) {
                predicates.add(cb.like(cb.lower(root.get("name")),
                        "%" + filter.nameSubstring().toLowerCase() + "%"));
            }
            if (filter.category() != null) {
                predicates.add(cb.equal(root.get("category"), filter.category()));
            }
            if (filter.readiness() != null) {
                predicates.add(cb.equal(root.get("readiness"), filter.readiness()));
            }
            if (filter.additionalFlags() != null && !filter.additionalFlags().isEmpty()) {
                for (var flag : filter.additionalFlags()) {
                    predicates.add(cb.isMember(flag, root.get("additionalFlags")));
                }
            }

            if (filter.sortBy() != null) {
                var path = root.get(filter.sortBy());
                query.orderBy(filter.sortDirection().equalsIgnoreCase("desc")
                        ? cb.desc(path) : cb.asc(path));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    public Product findById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Продукт не найден"));
    }
}