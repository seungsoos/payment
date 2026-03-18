package com.musinsa.payment.point.entity;

import com.musinsa.payment.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "point")
public class Point extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long walletId;

	@Column(nullable = false, unique = true, length = 50)
	private String pointKey;

	@Column(nullable = false)
	private Long earnedAmount;

	@Column(nullable = false)
	private Long remainingAmount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EarnType earnType;

	@Column(nullable = false)
	private LocalDateTime expiresAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PointStatus status;

	@Builder
	public Point(Long walletId, String pointKey, Long earnedAmount, EarnType earnType, LocalDateTime expiresAt) {
		this.walletId = walletId;
		this.pointKey = pointKey;
		this.earnedAmount = earnedAmount;
		this.remainingAmount = earnedAmount;
		this.earnType = earnType;
		this.expiresAt = expiresAt;
		this.status = PointStatus.ACTIVE;
	}

	public void use(Long amount) {
		this.remainingAmount -= amount;
	}

	public void restore(Long amount) {
		this.remainingAmount += amount;
	}

	public void cancel() {
		this.status = PointStatus.CANCELLED;
		this.remainingAmount = 0L;
	}

	public boolean isExpired() {
		return this.expiresAt.isBefore(LocalDateTime.now());
	}

	public boolean isUsable() {
		return this.status == PointStatus.ACTIVE && !isExpired() && this.remainingAmount > 0;
	}

	public boolean hasBeenUsed() {
		return this.remainingAmount < this.earnedAmount;
	}
}
