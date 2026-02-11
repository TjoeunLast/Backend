package com.example.project.domain.order.service.orderService;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.project.domain.order.domain.Order;
import com.example.project.domain.order.dto.orderResponse.AssignedDriverInfoResponse;
import com.example.project.domain.order.repository.OrderRepository;
import com.example.project.member.domain.Driver;
import com.example.project.member.domain.Users;
import com.example.project.member.repository.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderDriverQueryService {

    private final UsersRepository usersRepository;
    private final OrderRepository orderRepository; // 추가
    
    public AssignedDriverInfoResponse getAssignedDriverInfo(Long driverNo) { // driverNo == userId
        Users user = usersRepository.findById(driverNo)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        Driver driver = user.getDriver();
        if (driver == null) {
            throw new IllegalArgumentException("해당 유저의 드라이버 정보가 없습니다.");
        }

        return AssignedDriverInfoResponse.from(user, driver);
    }
    
    /**
     * 특정 오더에 신청한 모든 기사들의 정보를 조회 (화주용)
     */
    public List<AssignedDriverInfoResponse> getApplicantsInfo(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("오더를 찾을 수 없습니다."));

        // driverList에 담긴 userId(driverNo)들을 순회하며 정보를 가져옴
        return order.getDriverList().stream()
                .map(driverNo -> {
                    Users user = usersRepository.findById(driverNo).orElse(null);
                    if (user != null && user.getDriver() != null) {
                        return AssignedDriverInfoResponse.from(user, user.getDriver());
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
