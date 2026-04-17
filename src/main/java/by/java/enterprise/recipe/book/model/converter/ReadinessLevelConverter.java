package by.java.enterprise.recipe.book.model.converter;

import by.java.enterprise.recipe.book.model.enums.ReadinessLevel;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ReadinessLevelConverter implements AttributeConverter<ReadinessLevel, String> {

    @Override
    public String convertToDatabaseColumn(ReadinessLevel level) {
        if (level == null) return null;
        return switch (level) {
            case READY_TO_EAT -> "Готовый к употреблению";
            case SEMI_FINISHED -> "Полуфабрикат";
            case REQUIRES_COOKING -> "Требует приготовления";
        };
    }

    @Override
    public ReadinessLevel convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return switch (dbData) {
            case "Готовый к употреблению" -> ReadinessLevel.READY_TO_EAT;
            case "Полуфабрикат" -> ReadinessLevel.SEMI_FINISHED;
            case "Требует приготовления" -> ReadinessLevel.REQUIRES_COOKING;
            default -> throw new IllegalArgumentException("Неизвестное значение ready_to_eat: " + dbData);
        };
    }
}
