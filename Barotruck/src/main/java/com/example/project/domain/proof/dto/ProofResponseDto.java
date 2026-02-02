package com.example.project.domain.proof.dto;

import com.example.project.domain.proof.domain.Proof;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProofResponseDto {
    private Long proofId;
    private String receiptImageUrl;
    private String signatureImageUrl;
    private String recipientName;

    public static ProofResponseDto from(Proof proof) {
        return ProofResponseDto.builder()
                .proofId(proof.getProofId())
                .receiptImageUrl(proof.getReceiptImage() != null ? proof.getReceiptImage().getImageUrl() : "")
                .signatureImageUrl(proof.getSignatureImage() != null ? proof.getSignatureImage().getImageUrl() : "")
                .recipientName(proof.getRecipientName())
                .build();
    }
}
