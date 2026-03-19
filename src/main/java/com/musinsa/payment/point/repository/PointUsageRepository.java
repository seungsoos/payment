package com.musinsa.payment.point.repository;

import com.musinsa.payment.point.entity.PointUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointUsageRepository extends JpaRepository<PointUsage, Long> {

	List<PointUsage> findByTransactionId(Long transactionId);
}
