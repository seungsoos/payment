package com.musinsa.payment.point.dto;

import com.musinsa.payment.point.entity.EarnType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import static com.musinsa.payment.point.common.PointConstants.DEFAULT_EXPIRE_DAYS;

/**
 * 포인트 적립 요청
 * @param memberId 회원 ID
 * @param amount 적립 금액 (1 이상)
 * @param earnType 적립 유형 (NORMAL: 일반, MANUAL: 수기지급, 기본값 NORMAL)
 * @param expireDays 만료일 (1일~5년 미만, 기본값 365일)
 * @param idempotencyKey 멱등성 키 (중복 요청 방지)
 */
public record PointEarnRequest(
		@NotNull(message = "회원 ID는 필수입니다")
		Long memberId,

		@NotNull(message = "적립 금액은 필수입니다")
		@Min(value = 1, message = "적립 금액은 1 이상이어야 합니다")
		Long amount,

		EarnType earnType,

		Integer expireDays,

		@NotNull(message = "멱등성 키는 필수입니다")
		String idempotencyKey
) {
	public EarnType earnType() {
		return earnType != null ? earnType : EarnType.NORMAL;
	}

	public Integer expireDays() {
		return expireDays != null ? expireDays : DEFAULT_EXPIRE_DAYS;
	}
}
