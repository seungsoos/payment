package com.musinsa.payment.point.entity;

import com.musinsa.payment.common.entity.BaseEntity;
import com.musinsa.payment.common.exception.BusinessException;
import com.musinsa.payment.common.exception.Result;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원별 포인트 지갑 - 총 잔액 캐시 및 동시성 제어를 위한 락 대상
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "point_wallet")
public class PointWallet extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private Long memberId;

	@Column(nullable = false)
	private Long totalBalance = 0L;

	@Builder
	public PointWallet(Long memberId) {
		this.memberId = memberId;
		this.totalBalance = 0L;
	}

	public void addBalance(Long amount) {
		this.totalBalance += amount;
	}

	public void deductBalance(Long amount) {
		if (this.totalBalance < amount) {
			throw new BusinessException(Result.INSUFFICIENT_POINT);
		}
		this.totalBalance -= amount;
	}
}
