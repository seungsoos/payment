package com.musinsa.payment.point.repository;

import com.musinsa.payment.point.entity.PointWallet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointWalletRepository extends JpaRepository<PointWallet, Long> {
}
