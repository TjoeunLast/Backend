const LS_KEY = "payment_api_test_v7";
const SCENARIO_INDEX_KEY = "payment_api_test_scenario_idx_v4";
const PAYMENT_API_TEST_CONFIG = window.PaymentApiTestConfig || {};
const SCENARIO_STEPS = PAYMENT_API_TEST_CONFIG.scenarioSteps || [];
const PRESETS = PAYMENT_API_TEST_CONFIG.presets || {};

let scenarioIndex = -1;

function byId(id) {
    return document.getElementById(id);
}

function setValue(id, value) {
    const el = byId(id);
    if (el) el.value = value;
}

function getValue(id) {
    const el = byId(id);
    return el ? (el.value || "").trim() : "";
}

function log(message, payload) {
    const el = byId("log");
    if (!el) return;
    const line = "[" + new Date().toISOString() + "] " + message;
    if (payload === undefined) {
        el.textContent += "\n" + line;
        return;
    }
    let pretty = "";
    try {
        pretty = JSON.stringify(payload, null, 2);
    } catch (e) {
        pretty = String(payload);
    }
    el.textContent += "\n" + line + "\n" + pretty;
}

function clearLogs() {
    byId("log").textContent = "ready";
    byId("response").textContent = "none";
}

function pickAnyToken() {
    return getValue("adminToken") || getValue("shipperToken") || getValue("driverToken") || "";
}

function normalizePath(path) {
    return (path || "").trim().split("?")[0];
}

function getEffectivePaymentKey() {
    const paymentKey = getValue("paymentKey");
    if (paymentKey) return paymentKey;
    const clientKey = getValue("clientKey");
    if (clientKey) {
        setValue("paymentKey", clientKey);
        return clientKey;
    }
    return "";
}

function getCurrentPeriod() {
    const now = new Date();
    const month = String(now.getMonth() + 1).padStart(2, "0");
    return now.getFullYear() + "-" + month;
}

function getCurrentDate() {
    const now = new Date();
    const month = String(now.getMonth() + 1).padStart(2, "0");
    const day = String(now.getDate()).padStart(2, "0");
    return now.getFullYear() + "-" + month + "-" + day;
}

function getTemplateValues() {
    return {
        orderId: getValue("orderId"),
        disputeId: getValue("disputeId"),
        invoiceId: getValue("invoiceId"),
        shipperId: getValue("shipperId"),
        itemId: getValue("itemId"),
        driverUserId: getValue("driverUserId"),
        clientKey: getValue("clientKey"),
        paymentKey: getEffectivePaymentKey(),
        pgOrderId: getValue("pgOrderId"),
        amount: getValue("amount"),
        currentPeriod: getCurrentPeriod(),
        currentDate: getCurrentDate()
    };
}

function replaceAll(text, from, to) {
    return (text || "").split(from).join(to);
}

function resolveTemplates(text) {
    let out = text || "";
    const t = getTemplateValues();
    if (t.orderId) out = replaceAll(out, "<ORDER_ID>", t.orderId);
    if (t.disputeId) out = replaceAll(out, "<DISPUTE_ID>", t.disputeId);
    if (t.invoiceId) out = replaceAll(out, "<INVOICE_ID>", t.invoiceId);
    if (t.shipperId) out = replaceAll(out, "<SHIPPER_ID>", t.shipperId);
    if (t.itemId) out = replaceAll(out, "<ITEM_ID>", t.itemId);
    if (t.driverUserId) out = replaceAll(out, "<DRIVER_USER_ID>", t.driverUserId);
    if (t.clientKey) out = replaceAll(out, "<CLIENT_KEY>", t.clientKey);
    if (t.paymentKey) out = replaceAll(out, "<PAYMENT_KEY>", t.paymentKey);
    if (t.pgOrderId) out = replaceAll(out, "<PG_ORDER_ID>", t.pgOrderId);
    if (t.amount) out = replaceAll(out, "<AMOUNT>", t.amount);
    if (t.currentPeriod) out = replaceAll(out, "<CURRENT_PERIOD>", t.currentPeriod);
    if (t.currentDate) out = replaceAll(out, "<CURRENT_DATE>", t.currentDate);
    return out;
}

function hasUnresolvedTemplate(text) {
    return /<[A-Z0-9_]+>/.test(text || "");
}

function resolveTokenRole(method, path) {
    const p = normalizePath(resolveTemplates(path));
    const m = (method || "").toUpperCase();
    if (p.startsWith("/api/admin/payment/")) return "ADMIN";
    if (p === "/api/v1/payments/webhooks/toss") return "NONE";
    if (m === "POST" && /^\/api\/v1\/payments\/orders\/[^/]+\/confirm$/.test(p)) return "DRIVER";
    if (p.startsWith("/api/v1/payments/")) return "SHIPPER";
    if (m === "POST" && p === "/api/v1/orders") return "SHIPPER";
    if (m === "PATCH" && /^\/api\/v1\/orders\/[^/]+\/accept$/.test(p)) return "DRIVER";
    if (m === "PATCH" && /^\/api\/v1\/orders\/[^/]+\/status$/.test(p)) return "DRIVER";
    return "NONE";
}

