package com.example.ordersystem.service;

import com.example.ordersystem.domain.Dish;
import com.example.ordersystem.domain.FoodOrder;
import com.example.ordersystem.domain.OrderItem;
import com.example.ordersystem.domain.User;
import com.example.ordersystem.domain.Review;
import com.example.ordersystem.dto.Dtos;
import com.example.ordersystem.repository.DishRepository;
import com.example.ordersystem.repository.OrderItemRepository;
import com.example.ordersystem.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReviewService {
    private final AuthService authService;
    private final OrderService orderService;
    private final DishRepository dishRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReviewRepository reviewRepository;

    public ReviewService(AuthService authService,
                         OrderService orderService,
                         DishRepository dishRepository,
                         OrderItemRepository orderItemRepository,
                         ReviewRepository reviewRepository) {
        this.authService = authService;
        this.orderService = orderService;
        this.dishRepository = dishRepository;
        this.orderItemRepository = orderItemRepository;
        this.reviewRepository = reviewRepository;
    }

    @Transactional
    public Dtos.ReviewView create(String token, Dtos.ReviewRequest request) {
        User user = authService.requireUser(token);
        Dish dish = dishRepository.findById(request.dishId())
                .orElseThrow(() -> new IllegalArgumentException("菜品不存在"));
        FoodOrder order = null;
        if (request.orderId() != null) {
            order = orderService.requireOwnOrder(request.orderId(), user);
            boolean dishInOrder = orderItemRepository.findByOrderId(order.getId())
                    .stream()
                    .map(OrderItem::getDish)
                    .anyMatch(itemDish -> itemDish.getId().equals(dish.getId()));
            if (!dishInOrder) {
                throw new IllegalArgumentException("只能评价订单中的菜品");
            }
        }

        Review review = new Review();
        review.setUser(user);
        review.setDish(dish);
        review.setOrder(order);
        review.setRating(request.rating());
        review.setContent(request.content().trim());
        review.setSentiment(detectSentiment(request.rating(), request.content()));
        return toReviewView(reviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public List<Dtos.ReviewView> list(Long dishId) {
        List<Review> reviews = dishId == null
                ? reviewRepository.findAllByOrderByCreatedAtDesc()
                : reviewRepository.findByDishIdOrderByCreatedAtDesc(dishId);
        return reviews.stream().map(this::toReviewView).toList();
    }

    public Dtos.ReviewView toReviewView(Review review) {
        FoodOrder order = review.getOrder();
        return new Dtos.ReviewView(
                review.getId(),
                authService.toUserView(review.getUser()),
                review.getDish().getId(),
                review.getDish().getName(),
                order == null ? null : order.getId(),
                review.getRating(),
                review.getContent(),
                review.getSentiment(),
                review.getCreatedAt()
        );
    }

    private String detectSentiment(int rating, String content) {
        String text = content == null ? "" : content;
        if (rating >= 4 || containsAny(text, "好吃", "满意", "新鲜", "推荐", "喜欢", "不错")) {
            return "POSITIVE";
        }
        if (rating <= 2 || containsAny(text, "难吃", "太慢", "差", "贵", "凉", "咸", "油")) {
            return "NEGATIVE";
        }
        return "NEUTRAL";
    }

    private boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
