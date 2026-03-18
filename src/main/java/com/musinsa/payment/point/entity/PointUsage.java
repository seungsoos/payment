package com.musinsa.payment.point.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "point_usage")
@EntityListeners(AuditingEntityListener.class)
public class PointUsage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long transactionId;

	@Column(nullable = false)
	private Long pointId;

	@Column(nullable = false)
	private Long amount;

	@CreatedDate
	@Column(updatable = false)
	private LocalDateTime createdAt;

	@Builder
	public PointUsage(Long transactionId, Long pointId, Long amount) {
		this.transactionId = transactionId;
		this.pointId = pointId;
		this.amount = amount;
	}
}
