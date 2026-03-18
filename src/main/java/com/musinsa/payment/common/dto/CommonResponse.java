package com.musinsa.payment.common.dto;

import com.musinsa.payment.common.exception.Result;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CommonResponse<T> {

	private Meta meta;
	private T data;

	public static <T> CommonResponse<T> success(T data) {
		return new CommonResponse<>(new Meta(Result.SUCCESS), data);
	}

	public static <T> CommonResponse<T> error(Result result, String message) {
		return new CommonResponse<>(new Meta(result, message), null);
	}

	@Getter
	@NoArgsConstructor
	public static class Meta {
		private String code;
		private String message;

		public Meta(Result result) {
			this.code = result.getCode();
			this.message = result.getMessage();
		}

		public Meta(Result result, String message) {
			this.code = result.getCode();
			this.message = message;
		}
	}
}
