package com.musinsa.payment.point.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.musinsa.payment.point.dto.*;
import com.musinsa.payment.point.entity.EarnType;
import com.musinsa.payment.point.service.PointService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PointControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private PointService pointService;

	@Test
	@DisplayName("POST /api/points/earn - 정상 적립")
	void earn_success() throws Exception {
		PointEarnRequest request = new PointEarnRequest(1L, 1000L, EarnType.NORMAL, 365, UUID.randomUUID().toString());

		mockMvc.perform(post("/api/points/earn")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.meta.code").value("SUCCESS"))
				.andExpect(jsonPath("$.data.amount").value(1000))
				.andExpect(jsonPath("$.data.totalBalance").value(1000))
				.andExpect(jsonPath("$.data.pointKey").exists());
	}

	@Test
	@DisplayName("POST /api/points/earn - 필수값 누락 시 400")
	void earn_validationFail() throws Exception {
		String body = "{\"amount\": 1000}"; // memberId, idempotencyKey 누락

		mockMvc.perform(post("/api/points/earn")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.meta.code").value("BAD_REQUEST"));
	}

	@Test
	@DisplayName("POST /api/points/earn - 적립금액 0 이하 시 실패")
	void earn_invalidAmount() throws Exception {
		PointEarnRequest request = new PointEarnRequest(1L, 0L, EarnType.NORMAL, 365, UUID.randomUUID().toString());

		mockMvc.perform(post("/api/points/earn")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.meta.code").value("BAD_REQUEST"));
	}

	@Test
	@DisplayName("POST /api/points/earn/cancel - 정상 적립취소")
	void earnCancel_success() throws Exception {
		PointEarnResponse earned = pointService.earn(new PointEarnRequest(1L, 1000L, EarnType.NORMAL, 365, UUID.randomUUID().toString()));
		PointEarnCancelRequest request = new PointEarnCancelRequest(1L, earned.pointKey());

		mockMvc.perform(post("/api/points/earn/cancel")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.meta.code").value("SUCCESS"))
				.andExpect(jsonPath("$.data.cancelledAmount").value(1000))
				.andExpect(jsonPath("$.data.totalBalance").value(0));
	}

	@Test
	@DisplayName("POST /api/points/use - 정상 사용")
	void use_success() throws Exception {
		pointService.earn(new PointEarnRequest(1L, 1000L, EarnType.NORMAL, 365, UUID.randomUUID().toString()));
		PointUseRequest request = new PointUseRequest(1L, 500L, "ORDER-1", UUID.randomUUID().toString());

		mockMvc.perform(post("/api/points/use")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.meta.code").value("SUCCESS"))
				.andExpect(jsonPath("$.data.usedAmount").value(500))
				.andExpect(jsonPath("$.data.orderId").value("ORDER-1"))
				.andExpect(jsonPath("$.data.totalBalance").value(500));
	}

	@Test
	@DisplayName("POST /api/points/use/cancel - 정상 사용취소")
	void useCancel_success() throws Exception {
		pointService.earn(new PointEarnRequest(1L, 1000L, EarnType.NORMAL, 365, UUID.randomUUID().toString()));
		PointUseResponse used = pointService.use(new PointUseRequest(1L, 1000L, "ORDER-1", UUID.randomUUID().toString()));
		PointUseCancelRequest request = new PointUseCancelRequest(1L, used.pointKey(), 500L, UUID.randomUUID().toString());

		mockMvc.perform(post("/api/points/use/cancel")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.meta.code").value("SUCCESS"))
				.andExpect(jsonPath("$.data.cancelledAmount").value(500))
				.andExpect(jsonPath("$.data.totalBalance").value(500));
	}

	@Test
	@DisplayName("POST /api/points/use - 주문번호 누락 시 실패")
	void use_missingOrderId() throws Exception {
		String body = "{\"memberId\": 1, \"amount\": 500, \"idempotencyKey\": \"" + UUID.randomUUID() + "\"}";

		mockMvc.perform(post("/api/points/use")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.meta.code").value("BAD_REQUEST"));
	}
}
