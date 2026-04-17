package by.java.enterprise.recipe.book.model.converter;

import by.java.enterprise.recipe.book.model.enums.Type;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DishTypeConverter implements AttributeConverter<Type, String> {

    @Override
    public String convertToDatabaseColumn(Type type) {
        if (type == null) return null;
        return switch (type) {
            case DESSERT -> "Десерт";
            case FIRST_COURSE -> "Первое";
            case MAIN_COURSE -> "Второе";
            case DRINK -> "Напиток";
            case SALAD -> "Салат";
            case SOUP -> "Суп";
            case SNACK -> "Перекус";
        };
    }

    @Override
    public Type convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return switch (dbData) {
            case "Десерт" -> Type.DESSERT;
            case "Первое" -> Type.FIRST_COURSE;
            case "Второе" -> Type.MAIN_COURSE;
            case "Напиток" -> Type.DRINK;
            case "Салат" -> Type.SALAD;
            case "Суп" -> Type.SOUP;
            case "Перекус" -> Type.SNACK;
            default -> throw new IllegalArgumentException("Неизвестный тип блюда: " + dbData);
        };
    }
}
