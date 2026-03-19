package com.musinsa.payment.point.repository;

import com.musinsa.payment.point.entity.PointTransaction;
import com.musinsa.payment.point.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

	Optional<PointTransaction> findByIdempotencyKey(String idempotencyKey);

	Optional<PointTransaction> findByPointKey(String pointKey);

	List<PointTransaction> findByRelatedPointKeyAndType(String relatedPointKey, TransactionType type);
}
