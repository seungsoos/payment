package com.musinsa.payment.point.entity;

import com.musinsa.payment.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 포인트 정책 - 1회 최대 적립 금액, 개인별 최대 보유 금액 등 런타임 변경 가능한 정책값 관리
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "point_policy")
public class PointPolicy extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, unique = true, length = 50)
	private PolicyKey policyKey;

	@Column(nullable = false)
	private Long policyValue;

	@Column(length = 200)
	private String description;

	public void updateValue(Long policyValue) {
		this.policyValue = policyValue;
	}
}
