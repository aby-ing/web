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
    managerCategories: [],
    managerDishes: [],
    managerOrders: [],
    editingCategoryId: null,
    editingDishId: null,
    managementBase: "/api/merchant"
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
    const requiresMerchant = document.body.dataset.merchant === "true";
    state.managementBase = "/api/merchant";

    if (requiresAuth && !state.user) {
        location.replace("/login.html");
        return;
    }
    if (requiresMerchant && (!state.user || !isMerchant())) {
        location.replace(homeForRole(state.user?.role));
        return;
    }
    removeStudentManagementLinks(page);

    if ((page === "order" || page === "orders" || page === "reviews" || page === "ai") && state.user?.role === "MERCHANT") {
        location.replace("/merchant.html");
        return;
    }

    refreshAuthUi();
    ensureStudentCustomerService(page);

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
    if (page === "merchant") {
        await initManagerPage();
    }
    ensureStudentCustomerService(page);
}

function isMerchant() {
    return state.user && state.user.role === "MERCHANT";
}

function homeForRole(role) {
    if (role === "MERCHANT") {
        return "/merchant.html";
    }
    return "/order.html";
}

function managementApiBase() {
    return state.managementBase || "/api/merchant";
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
        location.replace(homeForRole(state.user.role));
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
    await loadDishes();
}

function ensureStudentCustomerService(page) {
    const authenticatedStudentPage = document.body.dataset.auth === "true" && page !== "merchant";
    if (!authenticatedStudentPage || !state.user || state.user.role === "MERCHANT") {
        return;
    }
    if (!$("#serviceFab")) {
        document.body.insertAdjacentHTML("beforeend", `
            <button class="customer-service-fab" type="button" id="serviceFab" aria-controls="servicePanel" aria-expanded="false">客服</button>
            <section class="customer-service-panel" id="servicePanel" aria-label="智能客服" hidden>
                <form class="customer-service-card" id="serviceForm">
                    <div class="customer-service-head">
                        <h2>智能客服</h2>
                        <button class="ghost-btn icon-btn" type="button" id="serviceCloseBtn" aria-label="关闭智能客服">×</button>
                    </div>
                    <label>问题<input id="serviceQuery" placeholder="营业时间、退单、打包自提"></label>
                    <button class="secondary-btn" type="submit">提问</button>
                    <p class="answer" id="serviceAnswer"></p>
                </form>
            </section>
        `);
    }
    initCustomerServicePanel();
}

function initCustomerServicePanel() {
    const fab = $("#serviceFab");
    const panel = $("#servicePanel");
    const form = $("#serviceForm");
    const closeBtn = $("#serviceCloseBtn");
    if (!fab || !panel || !form || fab.dataset.bound === "true") {
        return;
    }
    fab.dataset.bound = "true";
    form.addEventListener("submit", submitServiceQuestion);

    const setOpen = (open) => {
        panel.hidden = !open;
        panel.classList.toggle("open", open);
        fab.setAttribute("aria-expanded", String(open));
        if (open) {
            $("#serviceQuery")?.focus();
        }
    };

    fab.addEventListener("click", () => setOpen(panel.hidden));
    closeBtn?.addEventListener("click", () => setOpen(false));
    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && !panel.hidden) {
            setOpen(false);
        }
    });
}

async function initManagerPage() {
    $("#categoryForm").addEventListener("submit", saveCategory);
    $("#categoryResetBtn").addEventListener("click", resetCategoryForm);
    $("#dishForm").addEventListener("submit", saveDish);
    $("#dishResetBtn").addEventListener("click", resetDishForm);
    $("#generateDescBtn").addEventListener("click", generateDishDescription);
    $("#refreshAdminOrdersBtn").addEventListener("click", loadAdminOrders);
    $("#refreshAnalysisBtn").addEventListener("click", loadAnalysis);
    await loadManager();
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
    const isMerchantUser = state.user && state.user.role === "MERCHANT";
    if (chip) {
        chip.textContent = state.user
            ? `${state.user.nickname} · ${state.user.role === "MERCHANT" ? "商家" : "学生"}`
            : "未登录";
    }
    $$(".merchant-only").forEach((element) => {
        element.hidden = !isMerchantUser;
    });
    if (!isMerchantUser) {
        removeStudentManagementLinks(document.body.dataset.page || "");
    }
}

