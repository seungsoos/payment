package com.musinsa.payment.common.exception;

import lombok.Getter;

@Getter
public enum Result {

	SUCCESS("SUCCESS", "성공"),

	// 포인트
	POINT_NOT_FOUND("POINT_NOT_FOUND", "포인트를 찾을 수 없습니다"),
	POINT_ALREADY_CANCELLED("POINT_ALREADY_CANCELLED", "이미 취소된 포인트입니다"),
	POINT_ALREADY_USED("POINT_ALREADY_USED", "사용된 포인트가 존재하여 적립 취소할 수 없습니다"),
	POINT_ALREADY_EXPIRED("POINT_ALREADY_EXPIRED", "만료된 포인트입니다"),
	INSUFFICIENT_POINT("INSUFFICIENT_POINT", "포인트가 부족합니다"),
	EXCEED_MAX_EARN_AMOUNT("EXCEED_MAX_EARN_AMOUNT", "1회 최대 적립 금액을 초과하였습니다"),
	EXCEED_MAX_HOLD_AMOUNT("EXCEED_MAX_HOLD_AMOUNT", "최대 보유 가능 금액을 초과합니다"),
	INVALID_EARN_AMOUNT("INVALID_EARN_AMOUNT", "적립 금액은 1 이상이어야 합니다"),
	INVALID_EXPIRE_DAYS("INVALID_EXPIRE_DAYS", "만료일은 1일 이상 5년 미만이어야 합니다"),
	INVALID_USE_AMOUNT("INVALID_USE_AMOUNT", "사용 금액은 1 이상이어야 합니다"),
	INVALID_CANCEL_AMOUNT("INVALID_CANCEL_AMOUNT", "사용취소 금액이 올바르지 않습니다"),
	EXCEED_CANCEL_AMOUNT("EXCEED_CANCEL_AMOUNT", "사용취소 가능 금액을 초과하였습니다"),
	USE_TRANSACTION_NOT_FOUND("USE_TRANSACTION_NOT_FOUND", "사용 거래를 찾을 수 없습니다"),
	MEMBER_NOT_FOUND("MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다"),

	// 시스템
	BAD_REQUEST("BAD_REQUEST", "잘못된 요청입니다"),
	INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "내부 서버 오류가 발생했습니다");

	private final String code;
	private final String message;

	Result(String code, String message) {
		this.code = code;
		this.message = message;
	}
}
