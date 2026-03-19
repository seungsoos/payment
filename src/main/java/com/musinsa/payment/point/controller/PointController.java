package com.musinsa.payment.point.controller;

import com.musinsa.payment.point.dto.PointEarnCancelRequest;
import com.musinsa.payment.point.dto.PointEarnCancelResponse;
import com.musinsa.payment.point.dto.PointEarnRequest;
import com.musinsa.payment.point.dto.PointEarnResponse;
import com.musinsa.payment.point.service.PointService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointController {

	private final PointService pointService;

	@PostMapping("/earn")
	public PointEarnResponse earn(@RequestBody @Valid PointEarnRequest request) {
		return pointService.earn(request);
	}

	@PostMapping("/earn/cancel")
	public PointEarnCancelResponse earnCancel(@RequestBody @Valid PointEarnCancelRequest request) {
		return pointService.earnCancel(request);
	}
}
