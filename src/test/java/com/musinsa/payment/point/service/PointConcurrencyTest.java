package com.musinsa.payment.point.service;

import com.musinsa.payment.point.dto.PointEarnRequest;
import com.musinsa.payment.point.dto.PointEarnResponse;
import com.musinsa.payment.point.dto.PointUseRequest;
import com.musinsa.payment.point.entity.EarnType;
import com.musinsa.payment.point.repository.PointWalletRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PointConcurrencyTest {

	@Autowired
	private PointService pointService;

	@Autowired
	private PointWalletRepository pointWalletRepository;

	@Test
	@DisplayName("동시 적립 시 잔액 정합성 보장")
	void concurrentEarn() throws InterruptedException {
		Long memberId = 100L;
		int threadCount = 10;
		Long earnAmount = 1000L;

		// 지갑 생성
		pointService.earn(new PointEarnRequest(memberId, 1L, EarnType.NORMAL, 365, UUID.randomUUID().toString()));

		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);
		AtomicInteger successCount = new AtomicInteger();

		for (int i = 0; i < threadCount; i++) {
			executor.submit(() -> {
				try {
					pointService.earn(new PointEarnRequest(memberId, earnAmount, EarnType.NORMAL, 365, UUID.randomUUID().toString()));
					successCount.incrementAndGet();
				} catch (Exception e) {
					// 동시 요청 실패 허용
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();
		executor.shutdown();

		// 성공한 횟수 * 금액 + 초기 1원 = 최종 잔액
		Long expectedBalance = (successCount.get() * earnAmount) + 1L;
		Long actualBalance = pointWalletRepository.findByMemberId(memberId).orElseThrow().getTotalBalance();
		assertThat(actualBalance).isEqualTo(expectedBalance);
	}

	@Test
	@DisplayName("동시 사용 시 잔액 정합성 보장")
	void concurrentUse() throws InterruptedException {
		Long memberId = 200L;

		// 10000원 적립
		pointService.earn(new PointEarnRequest(memberId, 10000L, EarnType.NORMAL, 365, UUID.randomUUID().toString()));

		int threadCount = 10;
		Long useAmount = 1000L;

		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);
		AtomicInteger successCount = new AtomicInteger();

		for (int i = 0; i < threadCount; i++) {
			final int idx = i;
			executor.submit(() -> {
				try {
					pointService.use(new PointUseRequest(memberId, useAmount, "ORDER-" + idx, UUID.randomUUID().toString()));
					successCount.incrementAndGet();
				} catch (Exception e) {
					// 잔액 부족 등 실패 허용
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();
		executor.shutdown();

		// 성공한 횟수 * 금액 = 차감 금액
		Long expectedBalance = 10000L - (successCount.get() * useAmount);
		Long actualBalance = pointWalletRepository.findByMemberId(memberId).orElseThrow().getTotalBalance();
		assertThat(actualBalance).isEqualTo(expectedBalance);
	}

	@Test
	@DisplayName("동시 적립취소 시 이중 차감 방지")
	void concurrentEarnCancel() throws InterruptedException {
		Long memberId = 300L;

		PointEarnResponse earned = pointService.earn(new PointEarnRequest(memberId, 5000L, EarnType.NORMAL, 365, UUID.randomUUID().toString()));

		int threadCount = 5;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);
		AtomicInteger successCount = new AtomicInteger();

		for (int i = 0; i < threadCount; i++) {
			executor.submit(() -> {
				try {
					pointService.earnCancel(new com.musinsa.payment.point.dto.PointEarnCancelRequest(memberId, earned.pointKey()));
					successCount.incrementAndGet();
				} catch (Exception e) {
					// 중복 취소 실패 허용
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();
		executor.shutdown();

		// 모든 요청이 성공하더라도 잔액은 0 (멱등성 보장)
		Long actualBalance = pointWalletRepository.findByMemberId(memberId).orElseThrow().getTotalBalance();
		assertThat(actualBalance).isEqualTo(0L);
	}
}
