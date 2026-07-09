package com.example.ordersystem.service;

import com.example.ordersystem.domain.DiningType;
import com.example.ordersystem.domain.Dish;
import com.example.ordersystem.domain.FoodOrder;
import com.example.ordersystem.domain.OrderItem;
import com.example.ordersystem.domain.User;
import com.example.ordersystem.dto.Dtos;
import com.example.ordersystem.repository.DishRepository;
import com.example.ordersystem.repository.FoodOrderRepository;
import com.example.ordersystem.repository.OrderItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderService {
    private static final DateTimeFormatter ORDER_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final AuthService authService;
    private final FoodOrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DishRepository dishRepository;

    public OrderService(AuthService authService,
                        FoodOrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        DishRepository dishRepository) {
        this.authService = authService;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.dishRepository = dishRepository;
    }

    @Transactional
    public Dtos.OrderView create(String token, Dtos.OrderCreateRequest request) {
        User user = authService.requireUser(token);
        Map<Long, Integer> quantityByDish = mergeQuantities(request);

        FoodOrder order = new FoodOrder();
        order.setOrderNo(generateOrderNo());
        order.setUser(user);
        order.setDiningType(request.diningType() == null ? DiningType.DINE_IN : request.diningType());
        order.setRemark(request.remark() == null ? "" : request.remark().trim());
        order.setTotalAmount(BigDecimal.ZERO);
        orderRepository.save(order);

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entry : quantityByDish.entrySet()) {
            Dish dish = dishRepository.findById(entry.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("菜品不存在：" + entry.getKey()));
            int quantity = entry.getValue();
            if (!dish.isAvailable()) {
                throw new IllegalArgumentException(dish.getName() + " 已下架");
            }
            if (dish.getStock() < quantity) {
                throw new IllegalArgumentException(dish.getName() + " 库存不足");
            }
            BigDecimal subtotal = dish.getPrice().multiply(BigDecimal.valueOf(quantity));
            total = total.add(subtotal);

            dish.setStock(dish.getStock() - quantity);
            dish.setSales(dish.getSales() + quantity);
            dishRepository.save(dish);

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setDish(dish);
            item.setDishName(dish.getName());
            item.setUnitPrice(dish.getPrice());
            item.setQuantity(quantity);
            item.setSubtotal(subtotal);
            orderItemRepository.save(item);
        }

        order.setTotalAmount(total.setScale(2, java.math.RoundingMode.HALF_UP));
        return toOrderView(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public List<Dtos.OrderView> myOrders(String token) {
        User user = authService.requireUser(token);
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toOrderView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Dtos.OrderView> adminOrders(String token) {
        authService.requireAdmin(token);
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toOrderView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Dtos.OrderView> merchantOrders(String token) {
        authService.requireMerchantOrAdmin(token);
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toOrderView)
                .toList();
    }

    @Transactional
    public Dtos.OrderView updateStatus(String token, Long orderId, Dtos.OrderStatusRequest request) {
        authService.requireAdmin(token);
        FoodOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        order.setStatus(request.status());
        return toOrderView(orderRepository.save(order));
    }

    @Transactional
    public Dtos.OrderView updateMerchantStatus(String token, Long orderId, Dtos.OrderStatusRequest request) {
        authService.requireMerchantOrAdmin(token);
        FoodOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        order.setStatus(request.status());
        return toOrderView(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public FoodOrder requireOwnOrder(Long orderId, User user) {
        return orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("订单不存在或不属于当前用户"));
    }

    public Dtos.OrderView toOrderView(FoodOrder order) {
        List<Dtos.OrderItemView> items = orderItemRepository.findByOrderId(order.getId())
                .stream()
                .map(item -> new Dtos.OrderItemView(
                        item.getDish().getId(),
                        item.getDishName(),
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getSubtotal()
                ))
                .toList();
        return new Dtos.OrderView(
                order.getId(),
                order.getOrderNo(),
                authService.toUserView(order.getUser()),
                order.getTotalAmount(),
                order.getStatus(),
                order.getDiningType(),
                order.getRemark(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                items
        );
    }

    private Map<Long, Integer> mergeQuantities(Dtos.OrderCreateRequest request) {
        Map<Long, Integer> quantityByDish = new LinkedHashMap<>();
        for (Dtos.OrderLineRequest line : request.items()) {
            quantityByDish.merge(line.dishId(), line.quantity(), Integer::sum);
        }
        return quantityByDish;
    }

    private String generateOrderNo() {
        int tail = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "OD" + LocalDateTime.now().format(ORDER_NO_FORMAT) + tail;
    }
}
