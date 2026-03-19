package com.musinsa.payment.point.controller;

import com.musinsa.payment.point.dto.*;
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

	@PostMapping("/use")
	public PointUseResponse use(@RequestBody @Valid PointUseRequest request) {
		return pointService.use(request);
	}

	@PostMapping("/use/cancel")
	public PointUseCancelResponse useCancel(@RequestBody @Valid PointUseCancelRequest request) {
		return pointService.useCancel(request);
	}
}