function getTokenByRole(role) {
    if (role === "SHIPPER") return getValue("shipperToken");
    if (role === "DRIVER") return getValue("driverToken");
    if (role === "ADMIN") return getValue("adminToken");
    return "";
}

function refreshTokenHint() {
    const method = getValue("method");
    const pathTemplate = getValue("path");
    const resolvedPath = resolveTemplates(pathTemplate);
    const role = resolveTokenRole(method, resolvedPath);
    byId("tokenHint").textContent = role === "NONE" ? "자동 토큰: NONE" : "자동 토큰: " + role;
    byId("resolvedPathHint").textContent = "치환 path: " + resolvedPath;
}

function saveState() {
    const state = {
        shipperToken: getValue("shipperToken"),
        driverToken: getValue("driverToken"),
        adminToken: getValue("adminToken"),
        orderId: getValue("orderId"),
        disputeId: getValue("disputeId"),
        invoiceId: getValue("invoiceId"),
        shipperId: getValue("shipperId"),
        itemId: getValue("itemId"),
        driverUserId: getValue("driverUserId"),
        clientKey: getValue("clientKey"),
        paymentKey: getValue("paymentKey"),
        pgOrderId: getValue("pgOrderId"),
        amount: getValue("amount"),
        method: getValue("method"),
        path: getValue("path"),
        body: getValue("body")
    };
    localStorage.setItem(LS_KEY, JSON.stringify(state));
}

function loadState() {
    const raw = localStorage.getItem(LS_KEY);
    if (!raw) return;
    try {
        const s = JSON.parse(raw);
        setValue("shipperToken", s.shipperToken || "");
        setValue("driverToken", s.driverToken || "");
        setValue("adminToken", s.adminToken || "");
        setValue("orderId", s.orderId || "");
        setValue("disputeId", s.disputeId || "");
        setValue("invoiceId", s.invoiceId || "");
        setValue("shipperId", s.shipperId || "");
        setValue("itemId", s.itemId || "");
        setValue("driverUserId", s.driverUserId || "");
        setValue("clientKey", s.clientKey || "");
        setValue("paymentKey", s.paymentKey || "");
        setValue("pgOrderId", s.pgOrderId || "");
        setValue("amount", s.amount || "55000");
        setValue("method", s.method || "POST");
        setValue("path", s.path || "/api/v1/payments/orders/<ORDER_ID>/toss/prepare");
        setValue("body", s.body || "");
    } catch (e) {
        log("load state parse error", String(e));
    }
}

async function readResponseBody(res) {
    const text = await res.text();
    if (!text) return {};
    try {
        return JSON.parse(text);
    } catch (e) {
        return { raw: text };
    }
}

function extractData(json) {
    if (!json || typeof json !== "object") return null;
    if (json.success === true && json.data !== undefined) return json.data;
    return json;
}

function syncValuesFromResponse(responseBody) {
    const data = extractData(responseBody);
    if (!data || typeof data !== "object") return;
    if (data.orderId !== undefined && data.orderId !== null) setValue("orderId", String(data.orderId));
    if (data.disputeId !== undefined && data.disputeId !== null) setValue("disputeId", String(data.disputeId));
    if (data.invoiceId !== undefined && data.invoiceId !== null) setValue("invoiceId", String(data.invoiceId));
    if (data.itemId !== undefined && data.itemId !== null) setValue("itemId", String(data.itemId));
    if (data.shipperId !== undefined && data.shipperId !== null) setValue("shipperId", String(data.shipperId));
    if (data.driverUserId !== undefined && data.driverUserId !== null) setValue("driverUserId", String(data.driverUserId));
    if (data.pgOrderId !== undefined && data.pgOrderId !== null) setValue("pgOrderId", String(data.pgOrderId));
    if (data.amount !== undefined && data.amount !== null) setValue("amount", String(data.amount));
    if (data.paymentKey !== undefined && data.paymentKey !== null) setValue("paymentKey", String(data.paymentKey));
    refreshTokenHint();
}

async function loadContext(orderId) {
    const token = pickAnyToken();
    const headers = token ? { Authorization: "Bearer " + token } : {};
    const query = orderId ? ("?orderId=" + encodeURIComponent(orderId)) : "";
    const url = getValue("baseUrl") + "/api/v1/payments/api-test/context" + query;
    const res = await fetch(url, { method: "GET", headers: headers });
    const body = await readResponseBody(res);
    if (!res.ok || !body || body.success !== true || !body.data) {
        log("context load failed", { status: res.status, body: body });
        return false;
    }
    syncValuesFromResponse(body);
    saveState();
    return true;
}

