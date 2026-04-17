package by.java.enterprise.recipe.book.controller;

import by.java.enterprise.recipe.book.dto.request.DishCreateRequest;
import by.java.enterprise.recipe.book.dto.request.DishUpdateRequest;
import by.java.enterprise.recipe.book.dto.filter.DishFilter;
import by.java.enterprise.recipe.book.dto.response.DishResponse;
import by.java.enterprise.recipe.book.mapper.DishMapper;
import by.java.enterprise.recipe.book.model.Dish;
import by.java.enterprise.recipe.book.service.DishService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/dishes")
@RequiredArgsConstructor
public class DishController {

    private final DishService dishService;
    private final DishMapper dishMapper;

    @GetMapping
    public List<DishResponse> getAll(DishFilter filter) {
        return dishService.findAll(filter).stream()
                .map(dishMapper::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public DishResponse getById(@PathVariable UUID id) {
        Dish dish = dishService.findById(id);
        return dishMapper.toResponse(dish);
    }

    @PostMapping
    public ResponseEntity<DishResponse> create(@Valid @RequestBody DishCreateRequest request) {
        Dish dish = dishService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dishMapper.toResponse(dish));
    }

    @PutMapping("/{id}")
    public DishResponse update(@PathVariable UUID id, @Valid @RequestBody DishUpdateRequest request) {
        Dish dish = dishService.update(id, request);
        return dishMapper.toResponse(dish);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        dishService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
