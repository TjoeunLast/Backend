-- 리뷰, 신고, 정산 프론트 표시용 더미 시드
-- 실제 결제 인증 흐름보다는 관리자 화면에 값이 보이도록 맞춘 데이터입니다.



MERGE INTO USERS u
    USING (SELECT 'test4@4.4' email, '어드민테스트' name, '관리자123' nick, 'ADMIN' role FROM dual) s
    ON (u.email = s.email)
    WHEN NOT MATCHED THEN
        INSERT (user_id, name, nickname, email, password, is_owner, enrolldate, delflag, user_level)
            VALUES (SEQ_USER_ID.NEXTVAL, s.name, s.nick, s.email,
                    '$2a$10$4HC/eANBtL.QFcP6kaZ0z.dsOvgz/YhS7SS42Z7wuMAcKG3KSuauq',
                    s.role, CURRENT_DATE, 'N', 0);

-- 리뷰 데이터
MERGE INTO REVIEWS rv
USING (
    SELECT *
    FROM (
        SELECT
            (SELECT user_id FROM USERS WHERE email = 'yungi@gmail.com') AS w_id,
            (SELECT user_id FROM USERS WHERE email = 'juun@naver.com') AS t_id,
            (SELECT order_id
               FROM ORDERS
              WHERE user_id = (SELECT user_id FROM USERS WHERE email = 'yungi@gmail.com')
                AND driver_no = (SELECT user_id FROM USERS WHERE email = 'juun@naver.com')
                AND status = 'COMPLETED'
                AND distance = 395
                AND duration = 18000) AS o_id,
            5 AS rate,
            '정말 친절하시고 운전도 안전하게 잘 하십니다.' AS cont,
            '2025-10-16 10:00:00' AS c_at
        FROM dual
        UNION ALL
        SELECT
            (SELECT user_id FROM USERS WHERE email = 'jimyeong@daum.net') AS w_id,
            (SELECT user_id FROM USERS WHERE email = 'mikyeong@gmail.com') AS t_id,
            (SELECT order_id
               FROM ORDERS
              WHERE user_id = (SELECT user_id FROM USERS WHERE email = 'jimyeong@daum.net')
                AND driver_no = (SELECT user_id FROM USERS WHERE email = 'mikyeong@gmail.com')
                AND status = 'COMPLETED'
                AND distance = 45
                AND duration = 3600) AS o_id,
            4 AS rate,
            '시간 맞춰 잘 와주셨네요. 수고하셨습니다.' AS cont,
            '2025-11-20 18:00:00' AS c_at
        FROM dual
        UNION ALL
        SELECT
            (SELECT user_id FROM USERS WHERE email = 'jinil@kakao.com') AS w_id,
            (SELECT user_id FROM USERS WHERE email = 'sinwoo@outlook.com') AS t_id,
            (SELECT order_id
               FROM ORDERS
              WHERE user_id = (SELECT user_id FROM USERS WHERE email = 'jinil@kakao.com')
                AND driver_no = (SELECT user_id FROM USERS WHERE email = 'sinwoo@outlook.com')
                AND status = 'COMPLETED'
                AND distance = 180
                AND duration = 10800) AS o_id,
            5 AS rate,
            '냉동 온도 유지를 철저히 해주셔서 안심되었습니다.' AS cont,
            '2025-12-05 16:00:00' AS c_at
        FROM dual
        UNION ALL
        SELECT
            (SELECT user_id FROM USERS WHERE email = 'okdong@gmail.com') AS w_id,
            (SELECT user_id FROM USERS WHERE email = 'yumi@naver.com') AS t_id,
            (SELECT order_id
               FROM ORDERS
              WHERE user_id = (SELECT user_id FROM USERS WHERE email = 'okdong@gmail.com')
                AND driver_no = (SELECT user_id FROM USERS WHERE email = 'yumi@naver.com')
                AND status = 'COMPLETED'
                AND distance = 55
                AND duration = 420) AS o_id,
            3 AS rate,
            '연락이 조금 늦었지만 물건은 무사히 도착했습니다.' AS cont,
            '2026-01-10 14:00:00' AS c_at
        FROM dual
        UNION ALL
        SELECT
            (SELECT user_id FROM USERS WHERE email = 'jiman@daum.net') AS w_id,
            (SELECT user_id FROM USERS WHERE email = 'junhyeok@gmail.com') AS t_id,
            (SELECT order_id
               FROM ORDERS
              WHERE user_id = (SELECT user_id FROM USERS WHERE email = 'jiman@daum.net')
                AND driver_no = (SELECT user_id FROM USERS WHERE email = 'junhyeok@gmail.com')
                AND status = 'COMPLETED'
                AND distance = 110
                AND duration = 5400) AS o_id,
            5 AS rate,
            '짐이 많았는데도 꼼꼼하게 결박해서 운송해주셨어요.' AS cont,
            '2026-02-16 11:00:00' AS c_at
        FROM dual
    )
    WHERE w_id IS NOT NULL
      AND t_id IS NOT NULL
      AND o_id IS NOT NULL
) s ON (rv.order_id = s.o_id AND rv.writer_id = s.w_id)
WHEN NOT MATCHED THEN
    INSERT (writer_id, target_id, order_id, rating, content, created_at)
    VALUES (
        s.w_id,
        s.t_id,
        s.o_id,
        s.rate,
        s.cont,
        TO_TIMESTAMP(s.c_at, 'YYYY-MM-DD HH24:MI:SS')
    );

