    -- test1@1.1(화주), test2@2.2(차주) 기준
    -- Spring data.sql 호환용 일반 SQL 스크립트
    -- 재실행 시 이 스크립트가 만든 건만 지우고 다시 생성합니다.

    DELETE FROM DRIVER_PAYOUT_ITEMS
    WHERE ORDER_ID IN (
        SELECT ORDER_ID
        FROM ORDERS
        WHERE USER_ID = (SELECT USER_ID FROM USERS WHERE EMAIL = 'test1@1.1')
        AND DRIVER_NO = (SELECT USER_ID FROM USERS WHERE EMAIL = 'test2@2.2')
        AND MEMO LIKE 'AUTO_SETTLEMENT_TEST_T1_T2_%'
    );

    DELETE FROM TRANSPORT_PAYMENTS
    WHERE ORDER_ID IN (
        SELECT ORDER_ID
        FROM ORDERS
        WHERE USER_ID = (SELECT USER_ID FROM USERS WHERE EMAIL = 'test1@1.1')
        AND DRIVER_NO = (SELECT USER_ID FROM USERS WHERE EMAIL = 'test2@2.2')
        AND MEMO LIKE 'AUTO_SETTLEMENT_TEST_T1_T2_%'
    );

    DELETE FROM SETTLEMENT
    WHERE ORDER_ID IN (
        SELECT ORDER_ID
        FROM ORDERS
        WHERE USER_ID = (SELECT USER_ID FROM USERS WHERE EMAIL = 'test1@1.1')
        AND DRIVER_NO = (SELECT USER_ID FROM USERS WHERE EMAIL = 'test2@2.2')
        AND MEMO LIKE 'AUTO_SETTLEMENT_TEST_T1_T2_%'
    );

    DELETE FROM ORDERS
    WHERE USER_ID = (SELECT USER_ID FROM USERS WHERE EMAIL = 'test1@1.1')
    AND DRIVER_NO = (SELECT USER_ID FROM USERS WHERE EMAIL = 'test2@2.2')
    AND MEMO LIKE 'AUTO_SETTLEMENT_TEST_T1_T2_%';

    INSERT INTO ORDERS (
        USER_ID,
        DRIVER_NO,
        VERSION,
        DISTANCE,
        DURATION,
        STATUS,
        UPDATED,
        START_ADDR,
        START_PLACE,
        START_TYPE,
        START_SCHEDULE,
        PU_PROVINCE,
        START_LAT,
        START_LNG,
        END_ADDR,
        END_PLACE,
        END_TYPE,
        END_SCHEDULE,
        DO_PROVINCE,
        CARGO_CONTENT,
        LOAD_METHOD,
        WORK_TYPE,
        TONNAGE,
        REQ_CAR_TYPE,
        REQ_TONNAGE,
        DRIVE_MODE,
        LOAD_WEIGHT,
        BASE_PRICE,
        LABOR_FEE,
        PACKAGING_PRICE,
        INSURANCE_FEE,
        PAY_METHOD,
        START_NBH_ID,
        END_NBH_ID,
        MEMO,
        INSTANT,
        ACCEPTED,
        START_TIME,
        END_TIME,
        COMPLETED,
        CREATED_AT
    )
    WITH seed_rows AS (
        SELECT
            (SELECT USER_ID FROM USERS WHERE EMAIL = 'test1@1.1') AS SHIPPER_ID,
            (SELECT USER_ID FROM USERS WHERE EMAIL = 'test2@2.2') AS DRIVER_ID,
            LEVEL AS LVL,
            SYSTIMESTAMP - NUMTODSINTERVAL(LEVEL, 'DAY') AS COMPLETED_AT,
            SYSTIMESTAMP - NUMTODSINTERVAL(LEVEL, 'DAY') - NUMTODSINTERVAL(20, 'MINUTE') AS END_TIME_AT,
            SYSTIMESTAMP - NUMTODSINTERVAL(LEVEL, 'DAY') - NUMTODSINTERVAL(4, 'HOUR') AS START_TIME_AT,
            SYSTIMESTAMP - NUMTODSINTERVAL(LEVEL, 'DAY') - NUMTODSINTERVAL(6, 'HOUR') AS ACCEPTED_AT,
            SYSTIMESTAMP - NUMTODSINTERVAL(LEVEL, 'DAY') - NUMTODSINTERVAL(2, 'DAY') AS CREATED_AT
        FROM DUAL
        CONNECT BY LEVEL <= 20
    )
    SELECT
        SHIPPER_ID,
        DRIVER_ID,
        0,
        15 + LVL,
        90 + (LVL * 3),
        'COMPLETED',
        COMPLETED_AT,
        '서울 송파구 문정동 ' || LVL || '-1',
        '상차지 ' || LVL,
        '상차',
        TO_CHAR(START_TIME_AT, 'YYYY-MM-DD HH24:MI'),
        '서울',
        37.484000 + (LVL * 0.001),
        127.122000 + (LVL * 0.001),
        '경기 하남시 감일동 ' || LVL || '-2',
        '하차지 ' || LVL,
        '하차',
        TO_CHAR(END_TIME_AT, 'YYYY-MM-DD HH24:MI'),
        '경기',
        '정산 테스트 화물 ' || LVL,
        '수작업',
        '일반운송',
        1,
        '카고',
        '1톤',
        '편도',
        1000 + (LVL * 10),
        120000 + (LVL * 5000),
        10000,
        5000,
        2000,
        'CARD',
        NULL,
        NULL,
        'AUTO_SETTLEMENT_TEST_T1_T2_' || LPAD(LVL, 2, '0'),
        0,
        ACCEPTED_AT,
        START_TIME_AT,
        END_TIME_AT,
        COMPLETED_AT,
        CREATED_AT
    FROM seed_rows
    WHERE SHIPPER_ID IS NOT NULL
    AND DRIVER_ID IS NOT NULL;

    INSERT INTO SETTLEMENT (
        ORDER_ID,
        USER_ID,
        LEVEL_DISCOUNT,
        COUPON_DISCOUNT,
        TOTAL_PRICE,
        FEE_RATE,
        STATUS,
        FEE_DATE,
        FEE_COMPLETE_DATE,
        REMARK_FIELD_5,
        REMARK_FIELD_6
    )
    SELECT
        o.ORDER_ID,
        o.USER_ID,
        0,
        0,
        NVL(o.BASE_PRICE, 0) + NVL(o.LABOR_FEE, 0) + NVL(o.PACKAGING_PRICE, 0) + NVL(o.INSURANCE_FEE, 0),
        10,
        'COMPLETED',
        o.COMPLETED - NUMTODSINTERVAL(10, 'MINUTE'),
        o.COMPLETED,
        'AUTO_SETTLEMENT_TEST',
        'T1_T2'
    FROM ORDERS o
    WHERE o.USER_ID = (SELECT USER_ID FROM USERS WHERE EMAIL = 'test1@1.1')
    AND o.DRIVER_NO = (SELECT USER_ID FROM USERS WHERE EMAIL = 'test2@2.2')
    AND o.MEMO LIKE 'AUTO_SETTLEMENT_TEST_T1_T2_%'
    AND NOT EXISTS (
        SELECT 1
            FROM SETTLEMENT st
        WHERE st.ORDER_ID = o.ORDER_ID
    );

    INSERT INTO TRANSPORT_PAYMENTS (
        ORDER_ID,
        SHIPPER_USER_ID,
        DRIVER_USER_ID,
        AMOUNT,
        FEE_RATE_SNAPSHOT,
        FEE_AMOUNT_SNAPSHOT,
        NET_AMOUNT_SNAPSHOT,
        METHOD,
        PAYMENT_TIMING,
        STATUS,
        PG_TID,
        PROOF_URL,
        PAID_AT,
        CONFIRMED_AT,
        CREATED_AT
    )
    SELECT
        o.ORDER_ID,
        o.USER_ID,
        o.DRIVER_NO,
        NVL(o.BASE_PRICE, 0) + NVL(o.LABOR_FEE, 0) + NVL(o.PACKAGING_PRICE, 0) + NVL(o.INSURANCE_FEE, 0),
        0.1000,
        ROUND((NVL(o.BASE_PRICE, 0) + NVL(o.LABOR_FEE, 0) + NVL(o.PACKAGING_PRICE, 0) + NVL(o.INSURANCE_FEE, 0)) * 0.10),
        (NVL(o.BASE_PRICE, 0) + NVL(o.LABOR_FEE, 0) + NVL(o.PACKAGING_PRICE, 0) + NVL(o.INSURANCE_FEE, 0))
            - ROUND((NVL(o.BASE_PRICE, 0) + NVL(o.LABOR_FEE, 0) + NVL(o.PACKAGING_PRICE, 0) + NVL(o.INSURANCE_FEE, 0)) * 0.10),
        'CARD',
        'POSTPAID',
        'CONFIRMED',
        'MANUAL-SEED-' || o.ORDER_ID,
        NULL,
        o.COMPLETED - NUMTODSINTERVAL(10, 'MINUTE'),
        o.COMPLETED,
        o.CREATED_AT
    FROM ORDERS o
    WHERE o.USER_ID = (SELECT USER_ID FROM USERS WHERE EMAIL = 'test1@1.1')
    AND o.DRIVER_NO = (SELECT USER_ID FROM USERS WHERE EMAIL = 'test2@2.2')
    AND o.MEMO LIKE 'AUTO_SETTLEMENT_TEST_T1_T2_%'
    AND NOT EXISTS (
        SELECT 1
            FROM TRANSPORT_PAYMENTS tp
        WHERE tp.ORDER_ID = o.ORDER_ID
    );
        