package com.example.project.member.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.project.member.domain.Driver;
import com.example.project.member.domain.Users;
import com.example.project.member.dto.DriverRequest;
import com.example.project.member.repository.DriverRepository;
import com.example.project.member.repository.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DriverService {

    private final DriverRepository driverRepository;
    private final UsersRepository usersRepository;

    /**
     * 차주 정보 저장 및 수정 (C/U)
     */
    public void saveOrUpdate(Long userId, DriverRequest request) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 1. 중복 체크: 새 프로필 생성 시 차량 번호 중복 확인
        Optional<Driver> existingProfile = driverRepository.findByUser(user);

        if (existingProfile.isEmpty() && driverRepository.existsByCarNum(request.getCarNum())) {
            throw new RuntimeException("이미 등록된 차량 번호입니다: " + request.getCarNum());
        }

        Driver driver = existingProfile.orElse(new Driver());

        // 2. 데이터 매핑 및 저장
        Driver updatedDriver = Driver.builder()
                .driverId(driver.getDriverId())
                .carNum(request.getCarNum())
                .carType(request.getCarType())
                .tonnage(request.getTonnage())
                .career(request.getCareer())
                .bankName(request.getBankName())
                .accountNum(request.getAccountNum())
                .type(request.getType())
                .isSuspended(driver.getIsSuspended() == null ? "N" : driver.getIsSuspended()) // 기본값 유지
                .user(user)
                .build();

        driverRepository.save(updatedDriver);
    }

    /**
     * 차주 정보 조회 (R)
     */
    @Transactional(readOnly = true)
    public Driver getProfile(Long userId) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        return driverRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("차주 프로필이 존재하지 않습니다."));
    }

    /**
     * 차주 정보 삭제 (D)
     */
    public void deleteProfile(Long userId) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        driverRepository.findByUser(user).ifPresent(driverRepository::delete);
    }
    
    // 중복체크
    @Transactional(readOnly = true)
    public boolean isCarNumDuplicate(String carNum) {
        return driverRepository.existsByCarNum(carNum);
    }
}