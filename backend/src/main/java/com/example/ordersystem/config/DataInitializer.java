package com.example.ordersystem.config;

import com.example.ordersystem.domain.Category;
import com.example.ordersystem.domain.Dish;
import com.example.ordersystem.domain.Review;
import com.example.ordersystem.domain.Role;
import com.example.ordersystem.domain.User;
import com.example.ordersystem.repository.CategoryRepository;
import com.example.ordersystem.repository.DishRepository;
import com.example.ordersystem.repository.ReviewRepository;
import com.example.ordersystem.repository.UserRepository;
import com.example.ordersystem.service.PasswordService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner initData(UserRepository userRepository,
                               CategoryRepository categoryRepository,
                               DishRepository dishRepository,
                               ReviewRepository reviewRepository,
                               PasswordService passwordService) {
        return args -> {
            seedUsers(userRepository, passwordService);
            seedMenu(categoryRepository, dishRepository);
            seedReviews(userRepository, dishRepository, reviewRepository);
        };
    }

    private void seedUsers(UserRepository userRepository, PasswordService passwordService) {
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPasswordHash(passwordService.hash("123456"));
            admin.setNickname("餐厅管理员");
            admin.setPhone("18800000001");
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
        }
        if (userRepository.findByUsername("user").isEmpty()) {
            User user = new User();
            user.setUsername("user");
            user.setPasswordHash(passwordService.hash("123456"));
            user.setNickname("学生用户");
            user.setPhone("18800000002");
            user.setRole(Role.USER);
            userRepository.save(user);
        }
    }

    private void seedMenu(CategoryRepository categoryRepository, DishRepository dishRepository) {
        if (categoryRepository.count() > 0) {
            return;
        }

        Category combo = category("热门套餐", 1);
        Category rice = category("米饭主食", 2);
        Category noodles = category("面食粉类", 3);
        Category light = category("轻食饮品", 4);
        categoryRepository.saveAll(List.of(combo, rice, noodles, light));

        dishRepository.save(dish(combo, "香辣鸡腿饭", "鸡腿外皮微脆，酱汁香辣下饭，是午餐高峰的稳定选择。",
                "鸡腿肉, 米饭, 青菜, 辣椒酱", "香辣,套餐,高销量", "spicy-chicken.png", "18.80", 88, 36));
        dishRepository.save(dish(noodles, "番茄牛腩面", "番茄汤底酸甜开胃，牛腩炖煮软烂，适合想吃热汤面的同学。",
                "牛腩, 番茄, 手工面, 香菜", "酸甜,热汤,面食", "tomato-noodle.png", "22.00", 60, 24));
        dishRepository.save(dish(rice, "黑椒牛柳盖饭", "黑椒香气浓郁，牛柳口感嫩滑，配米饭饱腹感强。",
                "牛柳, 洋葱, 青椒, 米饭", "咸香,牛肉,盖饭", "beef-rice.png", "24.50", 48, 19));
        dishRepository.save(dish(light, "清爽鸡胸沙拉", "低油轻食搭配鸡胸肉和时蔬，适合健身或晚餐轻负担场景。",
                "鸡胸肉, 生菜, 玉米, 番茄", "清淡,低脂,轻食", "chicken-salad.png", "19.90", 45, 16));
        dishRepository.save(dish(combo, "麻辣香锅", "多种配菜现炒，麻辣味明显，适合 2-3 人共享。",
                "土豆, 莲藕, 火腿, 豆皮, 辣椒", "麻辣,共享,重口味", "spicy-pot.png", "32.00", 38, 28));
        dishRepository.save(dish(light, "柠檬红茶", "茶香清爽，柠檬酸度适中，可搭配主食解腻。",
                "红茶, 柠檬, 冰糖", "清爽,饮品,酸甜", "lemon-tea.png", "8.00", 120, 52));
        dishRepository.save(dish(rice, "蒜香排骨饭", "排骨酥香入味，蒜香明显，适合偏好家常口味的用户。",
                "猪肋排, 蒜蓉, 米饭, 西兰花", "蒜香,家常,套餐", "pork-ribs.png", "26.00", 40, 13));
        dishRepository.save(dish(rice, "素什锦炒饭", "多种蔬菜和米饭快炒，口味清淡，适合素食或预算有限用户。",
                "米饭, 胡萝卜, 青豆, 玉米, 鸡蛋", "清淡,素食,实惠", "veggie-rice.png", "13.50", 90, 21));
    }

    private void seedReviews(UserRepository userRepository, DishRepository dishRepository, ReviewRepository reviewRepository) {
        if (reviewRepository.count() > 0) {
            return;
        }
        User user = userRepository.findByUsername("user").orElse(null);
        List<Dish> dishes = dishRepository.findTop6ByAvailableTrueOrderBySalesDescIdAsc();
        if (user == null || dishes.size() < 3) {
            return;
        }
        reviewRepository.save(review(user, dishes.get(0), 5, "鸡腿饭很好吃，出餐也快，下次还会点。"));
        reviewRepository.save(review(user, dishes.get(1), 4, "柠檬红茶清爽，搭配辣口菜很合适。"));
        reviewRepository.save(review(user, dishes.get(2), 3, "味道不错，不过高峰期等得有点久。"));
    }

    private Category category(String name, int sortOrder) {
        Category category = new Category();
        category.setName(name);
        category.setSortOrder(sortOrder);
        category.setEnabled(true);
        return category;
    }

    private Dish dish(Category category,
                      String name,
                      String description,
                      String ingredients,
                      String tags,
                      String image,
                      String price,
                      int stock,
                      int sales) {
        Dish dish = new Dish();
        dish.setCategory(category);
        dish.setName(name);
        dish.setDescription(description);
        dish.setIngredients(ingredients);
        dish.setTasteTags(tags);
        dish.setImageUrl("/assets/" + image);
        dish.setPrice(new BigDecimal(price));
        dish.setStock(stock);
        dish.setSales(sales);
        dish.setAvailable(true);
        return dish;
    }

    private Review review(User user, Dish dish, int rating, String content) {
        Review review = new Review();
        review.setUser(user);
        review.setDish(dish);
        review.setRating(rating);
        review.setContent(content);
        review.setSentiment(rating >= 4 ? "POSITIVE" : "NEUTRAL");
        return review;
    }
}
