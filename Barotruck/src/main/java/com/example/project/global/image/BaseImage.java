package com.example.project.global.image;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

@Getter
@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseImage {

    @Column(nullable = false, length = 500)
    private String imageUrl;

    @Column(nullable = false)
    private String s3Key; // S3 삭제/수정 시 식별자

    @Column(nullable = false)
    private String originalName;

    protected BaseImage(String imageUrl, String s3Key, String originalName) {
        this.imageUrl = imageUrl;
        this.s3Key = s3Key;
        this.originalName = originalName;
    }
}
