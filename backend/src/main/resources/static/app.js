const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => Array.from(document.querySelectorAll(selector));

const state = {
    token: localStorage.getItem("orderSystemToken") || "",
    user: null,
    categories: [],
    dishes: [],
    selectedCategoryId: "",
    cart: loadCart(),
    orders: [],
    reviews: [],
    adminCategories: [],
    adminDishes: [],
    adminOrders: [],
    editingCategoryId: null,
    editingDishId: null
};

const statusLabels = {
    SUBMITTED: "已提交",
    PREPARING: "制作中",
    READY: "可取餐",
    COMPLETED: "已完成",
    CANCELED: "已取消"
};

const diningLabels = {
    DINE_IN: "堂食",
    TAKE_OUT: "打包自提"
};

document.addEventListener("DOMContentLoaded", init);

async function init() {
    bindGlobalEvents();
    await restoreSession();

    const page = document.body.dataset.page || "";
    const requiresAuth = document.body.dataset.auth === "true";
    const requiresAdmin = document.body.dataset.admin === "true";

    if (requiresAuth && !state.user) {
        location.replace("/login.html");
        return;
    }
    if (requiresAdmin && (!state.user || state.user.role !== "ADMIN")) {
        location.replace("/order.html");
        return;
    }

    refreshAuthUi();

    if (page === "login") {
        initLoginPage();
        return;
    }
    if (page === "order") {
        await initOrderPage();
    }
    if (page === "orders") {
        await initOrdersPage();
    }
    if (page === "reviews") {
        await initReviewsPage();
    }
    if (page === "ai") {
        await initAiPage();
    }
    if (page === "admin") {
        await initAdminPage();
    }
}

function bindGlobalEvents() {
    const logoutBtn = $("#logoutBtn");
    if (logoutBtn) {
        logoutBtn.addEventListener("click", handleLogout);
    }
    const shutdownBtn = $("#shutdownBtn");
    if (shutdownBtn) {
        shutdownBtn.addEventListener("click", handleShutdown);
    }
    document.addEventListener("click", handleAction);
}

function initLoginPage() {
    if (state.user) {
        location.replace("/order.html");
        return;
    }
    $("#loginForm").addEventListener("submit", handleLogin);
    $("#registerForm").addEventListener("submit", handleRegister);
}

async function initOrderPage() {
    $("#searchInput").addEventListener("input", debounce(loadDishes, 250));
    $("#clearCartBtn").addEventListener("click", () => {
        state.cart = [];
        saveCart();
        renderCart();
    });
    $("#submitOrderBtn").addEventListener("click", submitOrder);
    await loadMenu();
    renderCart();
}

async function initOrdersPage() {
    $("#refreshOrdersBtn").addEventListener("click", () => loadOrders(true));
    await loadOrders(false);
}

async function initReviewsPage() {
    $("#reviewForm").addEventListener("submit", submitReview);
    $("#refreshReviewsBtn").addEventListener("click", loadReviews);
    await loadDishes();
    await loadOrders(false);
    await loadReviews();
}

async function initAiPage() {
    $("#aiRecommendForm").addEventListener("submit", submitAiRecommend);
    $("#serviceForm").addEventListener("submit", submitServiceQuestion);
    await loadDishes();
}

async function initAdminPage() {
    $("#categoryForm").addEventListener("submit", saveCategory);
    $("#categoryResetBtn").addEventListener("click", resetCategoryForm);
    $("#dishForm").addEventListener("submit", saveDish);
    $("#dishResetBtn").addEventListener("click", resetDishForm);
    $("#generateDescBtn").addEventListener("click", generateDishDescription);
    $("#refreshAdminOrdersBtn").addEventListener("click", loadAdminOrders);
    $("#refreshAnalysisBtn").addEventListener("click", loadAnalysis);
    await loadAdmin();
}

