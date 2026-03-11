package com.example.project.domain.dispatch.repository;

import com.example.project.domain.dispatch.domain.DispatchJob;
import com.example.project.domain.dispatch.domain.DispatchOffer;
import com.example.project.domain.dispatch.domain.dispatchEnum.DispatchEnums.DispatchOfferStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DispatchOfferRepository extends JpaRepository<DispatchOffer, Long> {

    boolean existsByJobAndDriverUserId(DispatchJob job, Long driverUserId);

    List<DispatchOffer> findByJobOrderByWaveAscRankAsc(DispatchJob job);

    List<DispatchOffer> findByJob_DispatchJobIdOrderByWaveAscRankAsc(Long dispatchJobId);

    List<DispatchOffer> findByJobAndStatusIn(DispatchJob job, Collection<DispatchOfferStatus> statuses);

    List<DispatchOffer> findByDriverUserIdAndStatusInOrderByExpireAtAsc(Long driverUserId, Collection<DispatchOfferStatus> statuses);

    Optional<DispatchOffer> findByDispatchOfferIdAndDriverUserId(Long dispatchOfferId, Long driverUserId);
}
