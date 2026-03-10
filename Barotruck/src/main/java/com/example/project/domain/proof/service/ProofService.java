package com.example.project.domain.proof.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.project.domain.order.domain.Order;
import com.example.project.domain.order.repository.OrderRepository;
import com.example.project.domain.proof.domain.Proof;
import com.example.project.domain.proof.dto.ProofResponseDto;
import com.example.project.domain.proof.repository.ProofRepository;
import com.example.project.global.image.ImageUploadResponse;
import com.example.project.global.image.S3ImageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProofService {

    private final ProofRepository proofRepository;
    private final OrderRepository orderRepository;
    private final S3ImageService s3ImageService;

    @Transactional
    public void uploadProof(Long orderId, MultipartFile receiptFile, MultipartFile signatureFile, String recipientName) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. ID: " + orderId));

        Proof proof = proofRepository.findByOrder_OrderId(orderId)
                .orElseGet(() -> Proof.builder()
                        .order(order)
                        .build());

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

    @Transactional(readOnly = true)
    public ProofResponseDto getProofInfo(Long orderId) {
        Proof proof = proofRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("해당 주문의 증빙 내역을 찾을 수 없습니다. ID: " + orderId));

        return ProofResponseDto.from(proof);
    }
}
