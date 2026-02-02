package com.example.project.domain.proof.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.project.domain.proof.dto.ProofResponseDto;
import com.example.project.domain.proof.service.ProofService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/proof")
@RequiredArgsConstructor
public class ProofController {

    private final ProofService proofService;

    /**
     * 운송 완료 증빙 업로드 (차주용)
     * 인수증 사진과 서명 이미지를 한 번에 받습니다.
     */
    @PostMapping("/{orderId}")
    public ResponseEntity<Boolean> uploadProof(
            @PathVariable("orderId") Long orderId,
            @RequestPart(value = "receipt", required = false) MultipartFile receipt,
            @RequestPart(value = "signature", required = false) MultipartFile signature,
            @RequestParam("recipientName") String recipientName
    ) {
        proofService.uploadProof(orderId, receipt, signature, recipientName);
        return ResponseEntity.ok(true);
    }

    /**
     * 증빙 내역 상세 조회 (화주/관리자용)
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ProofResponseDto> getProof(@PathVariable("orderId") Long orderId) {
        // 서비스에서 엔티티를 조회하여 DTO로 변환해 반환합니다.
        return ResponseEntity.ok(proofService.getProofInfo(orderId));
    }
}
