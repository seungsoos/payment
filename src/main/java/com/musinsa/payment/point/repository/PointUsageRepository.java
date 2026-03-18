package com.musinsa.payment.point.repository;

import com.musinsa.payment.point.entity.PointUsage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointUsageRepository extends JpaRepository<PointUsage, Long> {
}
