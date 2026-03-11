package com.example.project.domain.dispatch.repository;

import com.example.project.domain.dispatch.domain.DriverAvailabilitySnapshot;
import com.example.project.domain.dispatch.domain.dispatchEnum.DispatchEnums.DriverAvailabilityStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DriverAvailabilitySnapshotRepository extends JpaRepository<DriverAvailabilitySnapshot, Long> {

    List<DriverAvailabilitySnapshot> findByAvailabilityStatus(DriverAvailabilityStatus availabilityStatus);
}
