package com.musinsa.payment.point.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 포인트 사용 요청
 * @param memberId 회원 ID
 * @param amount 사용 금액 (1 이상)
 * @param orderId 주문번호
 * @param idempotencyKey 멱등성 키 (중복 요청 방지)
 */
public record PointUseRequest(
		@NotNull(message = "회원 ID는 필수입니다")
		Long memberId,

		@NotNull(message = "사용 금액은 필수입니다")
		@Min(value = 1, message = "사용 금액은 1 이상이어야 합니다")
		Long amount,

		@NotBlank(message = "주문번호는 필수입니다")
		String orderId,

		@NotNull(message = "멱등성 키는 필수입니다")
		String idempotencyKey
) {
}
