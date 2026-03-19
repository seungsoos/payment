package com.musinsa.payment.point.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 포인트 적립취소 요청
 * @param memberId 회원 ID
 * @param pointKey 취소할 적립건의 고유 식별자 (멱등성 보장)
 */
public record PointEarnCancelRequest(
		@NotNull(message = "회원 ID는 필수입니다")
		Long memberId,

		@NotBlank(message = "포인트 키는 필수입니다")
		String pointKey
) {
}