-- 신고 데이터
MERGE INTO REPORTS r
USING (
    SELECT *
    FROM (
        SELECT
            (SELECT user_id FROM USERS WHERE email = 'jinil@kakao.com') AS rep_id,
            (SELECT user_id FROM USERS WHERE email = 'sinwoo@outlook.com') AS t_id,
            (SELECT order_id
               FROM ORDERS
              WHERE user_id = (SELECT user_id FROM USERS WHERE email = 'jinil@kakao.com')
                AND driver_no = (SELECT user_id FROM USERS WHERE email = 'sinwoo@outlook.com')
                AND status = 'COMPLETED'
                AND distance = 180
                AND duration = 10800) AS o_id,
            'NO_SHOW' AS r_type,
            '상차지에 연락 없이 2시간 늦게 도착했습니다.' AS desc_text,
            'REPORT' AS r_kind,
            'jinil@kakao.com' AS r_email,
            '운송 지연 신고' AS r_title,
            '2025-12-05 15:30:00' AS c_at
        FROM dual
        UNION ALL
        SELECT
            (SELECT user_id FROM USERS WHERE email = 'yumi@naver.com') AS rep_id,
            (SELECT user_id FROM USERS WHERE email = 'okdong@gmail.com') AS t_id,
            (SELECT order_id
               FROM ORDERS
              WHERE user_id = (SELECT user_id FROM USERS WHERE email = 'okdong@gmail.com')
                AND driver_no = (SELECT user_id FROM USERS WHERE email = 'yumi@naver.com')
                AND status = 'COMPLETED'
                AND distance = 55
                AND duration = 420) AS o_id,
            'ETC' AS r_type,
            '현장에서 추가 수작업 비용을 과다하게 요구합니다.' AS desc_text,
            'REPORT' AS r_kind,
            'yumi@naver.com' AS r_email,
            '부당 요구 신고' AS r_title,
            '2026-01-10 13:30:00' AS c_at
        FROM dual
        UNION ALL
        SELECT
            (SELECT user_id FROM USERS WHERE email = 'jimyeong@daum.net') AS rep_id,
            (SELECT user_id FROM USERS WHERE email = 'mikyeong@gmail.com') AS t_id,
            (SELECT order_id
               FROM ORDERS
              WHERE user_id = (SELECT user_id FROM USERS WHERE email = 'jimyeong@daum.net')
                AND driver_no = (SELECT user_id FROM USERS WHERE email = 'mikyeong@gmail.com')
                AND status = 'COMPLETED'
                AND distance = 45
                AND duration = 3600) AS o_id,
            'RUDE' AS r_type,
            '화주에게 시종일관 반말과 고성으로 응대했습니다.' AS desc_text,
            'REPORT' AS r_kind,
            'jimyeong@daum.net' AS r_email,
            '불친절 신고' AS r_title,
            '2025-11-20 17:30:00' AS c_at
        FROM dual
        UNION ALL
        SELECT
            (SELECT user_id FROM USERS WHERE email = 'yungi@gmail.com') AS rep_id,
            (SELECT user_id FROM USERS WHERE email = 'juun@naver.com') AS t_id,
            (SELECT order_id
               FROM ORDERS
              WHERE user_id = (SELECT user_id FROM USERS WHERE email = 'yungi@gmail.com')
                AND driver_no = (SELECT user_id FROM USERS WHERE email = 'juun@naver.com')
                AND status = 'COMPLETED'
                AND distance = 395
                AND duration = 18000) AS o_id,
            'ACCIDENT' AS r_type,
            '가전제품 모서리가 파손된 상태로 배송되었습니다.' AS desc_text,
            'REPORT' AS r_kind,
            'yungi@gmail.com' AS r_email,
            '물품 파손 신고' AS r_title,
            '2025-10-15 19:00:00' AS c_at
        FROM dual
        UNION ALL
        SELECT
            (SELECT user_id FROM USERS WHERE email = 'jiman@daum.net') AS rep_id,
            (SELECT user_id FROM USERS WHERE email = 'junhyeok@gmail.com') AS t_id,
            (SELECT order_id
               FROM ORDERS
              WHERE user_id = (SELECT user_id FROM USERS WHERE email = 'jiman@daum.net')
                AND driver_no = (SELECT user_id FROM USERS WHERE email = 'junhyeok@gmail.com')
                AND status = 'COMPLETED'
                AND distance = 110
                AND duration = 5400) AS o_id,
            'ETC' AS r_type,
            '신청한 차량 제원과 다른 차량이 도착했습니다.' AS desc_text,
            'REPORT' AS r_kind,
            'jiman@daum.net' AS r_email,
            '차량 불일치 신고' AS r_title,
            '2026-02-16 10:30:00' AS c_at
        FROM dual
        UNION ALL
        SELECT
            (SELECT user_id FROM USERS WHERE email = 'yungi@gmail.com') AS rep_id,
            (SELECT user_id FROM USERS WHERE email = 'juun@naver.com') AS t_id,
            (SELECT order_id
               FROM ORDERS
              WHERE user_id = (SELECT user_id FROM USERS WHERE email = 'yungi@gmail.com')
                AND driver_no = (SELECT user_id FROM USERS WHERE email = 'juun@naver.com')
                AND status = 'COMPLETED'
                AND distance = 395
                AND duration = 18000) AS o_id,
            'ETC' AS r_type,
            '파손 보상 범위와 처리 절차를 먼저 협의하고 싶습니다.' AS desc_text,
            'DISCUSS' AS r_kind,
            'yungi@gmail.com' AS r_email,
            '보상 절차 협의' AS r_title,
            '2025-10-15 19:10:00' AS c_at
        FROM dual
        UNION ALL
        SELECT
            (SELECT user_id FROM USERS WHERE email = 'jimyeong@daum.net') AS rep_id,
            (SELECT user_id FROM USERS WHERE email = 'mikyeong@gmail.com') AS t_id,
            (SELECT order_id
               FROM ORDERS
              WHERE user_id = (SELECT user_id FROM USERS WHERE email = 'jimyeong@daum.net')
                AND driver_no = (SELECT user_id FROM USERS WHERE email = 'mikyeong@gmail.com')
                AND status = 'COMPLETED'
                AND distance = 45
                AND duration = 3600) AS o_id,
            'ETC' AS r_type,
            '운송 완료 후 정산 기준과 영수증 처리 방식을 논의하려고 남겼습니다.' AS desc_text,
            'DISCUSS' AS r_kind,
            'jimyeong@daum.net' AS r_email,
            '정산 처리 협의' AS r_title,
            '2025-11-20 17:40:00' AS c_at
        FROM dual
    )
    WHERE rep_id IS NOT NULL
      AND t_id IS NOT NULL
      AND o_id IS NOT NULL
) s ON (
    r.order_id = s.o_id
    AND r.reporter_id = s.rep_id
    AND NVL(r.type, 'REPORT') = s.r_kind
)
WHEN NOT MATCHED THEN
    INSERT (
        reporter_id,
        target_id,
        order_id,
        report_type,
        description,
        status,
        type,
        email,
        title,
        created_at
    )
    VALUES (
        s.rep_id,
        s.t_id,
        s.o_id,
        s.r_type,
        s.desc_text,
        'PENDING',
        s.r_kind,
        s.r_email,
        s.r_title,
        TO_TIMESTAMP(s.c_at, 'YYYY-MM-DD HH24:MI:SS')
    );