async function api(path, options = {}) {
    const headers = options.headers || {};
    if (options.body) {
        headers["Content-Type"] = "application/json";
    }
    if (state.token) {
        headers["X-Auth-Token"] = state.token;
    }
    const response = await fetch(path, { ...options, headers });
    const payload = await response.json().catch(() => ({ success: false, message: "接口返回格式错误" }));
    if (!response.ok || !payload.success) {
        throw new Error(payload.message || "请求失败");
    }
    return payload.data;
}

async function restoreSession() {
    if (!state.token) {
        return;
    }
    try {
        state.user = await api("/api/auth/me");
    } catch (error) {
        clearSession();
    }
}

function saveSession(authResult) {
    state.token = authResult.token;
    state.user = authResult.user;
    localStorage.setItem("orderSystemToken", state.token);
}

function clearSession() {
    state.token = "";
    state.user = null;
    localStorage.removeItem("orderSystemToken");
}

function refreshAuthUi() {
    const chip = $("#userChip");
    const isAdmin = state.user && state.user.role === "ADMIN";
    if (chip) {
        chip.textContent = state.user
            ? `${state.user.nickname} · ${state.user.role === "ADMIN" ? "管理员" : "用户"}`
            : "未登录";
    }
    $$(".admin-only").forEach((element) => {
        element.hidden = !isAdmin;
    });
}

async function handleLogin(event) {
    event.preventDefault();
    try {
        const result = await api("/api/auth/login", {
            method: "POST",
            body: JSON.stringify({
                username: $("#loginUsername").value,
                password: $("#loginPassword").value
            })
        });
        saveSession(result);
        showToast("登录成功");
        window.setTimeout(() => location.href = "/order.html", 400);
    } catch (error) {
        showToast(error.message);
    }
}

async function handleRegister(event) {
    event.preventDefault();
    try {
        const result = await api("/api/auth/register", {
            method: "POST",
            body: JSON.stringify({
                username: $("#registerUsername").value,
                password: $("#registerPassword").value,
                nickname: $("#registerNickname").value,
                phone: $("#registerPhone").value
            })
        });
        saveSession(result);
        showToast("注册并登录成功");
        window.setTimeout(() => location.href = "/order.html", 400);
    } catch (error) {
        showToast(error.message);
    }
}

async function handleLogout() {
    try {
        if (state.token) {
            await api("/api/auth/logout", { method: "POST" });
        }
    } catch (error) {
        console.warn(error);
    }
    clearSession();
    showToast("已退出登录");
    window.setTimeout(() => location.href = "/login.html", 300);
}

async function handleShutdown() {
    if (!window.confirm("确定要停止本地服务吗？停止后网页会无法继续访问，需要重新运行 run.ps1 启动。")) {
        return;
    }
    try {
        await api("/api/system/shutdown", { method: "POST" });
        showToast("服务正在停止");
        window.setTimeout(() => {
            document.body.innerHTML = `
                <main class="login-page">
                    <section class="login-card">
                        <h1>服务已停止</h1>
                        <p>如需继续使用，请在 PowerShell 中重新运行：</p>
                        <pre>cd E:\\web\\campus-restaurant-order-system\n.\\run.ps1</pre>
                    </section>
                </main>
            `;
        }, 700);
    } catch (error) {
        showToast(error.message);
    }
}

async function loadMenu() {
    await Promise.all([loadCategories(), loadDishes()]);
}

async function loadCategories() {
    state.categories = await api("/api/categories");
    renderCategoryFilters();
}

async function loadDishes() {
    const searchInput = $("#searchInput");
    const keyword = searchInput ? searchInput.value.trim() : "";
    const params = new URLSearchParams();
    if (keyword) {
        params.set("keyword", keyword);
    }
    if (state.selectedCategoryId) {
        params.set("categoryId", state.selectedCategoryId);
    }
    state.dishes = await api(`/api/dishes${params.toString() ? "?" + params.toString() : ""}`);
    renderDishes();
    renderDishSelects();
}

