package com.musinsa.payment.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

	private final Result result;

	public BusinessException(Result result) {
		super(result.getMessage());
		this.result = result;
	}
}
