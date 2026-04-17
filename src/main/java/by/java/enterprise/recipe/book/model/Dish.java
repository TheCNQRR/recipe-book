package by.java.enterprise.recipe.book.model;

import by.java.enterprise.recipe.book.model.enums.DietaryFlags;
import by.java.enterprise.recipe.book.model.enums.Type;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "dish")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dish {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank
    @Size(min = 2)
    @Column(name = "name", nullable = false)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "photos", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> photos = new ArrayList<>();

    @NotNull
    @DecimalMin("0.0")
    @Column(name = "caloricity", nullable = false)
    private Double caloricity;

    @NotNull
    @DecimalMin("0.0")
    @Column(name = "proteins", nullable = false)
    private Double proteins;

    @NotNull
    @DecimalMin("0.0")
    @Column(name = "fats", nullable = false)
    private Double fats;

    @NotNull
    @DecimalMin("0.0")
    @Column(name = "carbs", nullable = false)
    private Double carbs;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(name = "portion_size", nullable = false)
    private Double portionSize;

    @NotNull
    @Column(name = "type", nullable = false)
    private Type type;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "dietary_flags", columnDefinition = "text[]")
    @Builder.Default
    private List<DietaryFlags> dietaryFlags = new ArrayList<>();

    @OneToMany(mappedBy = "dish", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DishProduct> ingredients = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