function renderCategoryFilters() {
    const container = $("#categoryFilters");
    if (!container) {
        return;
    }
    const activeAll = state.selectedCategoryId === "";
    container.innerHTML = [
        `<button type="button" class="category-chip ${activeAll ? "active" : ""}" data-action="select-category" data-id="">全部</button>`,
        ...state.categories.map((category) => `
            <button type="button" class="category-chip ${String(category.id) === String(state.selectedCategoryId) ? "active" : ""}"
                    data-action="select-category" data-id="${category.id}">
                ${escapeHtml(category.name)}
            </button>
        `)
    ].join("");
}

function renderDishes() {
    const grid = $("#dishGrid");
    if (!grid) {
        return;
    }
    const empty = $("#emptyDishes");
    if (empty) {
        empty.hidden = state.dishes.length > 0;
    }
    grid.innerHTML = state.dishes.map((dish) => `
        <article class="dish-card">
            <img src="${escapeAttr(dish.imageUrl || "/assets/default-dish.png")}" alt="${escapeAttr(dish.name)}"
                 onerror="this.src='/assets/default-dish.png'">
            <div class="dish-body">
                <div class="dish-title">
                    <strong>${escapeHtml(dish.name)}</strong>
                    <span class="price">${money(dish.price)}</span>
                </div>
                <p class="dish-desc">${escapeHtml(dish.description)}</p>
                <div class="tags">${escapeHtml(dish.tasteTags || "家常")}</div>
                <div class="dish-meta"><span>库存 ${dish.stock}</span><span>销量 ${dish.sales}</span></div>
                <button class="primary-btn" type="button" data-action="add-cart" data-id="${dish.id}" ${dish.stock <= 0 ? "disabled" : ""}>加入点餐车</button>
            </div>
        </article>
    `).join("");
}

function addToCart(dishId) {
    const dish = state.dishes.find((item) => String(item.id) === String(dishId))
        || state.adminDishes.find((item) => String(item.id) === String(dishId));
    if (!dish) {
        showToast("菜品不存在，请回到点餐页重试");
        return;
    }
    const item = state.cart.find((cartItem) => String(cartItem.dish.id) === String(dish.id));
    if (item) {
        if (item.quantity >= dish.stock) {
            showToast("库存不足");
            return;
        }
        item.quantity += 1;
    } else {
        state.cart.push({ dish, quantity: 1 });
    }
    saveCart();
    renderCart();
    showToast("已加入点餐车");
}

function renderCart() {
    const list = $("#cartItems");
    const total = $("#cartTotal");
    if (!list || !total) {
        return;
    }
    if (state.cart.length === 0) {
        list.innerHTML = `<div class="empty-state">点餐车为空</div>`;
    } else {
        list.innerHTML = state.cart.map((item) => `
            <div class="cart-item">
                <div><strong>${escapeHtml(item.dish.name)}</strong><p>${money(item.dish.price)} × ${item.quantity}</p></div>
                <div class="qty-control">
                    <button type="button" data-action="cart-dec" data-id="${item.dish.id}">-</button>
                    <span>${item.quantity}</span>
                    <button type="button" data-action="cart-inc" data-id="${item.dish.id}">+</button>
                    <button type="button" data-action="cart-remove" data-id="${item.dish.id}">×</button>
                </div>
            </div>
        `).join("");
    }
    total.textContent = money(cartTotal());
}

function cartTotal() {
    return state.cart.reduce((sum, item) => sum + Number(item.dish.price) * item.quantity, 0);
}

async function submitOrder() {
    if (state.cart.length === 0) {
        showToast("点餐车不能为空");
        return;
    }
    try {
        await api("/api/orders", {
            method: "POST",
            body: JSON.stringify({
                diningType: $("#diningType").value,
                remark: $("#orderRemark").value,
                items: state.cart.map((item) => ({ dishId: item.dish.id, quantity: item.quantity }))
            })
        });
        state.cart = [];
        saveCart();
        showToast("订单已提交");
        window.setTimeout(() => location.href = "/orders.html", 500);
    } catch (error) {
        showToast(error.message);
    }
}

async function loadOrders(showMessage) {
    try {
        state.orders = await api("/api/orders/my");
        renderOrders();
        renderOrderSelect();
        if (showMessage) {
            showToast("订单已刷新");
        }
    } catch (error) {
        showToast(error.message);
    }
}

