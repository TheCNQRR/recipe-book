package by.java.enterprise.recipe.book;

import by.java.enterprise.recipe.book.dto.request.DishCreateRequest;
import by.java.enterprise.recipe.book.dto.request.DishCreateRequest.IngredientDto;
import by.java.enterprise.recipe.book.exception.ValidationException;
import by.java.enterprise.recipe.book.model.Dish;
import by.java.enterprise.recipe.book.model.Product;
import by.java.enterprise.recipe.book.model.enums.DietaryFlags;
import by.java.enterprise.recipe.book.model.enums.Type;
import by.java.enterprise.recipe.book.repository.DishRepository;
import by.java.enterprise.recipe.book.repository.ProductRepository;
import by.java.enterprise.recipe.book.service.DishService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DishService: автоматический расчёт пищевой ценности и её валидация")
class DishServiceTest {

    @Mock
    private DishRepository dishRepository;
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private DishService dishService;

    @Captor
    private ArgumentCaptor<Dish> dishCaptor;


    private Product product(double cal, double prot, double fat, double carb) {
        return Product.builder()
                .id(UUID.randomUUID())
                .name("test-product")
                .caloricity(cal)
                .proteins(prot)
                .fats(fat)
                .carbs(carb)
                .additionalFlags(List.of(DietaryFlags.VEGAN, DietaryFlags.GLUTEN_FREE))
                .build();
    }

    private DishCreateRequest buildRequest(List<IngredientDto> ingredients,
                                           double portionSize,
                                           Type type,
                                           Double caloricity,
                                           Double proteins,
                                           Double fats,
                                           Double carbs,
                                           List<DietaryFlags> dietaryFlags) {
        return new DishCreateRequest(
                "Тестовое блюдо",
                List.of("http://photo.com/1.jpg"),
                caloricity,
                proteins,
                fats,
                carbs,
                portionSize,
                type != null ? type : Type.MAIN_COURSE,
                dietaryFlags != null ? dietaryFlags : List.of(),
                ingredients != null ? ingredients : List.of()
        );
    }

    private DishCreateRequest buildRequest(List<IngredientDto> ingredients, double portionSize) {
        return buildRequest(ingredients, portionSize, null, null, null, null, null, null);
    }

    private DishCreateRequest buildRequest(List<IngredientDto> ingredients, double portionSize, Double caloricity, Double proteins) {
        return buildRequest(ingredients, portionSize, null, caloricity, proteins, null, null, null);
    }

    @Nested
    @DisplayName("Автоматический расчёт калорийности и БЖУ")
    class CalculateNutrition {

