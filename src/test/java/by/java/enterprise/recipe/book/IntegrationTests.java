package by.java.enterprise.recipe.book;

import by.java.enterprise.recipe.book.dto.request.DishCreateRequest;
import by.java.enterprise.recipe.book.dto.request.ProductCreateRequest;
import by.java.enterprise.recipe.book.dto.request.ProductUpdateRequest;
import by.java.enterprise.recipe.book.dto.response.DishResponse;
import by.java.enterprise.recipe.book.dto.response.ProductResponse;
import by.java.enterprise.recipe.book.model.Product;
import by.java.enterprise.recipe.book.model.enums.DietaryFlags;
import by.java.enterprise.recipe.book.model.enums.ProductCategory;
import by.java.enterprise.recipe.book.model.enums.ReadinessLevel;
import by.java.enterprise.recipe.book.model.enums.Type;
import by.java.enterprise.recipe.book.repository.DishProductRepository;
import by.java.enterprise.recipe.book.repository.DishRepository;
import by.java.enterprise.recipe.book.repository.ProductRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IntegrationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private DishRepository dishRepository;

    @Autowired
    private DishProductRepository dishProductRepository;

    @BeforeAll
    void globalSetup() {
        cleanDatabase();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        dishProductRepository.deleteAll();
        dishRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Nested
    @DisplayName("Тесты API Продуктов")
    class ProductTests {

        @Test
        @DisplayName("проверка CRUD продукта")
        void fullProductLifecycle() {
            //Create
            var createReq = createValidProductRequest("Авокадо",  List.of(),160.0, 2.0, 14.0, 9.0);
            var createRes = restTemplate.postForEntity("/api/products", createReq, ProductResponse.class);
            UUID id = createRes.getBody().id();

            //Get by ID
            var getRes = restTemplate.getForEntity("/api/products/" + id, ProductResponse.class);
            assertThat(getRes.getBody().name()).isEqualTo("Авокадо");

            //Update
            var updateReq = new ProductUpdateRequest("Спелый Авокадо", List.of(), 170.0, 2.5, 15.0, 10.0, "Тропики",
                    ProductCategory.VEGETABLES, ReadinessLevel.READY_TO_EAT, List.of(DietaryFlags.VEGAN));
            restTemplate.put("/api/products/" + id, updateReq);

            var updatedRes = restTemplate.getForEntity("/api/products/" + id, ProductResponse.class);
            assertThat(updatedRes.getBody().name()).isEqualTo("Спелый Авокадо");
            assertThat(updatedRes.getBody().additionalFlags()).contains(DietaryFlags.VEGAN);

            //Delete
            restTemplate.delete("/api/products/" + id);
            assertThat(restTemplate.getForEntity("/api/products/" + id, Object.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Search: Фильтрация продуктов по подстроке имени")
        void shouldFilterProductsByName() {
            //Arrange
            productRepository.save(createProductEntity("Молоко", 3.0, 3.2, 4.8));
            productRepository.save(createProductEntity("Кефир", 3.0, 2.5, 4.0));

            //Act
            ResponseEntity<List<ProductResponse>> response = restTemplate.exchange(
                    "/api/products?nameSubstring=Мол",
                    HttpMethod.GET, null, new ParameterizedTypeReference<>() {});

            //Assert
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().get(0).name()).isEqualTo("Молоко");
        }
    }

    @Nested
    @DisplayName("Тесты API Блюд")
    class DishTests {

        @Test
        @DisplayName("Автоматический расчет КБЖУ блюда на основе ингредиентов")
        void shouldCalculateNutritionFromIngredients() {
            //Arrange
            var p1 = productRepository.save(createProductEntity("Курица", 25.0, 5.0, 0.0));
            var p2 = productRepository.save(createProductEntity("Рис", 7.0, 1.0, 70.0));

            var ingredients = List.of(
                    new DishCreateRequest.IngredientDto(p1.getId(), 200.0),
                    new DishCreateRequest.IngredientDto(p2.getId(), 100.0)
            );

            //Act
            var request = new DishCreateRequest("Плов", List.of(), null, null, null, null, 300.0, Type.MAIN_COURSE, List.of(), ingredients);

            var response = restTemplate.postForEntity("/api/dishes", request, DishResponse.class);

            //Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().proteins()).isEqualTo(57.0);
            assertThat(response.getBody().carbs()).isEqualTo(70.0);
        }

        @Test
        @DisplayName("АГЗ: Ошибка при обновлении продукта, если удаляемый флаг критичен для блюда")
        void shouldPreventProductFlagUpdateIfUsedInDish() {
            //Arrange
            var p = createProductEntity("Тофу", 10.0, 5.0, 1.0);
            p.setAdditionalFlags(List.of(DietaryFlags.VEGAN));
            p = productRepository.save(p);

            var dishReq = new DishCreateRequest("Веганский салат", List.of(), null, null, null, null, 100.0, Type.SALAD,
                    List.of(DietaryFlags.VEGAN), List.of(new DishCreateRequest.IngredientDto(p.getId(), 100.0)));
            restTemplate.postForEntity("/api/dishes", dishReq, DishResponse.class);

            //Act
            var updateReq = new ProductUpdateRequest("Тофу", List.of(), 100.0, 10.0, 5.0, 1.0, "...",
                    ProductCategory.VEGETABLES, ReadinessLevel.READY_TO_EAT, List.of());

            ResponseEntity<String> response = restTemplate.exchange("/api/products/" + p.getId(),
                    HttpMethod.PUT, new HttpEntity<>(updateReq), String.class);

            //Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).contains("Невозможно убрать флаги");
        }
    }

    @Test
    @DisplayName("АГЗ: Создание продукта с максимальным числом фото")
    void shouldCreateProductWithMaxPhotos() {
        //Arrange
        var request = createValidProductRequest("Томат", List.of("p1", "p2", "p3", "p4", "p5"), 20.0, 1.0, 0.0, 4.0);

        //Act
        ResponseEntity<ProductResponse> response = restTemplate.postForEntity("/api/products", request, ProductResponse.class);

        //Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().photos()).hasSize(5);
    }

    @Test
    @DisplayName("АГЗ: Создание продукта с превышением максимального числа фото")
    void shouldNotCreateProductWithExcessPhotos() {
        //Arrange
        var request = createValidProductRequest("Помидор", List.of("p1", "p2", "p3", "p4", "p5", "p6"),
                20.0, 1.0, 0.0, 4.0);

        //Act
        ResponseEntity<Object> response = restTemplate.postForEntity("/api/products", request, Object.class);

        //Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().toString()).contains("Максимальное количество фотографий – 5");
    }

    @ParameterizedTest(name = "Сумма БЖУ {0}+{1}+{2} = {3}")
    @MethodSource("nutritionProvider")
    @DisplayName("Валидация суммы нутриентов продукта")
    void validateProductNutrition(Double p, Double f, Double c, HttpStatus expectedStatus) {
        //Arrange
        var request = createValidProductRequest("Тест", List.of(), 100.0, p, f, c);

        //Act
        ResponseEntity<Object> response = restTemplate.postForEntity("/api/products", request, Object.class);

        //Assert
        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
    }

    static Stream<Arguments> nutritionProvider() {
        return Stream.of(
                Arguments.of(33.3, 33.3, 33.4, HttpStatus.CREATED),
                Arguments.of(33.4, 33.3, 33.4, HttpStatus.BAD_REQUEST),
                Arguments.of(0.0, 0.0, 0.0, HttpStatus.CREATED),
                Arguments.of(50.0, 50.0, 50.0, HttpStatus.BAD_REQUEST)
        );
    }

    @Test
    @DisplayName("Автоматическое определение типа блюда через макрос")
    void shouldExtractTypeFromMacro() {
        //Arrange
        var product = productRepository.save(createProductEntity("Огурец", 1.0, 0.0, 2.0));

        var ingredientDto = new DishCreateRequest.IngredientDto(product.getId(), 100.0);
        var request = new DishCreateRequest(
                "!салат Летний", List.of(),
                null, null, null, null,
                150.0, null, List.of(), List.of(ingredientDto)
        );

        //Act
        ResponseEntity<DishResponse> response = restTemplate.postForEntity("/api/dishes", request, DishResponse.class);

        //Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().type()).isEqualTo(Type.SALAD);
        assertThat(response.getBody().name()).isEqualTo("Летний");
    }

    @Test
    @DisplayName("ЭР: Ошибка при попытке создать блюдо без ингредиентов")
    void shouldFailWhenNoIngredients() {
        //Arrange
        var request = new DishCreateRequest("Пустота", List.of(), 0.0, 0.0, 0.0, 0.0, 100.0, Type.SNACK, List.of(), List.of());

        //Act
        ResponseEntity<Object> response = restTemplate.postForEntity("/api/dishes", request, Object.class);

        //Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Integrity: Запрет удаления продукта, используемого в блюде")
    void shouldPreventDeletionOfUsedProduct() {
        //Arrange
        var product = productRepository.save(createProductEntity("Мясо", 20.0, 15.0, 0.0));
        var ingredientDto = new DishCreateRequest.IngredientDto(product.getId(), 200.0);
        var dishRequest = new DishCreateRequest("Стейк", List.of(), null, null, null, null, 200.0, Type.MAIN_COURSE, List.of(), List.of(ingredientDto));
        restTemplate.postForEntity("/api/dishes", dishRequest, DishResponse.class);

        //Act
        ResponseEntity<Object> deleteResponse = restTemplate.exchange(
                "/api/products/" + product.getId(),
                HttpMethod.DELETE,
                null, Object.class
        );

        //Assert
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    private ProductCreateRequest createValidProductRequest(String name, List<String> photos, Double cal, Double p, Double f, Double c) {
        return new ProductCreateRequest(name, photos, cal, p, f, c, "Состав", ProductCategory.VEGETABLES, ReadinessLevel.READY_TO_EAT, List.of());
    }

    private Product createProductEntity(String name, Double p, Double f, Double c) {
        return Product.builder()
                .name(name)
                .caloricity(100.0).proteins(p).fats(f).carbs(c)
                .category(ProductCategory.VEGETABLES).readiness(ReadinessLevel.READY_TO_EAT)
                .build();
    }
}