function renderOrders() {
    const list = $("#myOrderList");
    if (!list) {
        return;
    }
    if (!state.orders.length) {
        list.innerHTML = `<div class="empty-state">暂无订单</div>`;
        return;
    }
    list.innerHTML = state.orders.map((order) => `
        <article class="order-card">
            <header>
                <div><strong>${escapeHtml(order.orderNo)}</strong><p>${formatDate(order.createdAt)} · ${diningLabels[order.diningType] || order.diningType}</p></div>
                <span class="status ${order.status}">${statusLabels[order.status] || order.status}</span>
            </header>
            <p>${order.items.map((item) => `${escapeHtml(item.dishName)} × ${item.quantity}`).join("，")}</p>
            <div class="cart-total"><span>金额</span><strong>${money(order.totalAmount)}</strong></div>
        </article>
    `).join("");
}

async function submitReview(event) {
    event.preventDefault();
    const orderId = $("#reviewOrderSelect").value;
    try {
        await api("/api/reviews", {
            method: "POST",
            body: JSON.stringify({
                dishId: Number($("#reviewDishSelect").value),
                orderId: orderId ? Number(orderId) : null,
                rating: Number($("#reviewRating").value),
                content: $("#reviewContent").value
            })
        });
        $("#reviewContent").value = "";
        await loadReviews();
        showToast("评价已提交");
    } catch (error) {
        showToast(error.message);
    }
}

async function loadReviews() {
    try {
        state.reviews = await api("/api/reviews");
        renderReviews();
    } catch (error) {
        showToast(error.message);
    }
}

function renderReviews() {
    const list = $("#reviewList");
    if (!list) {
        return;
    }
    if (!state.reviews.length) {
        list.innerHTML = `<div class="empty-state">暂无评价</div>`;
        return;
    }
    list.innerHTML = state.reviews.map((review) => `
        <article class="review-card">
            <div class="section-title"><strong>${escapeHtml(review.dishName)}</strong><span class="status ${review.sentiment}">${sentimentLabel(review.sentiment)}</span></div>
            <p>${"★".repeat(review.rating)}${"☆".repeat(5 - review.rating)} · ${escapeHtml(review.user.nickname)} · ${formatDate(review.createdAt)}</p>
            <p>${escapeHtml(review.content)}</p>
        </article>
    `).join("");
}

function renderDishSelects() {
    const reviewDishSelect = $("#reviewDishSelect");
    if (reviewDishSelect) {
        reviewDishSelect.innerHTML = state.dishes.map((dish) => `<option value="${dish.id}">${escapeHtml(dish.name)}</option>`).join("");
    }
    const adminDishCategory = $("#adminDishCategory");
    if (adminDishCategory) {
        const categoryOptions = state.adminCategories.length ? state.adminCategories : state.categories;
        adminDishCategory.innerHTML = categoryOptions
            .filter((category) => category.enabled)
            .map((category) => `<option value="${category.id}">${escapeHtml(category.name)}</option>`)
            .join("");
    }
    renderOrderSelect();
}

function renderOrderSelect() {
    const select = $("#reviewOrderSelect");
    if (!select) {
        return;
    }
    const options = [`<option value="">不关联订单</option>`].concat(
        state.orders.map((order) => `<option value="${order.id}">${escapeHtml(order.orderNo)} · ${money(order.totalAmount)}</option>`)
    );
    select.innerHTML = options.join("");
}

async function submitAiRecommend(event) {
    event.preventDefault();
    try {
        const result = await api("/api/ai/recommend", {
            method: "POST",
            body: JSON.stringify({
                taste: $("#aiTaste").value,
                budget: Number($("#aiBudget").value),
                people: Number($("#aiPeople").value),
                avoid: $("#aiAvoid").value
            })
        });
        renderAiRecommend(result);
    } catch (error) {
        showToast(error.message);
    }
}

