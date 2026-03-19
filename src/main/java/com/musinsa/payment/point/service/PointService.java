package com.musinsa.payment.point.service;

import com.musinsa.payment.common.exception.BusinessException;
import org.springframework.dao.DataIntegrityViolationException;
import com.musinsa.payment.common.exception.Result;
import com.musinsa.payment.point.dto.PointEarnCancelRequest;
import com.musinsa.payment.point.dto.PointEarnCancelResponse;
import com.musinsa.payment.point.dto.PointEarnRequest;
import com.musinsa.payment.point.dto.PointEarnResponse;
import com.musinsa.payment.point.entity.*;
import com.musinsa.payment.point.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {

	private final PointEarnRepository pointEarnRepository;
	private final PointWalletRepository pointWalletRepository;
	private final PointTransactionRepository pointTransactionRepository;
	private final PointValidator pointValidator;

	@Transactional
	public PointEarnResponse earn(PointEarnRequest request) {
		// 멱등성 체크
		PointEarnResponse duplicated = checkIdempotency(request);
		if (duplicated != null) {
			return duplicated;
		}

		// 검증
		pointValidator.validateExpireDays(request.expireDays());
		pointValidator.validateEarnAmount(request.amount());

		// 지갑 조회 (없으면 생성) + 락
		PointWallet wallet = getOrCreateWalletWithLock(request.memberId());
		pointValidator.validateMaxHoldAmount(wallet, request.amount());

		// 적립 처리
		String pointKey = generatePointKey();
		PointEarn point = createPoint(wallet, pointKey, request);
		wallet.addBalance(request.amount());
		createEarnTransaction(wallet, pointKey, request);

		log.info("포인트 적립 완료 - memberId: {}, pointKey: {}, amount: {}, balance: {}",
				request.memberId(), pointKey, request.amount(), wallet.getTotalBalance());

		return PointEarnResponse.of(point, request.memberId(), wallet.getTotalBalance());
	}

	@Transactional
	public PointEarnCancelResponse earnCancel(PointEarnCancelRequest request) {
		// 적립건 조회
		PointEarn point = getPointEarn(request.pointKey());

		// 이미 취소된 경우 기존 결과 반환 (pointKey 기반 멱등성)
		if (PointStatus.CANCELLED == point.getStatus()) {
			PointWallet wallet = getWallet(request);
			return PointEarnCancelResponse.of(point, request.memberId(), wallet.getTotalBalance());
		}

		// 검증
		pointValidator.validateEarnCancellable(point);

		// 지갑 조회 + 락
		PointWallet wallet = getPointWalletWithLock(request);

		// 적립취소 처리
		wallet.deductBalance(point.getEarnedAmount());
		point.cancel();
		createEarnCancelTransaction(wallet, point, request);

		log.info("포인트 적립취소 완료 - memberId: {}, pointKey: {}, amount: {}, balance: {}",
				request.memberId(), request.pointKey(), point.getEarnedAmount(), wallet.getTotalBalance());

		return PointEarnCancelResponse.of(point, request.memberId(), wallet.getTotalBalance());
	}

	private PointWallet getPointWalletWithLock(PointEarnCancelRequest request) {
		return pointWalletRepository.findByMemberIdWithLock(request.memberId())
				.orElseThrow(() -> new BusinessException(Result.MEMBER_NOT_FOUND));
	}

	private PointWallet getWallet(PointEarnCancelRequest request) {
		return pointWalletRepository.findByMemberId(request.memberId())
				.orElseThrow(() -> new BusinessException(Result.MEMBER_NOT_FOUND));
	}

	private PointEarn getPointEarn(String pointKey) {
		return pointEarnRepository.findByPointKey(pointKey)
				.orElseThrow(() -> new BusinessException(Result.POINT_NOT_FOUND));
	}

	private void createEarnCancelTransaction(PointWallet wallet, PointEarn point, PointEarnCancelRequest request) {
		pointTransactionRepository.save(PointTransaction.builder()
				.walletId(wallet.getId())
				.pointKey(generatePointKey())
				.type(TransactionType.EARN_CANCEL)
				.amount(point.getEarnedAmount())
				.relatedPointKey(request.pointKey())
				.build());
	}

	private PointEarnResponse checkIdempotency(PointEarnRequest request) {
		return pointTransactionRepository.findByIdempotencyKey(request.idempotencyKey())
				.map(existing -> {
					log.info("멱등성 키 중복 - idempotencyKey: {}", request.idempotencyKey());
					PointEarn point = getPointEarn(existing.getPointKey());
					PointWallet wallet = pointWalletRepository.findByMemberId(request.memberId())
							.orElseThrow(() -> new BusinessException(Result.MEMBER_NOT_FOUND));
					return PointEarnResponse.of(point, request.memberId(), wallet.getTotalBalance());
				})
				.orElse(null);
	}

	private PointEarn createPoint(PointWallet wallet, String pointKey, PointEarnRequest request) {
		return pointEarnRepository.save(getPointEarn(wallet, pointKey, request));
	}

	private PointEarn getPointEarn(PointWallet wallet, String pointKey, PointEarnRequest request) {
		return PointEarn.builder()
				.walletId(wallet.getId())
				.pointKey(pointKey)
				.earnedAmount(request.amount())
				.earnType(request.earnType())
				.expiresAt(LocalDateTime.now().plusDays(request.expireDays()))
				.build();
	}

	private void createEarnTransaction(PointWallet wallet, String pointKey, PointEarnRequest request) {
		pointTransactionRepository.save(getTransaction(wallet, pointKey, request));
	}

	private PointTransaction getTransaction(PointWallet wallet, String pointKey, PointEarnRequest request) {
		return PointTransaction.builder()
				.walletId(wallet.getId())
				.pointKey(pointKey)
				.type(TransactionType.EARN)
				.amount(request.amount())
				.idempotencyKey(request.idempotencyKey())
				.build();
	}

	private PointWallet getOrCreateWalletWithLock(Long memberId) {
		return pointWalletRepository.findByMemberIdWithLock(memberId)
				.orElseGet(() -> {
					try {
						return pointWalletRepository.save(
								PointWallet.builder().memberId(memberId).build()
						);
					} catch (DataIntegrityViolationException e) {
						log.warn("지갑 동시 생성 감지 - memberId: {}", memberId);
						throw new BusinessException(Result.DUPLICATE_REQUEST);
					}
				});
	}

	private String generatePointKey() {
		return "PT-" + UUID.randomUUID().toString().substring(0, 8);
	}
}
