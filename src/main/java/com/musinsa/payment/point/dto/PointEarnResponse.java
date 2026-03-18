package com.musinsa.payment.point.dto;

import com.musinsa.payment.point.entity.EarnType;
import com.musinsa.payment.point.entity.PointEarn;

import java.time.LocalDateTime;

/**
 * 포인트 적립 응답
 * @param pointKey 적립건 고유 식별자
 * @param memberId 회원 ID
 * @param amount 적립 금액
 * @param earnType 적립 유형 (NORMAL / MANUAL)
 * @param expiresAt 만료 일시
 * @param totalBalance 적립 후 총 보유 잔액
 * @param createdAt 적립 일시
 */
public record PointEarnResponse(
		String pointKey,
		Long memberId,
		Long amount,
		EarnType earnType,
		LocalDateTime expiresAt,
		Long totalBalance,
		LocalDateTime createdAt
) {
	public static PointEarnResponse of(PointEarn point, Long memberId, Long totalBalance) {
		return new PointEarnResponse(
				point.getPointKey(),
				memberId,
				point.getEarnedAmount(),
				point.getEarnType(),
				point.getExpiresAt(),
				totalBalance,
				point.getCreatedAt()
		);
	}
}