function renderAiRecommend(result) {
    $("#aiRecommendResult").innerHTML = `
        <article class="ai-result-card">
            <h2>推荐结果</h2>
            <p>${escapeHtml(result.summary)}</p>
            <div class="order-list">
                ${result.items.map((item) => `
                    <div class="cart-item">
                        <div><strong>${escapeHtml(item.dishName)}</strong><p>${money(item.price)} · ${escapeHtml(item.reason)}</p></div>
                        <button class="secondary-btn" type="button" data-action="ai-add-cart" data-id="${item.dishId}">加入</button>
                    </div>
                `).join("") || `<div class="empty-state">暂无推荐</div>`}
            </div>
        </article>
    `;
}

async function submitServiceQuestion(event) {
    event.preventDefault();
    try {
        const result = await api("/api/ai/customer-service", {
            method: "POST",
            body: JSON.stringify({ query: $("#serviceQuery").value })
        });
        $("#serviceAnswer").textContent = result.text;
    } catch (error) {
        showToast(error.message);
    }
}

async function loadAdmin() {
    try {
        const [categories, dishes, orders] = await Promise.all([
            api("/api/admin/categories"),
            api("/api/admin/dishes"),
            api("/api/admin/orders")
        ]);
        state.adminCategories = categories;
        state.adminDishes = dishes;
        state.adminOrders = orders;
        renderAdminCategories();
        renderAdminDishes();
        renderAdminOrders();
        renderDishSelects();
        await loadAnalysis();
    } catch (error) {
        showToast(error.message);
    }
}

function renderAdminCategories() {
    $("#adminCategoryTable").innerHTML = state.adminCategories.map((category) => `
        <tr>
            <td>${escapeHtml(category.name)}</td>
            <td>${category.sortOrder}</td>
            <td>${category.enabled ? "启用" : "停用"}</td>
            <td><div class="row-actions">
                <button class="secondary-btn" type="button" data-action="admin-edit-category" data-id="${category.id}">编辑</button>
                <button class="ghost-btn" type="button" data-action="admin-disable-category" data-id="${category.id}">停用</button>
            </div></td>
        </tr>
    `).join("");
}

function renderAdminDishes() {
    $("#adminDishTable").innerHTML = state.adminDishes.map((dish) => `
        <tr>
            <td>${escapeHtml(dish.name)}</td>
            <td>${escapeHtml(dish.categoryName)}</td>
            <td>${money(dish.price)}</td>
            <td>${dish.stock}</td>
            <td>${dish.sales}</td>
            <td>${dish.available ? "上架" : "下架"}</td>
            <td><div class="row-actions">
                <button class="secondary-btn" type="button" data-action="admin-edit-dish" data-id="${dish.id}">编辑</button>
                <button class="ghost-btn" type="button" data-action="admin-disable-dish" data-id="${dish.id}">下架</button>
            </div></td>
        </tr>
    `).join("");
}

function renderAdminOrders() {
    if (!state.adminOrders.length) {
        $("#adminOrderTable").innerHTML = `<tr><td colspan="6">暂无订单</td></tr>`;
        return;
    }
    $("#adminOrderTable").innerHTML = state.adminOrders.map((order) => `
        <tr>
            <td>${escapeHtml(order.orderNo)}</td>
            <td>${escapeHtml(order.user.nickname)}</td>
            <td>${money(order.totalAmount)}</td>
            <td><span class="status ${order.status}">${statusLabels[order.status]}</span></td>
            <td>${formatDate(order.createdAt)}</td>
            <td><div class="row-actions">
                <select id="status-${order.id}">${statusOptions(order.status)}</select>
                <button class="secondary-btn" type="button" data-action="admin-update-status" data-id="${order.id}">更新</button>
            </div></td>
        </tr>
    `).join("");
}

async function loadAdminOrders() {
    try {
        state.adminOrders = await api("/api/admin/orders");
        renderAdminOrders();
        showToast("后台订单已刷新");
    } catch (error) {
        showToast(error.message);
    }
}

