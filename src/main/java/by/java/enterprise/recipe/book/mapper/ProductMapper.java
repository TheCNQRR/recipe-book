package by.java.enterprise.recipe.book.mapper;

import by.java.enterprise.recipe.book.dto.request.ProductCreateRequest;
import by.java.enterprise.recipe.book.dto.request.ProductUpdateRequest;
import by.java.enterprise.recipe.book.dto.response.ProductResponse;
import by.java.enterprise.recipe.book.model.Product;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Product toEntity(ProductCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(ProductUpdateRequest request, @MappingTarget Product product);

    ProductResponse toResponse(Product product);
}
