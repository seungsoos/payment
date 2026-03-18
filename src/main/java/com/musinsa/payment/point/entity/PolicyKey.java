package com.musinsa.payment.point.entity;

import lombok.Getter;

@Getter
public enum PolicyKey {

	MAX_EARN_AMOUNT("1회 최대 적립 가능 금액"),
	MAX_HOLD_AMOUNT("개인별 최대 보유 가능 금액");

	private final String description;

	PolicyKey(String description) {
		this.description = description;
	}
}