-- 정산 데이터
MERGE INTO SETTLEMENT st
USING (
    SELECT *
    FROM (
        SELECT
            (SELECT user_id FROM USERS WHERE email = 'yungi@gmail.com') AS u_id,
            (SELECT order_id
               FROM ORDERS
              WHERE user_id = (SELECT user_id FROM USERS WHERE email = 'yungi@gmail.com')
                AND driver_no = (SELECT user_id FROM USERS WHERE email = 'juun@naver.com')
                AND status = 'COMPLETED'
                AND distance = 395
                AND duration = 18000) AS o_id,
            0 AS level_dc,
            0 AS coupon_dc,
            410000 AS price,
            10 AS fee_rate,
            'COMPLETED' AS stat,
            '2026-03-05 09:00:00' AS fee_at,
            '2026-03-05 18:00:00' AS done_at
        FROM dual
        UNION ALL
        SELECT
            (SELECT user_id FROM USERS WHERE email = 'jimyeong@daum.net') AS u_id,
            (SELECT order_id
               FROM ORDERS
              WHERE user_id = (SELECT user_id FROM USERS WHERE email = 'jimyeong@daum.net')
                AND driver_no = (SELECT user_id FROM USERS WHERE email = 'mikyeong@gmail.com')
                AND status = 'COMPLETED'
                AND distance = 45
                AND duration = 3600) AS o_id,
            0 AS level_dc,
            0 AS coupon_dc,
            90000 AS price,
            10 AS fee_rate,
            'COMPLETED' AS stat,
            '2026-03-06 09:00:00' AS fee_at,
            '2026-03-06 15:00:00' AS done_at
        FROM dual
        UNION ALL
        SELECT
            (SELECT user_id FROM USERS WHERE email = 'jinil@kakao.com') AS u_id,
            (SELECT order_id
               FROM ORDERS
              WHERE user_id = (SELECT user_id FROM USERS WHERE email = 'jinil@kakao.com')
                AND driver_no = (SELECT user_id FROM USERS WHERE email = 'sinwoo@outlook.com')
                AND status = 'COMPLETED'
                AND distance = 180
                AND duration = 10800) AS o_id,
            0 AS level_dc,
            0 AS coupon_dc,
            205000 AS price,
            10 AS fee_rate,
            'WAIT' AS stat,
            '2026-03-15 09:00:00' AS fee_at,
            NULL AS done_at
        FROM dual
        UNION ALL
        SELECT
            (SELECT user_id FROM USERS WHERE email = 'okdong@gmail.com') AS u_id,
            (SELECT order_id
               FROM ORDERS
              WHERE user_id = (SELECT user_id FROM USERS WHERE email = 'okdong@gmail.com')
                AND driver_no = (SELECT user_id FROM USERS WHERE email = 'yumi@naver.com')
                AND status = 'COMPLETED'
                AND distance = 55
                AND duration = 420) AS o_id,
            0 AS level_dc,
            0 AS coupon_dc,
            80000 AS price,
            10 AS fee_rate,
            'WAIT' AS stat,
            '2026-03-12 09:00:00' AS fee_at,
            NULL AS done_at
        FROM dual
        UNION ALL
        SELECT
            (SELECT user_id FROM USERS WHERE email = 'jiman@daum.net') AS u_id,
            (SELECT order_id
               FROM ORDERS
              WHERE user_id = (SELECT user_id FROM USERS WHERE email = 'jiman@daum.net')
                AND driver_no = (SELECT user_id FROM USERS WHERE email = 'junhyeok@gmail.com')
                AND status = 'COMPLETED'
                AND distance = 110
                AND duration = 5400) AS o_id,
            0 AS level_dc,
            0 AS coupon_dc,
            145000 AS price,
            10 AS fee_rate,
            'COMPLETED' AS stat,
            '2026-03-08 09:00:00' AS fee_at,
            '2026-03-08 13:00:00' AS done_at
        FROM dual
    )
    WHERE u_id IS NOT NULL
      AND o_id IS NOT NULL
) s ON (st.order_id = s.o_id)
WHEN NOT MATCHED THEN
    INSERT (
        order_id,
        user_id,
        level_discount,
        coupon_discount,
        total_price,
        fee_rate,
        status,
        fee_date,
        fee_complete_date
    )
    VALUES (
        s.o_id,
        s.u_id,
        s.level_dc,
        s.coupon_dc,
        s.price,
        s.fee_rate,
        s.stat,
        TO_TIMESTAMP(s.fee_at, 'YYYY-MM-DD HH24:MI:SS'),
        CASE
            WHEN s.done_at IS NOT NULL THEN TO_TIMESTAMP(s.done_at, 'YYYY-MM-DD HH24:MI:SS')
            ELSE NULL
        END
    );

