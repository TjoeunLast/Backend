package com.example.project.member.service;

import com.example.project.member.domain.Shipper;
import com.example.project.member.domain.Users;
import com.example.project.member.dto.ShipperRequest;
import com.example.project.member.repository.ShipperRepository;
import com.example.project.member.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ShipperService {

    private final ShipperRepository shipperRepository;
    private final UsersRepository usersRepository;

    /**
     * 화주 정보 저장 및 수정 (C/U)
     */
    public void saveOrUpdate(Long userId, ShipperRequest request) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 1. 중복 체크: 새 프로필 생성 시에만 사업자 번호 중복 확인
        Optional<Shipper> existingProfile = shipperRepository.findByUser(user);
        
        if (existingProfile.isEmpty() && shipperRepository.existsByBizRegNum(request.getBizRegNum())) {
            throw new RuntimeException("이미 등록된 사업자 번호입니다: " + request.getBizRegNum());
        }

        Shipper shipper = existingProfile.orElse(new Shipper());

        // 2. 데이터 매핑 및 저장
        Shipper updatedShipper = Shipper.builder()
                .shipperId(shipper.getShipperId())
                .companyName(request.getCompanyName())
                .bizRegNum(request.getBizRegNum())
                .representative(request.getRepresentative())
                .bizAddress(request.getBizAddress())
                .isCorporate(request.getIsCorporate())
                .user(user)
                .build();

        shipperRepository.save(updatedShipper);
    }

    /**
     * 화주 정보 조회 (R)
     */
    @Transactional(readOnly = true)
    public Shipper getProfile(Long userId) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        return shipperRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("화주 프로필이 존재하지 않습니다."));
    }

    /**
     * 화주 정보 삭제 (D)
     */
    public void deleteProfile(Long userId) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        shipperRepository.findByUser(user).ifPresent(shipperRepository::delete);
    }
    
    
    // 중복 체크
    @Transactional(readOnly = true)
    public boolean isBizRegNumDuplicate(String bizRegNum) {
        return shipperRepository.existsByBizRegNum(bizRegNum);
    }
}