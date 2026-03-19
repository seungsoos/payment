package com.musinsa.payment.common.idempotency;

import com.musinsa.payment.common.exception.BusinessException;
import com.musinsa.payment.common.exception.Result;
import com.musinsa.payment.point.repository.PointTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentAspect {

	private final PointTransactionRepository pointTransactionRepository;
	private final SpelExpressionParser parser = new SpelExpressionParser();
	private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

	@Around("@annotation(idempotent)")
	public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
		String idempotencyKey = parseKey(joinPoint, idempotent.keyExpression());

		if (pointTransactionRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
			log.info("멱등성 키 중복 - idempotencyKey: {}", idempotencyKey);
			throw new BusinessException(Result.DUPLICATE_REQUEST);
		}

		return joinPoint.proceed();
	}

	private String parseKey(ProceedingJoinPoint joinPoint, String expression) {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		String[] parameterNames = nameDiscoverer.getParameterNames(signature.getMethod());

		if (parameterNames == null) {
			return expression;
		}

		EvaluationContext context = new StandardEvaluationContext();
		Object[] args = joinPoint.getArgs();
		for (int i = 0; i < parameterNames.length; i++) {
			context.setVariable(parameterNames[i], args[i]);
		}

		return parser.parseExpression(expression).getValue(context, String.class);
	}
}