-- 결제 더미 데이터
MERGE INTO TRANSPORT_PAYMENTS tp
USING (
    SELECT *
    FROM (
        SELECT
            (SELECT order_id
               FROM ORDERS
              WHERE user_id = (SELECT user_id FROM USERS WHERE email = 'yungi@gmail.com')
                AND driver_no = (SELECT user_id FROM USERS WHERE email = 'juun@naver.com')
                AND status = 'COMPLETED'
                AND distance = 395
                AND duration = 18000) AS o_id,
            (SELECT user_id FROM USERS WHERE email = 'yungi@gmail.com') AS shipper_id,
            (SELECT user_id FROM USERS WHERE email = 'juun@naver.com') AS driver_id,
            410000 AS amt,
            0.1000 AS fee_rate,
            41000 AS fee_amt,
            369000 AS net_amt,
            'CARD' AS method,
            'POSTPAID' AS pay_timing,
            'CONFIRMED' AS pay_status,
            NULL AS pg_tid,
            NULL AS proof_url,
            '2026-03-05 09:30:00' AS paid_at,
            '2026-03-05 18:00:00' AS confirmed_at,
            '2026-03-05 09:00:00' AS created_at
        FROM dual
        UNION ALL
        SELECT
            (SELECT order_id
               FROM ORDERS
              WHERE user_id = (SELECT user_id FROM USERS WHERE email = 'jimyeong@daum.net')
                AND driver_no = (SELECT user_id FROM USERS WHERE email = 'mikyeong@gmail.com')
                AND status = 'COMPLETED'
                AND distance = 45
                AND duration = 3600) AS o_id,
            (SELECT user_id FROM USERS WHERE email = 'jimyeong@daum.net') AS shipper_id,
            (SELECT user_id FROM USERS WHERE email = 'mikyeong@gmail.com') AS driver_id,
            90000 AS amt,
            0.1000 AS fee_rate,
            9000 AS fee_amt,
            81000 AS net_amt,
            'TRANSFER' AS method,
            'POSTPAID' AS pay_timing,
            'CONFIRMED' AS pay_status,
            NULL AS pg_tid,
            'https://dummy.local/proof/transfer-90000' AS proof_url,
            '2026-03-06 09:10:00' AS paid_at,
            '2026-03-06 15:00:00' AS confirmed_at,
            '2026-03-06 09:00:00' AS created_at
        FROM dual
        UNION ALL
        SELECT
            (SELECT order_id
               FROM ORDERS
              WHERE user_id = (SELECT user_id FROM USERS WHERE email = 'jinil@kakao.com')
                AND driver_no = (SELECT user_id FROM USERS WHERE email = 'sinwoo@outlook.com')
                AND status = 'COMPLETED'
                AND distance = 180
                AND duration = 10800) AS o_id,
            (SELECT user_id FROM USERS WHERE email = 'jinil@kakao.com') AS shipper_id,
            (SELECT user_id FROM USERS WHERE email = 'sinwoo@outlook.com') AS driver_id,
            205000 AS amt,
            0.1000 AS fee_rate,
            20500 AS fee_amt,
            184500 AS net_amt,
            'CASH' AS method,
            'POSTPAID' AS pay_timing,
            'ADMIN_HOLD' AS pay_status,
            NULL AS pg_tid,
            NULL AS proof_url,
            NULL AS paid_at,
            NULL AS confirmed_at,
            '2026-03-15 09:00:00' AS created_at
        FROM dual
        UNION ALL
        SELECT
            (SELECT order_id
               FROM ORDERS
              WHERE user_id = (SELECT user_id FROM USERS WHERE email = 'okdong@gmail.com')
                AND driver_no = (SELECT user_id FROM USERS WHERE email = 'yumi@naver.com')
                AND status = 'COMPLETED'
                AND distance = 55
                AND duration = 420) AS o_id,
            (SELECT user_id FROM USERS WHERE email = 'okdong@gmail.com') AS shipper_id,
            (SELECT user_id FROM USERS WHERE email = 'yumi@naver.com') AS driver_id,
            80000 AS amt,
            0.1000 AS fee_rate,
            8000 AS fee_amt,
            72000 AS net_amt,
            'CARD' AS method,
            'POSTPAID' AS pay_timing,
            'ADMIN_HOLD' AS pay_status,
            NULL AS pg_tid,
            NULL AS proof_url,
            NULL AS paid_at,
            NULL AS confirmed_at,
            '2026-03-12 09:00:00' AS created_at
        FROM dual
        UNION ALL
        SELECT
            (SELECT order_id
               FROM ORDERS
              WHERE user_id = (SELECT user_id FROM USERS WHERE email = 'jiman@daum.net')
                AND driver_no = (SELECT user_id FROM USERS WHERE email = 'junhyeok@gmail.com')
                AND status = 'COMPLETED'
                AND distance = 110
                AND duration = 5400) AS o_id,
            (SELECT user_id FROM USERS WHERE email = 'jiman@daum.net') AS shipper_id,
            (SELECT user_id FROM USERS WHERE email = 'junhyeok@gmail.com') AS driver_id,
            145000 AS amt,
            0.1000 AS fee_rate,
            14500 AS fee_amt,
            130500 AS net_amt,
            'CARD' AS method,
            'POSTPAID' AS pay_timing,
            'CONFIRMED' AS pay_status,
            NULL AS pg_tid,
            NULL AS proof_url,
            '2026-03-08 09:20:00' AS paid_at,
            '2026-03-08 13:00:00' AS confirmed_at,
            '2026-03-08 09:00:00' AS created_at
        FROM dual
    )
    WHERE o_id IS NOT NULL
      AND shipper_id IS NOT NULL
      AND driver_id IS NOT NULL
) s ON (tp.order_id = s.o_id)
WHEN NOT MATCHED THEN
    INSERT (
        order_id,
        shipper_user_id,
        driver_user_id,
        amount,
        fee_rate_snapshot,
        fee_amount_snapshot,
        net_amount_snapshot,
        method,
        payment_timing,
        status,
        pg_tid,
        proof_url,
        paid_at,
        confirmed_at,
        created_at
    )
    VALUES (
        s.o_id,
        s.shipper_id,
        s.driver_id,
        s.amt,
        s.fee_rate,
        s.fee_amt,
        s.net_amt,
        s.method,
        s.pay_timing,
        s.pay_status,
        s.pg_tid,
        s.proof_url,
        CASE
            WHEN s.paid_at IS NOT NULL THEN TO_TIMESTAMP(s.paid_at, 'YYYY-MM-DD HH24:MI:SS')
            ELSE NULL
        END,
        CASE
            WHEN s.confirmed_at IS NOT NULL THEN TO_TIMESTAMP(s.confirmed_at, 'YYYY-MM-DD HH24:MI:SS')
            ELSE NULL
        END,
        TO_TIMESTAMP(s.created_at, 'YYYY-MM-DD HH24:MI:SS')
    );

