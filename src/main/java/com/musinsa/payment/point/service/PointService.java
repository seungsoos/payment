package com.musinsa.payment.point.service;

import com.musinsa.payment.common.exception.BusinessException;
import org.springframework.dao.DataIntegrityViolationException;
import com.musinsa.payment.common.exception.Result;
import com.musinsa.payment.point.dto.*;
import com.musinsa.payment.point.entity.*;
import com.musinsa.payment.point.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {

	private final PointEarnRepository pointEarnRepository;
	private final PointWalletRepository pointWalletRepository;
	private final PointTransactionRepository pointTransactionRepository;
	private final PointUsageRepository pointUsageRepository;
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
		// 지갑 조회 + 락 (동시성 제어를 위해 먼저 락 획득)
		PointWallet wallet = getWalletWithLock(request.memberId());

		// 적립건 조회
		PointEarn point = getPointEarn(request.pointKey());

		// 이미 취소된 경우 기존 결과 반환 (pointKey 기반 멱등성)
		if (PointStatus.CANCELLED == point.getStatus()) {
			return PointEarnCancelResponse.of(point, request.memberId(), wallet.getTotalBalance());
		}

		// 적립취소 처리
		wallet.deductBalance(point.getEarnedAmount());
		point.cancel();
		createEarnCancelTransaction(wallet, point, request);

		log.info("포인트 적립취소 완료 - memberId: {}, pointKey: {}, amount: {}, balance: {}",
				request.memberId(), request.pointKey(), point.getEarnedAmount(), wallet.getTotalBalance());

		return PointEarnCancelResponse.of(point, request.memberId(), wallet.getTotalBalance());
	}

	@Transactional
	public PointUseResponse use(PointUseRequest request) {
		// 멱등성 체크
		PointUseResponse duplicated = checkUseIdempotency(request);
		if (duplicated != null) {
			return duplicated;
		}

		// 지갑 조회 + 락
		PointWallet wallet = getWalletWithLock(request.memberId());

		// 거래 이력 생성
		String pointKey = generatePointKey();
		PointTransaction transaction = createUseTransaction(wallet, pointKey, request);

		// 적립건별 차감 + 사용-적립 매핑 생성
		deductFromEarnPoints(wallet, transaction, request.amount());

		// 지갑 잔액 차감
		wallet.deductBalance(request.amount());

		log.info("포인트 사용 완료 - memberId: {}, pointKey: {}, amount: {}, orderId: {}, balance: {}",
				request.memberId(), pointKey, request.amount(), request.orderId(), wallet.getTotalBalance());

		return PointUseResponse.of(pointKey, request.memberId(), request.amount(),
				request.orderId(), wallet.getTotalBalance(), transaction.getCreatedAt());
	}

	private PointTransaction createUseTransaction(PointWallet wallet, String pointKey, PointUseRequest request) {
		return pointTransactionRepository.save(PointTransaction.builder()
				.walletId(wallet.getId())
				.pointKey(pointKey)
				.type(TransactionType.USE)
				.amount(request.amount())
				.orderId(request.orderId())
				.idempotencyKey(request.idempotencyKey())
				.build());
	}

	private void deductFromEarnPoints(PointWallet wallet, PointTransaction transaction, Long amount) {
		List<PointEarn> usablePoints = pointEarnRepository.findUsablePoints(
				wallet.getId(), PointStatus.ACTIVE, LocalDateTime.now());

		Long remainingAmount = amount;
		for (PointEarn point : usablePoints) {
			if (remainingAmount <= 0) break;

			Long deductAmount = Math.min(point.getRemainingAmount(), remainingAmount);
			point.use(deductAmount);
			pointUsageRepository.save(PointUsage.builder()
					.transactionId(transaction.getId())
					.pointId(point.getId())
					.amount(deductAmount)
					.build());
			remainingAmount -= deductAmount;
		}
	}

	private PointUseResponse checkUseIdempotency(PointUseRequest request) {
		return pointTransactionRepository.findByIdempotencyKey(request.idempotencyKey())
				.map(existing -> {
					log.info("멱등성 키 중복 - idempotencyKey: {}", request.idempotencyKey());
					PointWallet wallet = getWallet(request.memberId());
					return PointUseResponse.of(existing.getPointKey(), request.memberId(), existing.getAmount(),
							existing.getOrderId(), wallet.getTotalBalance(), existing.getCreatedAt());
				})
				.orElse(null);
	}

	private PointWallet getWalletWithLock(Long memberId) {
		return pointWalletRepository.findByMemberIdWithLock(memberId)
				.orElseThrow(() -> new BusinessException(Result.MEMBER_NOT_FOUND));
	}

	private PointWallet getWallet(Long memberId) {
		return pointWalletRepository.findByMemberId(memberId)
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
