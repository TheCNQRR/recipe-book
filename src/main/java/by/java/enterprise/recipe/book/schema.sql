CREATE TABLE product
(
    id               UUID      DEFAULT gen_random_uuid() PRIMARY KEY,
    name             TEXT           NOT NULL,
    photos           JSONB     DEFAULT '[]'::jsonb,
    caloricity       DECIMAL(10, 2) NOT NULL,
    proteins         DECIMAL(10, 2) NOT NULL CHECK ( proteins >= 0 AND proteins <= 100 ),
    fats             DECIMAL(10, 2) NOT NULL CHECK ( fats >= 0 AND fats <= 100 ),
    carbs            DECIMAL(10, 2) NOT NULL CHECK ( carbs >= 0 AND carbs <= 100 ),
    ingredients      TEXT,
    category         TEXT           NOT NULL CHECK ( category in
                                                     ('Замороженный', 'Мясной', 'Овощи', 'Зелень', 'Специи', 'Крупы',
                                                      'Консервы', 'Жидкость', 'Сладости') ),
    ready_to_eat     TEXT           NOT NULL CHECK ( ready_to_eat in ('Готовый к употреблению', 'Полуфабрикат',
                                                                      'Требует приготовления') ),
    additional_flags TEXT[]    DEFAULT '{}',
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP
);

CREATE TABLE dish
(
    id            UUID      DEFAULT gen_random_uuid() PRIMARY KEY,
    name          TEXT           NOT NULL CHECK (LENGTH(name) >= 2),
    photos        JSONB     DEFAULT '[]'::jsonb,
    caloricity    DECIMAL(10, 2) NOT NULL CHECK (caloricity >= 0),
    proteins      DECIMAL(10, 2) NOT NULL CHECK (proteins >= 0),
    fats          DECIMAL(10, 2) NOT NULL CHECK (fats >= 0),
    carbs         DECIMAL(10, 2) NOT NULL CHECK (carbs >= 0),
    portion_size  DECIMAL(10, 2) NOT NULL CHECK (portion_size > 0),
    type          TEXT           NOT NULL CHECK (type IN
                                                 ('Десерт', 'Первое', 'Второе', 'Напиток', 'Салат', 'Суп', 'Перекус')),
    dietary_flags TEXT[]    DEFAULT '{}',
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP
);

CREATE TABLE dish_product
(
    id         UUID      DEFAULT gen_random_uuid() PRIMARY KEY,
    dish_id    UUID           NOT NULL REFERENCES dish (id) ON DELETE CASCADE,
    product_id UUID           NOT NULL REFERENCES product (id),
    quantity   DECIMAL(10, 2) NOT NULL CHECK (quantity > 0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (dish_id, product_id)
);

CREATE TABLE product_photos (
                                product_id UUID NOT NULL,
                                photo_url VARCHAR(500) NOT NULL,
                                PRIMARY KEY (product_id, photo_url),
                                CONSTRAINT fk_product_photos_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE
);