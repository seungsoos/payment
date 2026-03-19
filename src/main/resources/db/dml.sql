-- 포인트 정책 초기 데이터
MERGE INTO point_policy (policy_key, policy_value, description) KEY (policy_key) VALUES ('MAX_EARN_AMOUNT', 100000, '1회 최대 적립 가능 금액');
MERGE INTO point_policy (policy_key, policy_value, description) KEY (policy_key) VALUES ('MAX_HOLD_AMOUNT', 5000000, '개인별 최대 보유 가능 금액');
