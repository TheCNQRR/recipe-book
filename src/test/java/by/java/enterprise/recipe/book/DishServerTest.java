package by.java.enterprise.recipe.book;

import by.java.enterprise.recipe.book.dto.request.DishCreateRequest;
import by.java.enterprise.recipe.book.dto.request.DishCreateRequest.IngredientDto;
import by.java.enterprise.recipe.book.exception.ValidationException;
import by.java.enterprise.recipe.book.model.Dish;
import by.java.enterprise.recipe.book.model.Product;
import by.java.enterprise.recipe.book.model.enums.Type;
import by.java.enterprise.recipe.book.repository.DishRepository;
import by.java.enterprise.recipe.book.repository.ProductRepository;
import by.java.enterprise.recipe.book.service.DishService;
import org.assertj.core.data.Offset;
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
@DisplayName("DishService: Автоматический расчёт и валидация КБЖУ")
class DishServiceTest {

    @Mock
    private DishRepository dishRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private DishService dishService;

    @Captor
    private ArgumentCaptor<Dish> dishCaptor;

    private static final Offset<Double> PRECISION = within(0.0001);

    //метод для создания продуктов с заданными БЖУ
    private Product createProduct(double cal, double prot, double fat, double carb) {
        return Product.builder()
                .id(UUID.randomUUID())
                .name("Test Product")
                .caloricity(cal)
                .proteins(prot)
                .fats(fat)
                .carbs(carb)
                .build();
    }

    //метод для создания запроса
    private DishCreateRequest buildRequest(List<IngredientDto> ingredients, double portionSize) {
        return new DishCreateRequest(
                "Тестовое блюдо",
                List.of(),
                null, null, null, null,
                portionSize,
                Type.MAIN_COURSE,
                List.of(),
                ingredients
        );
    }

    @Nested
    @DisplayName("1. Расчёт пищевой ценности (calculateNutrition)")
    class CalculateNutritionTests {

