package by.java.enterprise.recipe.book.controller;

import by.java.enterprise.recipe.book.dto.request.ProductCreateRequest;
import by.java.enterprise.recipe.book.dto.request.ProductUpdateRequest;
import by.java.enterprise.recipe.book.dto.filter.ProductFilter;
import by.java.enterprise.recipe.book.dto.response.ProductResponse;
import by.java.enterprise.recipe.book.mapper.ProductMapper;
import by.java.enterprise.recipe.book.model.Product;
import by.java.enterprise.recipe.book.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductMapper productMapper;

    @GetMapping
    public List<ProductResponse> getAll(ProductFilter filter) {
        return productService.findAll(filter).stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable UUID id) {
        Product product = productService.findById(id);
        return productMapper.toResponse(product);
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductCreateRequest request) {
        Product product = productService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(productMapper.toResponse(product));
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody ProductUpdateRequest request) {
        Product product = productService.update(id, request);
        return productMapper.toResponse(product);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
