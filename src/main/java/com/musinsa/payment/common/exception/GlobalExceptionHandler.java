package com.musinsa.payment.common.exception;

import com.musinsa.payment.common.dto.CommonResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<CommonResponse<Void>> handleBusinessException(BusinessException e) {
		log.warn("Business exception: {}", e.getMessage());
		return ResponseEntity.ok(CommonResponse.error(e.getResult(), e.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<CommonResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
		String message = e.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.findFirst()
				.orElse("요청 값이 올바르지 않습니다");

		log.warn("Validation exception: {}", message);
		return ResponseEntity.ok(CommonResponse.error(Result.BAD_REQUEST, message));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<CommonResponse<Void>> handleException(Exception e) {
		log.error("Unexpected error: {}", e.getMessage());
		return ResponseEntity.ok(CommonResponse.error(Result.INTERNAL_SERVER_ERROR, Result.INTERNAL_SERVER_ERROR.getMessage()));
	}
}
