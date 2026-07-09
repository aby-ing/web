-- Campus Restaurant Order System MySQL initialization script.
-- Run with UTF-8, for example:
-- mysql -u root -p < database/init.sql

CREATE DATABASE IF NOT EXISTS restaurant_order
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE restaurant_order;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS reviews;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS food_orders;
DROP TABLE IF EXISTS dishes;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS users;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(128) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    phone VARCHAR(30) NULL,
    role VARCHAR(20) NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(60) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE dishes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    category_id BIGINT NOT NULL,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(500) NOT NULL,
    ingredients VARCHAR(200) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    image_url VARCHAR(500) NULL,
    taste_tags VARCHAR(120) NULL,
    stock INT NOT NULL DEFAULT 100,
    sales INT NOT NULL DEFAULT 0,
    available TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_dishes_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT ck_dishes_price CHECK (price > 0),
    CONSTRAINT ck_dishes_stock CHECK (stock >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE food_orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_no VARCHAR(40) NOT NULL,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    dining_type VARCHAR(20) NOT NULL,
    remark VARCHAR(300) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_food_orders_order_no UNIQUE (order_no),
    CONSTRAINT fk_food_orders_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_food_orders_total CHECK (total_amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE order_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    dish_id BIGINT NOT NULL,
    dish_name VARCHAR(80) NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    quantity INT NOT NULL,
    subtotal DECIMAL(10, 2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES food_orders (id),
    CONSTRAINT fk_order_items_dish FOREIGN KEY (dish_id) REFERENCES dishes (id),
    CONSTRAINT ck_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT ck_order_items_subtotal CHECK (subtotal >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE reviews (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    dish_id BIGINT NOT NULL,
    order_id BIGINT NULL,
    rating INT NOT NULL,
    content VARCHAR(500) NOT NULL,
    sentiment VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_reviews_dish FOREIGN KEY (dish_id) REFERENCES dishes (id),
    CONSTRAINT fk_reviews_order FOREIGN KEY (order_id) REFERENCES food_orders (id),
    CONSTRAINT ck_reviews_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Default password is 123456. password_hash is SHA-256.
INSERT INTO users (username, password_hash, nickname, phone, role, active) VALUES
('admin', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '餐厅管理员', '18800000001', 'ADMIN', 1),
('user', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '学生用户', '18800000002', 'USER', 1),
('merchant', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '餐厅商家', '18800000003', 'MERCHANT', 1);

INSERT INTO categories (name, sort_order, enabled) VALUES
('热门套餐', 1, 1),
('米饭主食', 2, 1),
('面食粉类', 3, 1),
('轻食饮品', 4, 1);

INSERT INTO dishes (category_id, name, description, ingredients, price, image_url, taste_tags, stock, sales, available) VALUES
(1, '香辣鸡腿饭', '鸡腿外皮微脆，酱汁香辣下饭，是午餐高峰的稳定选择。', '鸡腿肉, 米饭, 青菜, 辣椒酱', 18.80, '/assets/spicy-chicken.png', '香辣,套餐,高销量', 88, 36, 1),
(3, '番茄牛腩面', '番茄汤底酸甜开胃，牛腩炖煮软烂，适合想吃热汤面的同学。', '牛腩, 番茄, 手工面, 香菜', 22.00, '/assets/tomato-noodle.png', '酸甜,热汤,面食', 60, 24, 1),
(2, '黑椒牛柳盖饭', '黑椒香气浓郁，牛柳口感嫩滑，配米饭饱腹感强。', '牛柳, 洋葱, 青椒, 米饭', 24.50, '/assets/beef-rice.png', '咸香,牛肉,盖饭', 48, 19, 1),
(4, '清爽鸡胸沙拉', '低油轻食搭配鸡胸肉和时蔬，适合健身或晚餐轻负担场景。', '鸡胸肉, 生菜, 玉米, 番茄', 19.90, '/assets/chicken-salad.png', '清淡,低脂,轻食', 45, 16, 1),
(1, '麻辣香锅', '多种配菜现炒，麻辣味明显，适合 2-3 人共享。', '土豆, 莲藕, 火腿, 豆皮, 辣椒', 32.00, '/assets/spicy-pot.png', '麻辣,共享,重口味', 38, 28, 1),
(4, '柠檬红茶', '茶香清爽，柠檬酸度适中，可搭配主食解腻。', '红茶, 柠檬, 冰糖', 8.00, '/assets/lemon-tea.png', '清爽,饮品,酸甜', 120, 52, 1),
(2, '蒜香排骨饭', '排骨酥香入味，蒜香明显，适合偏好家常口味的用户。', '猪肋排, 蒜蓉, 米饭, 西兰花', 26.00, '/assets/pork-ribs.png', '蒜香,家常,套餐', 40, 13, 1),
(2, '素什锦炒饭', '多种蔬菜和米饭快炒，口味清淡，适合素食或预算有限用户。', '米饭, 胡萝卜, 青豆, 玉米, 鸡蛋', 13.50, '/assets/veggie-rice.png', '清淡,素食,实惠', 90, 21, 1);

INSERT INTO reviews (user_id, dish_id, rating, content, sentiment) VALUES
(2, 1, 5, '鸡腿饭很好吃，出餐也快，下次还会点。', 'POSITIVE'),
(2, 6, 4, '柠檬红茶清爽，搭配辣口菜很合适。', 'POSITIVE'),
(2, 2, 3, '味道不错，不过高峰期等得有点久。', 'NEUTRAL');
