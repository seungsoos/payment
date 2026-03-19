package com.musinsa.payment.point.repository;

import com.musinsa.payment.point.entity.PointEarn;
import com.musinsa.payment.point.entity.PointStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PointEarnRepository extends JpaRepository<PointEarn, Long> {

	Optional<PointEarn> findByPointKey(String pointKey);

	@Query("SELECT p FROM PointEarn p WHERE p.walletId = :walletId " +
			"AND p.status = :status AND p.expiresAt > :now AND p.remainingAmount > 0 " +
			"ORDER BY CASE WHEN p.earnType = 'MANUAL' THEN 0 ELSE 1 END, p.expiresAt ASC")
	List<PointEarn> findUsablePoints(Long walletId, PointStatus status, LocalDateTime now);
}
