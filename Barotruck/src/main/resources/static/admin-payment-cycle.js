(function () {
    const CYCLE_MAP = {
        A: {
            title: "사이클 A: 결제 정상 전이",
            desc: "주문 생성/배차/운송완료 -> Toss prepare -> 결제창 호출 -> Toss confirm -> 차주 confirm",
            inputs: [
                { id: "baseUrl", label: "Base URL", readonly: true, defaultValue: window.location.origin },
                { id: "shipperToken", label: "화주 토큰 (SHIPPER)", type: "textarea" },
                { id: "driverToken", label: "차주 토큰 (DRIVER)", type: "textarea" },
                { id: "clientKey", label: "토스 clientKey (test_ck...)" },
                { id: "prepareMethod", label: "prepare method", type: "select", defaultValue: "CARD", options: ["CARD", "TRANSFER"] },
                { id: "payChannel", label: "prepare payChannel", type: "select", defaultValue: "APP_CARD", options: ["APP_CARD", "CARD", "TRANSFER"] },
                { id: "orderId", label: "orderId" },
                { id: "pgOrderId", label: "pgOrderId" },
                { id: "paymentKey", label: "paymentKey (토스 성공 콜백값)" },
                { id: "amount", label: "amount", defaultValue: "55000" },
                {
                    id: "driverStatusFlow",
                    label: "운송 상태 전이",
                    type: "select",
                    defaultValue: "LOADING,IN_TRANSIT,UNLOADING,COMPLETED",
                    options: [
                        { label: "전체: LOADING -> IN_TRANSIT -> UNLOADING -> COMPLETED", value: "LOADING,IN_TRANSIT,UNLOADING,COMPLETED" },
                        { label: "이동부터: IN_TRANSIT -> UNLOADING -> COMPLETED", value: "IN_TRANSIT,UNLOADING,COMPLETED" },
                        { label: "완료만: COMPLETED", value: "COMPLETED" }
                    ]
                },
                { id: "successUrl", label: "successUrl", readonly: true },
                { id: "failUrl", label: "failUrl", readonly: true }
            ],
            actions: [
                { label: "0) 주문 생성/배차/운송완료", kind: "custom", custom: "bootstrapOrderFlow" },
                {
                    label: "1) Toss prepare",
                    kind: "request",
                    method: "POST",
                    tokenId: "shipperToken",
                    path: "/api/v1/payments/orders/{orderId}/toss/prepare",
                    required: ["orderId"],
                    body: {
                        method: "{prepareMethod}",
                        payChannel: "{payChannel}",
                        orderName: "Barotruck Toss Test"
                    }
                },
                { label: "2) 결제창 열기", kind: "custom", custom: "openTossPaymentWindow", required: ["clientKey", "pgOrderId", "amount"] },
                {
                    label: "3) Toss confirm",
                    kind: "request",
                    method: "POST",
                    tokenId: "shipperToken",
                    path: "/api/v1/payments/orders/{orderId}/toss/confirm",
                    required: ["orderId", "paymentKey"],
                    body: {
                        paymentKey: "{paymentKey}"
                    }
                },
                {
                    label: "4) Driver confirm",
                    kind: "request",
                    method: "POST",
                    tokenId: "driverToken",
                    path: "/api/v1/payments/orders/{orderId}/confirm",
                    required: ["orderId"]
                },
                { label: "5) context 동기화", kind: "custom", custom: "syncContext", required: ["orderId"] }
            ]
        },
        B: {
            title: "사이클 B: 관리자 수수료 청구 배치",
            desc: "월 기준 인보이스 배치 실행/개별 생성",
            inputs: [
                { id: "baseUrl", label: "Base URL", readonly: true, defaultValue: window.location.origin },
                { id: "adminToken", label: "관리자 토큰 (ADMIN)", type: "textarea" },
                { id: "period", label: "period (YYYY-MM)", defaultValue: currentYearMonth() },
                { id: "shipperUserId", label: "shipperUserId (개별 생성용)" }
            ],
            actions: [
                {
                    label: "1) fee-invoices/run",
                    kind: "request",
                    method: "POST",
                    tokenId: "adminToken",
                    path: "/api/admin/payment/fee-invoices/run?period={period}",
                    required: ["period"]
                },
                {
                    label: "2) fee-invoices/generate",
                    kind: "request",
                    method: "POST",
                    tokenId: "adminToken",
                    path: "/api/admin/payment/fee-invoices/generate?shipperUserId={shipperUserId}&period={period}",
                    required: ["shipperUserId", "period"]
                }
            ]
        },
        C: {
            title: "사이클 C: 화주 인보이스 납부",
            desc: "내 인보이스 조회 -> mark-paid 처리",
            inputs: [
                { id: "baseUrl", label: "Base URL", readonly: true, defaultValue: window.location.origin },
                { id: "shipperToken", label: "화주 토큰 (SHIPPER)", type: "textarea" },
                { id: "period", label: "period (YYYY-MM)", defaultValue: currentYearMonth() },
                { id: "invoiceId", label: "invoiceId" }
            ],
            actions: [
                {
                    label: "1) fee-invoices/me 조회",
                    kind: "request",
                    method: "GET",
                    tokenId: "shipperToken",
                    path: "/api/v1/payments/fee-invoices/me?period={period}",
                    required: ["period"]
                },
                {
                    label: "2) invoice mark-paid",
                    kind: "request",
                    method: "POST",
                    tokenId: "shipperToken",
                    path: "/api/v1/payments/fee-invoices/{invoiceId}/mark-paid",
                    required: ["invoiceId"]
                }
            ]
        },
        D: {
            title: "사이클 D: 관리자 차주 지급 배치",
            desc: "지급 배치 실행",
            inputs: [
                { id: "baseUrl", label: "Base URL", readonly: true, defaultValue: window.location.origin },
                { id: "adminToken", label: "관리자 토큰 (ADMIN)", type: "textarea" },
                { id: "payoutDate", label: "date (YYYY-MM-DD)", defaultValue: currentDate() }
            ],
            actions: [
                {
                    label: "1) payouts/run",
                    kind: "request",
                    method: "POST",
                    tokenId: "adminToken",
                    path: "/api/admin/payment/payouts/run?date={payoutDate}",
                    required: ["payoutDate"]
                }
            ]
        },
        E: {
            title: "사이클 E: 지급 실패 재시도",
            desc: "itemId 자동 조회(주문 기준/최신) -> 실패 item 재시도",
            inputs: [
                { id: "baseUrl", label: "Base URL", readonly: true, defaultValue: window.location.origin },
                { id: "adminToken", label: "관리자 토큰 (ADMIN)", type: "textarea" },
                { id: "orderId", label: "orderId (선택: 해당 주문 기준 조회)" },
                { id: "payoutDate", label: "date (선택: 먼저 지급 배치 실행)", defaultValue: currentDate() },
                { id: "itemId", label: "payout itemId" }
            ],
            actions: [
                {
                    label: "0) payouts/run (선택)",
                    kind: "request",
                    method: "POST",
                    tokenId: "adminToken",
                    path: "/api/admin/payment/payouts/run?date={payoutDate}",
                    required: ["payoutDate"]
                },
                {
                    label: "1) itemId 자동 조회",
                    kind: "custom",
                    custom: "resolvePayoutItemId"
                },
                {
                    label: "2) payout-item retry",
                    kind: "request",
                    method: "POST",
                    tokenId: "adminToken",
                    path: "/api/admin/payment/payout-items/{itemId}/retry",
                    required: ["itemId"]
                }
            ]
        },
        F: {
            title: "사이클 F: E2E 전체 흐름 (처음부터 끝까지)",
            desc: "주문 생성/배차/운송완료 -> Toss prepare -> 결제창 -> Toss confirm -> 차주 confirm -> 분쟁 생성/처리 -> 대사/재처리",
            inputs: [
                { id: "baseUrl", label: "Base URL", readonly: true, defaultValue: window.location.origin },
                { id: "shipperToken", label: "화주 토큰 (SHIPPER)", type: "textarea" },
                { id: "driverToken", label: "차주 토큰 (DRIVER)", type: "textarea" },
                { id: "adminToken", label: "관리자 토큰 (ADMIN)", type: "textarea" },
                { id: "clientKey", label: "토스 clientKey (test_ck...)" },
                { id: "prepareMethod", label: "prepare method", type: "select", defaultValue: "CARD", options: ["CARD", "TRANSFER"] },
                { id: "payChannel", label: "prepare payChannel", type: "select", defaultValue: "APP_CARD", options: ["APP_CARD", "CARD", "TRANSFER"] },
                { id: "orderId", label: "orderId" },
                { id: "pgOrderId", label: "pgOrderId" },
                { id: "paymentKey", label: "paymentKey (토스 성공 콜백값)" },
                { id: "amount", label: "amount", defaultValue: "55000" },
                {
                    id: "driverStatusFlow",
                    label: "운송 상태 전이",
                    type: "select",
                    defaultValue: "LOADING,IN_TRANSIT,UNLOADING,COMPLETED",
                    options: [
                        { label: "전체: LOADING -> IN_TRANSIT -> UNLOADING -> COMPLETED", value: "LOADING,IN_TRANSIT,UNLOADING,COMPLETED" },
                        { label: "이동부터: IN_TRANSIT -> UNLOADING -> COMPLETED", value: "IN_TRANSIT,UNLOADING,COMPLETED" },
                        { label: "완료만: COMPLETED", value: "COMPLETED" }
                    ]
                },
                { id: "successUrl", label: "successUrl", readonly: true },
                { id: "failUrl", label: "failUrl", readonly: true },
                { id: "disputeId", label: "disputeId" },
                { id: "driverUserId", label: "requesterUserId (배정 차주)" },
                {
                    id: "reasonCode",
                    label: "reasonCode",
                    type: "select",
                    defaultValue: "PRICE_MISMATCH",
                    options: [
                        "PRICE_MISMATCH",
                        "RECEIVED_AMOUNT_MISMATCH",
                        "PROOF_MISSING",
                        "FRAUD_SUSPECTED",
                        "OTHER"
                    ]
                },
                { id: "description", label: "description", defaultValue: "정산 금액 불일치 테스트" },
                { id: "attachmentUrl", label: "attachmentUrl", defaultValue: "https://example.com/dispute.png" },
                { id: "disputeStatus", label: "status", type: "select", defaultValue: "ADMIN_HOLD", options: ["PENDING", "ADMIN_HOLD", "ADMIN_FORCE_CONFIRMED", "ADMIN_REJECTED"] },
                { id: "adminMemo", label: "adminMemo", defaultValue: "운영 점검 상태 변경" }
            ],
            actions: [
                { label: "0) 주문 생성/배차/운송완료", kind: "custom", custom: "bootstrapOrderFlow" },
                {
                    label: "1) Toss prepare",
                    kind: "request",
                    method: "POST",
                    tokenId: "shipperToken",
                    path: "/api/v1/payments/orders/{orderId}/toss/prepare",
                    required: ["orderId"],
                    body: {
                        method: "{prepareMethod}",
                        payChannel: "{payChannel}",
                        orderName: "Barotruck Toss Test"
                    }
                },
                { label: "2) 결제창 열기", kind: "custom", custom: "openTossPaymentWindow", required: ["clientKey", "pgOrderId", "amount"] },
                {
                    label: "3) Toss confirm",
                    kind: "request",
                    method: "POST",
                    tokenId: "shipperToken",
                    path: "/api/v1/payments/orders/{orderId}/toss/confirm",
                    required: ["orderId", "paymentKey"],
                    body: {
                        paymentKey: "{paymentKey}"
                    }
                },
                {
                    label: "4) Driver confirm",
                    kind: "request",
                    method: "POST",
                    tokenId: "driverToken",
                    path: "/api/v1/payments/orders/{orderId}/confirm",
                    required: ["orderId"]
                },
                { label: "5) context 동기화", kind: "custom", custom: "syncContext", required: ["orderId"] },
                {
                    label: "6) 분쟁 생성",
                    kind: "request",
                    method: "POST",
                    tokenId: "adminToken",
                    path: "/api/admin/payment/orders/{orderId}/disputes",
                    required: ["orderId", "driverUserId"],
                    body: {
                        requesterUserId: "{driverUserId}",
                        reasonCode: "{reasonCode}",
                        description: "{description}",
                        attachmentUrl: "{attachmentUrl}"
                    }
                },
                {
                    label: "7) 분쟁 상태 변경",
                    kind: "request",
                    method: "PATCH",
                    tokenId: "adminToken",
                    path: "/api/admin/payment/orders/{orderId}/disputes/{disputeId}/status",
                    required: ["orderId", "disputeId", "disputeStatus"],
                    body: {
                        status: "{disputeStatus}",
                        adminMemo: "{adminMemo}"
                    }
                },
                { label: "8) context 동기화", kind: "custom", custom: "syncContext", required: ["orderId"] },
                {
                    label: "9) 대사 실행",
                    kind: "request",
                    method: "POST",
                    tokenId: "adminToken",
                    path: "/api/admin/payment/reconciliation/run"
                },
                {
                    label: "10) 토스 실패 재처리 큐",
                    kind: "request",
                    method: "POST",
                    tokenId: "adminToken",
                    path: "/api/admin/payment/toss/retries/run"
                },
                {
                    label: "11) prepared 만료 처리",
                    kind: "request",
                    method: "POST",
                    tokenId: "adminToken",
                    path: "/api/admin/payment/toss/expire-prepared/run"
                }
            ]
        }
    };

    function currentYearMonth() {
        const d = new Date();
        const m = String(d.getMonth() + 1).padStart(2, "0");
        return d.getFullYear() + "-" + m;
    }

    function currentDate() {
        const d = new Date();
        const m = String(d.getMonth() + 1).padStart(2, "0");
        const day = String(d.getDate()).padStart(2, "0");
        return d.getFullYear() + "-" + m + "-" + day;
    }

    function getCycleSuccessUrl() {
        return window.location.origin + window.location.pathname + "?flow=success";
    }

    function getCycleFailUrl() {
        return window.location.origin + window.location.pathname + "?flow=fail";
    }

    function detectCycle() {
        const p = window.location.pathname.toLowerCase();
        if (p.endsWith("/admin-cycle-a")) return "A";
        if (p.endsWith("/admin-cycle-b")) return "B";
        if (p.endsWith("/admin-cycle-c")) return "C";
        if (p.endsWith("/admin-cycle-d")) return "D";
        if (p.endsWith("/admin-cycle-e")) return "E";
        if (p.endsWith("/admin-cycle-f")) return "F";
        const q = (new URLSearchParams(window.location.search).get("cycle") || "A").toUpperCase();
        return CYCLE_MAP[q] ? q : "A";
    }

    const cycleCode = detectCycle();
    const config = CYCLE_MAP[cycleCode];
    const storageKey = "admin_payment_cycle_page_" + cycleCode + "_v1";

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

    function clearLog() {
        byId("log").textContent = "ready";
        byId("response").textContent = "none";
    }

    function renderInputs() {
        const root = byId("inputs");
        root.innerHTML = "";
        config.inputs.forEach((def) => {
            const row = document.createElement("div");
            row.className = "row";

            const label = document.createElement("label");
            label.setAttribute("for", def.id);
            label.textContent = def.label;

            let el;
            if (def.type === "textarea") {
                el = document.createElement("textarea");
            } else if (def.type === "select") {
                el = document.createElement("select");
                (def.options || []).forEach((opt) => {
                    const option = document.createElement("option");
                    if (typeof opt === "string") {
                        option.value = opt;
                        option.textContent = opt;
                    } else {
                        option.value = opt.value;
                        option.textContent = opt.label || opt.value;
                    }
                    el.appendChild(option);
                });
            } else {
                el = document.createElement("input");
            }
            el.id = def.id;
            el.className = "mono";
            if (def.readonly) el.readOnly = true;
            if (def.placeholder) el.placeholder = def.placeholder;
            if (def.defaultValue !== undefined) el.value = def.defaultValue;

            row.appendChild(label);
            row.appendChild(el);
            root.appendChild(row);
        });
    }

    function renderActions() {
        const root = byId("actions");
        root.innerHTML = "";
        config.actions.forEach((action, idx) => {
            const btn = document.createElement("button");
            btn.type = "button";
            btn.textContent = action.label;
            btn.addEventListener("click", function () {
                runAction(idx);
            });
            root.appendChild(btn);
        });
    }

    function collectValues() {
        const v = {};
        config.inputs.forEach((def) => {
            v[def.id] = getValue(def.id);
        });
        return v;
    }

    function saveInputs() {
        const state = collectValues();
        localStorage.setItem(storageKey, JSON.stringify(state));
        log("inputs saved", state);
    }

    function loadInputs() {
        const raw = localStorage.getItem(storageKey);
        if (!raw) return;
        try {
            const state = JSON.parse(raw);
            Object.keys(state).forEach((k) => setValue(k, state[k] || ""));
        } catch (e) {
            log("load inputs parse error", String(e));
        }
    }

    function resolveStringTemplate(text, values) {
        let out = text || "";
        Object.keys(values).forEach((k) => {
            out = out.split("{" + k + "}").join(values[k] || "");
        });
        return out;
    }

    function resolveValueTemplate(value, values) {
        if (typeof value === "string") {
            const resolved = resolveStringTemplate(value, values);
            if (/^-?\d+(\.\d+)?$/.test(resolved)) return Number(resolved);
            return resolved;
        }
        if (Array.isArray(value)) return value.map((v) => resolveValueTemplate(v, values));
        if (value && typeof value === "object") {
            const out = {};
            Object.keys(value).forEach((k) => {
                out[k] = resolveValueTemplate(value[k], values);
            });
            return out;
        }
        return value;
    }

    function hasUnresolved(text) {
        return /\{[a-zA-Z0-9_]+\}/.test(text || "");
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

    function extractData(responseBody) {
        if (!responseBody || typeof responseBody !== "object") return null;
        if (responseBody.success === true && responseBody.data !== undefined) return responseBody.data;
        return responseBody;
    }

    function findFirstValue(obj, key) {
        if (obj == null) return undefined;
        if (Array.isArray(obj)) {
            for (let i = 0; i < obj.length; i++) {
                const v = findFirstValue(obj[i], key);
                if (v !== undefined) return v;
            }
            return undefined;
        }
        if (typeof obj !== "object") return undefined;
        if (obj[key] !== undefined && obj[key] !== null) return obj[key];
        const keys = Object.keys(obj);
        for (let i = 0; i < keys.length; i++) {
            const v = findFirstValue(obj[keys[i]], key);
            if (v !== undefined) return v;
        }
        return undefined;
    }

    function syncKnownFields(responseBody) {
        const data = extractData(responseBody);
        if (!data) return;
        const targets = ["orderId", "disputeId", "invoiceId", "itemId", "shipperId", "driverUserId", "pgOrderId", "paymentKey", "amount"];
        targets.forEach((k) => {
            const v = findFirstValue(data, k);
            if (v !== undefined && byId(k)) setValue(k, String(v));
        });
    }

    function validateRequired(action, values) {
        const required = action.required || [];
        const missing = required.filter((id) => !values[id]);
        if (missing.length > 0) {
            alert("필수값 누락: " + missing.join(", "));
            return false;
        }
        return true;
    }

    async function runRequestAction(action) {
        const values = collectValues();
        if (!validateRequired(action, values)) return;

        const method = (action.method || "GET").toUpperCase();
        const path = resolveStringTemplate(action.path || "", values);
        if (!path || hasUnresolved(path)) {
            alert("path 템플릿 치환 실패: " + path);
            return;
        }

        const baseUrl = values.baseUrl || window.location.origin;
        const headers = {};
        if (action.tokenId) {
            const token = values[action.tokenId];
            if (!token) {
                alert(action.tokenId + " 값이 필요합니다.");
                return;
            }
            headers.Authorization = "Bearer " + token;
        }

        const options = { method: method, headers: headers };
        if (action.body !== undefined) {
            const bodyObj = resolveValueTemplate(action.body, values);
            headers["Content-Type"] = "application/json";
            options.body = JSON.stringify(bodyObj);
        }

        saveInputs();
        log("request", { method: method, url: baseUrl + path, body: options.body ? JSON.parse(options.body) : null });

        const res = await fetch(baseUrl + path, options);
        const body = await readResponseBody(res);
        syncKnownFields(body);
        saveInputs();

        byId("response").textContent = JSON.stringify({
            status: res.status,
            body: body
        }, null, 2);
        log("response", { status: res.status });
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
            memo: "admin cycle bootstrap",
            tag: ["admin", "cycle"],
            distance: 12000,
            duration: 1800
        };
    }

    async function patchDriverStatus(baseUrl, orderId, status, driverToken) {
        const url = baseUrl + "/api/v1/orders/" + orderId + "/status?newStatus=" + encodeURIComponent(status);
        const res = await fetch(url, {
            method: "PATCH",
            headers: { Authorization: "Bearer " + driverToken }
        });
        const body = await readResponseBody(res);
        log("driver status", { status: status, statusCode: res.status, body: body });
        return res.ok;
    }

    function parseDriverStatusFlow(raw) {
        if (!raw) return ["LOADING", "IN_TRANSIT", "UNLOADING", "COMPLETED"];
        const statuses = raw
                .split(/[\n,]/)
                .map((s) => s.trim().toUpperCase())
                .filter((s) => s.length > 0);
        return statuses.length > 0 ? statuses : ["LOADING", "IN_TRANSIT", "UNLOADING", "COMPLETED"];
    }

    async function bootstrapOrderFlow() {
        const values = collectValues();
        const baseUrl = values.baseUrl || window.location.origin;
        const shipperToken = values.shipperToken;
        const driverToken = values.driverToken;
        if (!shipperToken || !driverToken) {
            alert("shipperToken, driverToken이 필요합니다.");
            return;
        }

        const createRes = await fetch(baseUrl + "/api/v1/orders", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Authorization: "Bearer " + shipperToken
            },
            body: JSON.stringify(buildDummyOrderRequest())
        });
        const createBody = await readResponseBody(createRes);
        const created = extractData(createBody);
        const orderId = created && created.orderId ? String(created.orderId) : "";
        log("order create", { status: createRes.status, body: createBody });
        if (!createRes.ok || !orderId) {
            alert("주문 생성 실패");
            return;
        }

        const acceptRes = await fetch(baseUrl + "/api/v1/orders/" + orderId + "/accept", {
            method: "PATCH",
            headers: { Authorization: "Bearer " + driverToken }
        });
        const acceptBody = await readResponseBody(acceptRes);
        log("driver accept", { status: acceptRes.status, body: acceptBody });
        if (!acceptRes.ok) {
            alert("차주 수락 실패");
            return;
        }

        const statuses = parseDriverStatusFlow(values.driverStatusFlow);
        log("driver status flow", { statuses: statuses });
        for (let i = 0; i < statuses.length; i++) {
            const ok = await patchDriverStatus(baseUrl, orderId, statuses[i], driverToken);
            if (!ok) {
                alert("운송 상태 변경 실패: " + statuses[i]);
                return;
            }
        }

        setValue("orderId", orderId);
        await syncContext();
        saveInputs();
        alert("완료: orderId=" + orderId);
    }

    async function syncContext() {
        const values = collectValues();
        const baseUrl = values.baseUrl || window.location.origin;
        const orderId = values.orderId;
        if (!orderId) {
            alert("orderId가 필요합니다.");
            return;
        }
        const token = values.adminToken || values.shipperToken || values.driverToken || "";
        const headers = token ? { Authorization: "Bearer " + token } : {};
        const url = baseUrl + "/api/v1/payments/api-test/context?orderId=" + encodeURIComponent(orderId);
        const res = await fetch(url, { method: "GET", headers: headers });
        const body = await readResponseBody(res);
        syncKnownFields(body);
        saveInputs();
        byId("response").textContent = JSON.stringify({ status: res.status, body: body }, null, 2);
        log("context sync", { status: res.status });
    }

    async function resolvePayoutItemId() {
        const values = collectValues();
        const baseUrl = values.baseUrl || window.location.origin;
        const orderId = values.orderId;
        const token = values.adminToken || values.shipperToken || values.driverToken || "";
        const headers = token ? { Authorization: "Bearer " + token } : {};
        const query = orderId ? ("?orderId=" + encodeURIComponent(orderId)) : "";
        const url = baseUrl + "/api/v1/payments/api-test/context" + query;

        const res = await fetch(url, { method: "GET", headers: headers });
        const body = await readResponseBody(res);
        syncKnownFields(body);
        saveInputs();
        byId("response").textContent = JSON.stringify({ status: res.status, body: body }, null, 2);

        const itemId = getValue("itemId");
        if (itemId) {
            log("itemId resolved", { orderId: orderId || null, itemId: itemId });
            return;
        }
        log("itemId resolve failed", { orderId: orderId || null, status: res.status });
        alert("itemId를 찾지 못했습니다. payouts/run 먼저 실행하거나 orderId를 지정하세요.");
    }

    async function openTossPaymentWindow() {
        const values = collectValues();
        if (!values.clientKey || !values.pgOrderId || !values.amount) {
            alert("clientKey, pgOrderId, amount가 필요합니다.");
            return;
        }
        if (!window.TossPayments) {
            alert("토스 SDK 로드 실패");
            return;
        }

        const amount = Number(values.amount);
        if (!Number.isFinite(amount) || amount <= 0) {
            alert("amount 값이 유효하지 않습니다.");
            return;
        }

        saveInputs();

        try {
            const tossPayments = window.TossPayments(values.clientKey);
            const payment = tossPayments.payment({ customerKey: "cycle_" + Date.now() });
            await payment.requestPayment({
                method: "CARD",
                amount: {
                    currency: "KRW",
                    value: amount
                },
                orderId: values.pgOrderId,
                orderName: "Admin Cycle A Toss Test",
                successUrl: values.successUrl || getCycleSuccessUrl(),
                failUrl: values.failUrl || getCycleFailUrl()
            });
        } catch (firstError) {
            try {
                const tossPayments = window.TossPayments(values.clientKey);
                await tossPayments.requestPayment("카드", {
                    amount: amount,
                    orderId: values.pgOrderId,
                    orderName: "Admin Cycle A Toss Test",
                    successUrl: values.successUrl || getCycleSuccessUrl(),
                    failUrl: values.failUrl || getCycleFailUrl()
                });
            } catch (fallbackError) {
                log("결제창 호출 실패", {
                    firstError: String(firstError),
                    fallbackError: String(fallbackError)
                });
                alert("결제창 호출 실패");
            }
        }
    }

    function applyRedirectParams() {
        if (cycleCode !== "A" && cycleCode !== "F") return;
        const params = new URLSearchParams(window.location.search);
        const flow = params.get("flow");
        const paymentKey = params.get("paymentKey");
        const pgOrderId = params.get("orderId");
        const amount = params.get("amount");
        const code = params.get("code");
        const message = params.get("message");

        if (paymentKey && byId("paymentKey")) setValue("paymentKey", paymentKey);
        if (pgOrderId && byId("pgOrderId")) setValue("pgOrderId", pgOrderId);
        if (amount && byId("amount")) setValue("amount", amount);

        if (flow || paymentKey || pgOrderId || amount || code || message) {
            log("토스 리다이렉트 파라미터", {
                flow: flow,
                paymentKey: paymentKey,
                orderId: pgOrderId,
                amount: amount,
                code: code,
                message: message
            });
        }
    }

    async function runAction(index) {
        const action = config.actions[index];
        if (!action) return;
        if (action.kind === "request") {
            await runRequestAction(action);
            return;
        }
        if (action.kind === "custom") {
            if (action.custom === "bootstrapOrderFlow") {
                await bootstrapOrderFlow();
                return;
            }
            if (action.custom === "syncContext") {
                await syncContext();
                return;
            }
            if (action.custom === "resolvePayoutItemId") {
                await resolvePayoutItemId();
                return;
            }
            if (action.custom === "openTossPaymentWindow") {
                await openTossPaymentWindow();
                return;
            }
        }
    }

    function init() {
        byId("pageTitle").textContent = "[" + cycleCode + "] " + config.title;
        byId("pageDesc").textContent = config.desc;
        renderInputs();
        if (cycleCode === "A" || cycleCode === "F") {
            if (byId("successUrl")) setValue("successUrl", getCycleSuccessUrl());
            if (byId("failUrl")) setValue("failUrl", getCycleFailUrl());
        }
        loadInputs();
        if (cycleCode === "A" || cycleCode === "F") {
            if (!getValue("successUrl")) setValue("successUrl", getCycleSuccessUrl());
            if (!getValue("failUrl")) setValue("failUrl", getCycleFailUrl());
        }
        applyRedirectParams();
        renderActions();
        byId("saveBtn").addEventListener("click", saveInputs);
        byId("clearBtn").addEventListener("click", clearLog);
        log("page initialized", { cycle: cycleCode, url: window.location.href });
    }

    init();
})();
