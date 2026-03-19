package com.musinsa.payment.point.dto;

import java.time.LocalDateTime;

/**
 * 포인트 사용 응답
 * @param pointKey 사용 거래 고유 식별자
 * @param memberId 회원 ID
 * @param usedAmount 사용 금액
 * @param orderId 주문번호
 * @param totalBalance 사용 후 총 보유 잔액
 * @param createdAt 사용 일시
 */
public record PointUseResponse(
		String pointKey,
		Long memberId,
		Long usedAmount,
		String orderId,
		Long totalBalance,
		LocalDateTime createdAt
) {
	public static PointUseResponse of(String pointKey, Long memberId, Long usedAmount,
									  String orderId, Long totalBalance, LocalDateTime createdAt) {
		return new PointUseResponse(pointKey, memberId, usedAmount, orderId, totalBalance, createdAt);
	}
}
