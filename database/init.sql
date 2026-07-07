-- Campus Restaurant Order System SQL Server initialization script.
-- Use UTF-8 when running with sqlcmd: sqlcmd -f 65001 -S localhost\SQLEXPRESS -E -i database\init.sql

IF DB_ID(N'restaurant_order') IS NULL
BEGIN
    CREATE DATABASE restaurant_order;
END
GO

USE restaurant_order;
GO

DROP TABLE IF EXISTS dbo.reviews;
DROP TABLE IF EXISTS dbo.order_items;
DROP TABLE IF EXISTS dbo.food_orders;
DROP TABLE IF EXISTS dbo.dishes;
DROP TABLE IF EXISTS dbo.categories;
DROP TABLE IF EXISTS dbo.users;
DROP TABLE IF EXISTS dbo.C;
DROP TABLE IF EXISTS dbo.D;
DROP TABLE IF EXISTS dbo.S;
DROP TABLE IF EXISTS dbo.SC;
DROP TABLE IF EXISTS dbo.T;
DROP TABLE IF EXISTS dbo.TC;
GO

CREATE TABLE dbo.users (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    username NVARCHAR(50) NOT NULL,
    password_hash VARCHAR(128) NOT NULL,
    nickname NVARCHAR(50) NOT NULL,
    phone NVARCHAR(30) NULL,
    role VARCHAR(20) NOT NULL,
    active BIT NOT NULL CONSTRAINT df_users_active DEFAULT 1,
    created_at DATETIME2 NOT NULL CONSTRAINT df_users_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT uk_users_username UNIQUE (username)
);

CREATE TABLE dbo.categories (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(60) NOT NULL,
    sort_order INT NOT NULL CONSTRAINT df_categories_sort_order DEFAULT 0,
    enabled BIT NOT NULL CONSTRAINT df_categories_enabled DEFAULT 1
);

CREATE TABLE dbo.dishes (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    category_id BIGINT NOT NULL,
    name NVARCHAR(80) NOT NULL,
    description NVARCHAR(500) NOT NULL,
    ingredients NVARCHAR(200) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    image_url NVARCHAR(500) NULL,
    taste_tags NVARCHAR(120) NULL,
    stock INT NOT NULL CONSTRAINT df_dishes_stock DEFAULT 100,
    sales INT NOT NULL CONSTRAINT df_dishes_sales DEFAULT 0,
    available BIT NOT NULL CONSTRAINT df_dishes_available DEFAULT 1,
    created_at DATETIME2 NOT NULL CONSTRAINT df_dishes_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT fk_dishes_category FOREIGN KEY (category_id) REFERENCES dbo.categories (id),
    CONSTRAINT ck_dishes_price CHECK (price > 0),
    CONSTRAINT ck_dishes_stock CHECK (stock >= 0)
);

CREATE TABLE dbo.food_orders (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_no VARCHAR(40) NOT NULL,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    dining_type VARCHAR(20) NOT NULL,
    remark NVARCHAR(300) NULL,
    created_at DATETIME2 NOT NULL CONSTRAINT df_food_orders_created_at DEFAULT SYSDATETIME(),
    updated_at DATETIME2 NOT NULL CONSTRAINT df_food_orders_updated_at DEFAULT SYSDATETIME(),
    CONSTRAINT uk_food_orders_order_no UNIQUE (order_no),
    CONSTRAINT fk_food_orders_user FOREIGN KEY (user_id) REFERENCES dbo.users (id),
    CONSTRAINT ck_food_orders_total CHECK (total_amount >= 0)
);

CREATE TABLE dbo.order_items (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_id BIGINT NOT NULL,
    dish_id BIGINT NOT NULL,
    dish_name NVARCHAR(80) NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    quantity INT NOT NULL,
    subtotal DECIMAL(10, 2) NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES dbo.food_orders (id),
    CONSTRAINT fk_order_items_dish FOREIGN KEY (dish_id) REFERENCES dbo.dishes (id),
    CONSTRAINT ck_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT ck_order_items_subtotal CHECK (subtotal >= 0)
);

