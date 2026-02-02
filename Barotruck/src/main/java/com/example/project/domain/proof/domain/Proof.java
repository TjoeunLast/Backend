package com.example.project.domain.proof.domain;


//import com.example.project.domain.order.Order;
import com.example.project.global.image.ImageInfo;
import com.example.project.global.image.ImageUploadResponse;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "PROOFS")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Proof {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_proof_gen")
    @SequenceGenerator(name = "seq_proof_gen", sequenceName = "SEQ_PROOF_ID", allocationSize = 1)
    private Long proofId;

//    @OneToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "order_id")
//    private Order order; // 어떤 주문에 대한 증빙인지 연결

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name="imageUrl", column=@Column(name="receipt_img_url")),
        @AttributeOverride(name="s3Key", column=@Column(name="receipt_s3_key"))
    })
    private ImageInfo receiptImage; // 인수증/물품 사진

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name="imageUrl", column=@Column(name="signature_img_url")),
        @AttributeOverride(name="s3Key", column=@Column(name="signature_s3_key"))
    })
    private ImageInfo signatureImage; // 수령인 서명 이미지

    private String recipientName; // 실제 물건을 받은 사람 이름

    @Column(columnDefinition = "CLOB")
    private String ocrResult; // OCR로 읽어온 텍스트 (추후 확장용)

    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // 이미지 업데이트 편의 메서드 (Users 패턴 적용)
    public void updateReceiptImage(ImageUploadResponse res) {
        this.receiptImage = new ImageInfo(res);
    }

    public void updateSignatureImage(ImageUploadResponse res) {
        this.signatureImage = new ImageInfo(res);
    }
}
