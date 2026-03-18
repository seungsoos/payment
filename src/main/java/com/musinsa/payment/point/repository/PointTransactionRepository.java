package com.musinsa.payment.point.repository;

import com.musinsa.payment.point.entity.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

	Optional<PointTransaction> findByIdempotencyKey(String idempotencyKey);
}
