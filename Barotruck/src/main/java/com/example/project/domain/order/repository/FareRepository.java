package com.example.project.domain.order.repository;

import com.example.project.domain.order.domain.FarePolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FareRepository extends JpaRepository<FarePolicy, Long> {
    Optional<FarePolicy> findTop1ByDayTypeAndTimeType(FarePolicy.DayType dayType, FarePolicy.TimeType timeType);
}