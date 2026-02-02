package com.example.project.global.image;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageInfo {

    @Column(length = 500)
    private String imageUrl;

    private String s3Key;

    private String originalName;

    // 생성자: 서비스에서 받은 Response DTO를 바로 ImageInfo로 변환
    public ImageInfo(ImageUploadResponse res) {
        this.imageUrl = res.getImageUrl();
        this.s3Key = res.getS3Key();
        this.originalName = res.getOriginalName();
    }
}