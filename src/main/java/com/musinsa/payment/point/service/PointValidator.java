package com.musinsa.payment.point.service;

import com.musinsa.payment.common.exception.BusinessException;
import com.musinsa.payment.common.exception.Result;
import com.musinsa.payment.point.entity.*;
import com.musinsa.payment.point.repository.PointPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.musinsa.payment.point.common.PointConstants.*;

@Component
@RequiredArgsConstructor
public class PointValidator {

	private final PointPolicyRepository pointPolicyRepository;

	public void validateExpireDays(int expireDays) {
		if (expireDays < MIN_EXPIRE_DAYS || expireDays >= MAX_EXPIRE_DAYS) {
			throw new BusinessException(Result.INVALID_EXPIRE_DAYS);
		}
	}

	public void validateEarnAmount(Long amount) {
		Long maxEarnAmount = getPolicyValue(PolicyKey.MAX_EARN_AMOUNT);
		if (amount > maxEarnAmount) {
			throw new BusinessException(Result.EXCEED_MAX_EARN_AMOUNT);
		}
	}

	public void validateMaxHoldAmount(PointWallet wallet, Long amount) {
		Long maxHoldAmount = getPolicyValue(PolicyKey.MAX_HOLD_AMOUNT);
		if (wallet.getTotalBalance() + amount > maxHoldAmount) {
			throw new BusinessException(Result.EXCEED_MAX_HOLD_AMOUNT);
		}
	}

	private Long getPolicyValue(PolicyKey policyKey) {
		return pointPolicyRepository.findByPolicyKey(policyKey)
				.map(PointPolicy::getPolicyValue)
				.orElseThrow(() -> new BusinessException(Result.INTERNAL_SERVER_ERROR));
	}
}
