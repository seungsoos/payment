package com.musinsa.payment.point.dto;

import com.musinsa.payment.point.entity.PointEarn;

import java.time.LocalDateTime;

/**
 * 포인트 적립취소 응답
 * @param pointKey 취소된 적립건 고유 식별자
 * @param memberId 회원 ID
 * @param cancelledAmount 취소된 금액
 * @param totalBalance 취소 후 총 보유 잔액
 * @param cancelledAt 취소 일시
 */
public record PointEarnCancelResponse(
		String pointKey,
		Long memberId,
		Long cancelledAmount,
		Long totalBalance,
		LocalDateTime cancelledAt
) {
	public static PointEarnCancelResponse of(PointEarn point, Long memberId, Long totalBalance) {
		return new PointEarnCancelResponse(
				point.getPointKey(),
				memberId,
				point.getEarnedAmount(),
				totalBalance,
				point.getUpdatedAt()
		);
	}
}