async function loadContextForCurrentOrder() {
    const orderId = getValue("orderId");
    if (!orderId) {
        alert("orderId가 없습니다. 먼저 0번을 실행하세요.");
        return;
    }
    await loadContext(orderId);
}

function buildDummyOrderRequest() {
    const now = Date.now();
    return {
        startAddr: "서울 종로구 테스트출발 " + now,
        startPlace: "테스트 출발지",
        startType: "당상",
        startSchedule: "오늘 14:00",
        puProvince: "서울",
        startLat: 37.5729,
        startLng: 126.9794,
        endAddr: "경기 수원시 테스트도착 " + now,
        endPlace: "테스트 도착지",
        endType: "당착",
        endSchedule: "오늘 18:00",
        doProvince: "경기",
        cargoContent: "테스트 화물",
        loadMethod: "지게차",
        workType: "카고",
        tonnage: 1.0,
        reqCarType: "카고",
        reqTonnage: "1톤",
        driveMode: "편도",
        loadWeight: 1000,
        basePrice: 50000,
        laborFee: 3000,
        packagingPrice: 1000,
        insuranceFee: 1000,
        payMethod: "카드",
        instant: true,
        memo: "payment api test auto flow",
        tag: ["payment", "auto"],
        distance: 12000,
        duration: 1800
    };
}

async function patchDriverStatus(orderId, status, driverToken) {
    const url = getValue("baseUrl") + "/api/v1/orders/" + orderId + "/status?newStatus=" + encodeURIComponent(status);
    const res = await fetch(url, { method: "PATCH", headers: { Authorization: "Bearer " + driverToken } });
    const body = await readResponseBody(res);
    log("driver status response", { statusCode: res.status, status: status, body: body });
    return res.ok;
}

async function autoBootstrapOrderFlow() {
    const shipperToken = getValue("shipperToken");
    const driverToken = getValue("driverToken");
    if (!shipperToken || !driverToken) {
        alert("0번 실행에는 shipperToken, driverToken이 필요합니다.");
        return false;
    }

    const createUrl = getValue("baseUrl") + "/api/v1/orders";
    const createRes = await fetch(createUrl, {
        method: "POST",
        headers: { "Content-Type": "application/json", Authorization: "Bearer " + shipperToken },
        body: JSON.stringify(buildDummyOrderRequest())
    });
    const createBody = await readResponseBody(createRes);
    const created = extractData(createBody);
    const orderId = created && created.orderId ? String(created.orderId) : "";

    if (!createRes.ok || !orderId) {
        log("order create failed", { status: createRes.status, body: createBody });
        alert("주문 생성 실패");
        return false;
    }

    setValue("orderId", orderId);

    const acceptUrl = getValue("baseUrl") + "/api/v1/orders/" + orderId + "/accept";
    const acceptRes = await fetch(acceptUrl, {
        method: "PATCH",
        headers: { Authorization: "Bearer " + driverToken }
    });
    if (!acceptRes.ok) {
        log("driver accept failed", { status: acceptRes.status, body: await readResponseBody(acceptRes) });
        alert("차주 수락 실패");
        return false;
    }

    const statuses = ["LOADING", "IN_TRANSIT", "UNLOADING", "COMPLETED"];
    for (const s of statuses) {
        const ok = await patchDriverStatus(orderId, s, driverToken);
        if (!ok) {
            alert("운송 상태 변경 실패: " + s);
            return false;
        }
    }

    await loadContext(orderId);
    saveState();
    log("bootstrap success", { orderId: orderId });
    alert("0번 완료: orderId=" + orderId);
    return true;
}

function preset(name) {
    const p = PRESETS[name];
    if (!p) return;
    const copy = {
        method: p.method,
        path: p.path,
        body: p.body === null ? null : JSON.parse(JSON.stringify(p.body))
    };

    if (name === "tossConfirm" && copy.body) {
        const effectivePaymentKey = getEffectivePaymentKey();
        if (effectivePaymentKey) copy.body.paymentKey = effectivePaymentKey;
        if (getValue("pgOrderId")) copy.body.pgOrderId = getValue("pgOrderId");
        if (getValue("amount")) copy.body.amount = Number(getValue("amount"));
    }

    setValue("method", copy.method);
    setValue("path", copy.path);
    setValue("body", copy.body === null ? "" : JSON.stringify(copy.body, null, 2));
    refreshTokenHint();
    saveState();
}

