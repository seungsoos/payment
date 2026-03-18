package com.musinsa.payment.point.repository;

import com.musinsa.payment.point.entity.PolicyKey;
import com.musinsa.payment.point.entity.PointPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointPolicyRepository extends JpaRepository<PointPolicy, Long> {

	Optional<PointPolicy> findByPolicyKey(PolicyKey policyKey);
}
