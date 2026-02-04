package com.example.project.member.controller;

import com.example.project.member.domain.Shipper;
import com.example.project.member.domain.Users;
import com.example.project.member.dto.ShipperRequest;
import com.example.project.member.service.ShipperService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/shippers")
@RequiredArgsConstructor
public class ShipperController {

    private final ShipperService shipperService;

    // 본인의 화주 프로필 저장 및 수정
    @PostMapping("/me")
    public ResponseEntity<String> saveOrUpdateShipper(
            @AuthenticationPrincipal Users user,
            @Valid @RequestBody ShipperRequest request) {
        shipperService.saveOrUpdate(user.getUserId(), request);
        return ResponseEntity.ok("화주 프로필이 성공적으로 저장되었습니다.");
    }

    // 본인의 화주 프로필 조회
    @GetMapping("/me")
    public ResponseEntity<Shipper> getShipperProfile(
    		@AuthenticationPrincipal Users user) {
        return ResponseEntity.ok(shipperService.getProfile(user.getUserId()));
    }

    // 본인의 화주 프로필 삭제
    @DeleteMapping("/me")
    public ResponseEntity<String> deleteShipperProfile(
    		@AuthenticationPrincipal Users user) {
        shipperService.deleteProfile(user.getUserId());
        return ResponseEntity.ok("화주 프로필이 삭제되었습니다.");
    }
    
    
 // 사업자 등록 번호 중복 확인 API
    @GetMapping("/check-biz-num")
    public ResponseEntity<Boolean> checkBizRegNum(
    		@RequestParam("bizRegNum") String bizRegNum) {
        // 중복이면 true, 사용 가능하면 false 반환
        return ResponseEntity.ok(shipperService.isBizRegNumDuplicate(bizRegNum));
    }
    
}