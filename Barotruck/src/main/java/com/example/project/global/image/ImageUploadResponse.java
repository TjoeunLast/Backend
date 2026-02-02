package com.example.project.global.image;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ImageUploadResponse {
    private String imageUrl;
    private String s3Key;
    private String originalName;
}