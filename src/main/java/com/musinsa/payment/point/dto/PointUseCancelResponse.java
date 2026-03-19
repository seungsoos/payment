package com.musinsa.payment.point.dto;

import java.time.LocalDateTime;

/**
 * 포인트 사용취소 응답
 * @param pointKey 사용취소 거래 고유 식별자
 * @param memberId 회원 ID
 * @param cancelledAmount 취소된 금액
 * @param totalBalance 취소 후 총 보유 잔액
 * @param createdAt 사용취소 일시
 */
public record PointUseCancelResponse(
		String pointKey,
		Long memberId,
		Long cancelledAmount,
		Long totalBalance,
		LocalDateTime createdAt
) {
	public static PointUseCancelResponse of(String pointKey, Long memberId, Long cancelledAmount,
											Long totalBalance, LocalDateTime createdAt) {
		return new PointUseCancelResponse(pointKey, memberId, cancelledAmount, totalBalance, createdAt);
	}
}
