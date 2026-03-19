package com.musinsa.payment.point.entity;

import com.musinsa.payment.common.exception.BusinessException;
import com.musinsa.payment.common.exception.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class PointEarnTest {

	@Nested
	@DisplayName("use")
	class UseTest {

		@Test
		@DisplayName("정상 차감")
		void use_success() {
			PointEarn point = createPoint();
			point.use(500L);
			assertThat(point.getRemainingAmount()).isEqualTo(500L);
		}

		@Test
		@DisplayName("잔액 초과 차감 시 예외")
		void use_insufficientAmount() {
			PointEarn point = createPoint();
			assertThatThrownBy(() -> point.use(1001L))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getResult())
					.isEqualTo(Result.INSUFFICIENT_POINT);
		}

		@Test
		@DisplayName("경계값: 잔액 전체 차감")
		void use_exactAmount() {
			PointEarn point = createPoint();
			point.use(1000L);
			assertThat(point.getRemainingAmount()).isEqualTo(0L);
		}
	}

	@Nested
	@DisplayName("restore")
	class RestoreTest {

		@Test
		@DisplayName("정상 복원")
		void restore_success() {
			PointEarn point = createPoint();
			point.use(500L);
			point.restore(300L);
			assertThat(point.getRemainingAmount()).isEqualTo(800L);
		}

		@Test
		@DisplayName("원금 초과 복원 시 예외")
		void restore_exceedEarnedAmount() {
			PointEarn point = createPoint();
			point.use(500L);
			assertThatThrownBy(() -> point.restore(501L))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getResult())
					.isEqualTo(Result.INVALID_CANCEL_AMOUNT);
		}

		@Test
		@DisplayName("경계값: 원금까지 정확히 복원")
		void restore_exactAmount() {
			PointEarn point = createPoint();
			point.use(500L);
			point.restore(500L);
			assertThat(point.getRemainingAmount()).isEqualTo(1000L);
		}
	}

	@Nested
	@DisplayName("cancel")
	class CancelTest {

		@Test
		@DisplayName("정상 적립취소")
		void cancel_success() {
			PointEarn point = createPoint();
			point.cancel();
			assertThat(point.getStatus()).isEqualTo(PointStatus.CANCELLED);
			assertThat(point.getRemainingAmount()).isEqualTo(0L);
		}

		@Test
		@DisplayName("사용된 포인트 적립취소 시 예외")
		void cancel_alreadyUsed() {
			PointEarn point = createPoint();
			point.use(1L);
			assertThatThrownBy(point::cancel)
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getResult())
					.isEqualTo(Result.POINT_ALREADY_USED);
		}
	}

	@Nested
	@DisplayName("isExpired")
	class IsExpiredTest {

		@Test
		@DisplayName("만료되지 않은 포인트")
		void notExpired() {
			PointEarn point = createPoint();
			assertThat(point.isExpired()).isFalse();
		}

		@Test
		@DisplayName("만료된 포인트")
		void expired() {
			PointEarn point = createExpiredPoint();
			assertThat(point.isExpired()).isTrue();
		}
	}

	@Nested
	@DisplayName("isUsable")
	class IsUsableTest {

		@Test
		@DisplayName("사용 가능한 포인트")
		void usable() {
			PointEarn point = createPoint();
			assertThat(point.isUsable()).isTrue();
		}

		@Test
		@DisplayName("잔액 0이면 사용 불가")
		void notUsable_zeroRemaining() {
			PointEarn point = createPoint();
			point.use(1000L);
			assertThat(point.isUsable()).isFalse();
		}

		@Test
		@DisplayName("취소된 포인트는 사용 불가")
		void notUsable_cancelled() {
			PointEarn point = createPoint();
			point.cancel();
			assertThat(point.isUsable()).isFalse();
		}
	}

	private PointEarn createPoint() {
		return PointEarn.builder()
				.walletId(1L)
				.pointKey("PT-test")
				.earnedAmount(1000L)
				.earnType(EarnType.NORMAL)
				.expiresAt(LocalDateTime.now().plusDays(365))
				.build();
	}

	private PointEarn createExpiredPoint() {
		return PointEarn.builder()
				.walletId(1L)
				.pointKey("PT-expired")
				.earnedAmount(1000L)
				.earnType(EarnType.NORMAL)
				.expiresAt(LocalDateTime.now().minusDays(1))
				.build();
	}
}
