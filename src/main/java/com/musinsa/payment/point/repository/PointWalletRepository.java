package com.musinsa.payment.point.repository;

import com.musinsa.payment.point.entity.PointWallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PointWalletRepository extends JpaRepository<PointWallet, Long> {

	Optional<PointWallet> findByMemberId(Long memberId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT pw FROM PointWallet pw WHERE pw.memberId = :memberId")
	Optional<PointWallet> findByMemberIdWithLock(Long memberId);
}