async function saveCategory(event) {
    event.preventDefault();
    try {
        const body = {
            name: $("#categoryName").value,
            sortOrder: Number($("#categorySort").value || 0),
            enabled: $("#categoryEnabled").checked
        };
        const path = state.editingCategoryId ? `/api/admin/categories/${state.editingCategoryId}` : "/api/admin/categories";
        const method = state.editingCategoryId ? "PUT" : "POST";
        await api(path, { method, body: JSON.stringify(body) });
        resetCategoryForm();
        await loadAdmin();
        showToast("分类已保存");
    } catch (error) {
        showToast(error.message);
    }
}

function resetCategoryForm() {
    state.editingCategoryId = null;
    $("#categoryName").value = "";
    $("#categorySort").value = "0";
    $("#categoryEnabled").checked = true;
    $("#categorySaveBtn").textContent = "保存";
}

async function saveDish(event) {
    event.preventDefault();
    try {
        const body = {
            categoryId: Number($("#adminDishCategory").value),
            name: $("#adminDishName").value,
            price: Number($("#adminDishPrice").value),
            stock: Number($("#adminDishStock").value || 0),
            tasteTags: $("#adminDishTags").value,
            ingredients: $("#adminDishIngredients").value,
            imageUrl: $("#adminDishImage").value,
            description: $("#adminDishDescription").value,
            available: $("#adminDishAvailable").checked
        };
        const path = state.editingDishId ? `/api/admin/dishes/${state.editingDishId}` : "/api/admin/dishes";
        const method = state.editingDishId ? "PUT" : "POST";
        await api(path, { method, body: JSON.stringify(body) });
        resetDishForm();
        await loadAdmin();
        showToast("菜品已保存");
    } catch (error) {
        showToast(error.message);
    }
}

function resetDishForm() {
    state.editingDishId = null;
    $("#adminDishName").value = "";
    $("#adminDishPrice").value = "";
    $("#adminDishStock").value = "";
    $("#adminDishTags").value = "";
    $("#adminDishIngredients").value = "";
    $("#adminDishImage").value = "";
    $("#adminDishDescription").value = "";
    $("#adminDishAvailable").checked = true;
    $("#dishSaveBtn").textContent = "保存菜品";
}

async function generateDishDescription() {
    try {
        const result = await api("/api/admin/ai/description", {
            method: "POST",
            body: JSON.stringify({
                name: $("#adminDishName").value,
                ingredients: $("#adminDishIngredients").value,
                tasteTags: $("#adminDishTags").value
            })
        });
        $("#adminDishDescription").value = result.text;
        showToast("AI 描述已生成");
    } catch (error) {
        showToast(error.message);
    }
}

async function loadAnalysis() {
    try {
        const analysis = await api("/api/admin/ai/reviews-analysis");
        $("#analysisBox").innerHTML = `
            <p>评价数：${analysis.totalReviews}</p>
            <p>平均评分：${analysis.averageRating}</p>
            <p>情感统计：好评 ${analysis.sentimentCount.POSITIVE || 0}，中性 ${analysis.sentimentCount.NEUTRAL || 0}，差评 ${analysis.sentimentCount.NEGATIVE || 0}</p>
            <p>热点问题：${analysis.hotIssues.length ? analysis.hotIssues.join("、") : "暂无"}</p>
            <p>${escapeHtml(analysis.suggestion)}</p>
        `;
    } catch (error) {
        showToast(error.message);
    }
}