        @ParameterizedTest(name = "Кол-во={0}г. Ожидается: К={1}, Б={2}, Ж={3}, У={4}")
        @MethodSource("equivalencePartitioningForCalculation")
        @DisplayName("Эквивалентное разбиение: Корректный расчёт пропорций для одного ингредиента")
        void shouldCalculateProportionsCorrectly(double quantity, double expCal, double expProt, double expFat, double expCarb) {
            Product p = createProduct(200.0, 10.0, 20.0, 30.0);
            when(productRepository.findById(p.getId())).thenReturn(Optional.of(p));
            when(dishRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DishCreateRequest request = buildRequest(List.of(new IngredientDto(p.getId(), quantity)), 150.0);

            dishService.create(request);

            verify(dishRepository).save(dishCaptor.capture());
            Dish saved = dishCaptor.getValue();

            assertThat(saved.getCaloricity()).isCloseTo(expCal, PRECISION);
            assertThat(saved.getProteins()).isCloseTo(expProt, PRECISION);
            assertThat(saved.getFats()).isCloseTo(expFat, PRECISION);
            assertThat(saved.getCarbs()).isCloseTo(expCarb, PRECISION);
        }

        static Stream<Arguments> equivalencePartitioningForCalculation() {
            return Stream.of(
                    //Меньше 100г
                    Arguments.of(50.0, 100.0, 5.0, 10.0, 15.0),
                    //Ровно 100г
                    Arguments.of(100.0, 200.0, 10.0, 20.0, 30.0),
                    //Больше 100г
                    Arguments.of(250.0, 500.0, 25.0, 50.0, 75.0),
                    //Дробные значения
                    Arguments.of(33.33, 66.66, 3.333, 6.666, 9.999)
            );
        }

        @Test
        @DisplayName("Расчёт для нескольких ингредиентов (Сумма вкладов)")
        void shouldCalculateSumForMultipleIngredients() {
            Product p1 = createProduct(100.0, 10.0, 0.0, 0.0);
            Product p2 = createProduct(200.0, 0.0, 15.0, 5.0);

            when(productRepository.findById(p1.getId())).thenReturn(Optional.of(p1));
            when(productRepository.findById(p2.getId())).thenReturn(Optional.of(p2));
            when(dishRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DishCreateRequest request = buildRequest(List.of(
                    new IngredientDto(p1.getId(), 150.0),
                    new IngredientDto(p2.getId(), 50.0)
            ), 200.0);

            dishService.create(request);

            verify(dishRepository).save(dishCaptor.capture());
            Dish saved = dishCaptor.getValue();

            assertThat(saved.getCaloricity()).isCloseTo(250.0, PRECISION);
            assertThat(saved.getProteins()).isCloseTo(15.0, PRECISION);
            assertThat(saved.getFats()).isCloseTo(7.5, PRECISION);
            assertThat(saved.getCarbs()).isCloseTo(2.5, PRECISION);
        }

        @Test
        @DisplayName("Граничное значение массы ингредиента, стремящееся к нулю")
        void boundaryValueExtremeSmallQuantity() {
            Product p = createProduct(100.0, 10.0, 10.0, 10.0);
            when(productRepository.findById(p.getId())).thenReturn(Optional.of(p));
            when(dishRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DishCreateRequest request = buildRequest(List.of(new IngredientDto(p.getId(), 0.01)), 100.0);

            dishService.create(request);

            verify(dishRepository).save(dishCaptor.capture());
            Dish saved = dishCaptor.getValue();
            assertThat(saved.getProteins()).isCloseTo(0.001, PRECISION);
        }
    }

    @Nested
    @DisplayName("2. Валидация БЖУ на 100г порции (validateNutritionPer100g)")
    class ValidateNutritionTests {

        @ParameterizedTest(name = "Внутри границ (Сумма = {3}): успешное создание")
        @MethodSource("validMacroSumValues")
        @DisplayName("АГЗ: Сумма БЖУ <= 100 на 100г блюда (Допустимые значения)")
        void shouldAllowValidMacroSumPer100g(double prot, double fat, double carb, double sumPer100g) {
            Product p = createProduct(100.0, prot, fat, carb);
            when(productRepository.findById(p.getId())).thenReturn(Optional.of(p));
            when(dishRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DishCreateRequest request = buildRequest(List.of(new IngredientDto(p.getId(), 100.0)), 100.0);

            assertThatCode(() -> dishService.create(request)).doesNotThrowAnyException();
        }

        @ParameterizedTest(name = "За границами (Сумма = {3}): ожидается ValidationException")
        @MethodSource("invalidMacroSumValues")
        @DisplayName("АГЗ: Сумма БЖУ > 100 на 100г блюда (Недопустимые значения)")
        void shouldThrowWhenMacroSumExceeds100g(double prot, double fat, double carb, double sumPer100g) {
            Product p = createProduct(100.0, prot, fat, carb);
            when(productRepository.findById(p.getId())).thenReturn(Optional.of(p));

            DishCreateRequest request = buildRequest(List.of(new IngredientDto(p.getId(), 100.0)), 100.0);

            assertThatThrownBy(() -> dishService.create(request))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Сумма БЖУ на 100 г блюда не может превышать 100");
        }


        static Stream<Arguments> validMacroSumValues() {
            return Stream.of(
                    Arguments.of(30.0, 30.0, 30.0, 90.0),
                    Arguments.of(33.3, 33.3, 33.4, 100.0),
                    Arguments.of(100.0, 0.0, 0.0, 100.0)
            );
        }

        static Stream<Arguments> invalidMacroSumValues() {
            return Stream.of(
                    Arguments.of(33.3, 33.3, 33.41, 100.01),
                    Arguments.of(100.0, 0.0, 0.01, 100.01),
                    Arguments.of(50.0, 50.0, 50.0, 150.0)
            );
        }

        @ParameterizedTest(name = "Размер порции = {0}")
        @MethodSource("boundaryValuesForPortionSize")
        @DisplayName("Анализ граничных значений: Защита от деления на 0 при размере порции <= 0")
        void shouldHandleZeroOrNegativePortionSize(double portionSize) {
            Product p = createProduct(100.0, 100.0, 100.0, 100.0);
            when(productRepository.findById(p.getId())).thenReturn(Optional.of(p));
            when(dishRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DishCreateRequest request = buildRequest(List.of(new IngredientDto(p.getId(), 100.0)), portionSize);

            assertThatCode(() -> dishService.create(request)).doesNotThrowAnyException();
        }

        static Stream<Arguments> boundaryValuesForPortionSize() {
            return Stream.of(
                    Arguments.of(0.0),
                    Arguments.of(-1.0)
            );
        }
    }
}