-- 분쟁 더미 데이터
MERGE INTO PAYMENT_DISPUTES pd
USING (
    SELECT *
    FROM (
        SELECT
            (SELECT order_id
               FROM ORDERS
              WHERE user_id = (SELECT user_id FROM USERS WHERE email = 'jinil@kakao.com')
                AND driver_no = (SELECT user_id FROM USERS WHERE email = 'sinwoo@outlook.com')
                AND status = 'COMPLETED'
                AND distance = 180
                AND duration = 10800) AS o_id,
            (SELECT payment_id
               FROM TRANSPORT_PAYMENTS
              WHERE order_id = (
                    SELECT order_id
                    FROM ORDERS
                    WHERE user_id = (SELECT user_id FROM USERS WHERE email = 'jinil@kakao.com')
                      AND driver_no = (SELECT user_id FROM USERS WHERE email = 'sinwoo@outlook.com')
                      AND status = 'COMPLETED'
                      AND distance = 180
                      AND duration = 10800
                )) AS p_id,
            (SELECT user_id FROM USERS WHERE email = 'sinwoo@outlook.com') AS requester_id,
            (SELECT user_id FROM USERS WHERE email = 'sinwoo@outlook.com') AS created_by_id,
            'OTHER' AS reason_code,
            '프론트 정산 화면 확인용 더미 분쟁 데이터입니다.' AS desc_text,
            NULL AS attachment_url,
            'ADMIN_HOLD' AS dispute_status,
            '정산 보류 화면 표시용 메모' AS admin_memo,
            '2026-03-15 10:00:00' AS requested_at,
            '2026-03-15 10:30:00' AS processed_at
        FROM dual
        UNION ALL
        SELECT
            (SELECT order_id
               FROM ORDERS
              WHERE user_id = (SELECT user_id FROM USERS WHERE email = 'okdong@gmail.com')
                AND driver_no = (SELECT user_id FROM USERS WHERE email = 'yumi@naver.com')
                AND status = 'COMPLETED'
                AND distance = 55
                AND duration = 420) AS o_id,
            (SELECT payment_id
               FROM TRANSPORT_PAYMENTS
              WHERE order_id = (
                    SELECT order_id
                    FROM ORDERS
                    WHERE user_id = (SELECT user_id FROM USERS WHERE email = 'okdong@gmail.com')
                      AND driver_no = (SELECT user_id FROM USERS WHERE email = 'yumi@naver.com')
                      AND status = 'COMPLETED'
                      AND distance = 55
                      AND duration = 420
                )) AS p_id,
            (SELECT user_id FROM USERS WHERE email = 'yumi@naver.com') AS requester_id,
            (SELECT user_id FROM USERS WHERE email = 'yumi@naver.com') AS created_by_id,
            'OTHER' AS reason_code,
            '프론트 정산 화면 확인용 더미 분쟁 데이터입니다.' AS desc_text,
            NULL AS attachment_url,
            'ADMIN_HOLD' AS dispute_status,
            '정산 보류 화면 표시용 메모' AS admin_memo,
            '2026-03-12 10:00:00' AS requested_at,
            '2026-03-12 10:20:00' AS processed_at
        FROM dual
    )
    WHERE o_id IS NOT NULL
      AND p_id IS NOT NULL
      AND requester_id IS NOT NULL
      AND created_by_id IS NOT NULL
) s ON (pd.order_id = s.o_id)
WHEN NOT MATCHED THEN
    INSERT (
        order_id,
        payment_id,
        requester_user_id,
        created_by_user_id,
        reason_code,
        description,
        attachment_url,
        status,
        admin_memo,
        requested_at,
        processed_at
    )
    VALUES (
        s.o_id,
        s.p_id,
        s.requester_id,
        s.created_by_id,
        s.reason_code,
        s.desc_text,
        s.attachment_url,
        s.dispute_status,
        s.admin_memo,
        TO_TIMESTAMP(s.requested_at, 'YYYY-MM-DD HH24:MI:SS'),
        TO_TIMESTAMP(s.processed_at, 'YYYY-MM-DD HH24:MI:SS')
    );

COMMIT;