function loadScenarioIndex() {
    const raw = localStorage.getItem(SCENARIO_INDEX_KEY);
    if (!raw) {
        scenarioIndex = -1;
        return;
    }
    const idx = Number(raw);
    scenarioIndex = Number.isInteger(idx) && idx >= 0 && idx < SCENARIO_STEPS.length ? idx : -1;
}

function saveScenarioIndex() {
    localStorage.setItem(SCENARIO_INDEX_KEY, String(scenarioIndex));
}

function renderScenarioStatus() {
    const el = byId("scenarioStatus");
    if (!el) return;
    if (scenarioIndex < 0) {
        el.textContent = "시나리오 대기중\n- 먼저 0번을 눌러 orderId 생성\n- 그 다음 1단계부터 시작";
        return;
    }
    const current = SCENARIO_STEPS[scenarioIndex];
    const lines = [];
    lines.push("현재 단계: " + (scenarioIndex + 1) + "/" + SCENARIO_STEPS.length);
    lines.push("현재 작업: " + current.title);
    lines.push("필요 토큰: " + current.token);
    lines.push("");
    for (let i = 0; i < SCENARIO_STEPS.length; i++) {
        const prefix = i === scenarioIndex ? ">> " : "   ";
        lines.push(prefix + (i + 1) + ". " + SCENARIO_STEPS[i].title);
    }
    el.textContent = lines.join("\n");
}

function ensureOrderReady() {
    if (getValue("orderId")) return true;
    alert("orderId가 없습니다. 0번을 먼저 실행하세요.");
    return false;
}

async function applyScenarioStep(index) {
    if (index < 0 || index >= SCENARIO_STEPS.length) return;
    if (!ensureOrderReady()) return;
    scenarioIndex = index;
    const step = SCENARIO_STEPS[scenarioIndex];
    preset(step.preset);
    renderScenarioStatus();
    saveScenarioIndex();
    log("scenario step set", { step: scenarioIndex + 1, title: step.title });
}

async function startScenario() {
    await applyScenarioStep(0);
}

async function repeatScenarioStep() {
    if (scenarioIndex < 0) {
        await startScenario();
        return;
    }
    await applyScenarioStep(scenarioIndex);
}

async function nextScenarioStep() {
    if (scenarioIndex < 0) {
        await startScenario();
        return;
    }
    if (scenarioIndex >= SCENARIO_STEPS.length - 1) {
        alert("마지막 단계입니다.");
        renderScenarioStatus();
        return;
    }
    await applyScenarioStep(scenarioIndex + 1);
}

async function sendRequest() {
    const method = getValue("method");
    const pathTemplate = getValue("path");
    const bodyTemplate = getValue("body");
    if (pathTemplate.includes("/toss/confirm")) getEffectivePaymentKey();
    const path = resolveTemplates(pathTemplate);
    const bodyText = resolveTemplates(bodyTemplate);

    if (!path) {
        alert("path는 필수입니다.");
        return;
    }
    if (hasUnresolvedTemplate(path)) {
        alert("path 템플릿이 남아 있습니다.");
        return;
    }

    const tokenRole = resolveTokenRole(method, path);
    const token = getTokenByRole(tokenRole);
    if (tokenRole !== "NONE" && !token) {
        alert(tokenRole + " 토큰이 없습니다.");
        return;
    }

    const url = getValue("baseUrl") + path;
    const headers = {};
    if (tokenRole !== "NONE") headers.Authorization = "Bearer " + token;

    const options = { method: method, headers: headers };
    if (method !== "GET" && method !== "DELETE" && bodyText) {
        if (hasUnresolvedTemplate(bodyText)) {
            alert("body 템플릿이 남아 있습니다.");
            return;
        }
        headers["Content-Type"] = "application/json";
        try {
            options.body = JSON.stringify(JSON.parse(bodyText));
        } catch (e) {
            alert("JSON body 파싱 실패");
            return;
        }
    }

    log("request", { method: method, url: url, tokenRole: tokenRole });
    const res = await fetch(url, options);
    const responseBody = await readResponseBody(res);
    syncValuesFromResponse(responseBody);
    if (getValue("orderId")) await loadContext(getValue("orderId"));
    saveState();

    byId("response").textContent = JSON.stringify({
        status: res.status,
        tokenRole: tokenRole,
        body: responseBody
    }, null, 2);
    log("response", { status: res.status });
}

function init() {
    setValue("baseUrl", window.location.origin);
    loadState();
    getEffectivePaymentKey();
    loadScenarioIndex();
    refreshTokenHint();
    renderScenarioStatus();
    ["method", "path", "body", "orderId", "invoiceId", "itemId", "clientKey", "paymentKey", "pgOrderId", "amount"].forEach((id) => {
        const el = byId(id);
        if (el) el.addEventListener("input", refreshTokenHint);
    });
    log("page initialized", { baseUrl: getValue("baseUrl") });
}

init();
