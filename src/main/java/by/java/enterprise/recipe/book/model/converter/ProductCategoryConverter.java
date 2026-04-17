package by.java.enterprise.recipe.book.model.converter;

import by.java.enterprise.recipe.book.model.enums.ProductCategory;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ProductCategoryConverter implements AttributeConverter<ProductCategory, String> {

    @Override
    public String convertToDatabaseColumn(ProductCategory category) {
        if (category == null) return null;
        return switch (category) {
            case FROZEN -> "Замороженный";
            case MEAT -> "Мясной";
            case VEGETABLES -> "Овощи";
            case GREENS -> "Зелень";
            case SPICES -> "Специи";
            case CEREALS -> "Крупы";
            case CANNED -> "Консервы";
            case LIQUID -> "Жидкость";
            case SWEETS -> "Сладости";
        };
    }

    @Override
    public ProductCategory convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return switch (dbData) {
            case "Замороженный" -> ProductCategory.FROZEN;
            case "Мясной" -> ProductCategory.MEAT;
            case "Овощи" -> ProductCategory.VEGETABLES;
            case "Зелень" -> ProductCategory.GREENS;
            case "Специи" -> ProductCategory.SPICES;
            case "Крупы" -> ProductCategory.CEREALS;
            case "Консервы" -> ProductCategory.CANNED;
            case "Жидкость" -> ProductCategory.LIQUID;
            case "Сладости" -> ProductCategory.SWEETS;
            default -> throw new IllegalArgumentException("Неизвестная категория: " + dbData);
        };
    }
}
