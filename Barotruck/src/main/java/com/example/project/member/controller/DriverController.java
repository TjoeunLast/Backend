package com.example.project.member.controller;

import com.example.project.member.domain.Driver;
import com.example.project.member.domain.Users;
import com.example.project.member.dto.DriverRequest;
import com.example.project.member.service.DriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    // 본인의 차주 프로필 저장 및 수정
    @PostMapping("/me")
    public ResponseEntity<String> saveOrUpdateDriver(
            @AuthenticationPrincipal Users user,
            @Valid @RequestBody DriverRequest request) {
        driverService.saveOrUpdate(user.getUserId(), request);
        return ResponseEntity.ok("차주 프로필이 성공적으로 저장되었습니다.");
    }

    // 본인의 차주 프로필 조회
    @GetMapping("/me")
    public ResponseEntity<Driver> getDriverProfile(@AuthenticationPrincipal Users user) {
        return ResponseEntity.ok(driverService.getProfile(user.getUserId()));
    }

    // 본인의 차주 프로필 삭제
    @DeleteMapping("/me")
    public ResponseEntity<String> deleteDriverProfile(@AuthenticationPrincipal Users user) {
        driverService.deleteProfile(user.getUserId());
        return ResponseEntity.ok("차주 프로필이 삭제되었습니다.");
    }
    
    
 // 차량 번호 중복 확인 API
    @GetMapping("/check-car-num")
    public ResponseEntity<Boolean> checkCarNum(
    		@RequestParam("carNum") String carNum) {
        // 중복이면 true, 사용 가능하면 false 반환
        return ResponseEntity.ok(driverService.isCarNumDuplicate(carNum));
    }
    
}