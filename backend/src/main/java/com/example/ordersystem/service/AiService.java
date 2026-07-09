package com.example.ordersystem.service;

import com.example.ordersystem.domain.Dish;
import com.example.ordersystem.domain.OrderItem;
import com.example.ordersystem.domain.Review;
import com.example.ordersystem.dto.Dtos;
import com.example.ordersystem.repository.DishRepository;
import com.example.ordersystem.repository.OrderItemRepository;
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
import java.util.Optional;

@Service
public class AiService {
    private static final String CUSTOMER_SERVICE_AGENT = """
            你是“校园餐厅智能客服小助手”，人设是耐心、礼貌、清楚、像真实餐厅前台。
            你只回答本校园餐厅点餐系统相关问题，包括营业时间、堂食、打包自提、订单状态、退单说明、菜品推荐、预算建议、忌口提醒。
            回复要求：
            1. 使用中文，语气自然亲切，不要机械复读。
            2. 先直接回答用户问题，再给一个可执行建议。
            3. 如果用户要真实配送、在线退款、人工赔付、修改数据库等系统未提供能力，要明确说明当前系统暂不支持，并给替代方案。
            4. 不编造不存在的优惠、配送范围、联系电话或线下承诺。
            5. 控制在 120 字以内，必要时可分点。
            """;

    private static final String BUSINESS_AGENT = """
            你是校园餐厅经营分析 Agent。请基于输入的真实系统数据给出简洁、可执行的中文建议。
            不要编造数据，不要输出 Markdown 表格。重点说明原因和下一步动作。
            """;

    private final DishRepository dishRepository;
    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final OpenAiAgentClient openAiAgentClient;

    public AiService(DishRepository dishRepository,
                     ReviewRepository reviewRepository,
                     OrderItemRepository orderItemRepository,
                     OpenAiAgentClient openAiAgentClient) {
        this.dishRepository = dishRepository;
        this.reviewRepository = reviewRepository;
        this.orderItemRepository = orderItemRepository;
        this.openAiAgentClient = openAiAgentClient;
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

        BigDecimal finalTotal = total.setScale(2, RoundingMode.HALF_UP);
        String summary = items.isEmpty()
                ? "当前没有可推荐菜品，请先检查菜品库存或上架状态。"
                : "已按 " + people + " 人、约 " + perPersonBudget + " 元/人的预算生成组合，预计合计 " + finalTotal + " 元。";

        Optional<String> agentSummary = openAiAgentClient.ask(BUSINESS_AGENT,
                "请为点餐推荐生成一句自然中文总结。\n"
                        + "用户需求：人数=" + people + "，人均预算=" + perPersonBudget + "，口味=" + emptyToNone(request.taste())
                        + "，忌口=" + emptyToNone(request.avoid()) + "\n"
                        + "推荐结果：" + items + "\n"
                        + "预计总价：" + finalTotal);
        return new Dtos.AiRecommendResult(agentSummary.orElse(summary), finalTotal, items);
    }

    public Dtos.AiTextResult generateDescription(Dtos.AiDescriptionRequest request) {
        String ingredients = StringUtils.hasText(request.ingredients()) ? request.ingredients().trim() : "精选食材";
        String tags = StringUtils.hasText(request.tasteTags()) ? request.tasteTags().trim() : "家常口味";
        Optional<String> agentText = openAiAgentClient.ask(
                "你是校园餐厅菜单文案 Agent。根据菜名、食材、口味标签生成 60-100 字中文菜品介绍，语气真实、有食欲，不夸大功效。",
                "菜名：" + request.name().trim() + "\n食材：" + ingredients + "\n口味标签：" + tags
        );
        String fallback = request.name().trim() + "选用" + ingredients + "，突出" + tags
                + "的风味，适合校园餐厅快餐场景。出餐稳定、口味清晰，可作为午餐或晚餐的高频选择。";
        return new Dtos.AiTextResult(agentText.orElse(fallback));
    }

    @Transactional(readOnly = true)
    public Dtos.AiTextResult customerService(Dtos.CustomerServiceRequest request) {
        String query = request.query().trim();
        String context = buildCustomerServiceContext();
        Optional<String> agentAnswer = openAiAgentClient.ask(CUSTOMER_SERVICE_AGENT,
                context + "\n\n用户问题：" + query + "\n请以客服身份回复。");
        return new Dtos.AiTextResult(agentAnswer.orElse(localCustomerService(query)));
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
        String fallback = issues.isEmpty()
                ? "当前评价整体稳定，可继续保持出餐速度和菜品口味。"
                : "建议优先关注：" + String.join("、", issues) + "。";
        String suggestion = openAiAgentClient.ask(BUSINESS_AGENT,
                "评价总数：" + reviews.size()
                        + "\n平均评分：" + Math.round(average * 10.0) / 10.0
                        + "\n情感统计：" + sentimentCount
                        + "\n热点问题：" + issues
                        + "\n请输出一句经营建议。").orElse(fallback);
        return new Dtos.AiReviewAnalysis(
                reviews.size(),
                Math.round(average * 10.0) / 10.0,
                sentimentCount,
                issues,
                suggestion
        );
    }

