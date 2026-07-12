package com.example.ordersystem.controller;

import com.example.ordersystem.common.ApiResponse;
import com.example.ordersystem.dto.Dtos;
import com.example.ordersystem.service.AuthService;
import com.example.ordersystem.service.MenuService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MenuController {
    private final MenuService menuService;
    private final AuthService authService;

    public MenuController(MenuService menuService, AuthService authService) {
        this.menuService = menuService;
        this.authService = authService;
    }

    @GetMapping("/categories")
    public ApiResponse<List<Dtos.CategoryView>> categories() {
        return ApiResponse.ok(menuService.categories(false));
    }

    @GetMapping("/dishes")
    public ApiResponse<List<Dtos.DishView>> dishes(@RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) Long categoryId) {
        return ApiResponse.ok(menuService.dishes(keyword, categoryId, false));
    }

    @GetMapping("/dishes/hot")
    public ApiResponse<List<Dtos.DishView>> hotDishes() {
        return ApiResponse.ok(menuService.hotDishes());
    }

    @GetMapping("/admin/categories")
    public ApiResponse<List<Dtos.CategoryView>> adminCategories(
            @RequestHeader(value = "X-Auth-Token", required = false) String token) {
        authService.requireAdmin(token);
        return ApiResponse.ok(menuService.categories(true));
    }

    @PostMapping("/admin/categories")
    public ApiResponse<Dtos.CategoryView> createCategory(
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @Valid @RequestBody Dtos.CategoryRequest request) {
        authService.requireAdmin(token);
        return ApiResponse.ok("分类已创建", menuService.createCategory(request));
    }

    @PutMapping("/admin/categories/{id}")
    public ApiResponse<Dtos.CategoryView> updateCategory(
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @PathVariable Long id,
            @Valid @RequestBody Dtos.CategoryRequest request) {
        authService.requireAdmin(token);
        return ApiResponse.ok("分类已更新", menuService.updateCategory(id, request));
    }

    @DeleteMapping("/admin/categories/{id}")
    public ApiResponse<Void> deleteCategory(
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @PathVariable Long id) {
        authService.requireAdmin(token);
        menuService.disableCategory(id);
        return ApiResponse.ok("分类已停用", null);
    }

    @GetMapping("/admin/dishes")
    public ApiResponse<List<Dtos.DishView>> adminDishes(
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId) {
        authService.requireAdmin(token);
        return ApiResponse.ok(menuService.dishes(keyword, categoryId, true));
    }

    @PostMapping("/admin/dishes")
    public ApiResponse<Dtos.DishView> createDish(
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @Valid @RequestBody Dtos.DishRequest request) {
        authService.requireAdmin(token);
        return ApiResponse.ok("菜品已创建", menuService.createDish(request));
    }

    @PutMapping("/admin/dishes/{id}")
    public ApiResponse<Dtos.DishView> updateDish(
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @PathVariable Long id,
            @Valid @RequestBody Dtos.DishRequest request) {
        authService.requireAdmin(token);
        return ApiResponse.ok("菜品已更新", menuService.updateDish(id, request));
    }

    @DeleteMapping("/admin/dishes/{id}")
    public ApiResponse<Void> deleteDish(
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @PathVariable Long id) {
        authService.requireAdmin(token);
        menuService.disableDish(id);
        return ApiResponse.ok("菜品已下架", null);
    }
    @GetMapping("/merchant/categories")
    public ApiResponse<List<Dtos.CategoryView>> merchantCategories(
            @RequestHeader(value = "X-Auth-Token", required = false) String token) {
        authService.requireMerchant(token);
        return ApiResponse.ok(menuService.categories(true));
    }

    @PostMapping("/merchant/categories")
    public ApiResponse<Dtos.CategoryView> createMerchantCategory(
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @Valid @RequestBody Dtos.CategoryRequest request) {
        authService.requireMerchant(token);
        return ApiResponse.ok("分类已创建", menuService.createCategory(request));
    }

    @PutMapping("/merchant/categories/{id}")
    public ApiResponse<Dtos.CategoryView> updateMerchantCategory(
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @PathVariable Long id,
            @Valid @RequestBody Dtos.CategoryRequest request) {
        authService.requireMerchant(token);
        return ApiResponse.ok("分类已更新", menuService.updateCategory(id, request));
    }

    @DeleteMapping("/merchant/categories/{id}")
    public ApiResponse<Void> deleteMerchantCategory(
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @PathVariable Long id) {
        authService.requireMerchant(token);
        menuService.disableCategory(id);
        return ApiResponse.ok("分类已停用", null);
    }

    @GetMapping("/merchant/dishes")
    public ApiResponse<List<Dtos.DishView>> merchantDishes(
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId) {
        authService.requireMerchant(token);
        return ApiResponse.ok(menuService.dishes(keyword, categoryId, true));
    }

    @PostMapping("/merchant/dishes")
    public ApiResponse<Dtos.DishView> createMerchantDish(
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @Valid @RequestBody Dtos.DishRequest request) {
        authService.requireMerchant(token);
        return ApiResponse.ok("菜品已创建", menuService.createDish(request));
    }

    @PutMapping("/merchant/dishes/{id}")
    public ApiResponse<Dtos.DishView> updateMerchantDish(
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @PathVariable Long id,
            @Valid @RequestBody Dtos.DishRequest request) {
        authService.requireMerchant(token);
        return ApiResponse.ok("菜品已更新", menuService.updateDish(id, request));
    }

    @DeleteMapping("/merchant/dishes/{id}")
    public ApiResponse<Void> deleteMerchantDish(
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @PathVariable Long id) {
        authService.requireMerchant(token);
        menuService.disableDish(id);
        return ApiResponse.ok("菜品已下架", null);
    }
}
