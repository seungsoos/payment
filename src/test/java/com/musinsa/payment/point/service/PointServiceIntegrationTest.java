package com.musinsa.payment.point.service;

import com.musinsa.payment.common.exception.BusinessException;
import com.musinsa.payment.common.exception.Result;
import com.musinsa.payment.point.dto.*;
import com.musinsa.payment.point.entity.*;
import com.musinsa.payment.point.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class PointServiceIntegrationTest {

	@Autowired
	private PointService pointService;

	@Autowired
	private PointEarnRepository pointEarnRepository;

	@Autowired
	private PointWalletRepository pointWalletRepository;

	@Autowired
	private PointTransactionRepository pointTransactionRepository;

	@Autowired
	private PointUsageRepository pointUsageRepository;

	private static final Long MEMBER_ID = 1L;

	@Nested
	@DisplayName("과제 예시 시나리오 (만료 제외)")
	class ExampleScenario {

		@Test
		@DisplayName("적립 → 사용 → 부분 사용취소 흐름")
		void earnUseAndPartialCancel() {
			// 1. 1000원 적립 (pointKey: A)
			PointEarnResponse earnA = pointService.earn(new PointEarnRequest(MEMBER_ID, 1000L, EarnType.NORMAL, 365, idempotencyKey()));
			assertThat(earnA.totalBalance()).isEqualTo(1000L);

			// 2. 500원 적립 (pointKey: B)
			PointEarnResponse earnB = pointService.earn(new PointEarnRequest(MEMBER_ID, 500L, EarnType.NORMAL, 365, idempotencyKey()));
			assertThat(earnB.totalBalance()).isEqualTo(1500L);

			// 3. 주문번호 A1234에서 1200원 사용 (pointKey: C)
			PointUseResponse useC = pointService.use(new PointUseRequest(MEMBER_ID, 1200L, "A1234", idempotencyKey()));
			assertThat(useC.totalBalance()).isEqualTo(300L);

			// A에서 1000원, B에서 200원 사용 검증
			PointEarn pointA = pointEarnRepository.findByPointKey(earnA.pointKey()).orElseThrow();
			PointEarn pointB = pointEarnRepository.findByPointKey(earnB.pointKey()).orElseThrow();
			assertThat(pointA.getRemainingAmount()).isEqualTo(0L);
			assertThat(pointB.getRemainingAmount()).isEqualTo(300L);

			// 4. C의 1200원 중 1100원 부분 사용취소
			PointUseCancelResponse cancelD = pointService.useCancel(new PointUseCancelRequest(MEMBER_ID, useC.pointKey(), 1100L, idempotencyKey()));
			assertThat(cancelD.totalBalance()).isEqualTo(1400L);

			// A 복원 1000원, B 복원 100원
			PointEarn updatedA = pointEarnRepository.findByPointKey(earnA.pointKey()).orElseThrow();
			PointEarn updatedB = pointEarnRepository.findByPointKey(earnB.pointKey()).orElseThrow();
			assertThat(updatedA.getRemainingAmount()).isEqualTo(1000L);
			assertThat(updatedB.getRemainingAmount()).isEqualTo(400L);
		}
	}

	@Nested
	@DisplayName("적립")
	class Earn {

		@Test
		@DisplayName("정상 적립")
		void earn_success() {
			PointEarnResponse response = pointService.earn(new PointEarnRequest(MEMBER_ID, 1000L, EarnType.NORMAL, 365, idempotencyKey()));

			assertThat(response.pointKey()).startsWith("PT-");
			assertThat(response.amount()).isEqualTo(1000L);
			assertThat(response.totalBalance()).isEqualTo(1000L);
			assertThat(response.earnType()).isEqualTo(EarnType.NORMAL);
		}

		@Test
		@DisplayName("수기지급 적립")
		void earn_manual() {
			PointEarnResponse response = pointService.earn(new PointEarnRequest(MEMBER_ID, 500L, EarnType.MANUAL, 180, idempotencyKey()));

			assertThat(response.earnType()).isEqualTo(EarnType.MANUAL);
		}

		@Test
		@DisplayName("1회 최대 적립 금액 초과 시 실패")
		void earn_exceedMaxEarnAmount() {
			assertThatThrownBy(() -> pointService.earn(new PointEarnRequest(MEMBER_ID, 100001L, EarnType.NORMAL, 365, idempotencyKey())))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getResult())
					.isEqualTo(Result.EXCEED_MAX_EARN_AMOUNT);
		}

		@Test
		@DisplayName("경계값: 최대 적립 금액 정확히 10만원은 성공")
		void earn_exactMaxAmount() {
			PointEarnResponse response = pointService.earn(new PointEarnRequest(MEMBER_ID, 100000L, EarnType.NORMAL, 365, idempotencyKey()));
			assertThat(response.amount()).isEqualTo(100000L);
		}

		@Test
		@DisplayName("최대 보유 금액 초과 시 실패")
		void earn_exceedMaxHoldAmount() {
			// 500만원까지 적립 (100000 * 50 = 5000000)
			for (int i = 0; i < 50; i++) {
				pointService.earn(new PointEarnRequest(MEMBER_ID, 100000L, EarnType.NORMAL, 365, idempotencyKey()));
			}

			assertThatThrownBy(() -> pointService.earn(new PointEarnRequest(MEMBER_ID, 1L, EarnType.NORMAL, 365, idempotencyKey())))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getResult())
					.isEqualTo(Result.EXCEED_MAX_HOLD_AMOUNT);
		}

		@Test
		@DisplayName("만료일 경계값: 0일은 실패")
		void earn_expireDaysZero() {
			assertThatThrownBy(() -> pointService.earn(new PointEarnRequest(MEMBER_ID, 1000L, EarnType.NORMAL, 0, idempotencyKey())))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getResult())
					.isEqualTo(Result.INVALID_EXPIRE_DAYS);
		}

		@Test
		@DisplayName("만료일 경계값: 1일은 성공")
		void earn_expireDaysOne() {
			PointEarnResponse response = pointService.earn(new PointEarnRequest(MEMBER_ID, 1000L, EarnType.NORMAL, 1, idempotencyKey()));
			assertThat(response.pointKey()).isNotNull();
		}

		@Test
		@DisplayName("만료일 경계값: 1825일(5년)은 실패")
		void earn_expireDaysFiveYears() {
			assertThatThrownBy(() -> pointService.earn(new PointEarnRequest(MEMBER_ID, 1000L, EarnType.NORMAL, 1825, idempotencyKey())))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getResult())
					.isEqualTo(Result.INVALID_EXPIRE_DAYS);
		}

		@Test
		@DisplayName("만료일 경계값: 1824일은 성공")
		void earn_expireDaysMaxMinus1() {
			PointEarnResponse response = pointService.earn(new PointEarnRequest(MEMBER_ID, 1000L, EarnType.NORMAL, 1824, idempotencyKey()));
			assertThat(response.pointKey()).isNotNull();
		}
	}

	@Nested
	@DisplayName("적립취소")
	class EarnCancel {

		@Test
		@DisplayName("정상 적립취소")
		void earnCancel_success() {
			PointEarnResponse earned = pointService.earn(new PointEarnRequest(MEMBER_ID, 1000L, EarnType.NORMAL, 365, idempotencyKey()));
			PointEarnCancelResponse response = pointService.earnCancel(new PointEarnCancelRequest(MEMBER_ID, earned.pointKey()));

			assertThat(response.cancelledAmount()).isEqualTo(1000L);
			assertThat(response.totalBalance()).isEqualTo(0L);
		}

		@Test
		@DisplayName("사용된 포인트 적립취소 시 실패")
		void earnCancel_alreadyUsed() {
			PointEarnResponse earned = pointService.earn(new PointEarnRequest(MEMBER_ID, 1000L, EarnType.NORMAL, 365, idempotencyKey()));
			pointService.use(new PointUseRequest(MEMBER_ID, 500L, "ORDER-1", idempotencyKey()));

			assertThatThrownBy(() -> pointService.earnCancel(new PointEarnCancelRequest(MEMBER_ID, earned.pointKey())))
					.isInstanceOf(BusinessException.class);
		}

		@Test
		@DisplayName("이미 취소된 포인트 적립취소 시 멱등성 보장")
		void earnCancel_idempotent() {
			PointEarnResponse earned = pointService.earn(new PointEarnRequest(MEMBER_ID, 1000L, EarnType.NORMAL, 365, idempotencyKey()));
			PointEarnCancelResponse first = pointService.earnCancel(new PointEarnCancelRequest(MEMBER_ID, earned.pointKey()));
			PointEarnCancelResponse second = pointService.earnCancel(new PointEarnCancelRequest(MEMBER_ID, earned.pointKey()));

			assertThat(first.totalBalance()).isEqualTo(second.totalBalance());
		}

		@Test
		@DisplayName("다른 회원의 적립건 취소 시 실패")
		void earnCancel_differentMember() {
			PointEarnResponse earned = pointService.earn(new PointEarnRequest(MEMBER_ID, 1000L, EarnType.NORMAL, 365, idempotencyKey()));

			// 존재하지 않는 회원이라 MEMBER_NOT_FOUND 발생
			assertThatThrownBy(() -> pointService.earnCancel(new PointEarnCancelRequest(999L, earned.pointKey())))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getResult())
					.isEqualTo(Result.MEMBER_NOT_FOUND);
		}
	}

	@Nested
	@DisplayName("사용")
	class Use {

		@Test
		@DisplayName("정상 사용")
		void use_success() {
			pointService.earn(new PointEarnRequest(MEMBER_ID, 1000L, EarnType.NORMAL, 365, idempotencyKey()));
			PointUseResponse response = pointService.use(new PointUseRequest(MEMBER_ID, 500L, "ORDER-1", idempotencyKey()));

			assertThat(response.usedAmount()).isEqualTo(500L);
			assertThat(response.totalBalance()).isEqualTo(500L);
			assertThat(response.orderId()).isEqualTo("ORDER-1");
		}

		@Test
		@DisplayName("수기지급 포인트 우선 사용")
		void use_manualFirst() {
			PointEarnResponse normal = pointService.earn(new PointEarnRequest(MEMBER_ID, 1000L, EarnType.NORMAL, 365, idempotencyKey()));
			PointEarnResponse manual = pointService.earn(new PointEarnRequest(MEMBER_ID, 500L, EarnType.MANUAL, 365, idempotencyKey()));

			pointService.use(new PointUseRequest(MEMBER_ID, 300L, "ORDER-1", idempotencyKey()));

			// 수기지급에서 먼저 차감
			PointEarn manualPoint = pointEarnRepository.findByPointKey(manual.pointKey()).orElseThrow();
			PointEarn normalPoint = pointEarnRepository.findByPointKey(normal.pointKey()).orElseThrow();
			assertThat(manualPoint.getRemainingAmount()).isEqualTo(200L);
			assertThat(normalPoint.getRemainingAmount()).isEqualTo(1000L);
		}

		@Test
		@DisplayName("잔액 부족 시 실패")
		void use_insufficientPoint() {
			pointService.earn(new PointEarnRequest(MEMBER_ID, 1000L, EarnType.NORMAL, 365, idempotencyKey()));

			assertThatThrownBy(() -> pointService.use(new PointUseRequest(MEMBER_ID, 1001L, "ORDER-1", idempotencyKey())))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getResult())
					.isEqualTo(Result.INSUFFICIENT_POINT);
		}

		@Test
		@DisplayName("여러 적립건에 걸쳐 사용")
		void use_multipleEarns() {
			PointEarnResponse earnA = pointService.earn(new PointEarnRequest(MEMBER_ID, 300L, EarnType.NORMAL, 365, idempotencyKey()));
			PointEarnResponse earnB = pointService.earn(new PointEarnRequest(MEMBER_ID, 400L, EarnType.NORMAL, 365, idempotencyKey()));

			pointService.use(new PointUseRequest(MEMBER_ID, 500L, "ORDER-1", idempotencyKey()));

			PointEarn a = pointEarnRepository.findByPointKey(earnA.pointKey()).orElseThrow();
			PointEarn b = pointEarnRepository.findByPointKey(earnB.pointKey()).orElseThrow();
			assertThat(a.getRemainingAmount()).isEqualTo(0L);
			assertThat(b.getRemainingAmount()).isEqualTo(200L);
		}
	}

	@Nested
	@DisplayName("사용취소")
	class UseCancel {

		@Test
		@DisplayName("전체 사용취소")
		void useCancel_full() {
			pointService.earn(new PointEarnRequest(MEMBER_ID, 1000L, EarnType.NORMAL, 365, idempotencyKey()));
			PointUseResponse used = pointService.use(new PointUseRequest(MEMBER_ID, 1000L, "ORDER-1", idempotencyKey()));

			PointUseCancelResponse response = pointService.useCancel(new PointUseCancelRequest(MEMBER_ID, used.pointKey(), 1000L, idempotencyKey()));

			assertThat(response.cancelledAmount()).isEqualTo(1000L);
			assertThat(response.totalBalance()).isEqualTo(1000L);
		}

		@Test
		@DisplayName("부분 사용취소")
		void useCancel_partial() {
			pointService.earn(new PointEarnRequest(MEMBER_ID, 1000L, EarnType.NORMAL, 365, idempotencyKey()));
			PointUseResponse used = pointService.use(new PointUseRequest(MEMBER_ID, 1000L, "ORDER-1", idempotencyKey()));

			PointUseCancelResponse response = pointService.useCancel(new PointUseCancelRequest(MEMBER_ID, used.pointKey(), 600L, idempotencyKey()));

			assertThat(response.cancelledAmount()).isEqualTo(600L);
			assertThat(response.totalBalance()).isEqualTo(600L);
		}

		@Test
		@DisplayName("2차 부분 사용취소")
		void useCancel_partialTwice() {
			pointService.earn(new PointEarnRequest(MEMBER_ID, 1000L, EarnType.NORMAL, 365, idempotencyKey()));
			PointUseResponse used = pointService.use(new PointUseRequest(MEMBER_ID, 1000L, "ORDER-1", idempotencyKey()));

			pointService.useCancel(new PointUseCancelRequest(MEMBER_ID, used.pointKey(), 600L, idempotencyKey()));
			PointUseCancelResponse second = pointService.useCancel(new PointUseCancelRequest(MEMBER_ID, used.pointKey(), 400L, idempotencyKey()));

			assertThat(second.totalBalance()).isEqualTo(1000L);
		}

		@Test
		@DisplayName("취소 가능 금액 초과 시 실패")
		void useCancel_exceedCancellableAmount() {
			pointService.earn(new PointEarnRequest(MEMBER_ID, 1000L, EarnType.NORMAL, 365, idempotencyKey()));
			PointUseResponse used = pointService.use(new PointUseRequest(MEMBER_ID, 1000L, "ORDER-1", idempotencyKey()));

			assertThatThrownBy(() -> pointService.useCancel(new PointUseCancelRequest(MEMBER_ID, used.pointKey(), 1001L, idempotencyKey())))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getResult())
					.isEqualTo(Result.EXCEED_CANCEL_AMOUNT);
		}

		@Test
		@DisplayName("만료된 적립건 사용취소 시 신규적립 처리")
		void useCancel_expiredPointNewEarn() {
			// 만료일이 매우 짧은 포인트 적립 (테스트용으로 expiresAt을 과거로 설정하기 위해 리플렉션 사용)
			PointEarnResponse earned = pointService.earn(new PointEarnRequest(MEMBER_ID, 1000L, EarnType.NORMAL, 1, idempotencyKey()));
			PointUseResponse used = pointService.use(new PointUseRequest(MEMBER_ID, 1000L, "ORDER-1", idempotencyKey()));

			// 만료 처리 (직접 UPDATE 쿼리로 처리)
			pointEarnRepository.findByPointKey(earned.pointKey()).ifPresent(p -> {
				try {
					var field = PointEarn.class.getDeclaredField("expiresAt");
					field.setAccessible(true);
					field.set(p, LocalDateTime.now().minusDays(1));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});

			PointUseCancelResponse response = pointService.useCancel(new PointUseCancelRequest(MEMBER_ID, used.pointKey(), 1000L, idempotencyKey()));

			assertThat(response.totalBalance()).isEqualTo(1000L);
		}
	}

	@Nested
	@DisplayName("엣지 케이스")
	class EdgeCase {

		@Test
		@DisplayName("적립 후 전액 사용 후 전액 사용취소 후 다시 전액 사용")
		void fullCycle() {
			pointService.earn(new PointEarnRequest(MEMBER_ID, 1000L, EarnType.NORMAL, 365, idempotencyKey()));
			PointUseResponse used = pointService.use(new PointUseRequest(MEMBER_ID, 1000L, "ORDER-1", idempotencyKey()));
			pointService.useCancel(new PointUseCancelRequest(MEMBER_ID, used.pointKey(), 1000L, idempotencyKey()));

			// 복원 후 다시 사용 가능
			PointUseResponse reused = pointService.use(new PointUseRequest(MEMBER_ID, 1000L, "ORDER-2", idempotencyKey()));
			assertThat(reused.totalBalance()).isEqualTo(0L);
		}

		@Test
		@DisplayName("포인트 잔액 0원일 때 사용 시도")
		void use_zeroBalance() {
			pointService.earn(new PointEarnRequest(MEMBER_ID, 1000L, EarnType.NORMAL, 365, idempotencyKey()));
			pointService.use(new PointUseRequest(MEMBER_ID, 1000L, "ORDER-1", idempotencyKey()));

			assertThatThrownBy(() -> pointService.use(new PointUseRequest(MEMBER_ID, 1L, "ORDER-2", idempotencyKey())))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getResult())
					.isEqualTo(Result.INSUFFICIENT_POINT);
		}

		@Test
		@DisplayName("적립취소 후 같은 포인트 재취소 시 멱등성")
		void earnCancel_twice() {
			PointEarnResponse earned = pointService.earn(new PointEarnRequest(MEMBER_ID, 1000L, EarnType.NORMAL, 365, idempotencyKey()));
			pointService.earnCancel(new PointEarnCancelRequest(MEMBER_ID, earned.pointKey()));
			PointEarnCancelResponse second = pointService.earnCancel(new PointEarnCancelRequest(MEMBER_ID, earned.pointKey()));

			// 멱등성: 두 번째도 성공, 잔액 동일
			assertThat(second.totalBalance()).isEqualTo(0L);
		}

		@Test
		@DisplayName("여러 주문에서 각각 사용 후 각각 취소")
		void multipleOrdersUseAndCancel() {
			pointService.earn(new PointEarnRequest(MEMBER_ID, 3000L, EarnType.NORMAL, 365, idempotencyKey()));

			PointUseResponse use1 = pointService.use(new PointUseRequest(MEMBER_ID, 1000L, "ORDER-1", idempotencyKey()));
			PointUseResponse use2 = pointService.use(new PointUseRequest(MEMBER_ID, 500L, "ORDER-2", idempotencyKey()));

			assertThat(pointWalletRepository.findByMemberId(MEMBER_ID).orElseThrow().getTotalBalance()).isEqualTo(1500L);

			pointService.useCancel(new PointUseCancelRequest(MEMBER_ID, use1.pointKey(), 1000L, idempotencyKey()));
			pointService.useCancel(new PointUseCancelRequest(MEMBER_ID, use2.pointKey(), 500L, idempotencyKey()));

			assertThat(pointWalletRepository.findByMemberId(MEMBER_ID).orElseThrow().getTotalBalance()).isEqualTo(3000L);
		}

		@Test
		@DisplayName("여러 적립건에서 사용 후 3차 부분취소")
		void partialCancel_threeTimes() {
			pointService.earn(new PointEarnRequest(MEMBER_ID, 500L, EarnType.NORMAL, 365, idempotencyKey()));
			pointService.earn(new PointEarnRequest(MEMBER_ID, 500L, EarnType.NORMAL, 365, idempotencyKey()));
			PointUseResponse used = pointService.use(new PointUseRequest(MEMBER_ID, 1000L, "ORDER-1", idempotencyKey()));

			pointService.useCancel(new PointUseCancelRequest(MEMBER_ID, used.pointKey(), 300L, idempotencyKey()));
			pointService.useCancel(new PointUseCancelRequest(MEMBER_ID, used.pointKey(), 300L, idempotencyKey()));
			PointUseCancelResponse third = pointService.useCancel(new PointUseCancelRequest(MEMBER_ID, used.pointKey(), 400L, idempotencyKey()));

			assertThat(third.totalBalance()).isEqualTo(1000L);
		}

		@Test
		@DisplayName("경계값: 최대 보유 금액 정확히 도달 후 1원 적립 실패")
		void earn_exactMaxHoldThenOneMore() {
			// 4,999,999원 적립
			for (int i = 0; i < 49; i++) {
				pointService.earn(new PointEarnRequest(MEMBER_ID, 100000L, EarnType.NORMAL, 365, idempotencyKey()));
			}
			pointService.earn(new PointEarnRequest(MEMBER_ID, 99999L, EarnType.NORMAL, 365, idempotencyKey()));

			// 정확히 한도까지
			pointService.earn(new PointEarnRequest(MEMBER_ID, 1L, EarnType.NORMAL, 365, idempotencyKey()));

			// 1원 더 적립 시 실패
			assertThatThrownBy(() -> pointService.earn(new PointEarnRequest(MEMBER_ID, 1L, EarnType.NORMAL, 365, idempotencyKey())))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getResult())
					.isEqualTo(Result.EXCEED_MAX_HOLD_AMOUNT);
		}

		@Test
		@DisplayName("사용취소 후 남은 취소 가능 금액 정확히 취소")
		void useCancel_exactRemainingAmount() {
			pointService.earn(new PointEarnRequest(MEMBER_ID, 1000L, EarnType.NORMAL, 365, idempotencyKey()));
			PointUseResponse used = pointService.use(new PointUseRequest(MEMBER_ID, 1000L, "ORDER-1", idempotencyKey()));

			pointService.useCancel(new PointUseCancelRequest(MEMBER_ID, used.pointKey(), 700L, idempotencyKey()));

			// 남은 300원 정확히 취소
			PointUseCancelResponse response = pointService.useCancel(new PointUseCancelRequest(MEMBER_ID, used.pointKey(), 300L, idempotencyKey()));
			assertThat(response.totalBalance()).isEqualTo(1000L);

			// 0원 남은 상태에서 1원 더 취소 시 실패
			assertThatThrownBy(() -> pointService.useCancel(new PointUseCancelRequest(MEMBER_ID, used.pointKey(), 1L, idempotencyKey())))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getResult())
					.isEqualTo(Result.EXCEED_CANCEL_AMOUNT);
		}

		@Test
		@DisplayName("존재하지 않는 pointKey로 적립취소 시 실패")
		void earnCancel_notFound() {
			pointService.earn(new PointEarnRequest(MEMBER_ID, 1000L, EarnType.NORMAL, 365, idempotencyKey()));

			assertThatThrownBy(() -> pointService.earnCancel(new PointEarnCancelRequest(MEMBER_ID, "PT-invalid")))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getResult())
					.isEqualTo(Result.POINT_NOT_FOUND);
		}

		@Test
		@DisplayName("존재하지 않는 pointKey로 사용취소 시 실패")
		void useCancel_notFound() {
			pointService.earn(new PointEarnRequest(MEMBER_ID, 1000L, EarnType.NORMAL, 365, idempotencyKey()));

			assertThatThrownBy(() -> pointService.useCancel(new PointUseCancelRequest(MEMBER_ID, "PT-invalid", 100L, idempotencyKey())))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getResult())
					.isEqualTo(Result.USE_TRANSACTION_NOT_FOUND);
		}

		@Test
		@DisplayName("수기지급 포인트 사용 후 취소 시 수기지급 유지")
		void useCancel_manualEarnTypePreserved() {
			PointEarnResponse manual = pointService.earn(new PointEarnRequest(MEMBER_ID, 1000L, EarnType.MANUAL, 365, idempotencyKey()));
			PointUseResponse used = pointService.use(new PointUseRequest(MEMBER_ID, 1000L, "ORDER-1", idempotencyKey()));
			pointService.useCancel(new PointUseCancelRequest(MEMBER_ID, used.pointKey(), 1000L, idempotencyKey()));

			// 수기지급 적립건의 잔액이 복원되고 earnType 유지
			PointEarn restored = pointEarnRepository.findByPointKey(manual.pointKey()).orElseThrow();
			assertThat(restored.getRemainingAmount()).isEqualTo(1000L);
			assertThat(restored.getEarnType()).isEqualTo(EarnType.MANUAL);
		}
	}

	private String idempotencyKey() {
		return UUID.randomUUID().toString();
	}

}
