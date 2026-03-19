package com.musinsa.payment.point.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 포인트 사용취소 요청
 * @param memberId 회원 ID
 * @param pointKey 취소할 사용 거래의 고유 식별자
 * @param cancelAmount 취소 금액 (전체 또는 부분)
 * @param idempotencyKey 멱등성 키 (중복 요청 방지)
 */
public record PointUseCancelRequest(
		@NotNull(message = "회원 ID는 필수입니다")
		Long memberId,

		@NotBlank(message = "포인트 키는 필수입니다")
		String pointKey,

		@NotNull(message = "취소 금액은 필수입니다")
		@Min(value = 1, message = "취소 금액은 1 이상이어야 합니다")
		Long cancelAmount,

		@NotNull(message = "멱등성 키는 필수입니다")
		String idempotencyKey
) {
}
