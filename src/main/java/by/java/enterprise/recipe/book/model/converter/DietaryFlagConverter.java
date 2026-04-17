package by.java.enterprise.recipe.book.model.converter;

import by.java.enterprise.recipe.book.model.enums.DietaryFlags;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DietaryFlagConverter implements AttributeConverter<DietaryFlags, String> {

    @Override
    public String convertToDatabaseColumn(DietaryFlags flag) {
        if (flag == null) return null;
        return switch (flag) {
            case VEGAN -> "Веган";
            case GLUTEN_FREE -> "Без глютена";
            case SUGAR_FREE -> "Без сахара";
        };
    }

    @Override
    public DietaryFlags convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return switch (dbData) {
            case "Веган" -> DietaryFlags.VEGAN;
            case "Без глютена" -> DietaryFlags.GLUTEN_FREE;
            case "Без сахара" -> DietaryFlags.SUGAR_FREE;
            default -> throw new IllegalArgumentException("Неизвестный флаг: " + dbData);
        };
    }

    public static String toRussian(DietaryFlags flag) {
        return switch (flag) {
            case VEGAN -> "Веган";
            case GLUTEN_FREE -> "Без глютена";
            case SUGAR_FREE -> "Без сахара";
        };
    }
}