function removeStudentManagementLinks(page) {
    if (page === "merchant") {
        return;
    }
    $$('a[href="/merchant.html"], a[href="/admin.html"], .admin-only').forEach((element) => {
        element.remove();
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
        window.setTimeout(() => location.href = homeForRole(result.user.role), 400);
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
        || state.managerDishes.find((item) => String(item.id) === String(dishId));
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
        const categoryOptions = state.managerCategories.length ? state.managerCategories : state.categories;
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
    const query = $("#serviceQuery")?.value.trim() || "";
    if (!query) {
        showToast("请输入要咨询的问题");
        $("#serviceQuery")?.focus();
        return;
    }
    try {
        const result = await api("/api/ai/customer-service", {
            method: "POST",
            body: JSON.stringify({ query })
        });
        $("#serviceAnswer").textContent = result.text;
    } catch (error) {
        showToast(error.message);
    }
}

async function loadManager() {
    try {
        const [categories, dishes, orders] = await Promise.all([
            api(`${managementApiBase()}/categories`),
            api(`${managementApiBase()}/dishes`),
            api(`${managementApiBase()}/orders`)
        ]);
        state.managerCategories = categories;
        state.managerDishes = dishes;
        state.managerOrders = orders;
        renderManagerCategories();
        renderManagerDishes();
        renderManagerOrders();
        renderDishSelects();
        await loadAnalysis();
    } catch (error) {
        showToast(error.message);
    }
}

function renderManagerCategories() {
    $("#adminCategoryTable").innerHTML = state.managerCategories.map((category) => `
        <tr>
            <td>${escapeHtml(category.name)}</td>
            <td>${category.sortOrder}</td>
            <td>${category.enabled ? "启用" : "停用"}</td>
            <td><div class="row-actions">
                <button class="secondary-btn" type="button" data-action="manager-edit-category" data-id="${category.id}">编辑</button>
                <button class="ghost-btn" type="button" data-action="manager-disable-category" data-id="${category.id}" ${category.enabled ? "" : "disabled"}>停用</button>
            </div></td>
        </tr>
    `).join("");
}

function renderManagerDishes() {
    $("#adminDishTable").innerHTML = state.managerDishes.map((dish) => `
        <tr class="${String(state.editingDishId) === String(dish.id) ? "editing-row" : ""}">
            <td>${escapeHtml(dish.name)}</td>
            <td>${escapeHtml(dish.categoryName)}</td>
            <td>${money(dish.price)}</td>
            <td>${dish.stock}</td>
            <td>${dish.sales}</td>
            <td>${dish.available ? "上架" : "下架"}</td>
            <td><div class="row-actions">
                <button class="secondary-btn" type="button" data-action="manager-edit-dish" data-id="${dish.id}">编辑</button>
                <button class="ghost-btn" type="button" data-action="manager-disable-dish" data-id="${dish.id}" ${dish.available ? "" : "disabled"}>下架</button>
            </div></td>
        </tr>
    `).join("");
}

function renderManagerOrders() {
    if (!state.managerOrders.length) {
        $("#adminOrderTable").innerHTML = `<tr><td colspan="6">暂无订单</td></tr>`;
        return;
    }
    $("#adminOrderTable").innerHTML = state.managerOrders.map((order) => `
        <tr>
            <td>${escapeHtml(order.orderNo)}</td>
            <td>${escapeHtml(order.user.nickname)}</td>
            <td>${money(order.totalAmount)}</td>
            <td><span class="status ${order.status}">${statusLabels[order.status]}</span></td>
            <td>${formatDate(order.createdAt)}</td>
            <td><div class="row-actions">
                <select id="status-${order.id}">${statusOptions(order.status)}</select>
                <button class="secondary-btn" type="button" data-action="manager-update-status" data-id="${order.id}">更新</button>
            </div></td>
        </tr>
    `).join("");
}

async function loadAdminOrders() {
    try {
        state.managerOrders = await api(`${managementApiBase()}/orders`);
        renderManagerOrders();
        showToast("订单已刷新");
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
        const path = state.editingCategoryId ? `${managementApiBase()}/categories/${state.editingCategoryId}` : `${managementApiBase()}/categories`;
        const method = state.editingCategoryId ? "PUT" : "POST";
        await api(path, { method, body: JSON.stringify(body) });
        resetCategoryForm();
        await loadManager();
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
        const path = state.editingDishId ? `${managementApiBase()}/dishes/${state.editingDishId}` : `${managementApiBase()}/dishes`;
        const method = state.editingDishId ? "PUT" : "POST";
        const message = state.editingDishId ? "菜品已更新" : "菜品已创建";
        await api(path, { method, body: JSON.stringify(body) });
        resetDishForm();
        await loadManager();
        showToast(message);
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
    const editState = $("#dishEditState");
    if (editState) {
        editState.textContent = "新增菜品";
    }
    renderManagerDishes();
}

function editDish(dishId) {
    const dish = state.managerDishes.find((item) => String(item.id) === String(dishId));
    if (!dish) {
        showToast("菜品不存在，请刷新后重试");
        return;
    }

    state.editingDishId = dish.id;
    renderDishSelects();
    $("#adminDishCategory").value = String(dish.categoryId);
    $("#adminDishName").value = dish.name || "";
    $("#adminDishPrice").value = dish.price ?? "";
    $("#adminDishStock").value = dish.stock ?? "";
    $("#adminDishTags").value = dish.tasteTags || "";
    $("#adminDishIngredients").value = dish.ingredients || "";
    $("#adminDishImage").value = dish.imageUrl || "";
    $("#adminDishDescription").value = dish.description || "";
    $("#adminDishAvailable").checked = Boolean(dish.available);
    $("#dishSaveBtn").textContent = "更新菜品";
    const editState = $("#dishEditState");
    if (editState) {
        editState.textContent = `正在编辑：${dish.name}`;
    }
    renderManagerDishes();
    $("#dishManagerPanel")?.scrollIntoView({ behavior: "smooth", block: "start" });
    window.setTimeout(() => $("#adminDishName")?.focus(), 180);
    showToast("已进入菜品编辑模式");
}

async function generateDishDescription() {
    try {
        const result = await api(`${managementApiBase()}/ai/description`, {
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
        const analysis = await api(`${managementApiBase()}/ai/reviews-analysis`);
        const positive = analysis.sentimentCount.POSITIVE || 0;
        const neutral = analysis.sentimentCount.NEUTRAL || 0;
        const negative = analysis.sentimentCount.NEGATIVE || 0;
        $("#analysisBox").innerHTML = `
            <div class="analysis-summary">
                <div><span>评价总数</span><strong>${analysis.totalReviews}</strong></div>
                <div><span>平均评分</span><strong>${analysis.averageRating}</strong></div>
                <div><span>好评</span><strong>${positive}</strong></div>
                <div><span>中性</span><strong>${neutral}</strong></div>
                <div><span>差评</span><strong>${negative}</strong></div>
            </div>
            <div class="analysis-detail">
                <h2>热点问题</h2>
                <p>${analysis.hotIssues.length ? analysis.hotIssues.map(escapeHtml).join("、") : "暂无明显集中问题"}</p>
            </div>
            <div class="analysis-detail">
                <h2>经营建议</h2>
                <p>${escapeHtml(analysis.suggestion)}</p>
            </div>
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
    if (action === "manager-edit-category" || action === "admin-edit-category") {
        const category = state.managerCategories.find((item) => String(item.id) === String(id));
        if (category) {
            state.editingCategoryId = category.id;
            $("#categoryName").value = category.name;
            $("#categorySort").value = category.sortOrder;
            $("#categoryEnabled").checked = category.enabled;
            $("#categorySaveBtn").textContent = "更新";
        }
    }
    if (action === "manager-disable-category" || action === "admin-disable-category") {
        await adminDisable(`${managementApiBase()}/categories/${id}`, "分类已停用", loadManager);
    }
    if (action === "manager-edit-dish" || action === "admin-edit-dish") {
        editDish(id);
    }
    if (action === "manager-disable-dish" || action === "admin-disable-dish") {
        await adminDisable(`${managementApiBase()}/dishes/${id}`, "菜品已下架", loadManager);
    }
    if (action === "manager-update-status" || action === "admin-update-status") {
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
        await api(`${managementApiBase()}/orders/${id}/status`, {
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

