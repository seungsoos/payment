package com.musinsa.payment.point.entity;

import com.musinsa.payment.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "point_policy")
public class PointPolicy extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 50)
	private String policyKey;

	@Column(nullable = false)
	private Long policyValue;

	@Column(length = 200)
	private String description;

	public void updateValue(Long policyValue) {
		this.policyValue = policyValue;
	}
}
