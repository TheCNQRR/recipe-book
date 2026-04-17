package by.java.enterprise.recipe.book.model;

import by.java.enterprise.recipe.book.model.enums.DietaryFlags;
import by.java.enterprise.recipe.book.model.enums.ProductCategory;
import by.java.enterprise.recipe.book.model.enums.ReadinessLevel;
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
@Table(name = "product")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

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
    @DecimalMax("100.0")
    @Column(name = "proteins", nullable = false)
    private Double proteins;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("100.0")
    @Column(name = "fats", nullable = false)
    private Double fats;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("100.0")
    @Column(name = "carbs", nullable = false)
    private Double carbs;

    @Column(name = "ingredients", columnDefinition = "TEXT")
    private String composition;

    @NotNull
    @Column(name = "category", nullable = false)
    private ProductCategory category;

    @NotNull
    @Column(name = "ready_to_eat", nullable = false)
    private ReadinessLevel readiness;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "additional_flags", columnDefinition = "text[]")
    @Builder.Default
    private List<DietaryFlags> additionalFlags = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
