package com.musinsa.payment.point.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 포인트 거래 이력 - 적립/적립취소/사용/사용취소 모든 거래를 불변으로 기록
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "point_transaction")
@EntityListeners(AuditingEntityListener.class)
public class PointTransaction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long walletId;

	@Column(nullable = false, unique = true, length = 50)
	private String pointKey;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TransactionType type;

	@Column(nullable = false)
	private Long amount;

	@Column(length = 100)
	private String orderId;

	@Column(length = 50)
	private String relatedPointKey;

	@Column(unique = true, length = 100)
	private String idempotencyKey;

	@CreatedDate
	@Column(updatable = false)
	private LocalDateTime createdAt;

	@Builder
	public PointTransaction(Long walletId, String pointKey, TransactionType type, Long amount, String orderId, String relatedPointKey, String idempotencyKey) {
		this.walletId = walletId;
		this.pointKey = pointKey;
		this.type = type;
		this.amount = amount;
		this.orderId = orderId;
		this.relatedPointKey = relatedPointKey;
		this.idempotencyKey = idempotencyKey;
	}
}
