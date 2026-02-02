package com.example.project.domain.proof.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.project.domain.proof.domain.Proof;
import com.example.project.domain.proof.dto.ProofResponseDto; // DTO 임포트 확인
import com.example.project.domain.proof.repository.ProofRepository;
import com.example.project.global.image.ImageUploadResponse;
import com.example.project.global.image.S3ImageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProofService {

    private final ProofRepository proofRepository;
    private final S3ImageService s3ImageService;

    // 1. [Create/Update] 증빙 업로드 로직 (기존 코드 유지)
    @Transactional
    public void uploadProof(Long orderId, MultipartFile receiptFile, MultipartFile signatureFile, String recipientName) {
        Proof proof = proofRepository.findByOrder_OrderId(orderId)
                .orElse(new Proof());
        
        if (receiptFile != null && !receiptFile.isEmpty()) {
            ImageUploadResponse receiptRes = s3ImageService.upload(receiptFile, "proofs/receipts");
            proof.updateReceiptImage(receiptRes);
        }

        if (signatureFile != null && !signatureFile.isEmpty()) {
            ImageUploadResponse signatureRes = s3ImageService.upload(signatureFile, "proofs/signatures");
            proof.updateSignatureImage(signatureRes);
        }

        proof.setRecipientName(recipientName);
        proofRepository.save(proof);
    }

    // 2. [Read] 증빙 정보 조회 로직 (추가된 부분)
    @Transactional(readOnly = true)
    public ProofResponseDto getProofInfo(Long orderId) {
        // 주문 ID로 증빙 데이터를 찾고, 없으면 예외를 발생시킵니다.
        Proof proof = proofRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("해당 주문의 증빙 내역을 찾을 수 없습니다. ID: " + orderId));

        // 미리 만들어둔 DTO의 from 메서드를 사용해 변환하여 반환합니다.
        return ProofResponseDto.from(proof);
    }
}