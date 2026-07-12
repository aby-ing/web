package com.example.ordersystem.service;

import com.example.ordersystem.domain.Category;
import com.example.ordersystem.domain.Dish;
import com.example.ordersystem.dto.Dtos;
import com.example.ordersystem.repository.CategoryRepository;
import com.example.ordersystem.repository.DishRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
public class MenuService {
    private final CategoryRepository categoryRepository;
    private final DishRepository dishRepository;

    public MenuService(CategoryRepository categoryRepository, DishRepository dishRepository) {
        this.categoryRepository = categoryRepository;
        this.dishRepository = dishRepository;
    }

    @Transactional(readOnly = true)
    public List<Dtos.CategoryView> categories(boolean includeDisabled) {
        List<Category> categories = includeDisabled
                ? categoryRepository.findAllByOrderBySortOrderAscIdAsc()
                : categoryRepository.findByEnabledTrueOrderBySortOrderAscIdAsc();
        return categories.stream().map(this::toCategoryView).toList();
    }

    @Transactional(readOnly = true)
    public List<Dtos.DishView> dishes(String keyword, Long categoryId, boolean includeUnavailable) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        return dishRepository.search(normalizedKeyword, categoryId, !includeUnavailable)
                .stream()
                .map(this::toDishView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Dtos.DishView> hotDishes() {
        return dishRepository.findTop6ByAvailableTrueOrderBySalesDescIdAsc()
                .stream()
                .map(this::toDishView)
                .toList();
    }

    @Transactional
    public Dtos.CategoryView createCategory(Dtos.CategoryRequest request) {
        Category category = new Category();
        applyCategory(category, request);
        ensureUniqueCategoryName(category);
        return toCategoryView(categoryRepository.save(category));
    }

    @Transactional
    public Dtos.CategoryView updateCategory(Long id, Dtos.CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("分类不存在"));
        applyCategory(category, request);
        ensureUniqueCategoryName(category);
        if (!category.isEnabled() && dishRepository.existsByCategoryIdAndAvailableTrue(category.getId())) {
            throw new IllegalArgumentException("分类下仍有上架菜品，请先下架或移动菜品");
        }
        return toCategoryView(categoryRepository.save(category));
    }

    @Transactional
    public void disableCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("分类不存在"));
        if (dishRepository.existsByCategoryIdAndAvailableTrue(id)) {
            throw new IllegalArgumentException("分类下仍有上架菜品，请先下架或移动菜品");
        }
        category.setEnabled(false);
        categoryRepository.save(category);
    }

    @Transactional
    public Dtos.DishView createDish(Dtos.DishRequest request) {
        Dish dish = new Dish();
        applyDish(dish, request);
        return toDishView(dishRepository.save(dish));
    }

    @Transactional
    public Dtos.DishView updateDish(Long id, Dtos.DishRequest request) {
        Dish dish = dishRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("菜品不存在"));
        applyDish(dish, request);
        return toDishView(dishRepository.save(dish));
    }

    @Transactional
    public void disableDish(Long id) {
        Dish dish = dishRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("菜品不存在"));
        dish.setAvailable(false);
        dishRepository.save(dish);
    }

    public Dtos.CategoryView toCategoryView(Category category) {
        return new Dtos.CategoryView(
                category.getId(),
                category.getName(),
                category.getSortOrder(),
                category.isEnabled()
        );
    }

    public Dtos.DishView toDishView(Dish dish) {
        Category category = dish.getCategory();
        return new Dtos.DishView(
                dish.getId(),
                category.getId(),
                category.getName(),
                dish.getName(),
                dish.getDescription(),
                dish.getIngredients(),
                dish.getPrice(),
                dish.getImageUrl(),
                dish.getTasteTags(),
                dish.getStock(),
                dish.getSales(),
                dish.isAvailable()
        );
    }

    private void applyCategory(Category category, Dtos.CategoryRequest request) {
        category.setName(request.name().trim());
        category.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        category.setEnabled(request.enabled() == null || request.enabled());
    }

    private void ensureUniqueCategoryName(Category category) {
        boolean duplicated = category.getId() == null
                ? categoryRepository.existsByNameIgnoreCase(category.getName())
                : categoryRepository.existsByNameIgnoreCaseAndIdNot(category.getName(), category.getId());
        if (duplicated) {
            throw new IllegalArgumentException("分类名称已存在");
        }
    }

    private void applyDish(Dish dish, Dtos.DishRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("菜品分类不存在"));
        if (!category.isEnabled()) {
            throw new IllegalArgumentException("不能选择已停用分类");
        }
        BigDecimal price = request.price().setScale(2, java.math.RoundingMode.HALF_UP);
        dish.setCategory(category);
        dish.setName(request.name().trim());
        dish.setDescription(request.description().trim());
        dish.setIngredients(request.ingredients().trim());
        dish.setPrice(price);
        dish.setImageUrl(trimToEmpty(request.imageUrl()));
        dish.setTasteTags(trimToEmpty(request.tasteTags()));
        dish.setStock(request.stock() == null ? 100 : request.stock());
        dish.setAvailable(request.available() == null || request.available());
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
