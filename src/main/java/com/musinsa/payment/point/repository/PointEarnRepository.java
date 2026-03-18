package com.musinsa.payment.point.repository;

import com.musinsa.payment.point.entity.PointEarn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointEarnRepository extends JpaRepository<PointEarn, Long> {

	Optional<PointEarn> findByPointKey(String pointKey);
}
