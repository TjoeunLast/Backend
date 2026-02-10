package com.example.project.domain.order.service.orderService;

import com.example.project.domain.order.domain.Order;
import com.example.project.domain.order.repository.OrderDriverQueryRepository;
import com.example.project.domain.order.repository.OrderRepository;
import com.example.project.member.repository.UsersRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import com.example.project.domain.order.dto.orderResponse.AssignedDriverInfoResponse;
import com.example.project.member.domain.Driver;
import com.example.project.member.domain.Users;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderDriverQueryService {

    private final UsersRepository usersRepository;

    public AssignedDriverInfoResponse getAssignedDriverInfo(Long driverNo) { // driverNo == userId
        Users user = usersRepository.findById(driverNo)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        Driver driver = user.getDriver();
        if (driver == null) {
            throw new IllegalArgumentException("해당 유저의 드라이버 정보가 없습니다.");
        }

        return AssignedDriverInfoResponse.from(user, driver);
    }
}