    @Transactional(readOnly = true)
    public Dtos.AiHotDishAnalysis analyzeHotDishes() {
        List<OrderItem> items = orderItemRepository.findAll();
        if (items.isEmpty()) {
            return new Dtos.AiHotDishAnalysis(
                    0,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    List.of(),
                    "暂无订单数据，建议先完成几笔测试订单，再查看热门菜品和经营建议。"
            );
        }

        Map<Long, SalesStat> stats = new LinkedHashMap<>();
        for (OrderItem item : items) {
            Long dishId = item.getDish().getId();
            SalesStat stat = stats.computeIfAbsent(dishId, id -> new SalesStat(dishId, item.getDishName()));
            stat.quantity += item.getQuantity();
            stat.revenue = stat.revenue.add(item.getSubtotal());
        }

        long totalQuantity = stats.values().stream().mapToLong(stat -> stat.quantity).sum();
        BigDecimal totalRevenue = stats.values().stream()
                .map(stat -> stat.revenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        List<Dtos.AiHotDishItem> topDishes = stats.values().stream()
                .sorted(Comparator.comparingLong(SalesStat::quantity).reversed()
                        .thenComparing(SalesStat::revenue, Comparator.reverseOrder()))
                .limit(5)
                .map(stat -> new Dtos.AiHotDishItem(
                        stat.dishId,
                        stat.dishName,
                        stat.quantity,
                        stat.revenue.setScale(2, RoundingMode.HALF_UP),
                        Math.round((stat.quantity * 1000.0 / Math.max(totalQuantity, 1))) / 10.0
                ))
                .toList();

        String fallback = buildHotDishSuggestion(topDishes, totalQuantity);
        String suggestion = openAiAgentClient.ask(BUSINESS_AGENT,
                "总销量：" + totalQuantity
                        + "\n总销售额：" + totalRevenue
                        + "\n热门菜品：" + topDishes
                        + "\n请根据热门菜品给出一句经营建议。").orElse(fallback);
        return new Dtos.AiHotDishAnalysis(totalQuantity, totalRevenue, topDishes, suggestion);
    }

    private String buildCustomerServiceContext() {
        List<Dish> hotDishes = dishRepository.findTop6ByAvailableTrueOrderBySalesDescIdAsc();
        String menu = hotDishes.stream()
                .map(dish -> dish.getName() + "，价格" + dish.getPrice() + "元，库存" + dish.getStock()
                        + "，标签：" + emptyToNone(dish.getTasteTags()))
                .toList()
                .toString();
        return """
                餐厅业务规则：
                - 营业时间：07:00-21:00。
                - 支持堂食和打包自提；当前系统不接入真实骑手配送。
                - 订单提交后可在“订单”页面查看状态；如需取消，需联系管理员处理。
                - 已完成订单建议通过评价反馈问题。
                - 可引导用户使用 AI 点餐助手填写口味、预算、人数、忌口。
                当前热门/可推荐菜品：
                """ + menu;
    }

    private String localCustomerService(String query) {
        if (containsAny(query, "营业", "几点", "时间")) {
            return "您好，餐厅营业时间是 07:00-21:00。午餐和晚餐高峰建议提前 10-15 分钟下单，取餐会更顺畅。";
        }
        if (containsAny(query, "配送", "外卖", "打包", "自提")) {
            return "您好，系统支持堂食和打包自提；当前版本暂不接入真实骑手配送。下单时可以选择用餐方式。";
        }
        if (containsAny(query, "退", "取消", "退款")) {
            return "您好，订单提交后如需取消，建议尽快联系管理员处理；如果订单已完成，可以通过评价反馈具体问题。";
        }
        if (containsAny(query, "辣", "清淡", "推荐", "预算", "吃什么")) {
            return "可以告诉我口味、预算、人数和忌口，我会结合库存、价格和热销菜帮你推荐；也可以直接使用 AI 点餐助手。";
        }
        return "您好，我可以帮您解答营业时间、打包自提、退单说明和菜品推荐问题。您也可以补充预算、人数或口味，我会更准确地建议。";
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
        return new DishScore(dish, score, reasons.isEmpty() ? "综合价格、库存和销量推荐" : String.join("；", reasons));
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

    private String buildHotDishSuggestion(List<Dtos.AiHotDishItem> topDishes, long totalQuantity) {
        if (topDishes.isEmpty()) {
            return "暂无热门菜品数据，建议通过促销或套餐引导用户下单，积累订单样本。";
        }
        Dtos.AiHotDishItem first = topDishes.get(0);
        if (first.quantityShare() >= 45) {
            return "当前销量集中在“" + first.dishName() + "”，建议保证备货和出餐效率，同时搭配饮品或小食做套餐，提升客单价。";
        }
        if (topDishes.size() >= 3) {
            return "热门菜品分布较均衡，可将“" + first.dishName() + "”设为首页推荐，并观察低销量菜品是否需要优化图片、价格或描述。";
        }
        return "订单总销量为 " + totalQuantity + " 份，建议继续积累数据，并围绕高销量菜品设计午餐高峰套餐。";
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

    private String emptyToNone(String value) {
        return StringUtils.hasText(value) ? value.trim() : "无";
    }

    private record DishScore(Dish dish, int score, String reason) {
    }

    private static final class SalesStat {
        private final Long dishId;
        private final String dishName;
        private long quantity;
        private BigDecimal revenue = BigDecimal.ZERO;

        private SalesStat(Long dishId, String dishName) {
            this.dishId = dishId;
            this.dishName = dishName;
        }

        private long quantity() {
            return quantity;
        }

        private BigDecimal revenue() {
            return revenue;
        }
    }
}