CREATE TABLE dbo.reviews (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    dish_id BIGINT NOT NULL,
    order_id BIGINT NULL,
    rating INT NOT NULL,
    content NVARCHAR(500) NOT NULL,
    sentiment VARCHAR(20) NOT NULL,
    created_at DATETIME2 NOT NULL CONSTRAINT df_reviews_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES dbo.users (id),
    CONSTRAINT fk_reviews_dish FOREIGN KEY (dish_id) REFERENCES dbo.dishes (id),
    CONSTRAINT fk_reviews_order FOREIGN KEY (order_id) REFERENCES dbo.food_orders (id),
    CONSTRAINT ck_reviews_rating CHECK (rating BETWEEN 1 AND 5)
);
GO

-- Default password is 123456. password_hash is SHA-256.
INSERT INTO dbo.users (username, password_hash, nickname, phone, role, active) VALUES
(N'admin', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', N'餐厅管理员', N'18800000001', 'ADMIN', 1),
(N'user', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', N'学生用户', N'18800000002', 'USER', 1);

INSERT INTO dbo.categories (name, sort_order, enabled) VALUES
(N'热门套餐', 1, 1),
(N'米饭主食', 2, 1),
(N'面食粉类', 3, 1),
(N'轻食饮品', 4, 1);

INSERT INTO dbo.dishes (category_id, name, description, ingredients, price, image_url, taste_tags, stock, sales, available) VALUES
(1, N'香辣鸡腿饭', N'鸡腿外皮微脆，酱汁香辣下饭，是午餐高峰的稳定选择。', N'鸡腿肉, 米饭, 青菜, 辣椒酱', 18.80, N'/assets/spicy-chicken.png', N'香辣,套餐,高销量', 88, 36, 1),
(3, N'番茄牛腩面', N'番茄汤底酸甜开胃，牛腩炖煮软烂，适合想吃热汤面的同学。', N'牛腩, 番茄, 手工面, 香菜', 22.00, N'/assets/tomato-noodle.png', N'酸甜,热汤,面食', 60, 24, 1),
(2, N'黑椒牛柳盖饭', N'黑椒香气浓郁，牛柳口感嫩滑，配米饭饱腹感强。', N'牛柳, 洋葱, 青椒, 米饭', 24.50, N'/assets/beef-rice.png', N'咸香,牛肉,盖饭', 48, 19, 1),
(4, N'清爽鸡胸沙拉', N'低油轻食搭配鸡胸肉和时蔬，适合健身或晚餐轻负担场景。', N'鸡胸肉, 生菜, 玉米, 番茄', 19.90, N'/assets/chicken-salad.png', N'清淡,低脂,轻食', 45, 16, 1),
(1, N'麻辣香锅', N'多种配菜现炒，麻辣味明显，适合 2-3 人共享。', N'土豆, 莲藕, 火腿, 豆皮, 辣椒', 32.00, N'/assets/spicy-pot.png', N'麻辣,共享,重口味', 38, 28, 1),
(4, N'柠檬红茶', N'茶香清爽，柠檬酸度适中，可搭配主食解腻。', N'红茶, 柠檬, 冰糖', 8.00, N'/assets/lemon-tea.png', N'清爽,饮品,酸甜', 120, 52, 1),
(2, N'蒜香排骨饭', N'排骨酥香入味，蒜香明显，适合偏好家常口味的用户。', N'猪肋排, 蒜蓉, 米饭, 西兰花', 26.00, N'/assets/pork-ribs.png', N'蒜香,家常,套餐', 40, 13, 1),
(2, N'素什锦炒饭', N'多种蔬菜和米饭快炒，口味清淡，适合素食或预算有限用户。', N'米饭, 胡萝卜, 青豆, 玉米, 鸡蛋', 13.50, N'/assets/veggie-rice.png', N'清淡,素食,实惠', 90, 21, 1);

INSERT INTO dbo.reviews (user_id, dish_id, rating, content, sentiment) VALUES
(2, 1, 5, N'鸡腿饭很好吃，出餐也快，下次还会点。', 'POSITIVE'),
(2, 6, 4, N'柠檬红茶清爽，搭配辣口菜很合适。', 'POSITIVE'),
(2, 2, 3, N'味道不错，不过高峰期等得有点久。', 'NEUTRAL');
GO