async function handleAction(event) {
    const target = event.target.closest("[data-action]");
    if (!target) {
        return;
    }
    const action = target.dataset.action;
    const id = target.dataset.id;

    if (action === "select-category") {
        state.selectedCategoryId = id;
        renderCategoryFilters();
        await loadDishes();
    }
    if (action === "add-cart" || action === "ai-add-cart") {
        addToCart(id);
    }
    if (action === "cart-inc") {
        const item = state.cart.find((cartItem) => String(cartItem.dish.id) === String(id));
        if (item) {
            item.quantity += 1;
            saveCart();
            renderCart();
        }
    }
    if (action === "cart-dec") {
        const item = state.cart.find((cartItem) => String(cartItem.dish.id) === String(id));
        if (item) {
            item.quantity -= 1;
            if (item.quantity <= 0) {
                state.cart = state.cart.filter((cartItem) => String(cartItem.dish.id) !== String(id));
            }
            saveCart();
            renderCart();
        }
    }
    if (action === "cart-remove") {
        state.cart = state.cart.filter((cartItem) => String(cartItem.dish.id) !== String(id));
        saveCart();
        renderCart();
    }
    if (action === "admin-edit-category") {
        const category = state.adminCategories.find((item) => String(item.id) === String(id));
        if (category) {
            state.editingCategoryId = category.id;
            $("#categoryName").value = category.name;
            $("#categorySort").value = category.sortOrder;
            $("#categoryEnabled").checked = category.enabled;
            $("#categorySaveBtn").textContent = "更新";
        }
    }
    if (action === "admin-disable-category") {
        await adminDisable(`/api/admin/categories/${id}`, "分类已停用", loadAdmin);
    }
    if (action === "admin-edit-dish") {
        const dish = state.adminDishes.find((item) => String(item.id) === String(id));
        if (dish) {
            state.editingDishId = dish.id;
            $("#adminDishCategory").value = dish.categoryId;
            $("#adminDishName").value = dish.name;
            $("#adminDishPrice").value = dish.price;
            $("#adminDishStock").value = dish.stock;
            $("#adminDishTags").value = dish.tasteTags || "";
            $("#adminDishIngredients").value = dish.ingredients;
            $("#adminDishImage").value = dish.imageUrl || "";
            $("#adminDishDescription").value = dish.description;
            $("#adminDishAvailable").checked = dish.available;
            $("#dishSaveBtn").textContent = "更新菜品";
        }
    }
    if (action === "admin-disable-dish") {
        await adminDisable(`/api/admin/dishes/${id}`, "菜品已下架", loadAdmin);
    }
    if (action === "admin-update-status") {
        await updateAdminOrderStatus(id);
    }
}

async function adminDisable(path, message, after) {
    try {
        await api(path, { method: "DELETE" });
        await after();
        showToast(message);
    } catch (error) {
        showToast(error.message);
    }
}

async function updateAdminOrderStatus(id) {
    try {
        await api(`/api/admin/orders/${id}/status`, {
            method: "PUT",
            body: JSON.stringify({ status: $(`#status-${id}`).value })
        });
        await loadAdminOrders();
        showToast("订单状态已更新");
    } catch (error) {
        showToast(error.message);
    }
}

function statusOptions(selected) {
    return Object.entries(statusLabels).map(([value, label]) => `
        <option value="${value}" ${value === selected ? "selected" : ""}>${label}</option>
    `).join("");
}

function sentimentLabel(sentiment) {
    if (sentiment === "POSITIVE") {
        return "好评";
    }
    if (sentiment === "NEGATIVE") {
        return "待改进";
    }
    return "中性";
}

function loadCart() {
    try {
        return JSON.parse(localStorage.getItem("orderSystemCart") || "[]");
    } catch (error) {
        return [];
    }
}

function saveCart() {
    localStorage.setItem("orderSystemCart", JSON.stringify(state.cart));
}

function money(value) {
    return `¥${Number(value || 0).toFixed(2)}`;
}

function formatDate(value) {
    if (!value) {
        return "";
    }
    return new Date(value).toLocaleString("zh-CN", { hour12: false });
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function escapeAttr(value) {
    return escapeHtml(value);
}

function showToast(message) {
    const toast = $("#toast");
    if (!toast) {
        return;
    }
    toast.textContent = message;
    toast.classList.add("show");
    window.clearTimeout(showToast.timer);
    showToast.timer = window.setTimeout(() => toast.classList.remove("show"), 2600);
}

function debounce(fn, delay) {
    let timer;
    return (...args) => {
        window.clearTimeout(timer);
        timer = window.setTimeout(() => fn(...args), delay);
    };
}
