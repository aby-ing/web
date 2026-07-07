package com.example.ordersystem.service;

import com.example.ordersystem.domain.Dish;
import com.example.ordersystem.domain.Review;
import com.example.ordersystem.dto.Dtos;
import com.example.ordersystem.repository.DishRepository;
import com.example.ordersystem.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AiService {
    private final DishRepository dishRepository;
    private final ReviewRepository reviewRepository;

    public AiService(DishRepository dishRepository, ReviewRepository reviewRepository) {
        this.dishRepository = dishRepository;
        this.reviewRepository = reviewRepository;
    }

    @Transactional(readOnly = true)
    public Dtos.AiRecommendResult recommend(Dtos.AiRecommendRequest request) {
        int people = request.people() == null ? 1 : request.people();
        BigDecimal perPersonBudget = request.budget() == null ? BigDecimal.valueOf(30) : request.budget();
        BigDecimal totalBudget = perPersonBudget.multiply(BigDecimal.valueOf(people)).setScale(2, RoundingMode.HALF_UP);
        String taste = normalize(request.taste());
        String avoid = normalize(request.avoid());

        List<DishScore> candidates = dishRepository.search(null, null, true).stream()
                .filter(dish -> dish.getStock() > 0)
                .map(dish -> scoreDish(dish, taste, avoid, perPersonBudget))
                .filter(score -> score.score() > -500)
                .sorted(Comparator.comparingInt(DishScore::score).reversed()
                        .thenComparing(score -> score.dish().getPrice()))
                .toList();

        List<Dtos.AiRecommendItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        int maxItems = Math.min(Math.max(people + 1, 2), 6);
        for (DishScore candidate : candidates) {
            Dish dish = candidate.dish();
            BigDecimal nextTotal = total.add(dish.getPrice());
            if (items.size() >= maxItems) {
                break;
            }
            if (nextTotal.compareTo(totalBudget) <= 0 || items.isEmpty()) {
                total = nextTotal;
                items.add(new Dtos.AiRecommendItem(
                        dish.getId(),
                        dish.getName(),
                        dish.getPrice(),
                        candidate.reason()
                ));
            }
        }

        String summary = "已按 " + people + " 人、约 " + perPersonBudget + " 元/人的预算生成组合，预计合计 "
                + total.setScale(2, RoundingMode.HALF_UP) + " 元。";
        if (items.isEmpty()) {
            summary = "当前没有可推荐菜品，请先检查菜品库存或上架状态。";
        }
        return new Dtos.AiRecommendResult(summary, total.setScale(2, RoundingMode.HALF_UP), items);
    }

    public Dtos.AiTextResult generateDescription(Dtos.AiDescriptionRequest request) {
        String ingredients = StringUtils.hasText(request.ingredients()) ? request.ingredients().trim() : "精选食材";
        String tags = StringUtils.hasText(request.tasteTags()) ? request.tasteTags().trim() : "家常口味";
        String text = request.name().trim() + "选用" + ingredients + "，突出" + tags
                + "的风味，适合校园餐厅快餐场景。出餐稳定、口味清晰，可作为午餐或晚餐的高频选择。";
        return new Dtos.AiTextResult(text);
    }

    public Dtos.AiTextResult customerService(Dtos.CustomerServiceRequest request) {
        String query = request.query();
        if (containsAny(query, "营业", "几点", "时间")) {
            return new Dtos.AiTextResult("餐厅营业时间为 07:00-21:00，午餐和晚餐高峰建议提前 10-15 分钟下单。");
        }
        if (containsAny(query, "配送", "外卖", "打包", "自提")) {
            return new Dtos.AiTextResult("系统支持堂食和打包自提。本版本为模拟点餐系统，暂不接入真实骑手配送。");
        }
        if (containsAny(query, "退", "取消", "退款")) {
            return new Dtos.AiTextResult("订单提交后可联系管理员取消；若订单已完成，则建议通过评价反馈问题。");
        }
        if (containsAny(query, "辣", "清淡", "推荐", "预算")) {
            return new Dtos.AiTextResult("可以在 AI 点餐助手中填写口味、预算和人数，系统会结合菜品标签、价格和销量给出组合。");
        }
        return new Dtos.AiTextResult("已收到问题。当前模拟客服可回答营业时间、打包自提、退单和点餐推荐相关问题。");
    }

    @Transactional(readOnly = true)
    public Dtos.AiReviewAnalysis analyzeReviews() {
        List<Review> reviews = reviewRepository.findAllByOrderByCreatedAtDesc();
        Map<String, Long> sentimentCount = new LinkedHashMap<>();
        sentimentCount.put("POSITIVE", reviews.stream().filter(r -> "POSITIVE".equals(r.getSentiment())).count());
        sentimentCount.put("NEUTRAL", reviews.stream().filter(r -> "NEUTRAL".equals(r.getSentiment())).count());
        sentimentCount.put("NEGATIVE", reviews.stream().filter(r -> "NEGATIVE".equals(r.getSentiment())).count());

        double average = reviews.stream().mapToInt(Review::getRating).average().orElse(0);
        List<String> issues = detectHotIssues(reviews);
        String suggestion = issues.isEmpty()
                ? "当前评价整体稳定，可继续保持出餐速度和菜品口味。"
                : "建议优先关注：" + String.join("、", issues) + "。";
        return new Dtos.AiReviewAnalysis(
                reviews.size(),
                Math.round(average * 10.0) / 10.0,
                sentimentCount,
                issues,
                suggestion
        );
    }

    private DishScore scoreDish(Dish dish, String taste, String avoid, BigDecimal perPersonBudget) {
        String text = normalize(dish.getName() + " " + dish.getDescription() + " " + dish.getIngredients() + " " + dish.getTasteTags());
        if (StringUtils.hasText(avoid) && text.contains(avoid)) {
            return new DishScore(dish, -1000, "已避开忌口项");
        }

        int score = dish.getSales();
        List<String> reasons = new ArrayList<>();
        if (StringUtils.hasText(taste) && text.contains(taste)) {
            score += 40;
            reasons.add("匹配口味");
        }
        if (dish.getPrice().compareTo(perPersonBudget) <= 0) {
            score += 20;
            reasons.add("符合预算");
        }
        if (dish.getSales() >= 20) {
            score += 10;
            reasons.add("销量较高");
        }
        if (StringUtils.hasText(dish.getTasteTags())) {
            reasons.add("标签：" + dish.getTasteTags());
        }
        return new DishScore(dish, score, reasons.isEmpty() ? "综合价格、库存和销量推荐" : String.join("，", reasons));
    }

    private List<String> detectHotIssues(List<Review> reviews) {
        Map<String, Long> count = new LinkedHashMap<>();
        count.put("出餐速度", reviews.stream().filter(r -> containsAny(r.getContent(), "慢", "等", "久")).count());
        count.put("菜品温度", reviews.stream().filter(r -> containsAny(r.getContent(), "凉", "冷")).count());
        count.put("口味稳定", reviews.stream().filter(r -> containsAny(r.getContent(), "咸", "淡", "油", "辣")).count());
        count.put("价格感知", reviews.stream().filter(r -> containsAny(r.getContent(), "贵", "价格")).count());
        return count.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .limit(3)
                .toList();
    }

    private boolean containsAny(String text, String... words) {
        String value = text == null ? "" : text;
        for (String word : words) {
            if (value.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record DishScore(Dish dish, int score, String reason) {
    }
}
