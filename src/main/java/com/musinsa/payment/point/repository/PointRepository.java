package com.musinsa.payment.point.repository;

import com.musinsa.payment.point.entity.Point;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointRepository extends JpaRepository<Point, Long> {
}
