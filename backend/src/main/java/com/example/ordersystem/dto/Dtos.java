package com.example.ordersystem.dto;

import com.example.ordersystem.domain.DiningType;
import com.example.ordersystem.domain.OrderStatus;
import com.example.ordersystem.domain.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class Dtos {
    private Dtos() {
    }

    public record RegisterRequest(
            @NotBlank(message = "用户名不能为空")
            @Size(min = 3, max = 30, message = "用户名长度应为 3-30 位")
            String username,
            @NotBlank(message = "密码不能为空")
            @Size(min = 6, max = 50, message = "密码长度应为 6-50 位")
            String password,
            @NotBlank(message = "昵称不能为空")
            @Size(max = 50, message = "昵称不能超过 50 位")
            String nickname,
            @Size(max = 30, message = "手机号不能超过 30 位")
            String phone
    ) {
    }

    public record LoginRequest(
            @NotBlank(message = "用户名不能为空")
            String username,
            @NotBlank(message = "密码不能为空")
            String password
    ) {
    }

    public record UserView(
            Long id,
            String username,
            String nickname,
            String phone,
            Role role
    ) {
    }

    public record AuthResult(
            String token,
            UserView user
    ) {
    }

    public record CategoryRequest(
            @NotBlank(message = "分类名称不能为空")
            @Size(max = 60, message = "分类名称不能超过 60 位")
            String name,
            Integer sortOrder,
            Boolean enabled
    ) {
    }

    public record CategoryView(
            Long id,
            String name,
            Integer sortOrder,
            boolean enabled
    ) {
    }

    public record DishRequest(
            @NotNull(message = "菜品分类不能为空")
            Long categoryId,
            @NotBlank(message = "菜品名称不能为空")
            @Size(max = 80, message = "菜品名称不能超过 80 位")
            String name,
            @NotBlank(message = "菜品描述不能为空")
            @Size(max = 500, message = "菜品描述不能超过 500 位")
            String description,
            @NotBlank(message = "主要食材不能为空")
            @Size(max = 200, message = "食材不能超过 200 位")
            String ingredients,
            @NotNull(message = "价格不能为空")
            @DecimalMin(value = "0.01", message = "价格必须大于 0")
            BigDecimal price,
            @Size(max = 500, message = "图片地址不能超过 500 位")
            String imageUrl,
            @Size(max = 120, message = "口味标签不能超过 120 位")
            String tasteTags,
            @Min(value = 0, message = "库存不能小于 0")
            Integer stock,
            Boolean available
    ) {
    }

    public record DishView(
            Long id,
            Long categoryId,
            String categoryName,
            String name,
            String description,
            String ingredients,
            BigDecimal price,
            String imageUrl,
            String tasteTags,
            Integer stock,
            Integer sales,
            boolean available
    ) {
    }

    public record OrderLineRequest(
            @NotNull(message = "菜品不能为空")
            Long dishId,
            @NotNull(message = "数量不能为空")
            @Positive(message = "数量必须大于 0")
            Integer quantity
    ) {
    }

    public record OrderCreateRequest(
            @NotEmpty(message = "点餐车不能为空")
            List<@Valid OrderLineRequest> items,
            DiningType diningType,
            @Size(max = 300, message = "备注不能超过 300 位")
            String remark
    ) {
    }

    public record OrderStatusRequest(
            @NotNull(message = "订单状态不能为空")
            OrderStatus status
    ) {
    }

    public record OrderItemView(
            Long dishId,
            String dishName,
            BigDecimal unitPrice,
            Integer quantity,
            BigDecimal subtotal
    ) {
    }

    public record OrderView(
            Long id,
            String orderNo,
            UserView user,
            BigDecimal totalAmount,
            OrderStatus status,
            DiningType diningType,
            String remark,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<OrderItemView> items
    ) {
    }

    public record ReviewRequest(
            @NotNull(message = "菜品不能为空")
            Long dishId,
            Long orderId,
            @NotNull(message = "评分不能为空")
            @Min(value = 1, message = "评分最低为 1")
            @Max(value = 5, message = "评分最高为 5")
            Integer rating,
            @NotBlank(message = "评价内容不能为空")
            @Size(max = 500, message = "评价内容不能超过 500 位")
            String content
    ) {
    }

    public record ReviewView(
            Long id,
            UserView user,
            Long dishId,
            String dishName,
            Long orderId,
            Integer rating,
            String content,
            String sentiment,
            LocalDateTime createdAt
    ) {
    }

    public record AiRecommendRequest(
            @Size(max = 120, message = "口味描述不能超过 120 位")
            String taste,
            @DecimalMin(value = "0.01", message = "预算必须大于 0")
            BigDecimal budget,
            @Min(value = 1, message = "人数至少为 1")
            @Max(value = 20, message = "人数不能超过 20")
            Integer people,
            @Size(max = 120, message = "忌口说明不能超过 120 位")
            String avoid
    ) {
    }

    public record AiRecommendItem(
            Long dishId,
            String dishName,
            BigDecimal price,
            String reason
    ) {
    }

    public record AiRecommendResult(
            String summary,
            BigDecimal estimatedTotal,
            List<AiRecommendItem> items
    ) {
    }

    public record AiDescriptionRequest(
            @NotBlank(message = "菜品名称不能为空")
            String name,
            @Size(max = 200, message = "食材不能超过 200 位")
            String ingredients,
            @Size(max = 120, message = "口味标签不能超过 120 位")
            String tasteTags
    ) {
    }

    public record CustomerServiceRequest(
            @NotBlank(message = "问题不能为空")
            @Size(max = 200, message = "问题不能超过 200 位")
            String query
    ) {
    }

    public record AiTextResult(String text) {
    }

    public record AiReviewAnalysis(
            long totalReviews,
            double averageRating,
            Map<String, Long> sentimentCount,
            List<String> hotIssues,
            String suggestion
    ) {
    }

    public record AiHotDishItem(
            Long dishId,
            String dishName,
            long quantity,
            BigDecimal revenue,
            double quantityShare
    ) {
    }

    public record AiHotDishAnalysis(
            long totalQuantity,
            BigDecimal totalRevenue,
            List<AiHotDishItem> topDishes,
            String suggestion
    ) {
    }
}
