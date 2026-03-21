package com.musinsa.payment.point.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 포인트 만료 스케줄러
 *
 * 만료일이 지난 ACTIVE 상태의 포인트를 EXPIRED로 변경하고,
 * 해당 금액만큼 지갑의 totalBalance를 차감하여 실제 사용 가능 잔액과 일치시킨다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointExpireScheduler {

	// @Scheduled(cron = "0 0 0 * * *")
	// public void expirePoints() {
	// }
}
