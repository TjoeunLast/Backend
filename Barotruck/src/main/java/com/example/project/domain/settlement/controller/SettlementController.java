package com.example.project.domain.settlement.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.project.domain.settlement.dto.SettlementRequest;
import com.example.project.domain.settlement.service.SettlementService;
import com.example.project.member.domain.Users;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    // 결제 요청 생성
    @PostMapping("/init")
    public ResponseEntity<String> init(
    		@RequestBody SettlementRequest request,                           
    		@AuthenticationPrincipal Users user) {
        settlementService.initiateSettlement(request, user);
        return ResponseEntity.ok("결제 준비가 완료되었습니다.");
    }

    // 결제 승인 (실제 결제 프로세스 완료 후 호출)
    @PatchMapping("/{orderId}/complete")
    public ResponseEntity<String> complete(@PathVariable Long orderId) {
        settlementService.completeSettlement(orderId);
        return ResponseEntity.ok("결제가 성공적으로 완료되었습니다.");
    }
}