        @Test
        @DisplayName("Пустой список ингредиентов - все значения 0")
        void shouldSetZeroWhenNoIngredients() {
            DishCreateRequest request = buildRequest(List.of(), 250.0);
            when(dishRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            dishService.create(request);

            verify(dishRepository).save(dishCaptor.capture());
            Dish saved = dishCaptor.getValue();
            assertThat(saved.getCaloricity()).isZero();
            assertThat(saved.getProteins()).isZero();
            assertThat(saved.getFats()).isZero();
            assertThat(saved.getCarbs()).isZero();
        }

        @Test
        @DisplayName("Один ингредиент ровно 100 г - значения продукта переносятся без изменений")
        void shouldCopyProductValuesForExactly100g() {
            Product p = product(200.0, 10.0, 5.0, 30.0);
            when(productRepository.findById(p.getId())).thenReturn(Optional.of(p));
            when(dishRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DishCreateRequest request = buildRequest(
                    List.of(new IngredientDto(p.getId(), 100.0)), 250.0);

            dishService.create(request);

            verify(dishRepository).save(dishCaptor.capture());
            Dish saved = dishCaptor.getValue();
            assertThat(saved.getCaloricity()).isEqualTo(200.0);
            assertThat(saved.getProteins()).isEqualTo(10.0);
            assertThat(saved.getFats()).isEqualTo(5.0);
            assertThat(saved.getCarbs()).isEqualTo(30.0);
        }

        @ParameterizedTest(name = "Количество = {0} г - калорийность = {1}")
        @MethodSource("quantityAndExpectedCalories")
        @DisplayName("Масштабирование БЖУ пропорционально количеству продукта")
        void shouldScaleNutritionByQuantity(double quantity, double expectedCal) {
            Product p = product(150.0, 12.0, 7.0, 20.0);
            when(productRepository.findById(p.getId())).thenReturn(Optional.of(p));
            when(dishRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DishCreateRequest request = buildRequest(
                    List.of(new IngredientDto(p.getId(), quantity)), 250.0);

            dishService.create(request);

            verify(dishRepository).save(dishCaptor.capture());
            Dish saved = dishCaptor.getValue();
            assertThat(saved.getCaloricity()).isEqualTo(expectedCal);
            assertThat(saved.getProteins()).isEqualTo(12.0 * quantity / 100.0);
        }

        static Stream<Arguments> quantityAndExpectedCalories() {
            return Stream.of(
                    Arguments.of(50.0, 75.0),
                    Arguments.of(200.0, 300.0),
                    Arguments.of(100.0, 150.0)
            );
        }

        @Test
        @DisplayName("Несколько ингредиентов — итоговая ценность равна сумме вкладов")
        void shouldSumContributionsFromMultipleIngredients() {
            Product p1 = product(100.0, 10.0, 5.0, 20.0);
            Product p2 = product(200.0, 15.0, 8.0, 25.0);
            when(productRepository.findById(p1.getId())).thenReturn(Optional.of(p1));
            when(productRepository.findById(p2.getId())).thenReturn(Optional.of(p2));
            when(dishRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DishCreateRequest request = buildRequest(
                    List.of(
                            new IngredientDto(p1.getId(), 150.0),
                            new IngredientDto(p2.getId(), 50.0)
                    ), 250.0);

            dishService.create(request);

            verify(dishRepository).save(dishCaptor.capture());
            Dish saved = dishCaptor.getValue();
            double expectedCal = 100.0 * 1.5 + 200.0 * 0.5;
            assertThat(saved.getCaloricity()).isEqualTo(expectedCal);
            assertThat(saved.getProteins()).isEqualTo(10.0 * 1.5 + 15.0 * 0.5);
        }
    }

    @Nested
    @DisplayName("Валидация: сумма БЖУ на 100 г <= 100")
    class ValidateNutritionPer100g {

        private Product createProductForSum(double prot, double fat, double carb) {
            return product(0.0, prot, fat, carb);
        }

        @Test
        @DisplayName("Сумма БЖУ на 100 г = 100.0 (допустимо)")
        void shouldAllowSumExactly100() {
            Product p = createProductForSum(40.0, 30.0, 30.0);
            when(productRepository.findById(p.getId())).thenReturn(Optional.of(p));
            when(dishRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DishCreateRequest request = buildRequest(
                    List.of(new IngredientDto(p.getId(), 200.0)), 200.0);

            assertThatCode(() -> dishService.create(request)).doesNotThrowAnyException();
        }

        @ParameterizedTest(name = "Б={0}, Ж={1}, У={2} - ошибка={3}")
        @MethodSource("boundarySumArguments")
        @DisplayName("Проверка граничных значений суммы БЖУ на 100 г")
        void shouldThrowWhenSumExceeds100(double prot, double fat, double carb, boolean shouldFail) {
            Product p = createProductForSum(prot, fat, carb);
            when(productRepository.findById(p.getId())).thenReturn(Optional.of(p));

            DishCreateRequest request = buildRequest(
                    List.of(new IngredientDto(p.getId(), 100.0)), 100.0);

            if (shouldFail) {
                assertThatThrownBy(() -> dishService.create(request))
                        .isInstanceOf(ValidationException.class)
                        .hasMessageContaining("Сумма БЖУ на 100 г");
            } else {
                when(dishRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
                assertThatCode(() -> dishService.create(request)).doesNotThrowAnyException();
            }
        }

        static Stream<Arguments> boundarySumArguments() {
            return Stream.of(
                    Arguments.of(40.0, 30.0, 30.0, false),
                    Arguments.of(10.0, 10.0, 10.0, false),
                    Arguments.of(40.0, 30.0, 30.1, true),
                    Arguments.of(101.0, 0.0, 0.0, true)
            );
        }

        @Test
        @DisplayName("Порция 0 — валидация пропускается, исключение не выбрасывается")
        void shouldSkipValidationWhenPortionIsZero() {
            Product p = createProductForSum(80.0, 80.0, 80.0);
            when(productRepository.findById(p.getId())).thenReturn(Optional.of(p));
            when(dishRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DishCreateRequest request = buildRequest(
                    List.of(new IngredientDto(p.getId(), 100.0)), 0.0);

            assertThatCode(() -> dishService.create(request)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Сценарии создания блюда")
    class CreateDishScenarios {

        @Test
        @DisplayName("Ручная установка калорийности перезаписывает автоматический расчёт")
        void shouldOverrideCaloricityIfExplicitlyProvided() {
            Product p = product(100.0, 10.0, 5.0, 20.0);
            when(productRepository.findById(p.getId())).thenReturn(Optional.of(p));
            when(dishRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DishCreateRequest request = buildRequest(
                    List.of(new IngredientDto(p.getId(), 100.0)), 250.0,
                    500.0, 30.0);

            dishService.create(request);

            verify(dishRepository).save(dishCaptor.capture());
            Dish saved = dishCaptor.getValue();
            assertThat(saved.getCaloricity()).isEqualTo(500.0);
            assertThat(saved.getProteins()).isEqualTo(30.0);
            assertThat(saved.getFats()).isEqualTo(5.0);
        }
    }
}
