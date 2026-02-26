window.PaymentApiTestConfig = {
    scenarioSteps: [
        { title: "1) Toss prepare", preset: "tossPrepare", token: "SHIPPER" },
        { title: "2) Toss confirm", preset: "tossConfirm", token: "SHIPPER" },
        { title: "3) Driver confirm", preset: "driverConfirm", token: "DRIVER" },
        { title: "4) Admin fee batch", preset: "adminFeeRun", token: "ADMIN" },
        { title: "5) Fee invoice get", preset: "feeInvoiceMe", token: "SHIPPER" },
        { title: "6) Fee invoice mark-paid", preset: "feeInvoicePaid", token: "SHIPPER" },
        { title: "7) Admin payout batch", preset: "adminPayoutRun", token: "ADMIN" },
        { title: "8) Admin payout retry", preset: "adminRetryItem", token: "ADMIN" },
        { title: "9) Admin reconciliation", preset: "adminReconciliation", token: "ADMIN" }
    ],
    presets: {
        tossPrepare: {
            method: "POST",
            path: "/api/v1/payments/orders/<ORDER_ID>/toss/prepare",
            body: {
                method: "CARD",
                payChannel: "APP_CARD",
                successUrl: "barotruck://pay/success",
                failUrl: "barotruck://pay/fail"
            }
        },
        tossConfirm: {
            method: "POST",
            path: "/api/v1/payments/orders/<ORDER_ID>/toss/confirm",
            body: {
                paymentKey: "<PAYMENT_KEY>",
                pgOrderId: "<PG_ORDER_ID>",
                amount: "<AMOUNT>"
            }
        },
        driverConfirm: {
            method: "POST",
            path: "/api/v1/payments/orders/<ORDER_ID>/confirm",
            body: null
        },
        adminFeeRun: {
            method: "POST",
            path: "/api/admin/payment/fee-invoices/run?period=2026-02",
            body: null
        },
        feeInvoiceMe: {
            method: "GET",
            path: "/api/v1/payments/fee-invoices/me?period=2026-02",
            body: null
        },
        feeInvoicePaid: {
            method: "POST",
            path: "/api/v1/payments/fee-invoices/<INVOICE_ID>/mark-paid",
            body: null
        },
        adminPayoutRun: {
            method: "POST",
            path: "/api/admin/payment/payouts/run?date=2026-02-23",
            body: null
        },
        adminRetryItem: {
            method: "POST",
            path: "/api/admin/payment/payout-items/<ITEM_ID>/retry",
            body: null
        },
        adminReconciliation: {
            method: "POST",
            path: "/api/admin/payment/reconciliation/run",
            body: null
        }
    }
};

