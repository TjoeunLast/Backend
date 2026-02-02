package com.example.project.global.image;


import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3ImageService {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public ImageUploadResponse upload(MultipartFile file, String dirName) {
        // 1. 파일명 중복 방지를 위한 UUID 생성
        String originalName = file.getOriginalFilename();
        String s3Key = dirName + "/" + UUID.randomUUID() + "_" + originalName;

        // 2. S3에 업로드할 메타데이터 설정
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        try {
            // 3. S3 업로드 실행
            amazonS3.putObject(bucket, s3Key, file.getInputStream(), metadata);
        } catch (IOException e) {
            throw new RuntimeException("이미지 업로드 중 오류가 발생했습니다.", e);
        }

        // 4. 결과 반환 (이 정보를 받아서 각 도메인 엔티티를 생성하면 됨)
        return ImageUploadResponse.builder()
                .imageUrl(amazonS3.getUrl(bucket, s3Key).toString())
                .s3Key(s3Key)
                .originalName(originalName)
                .build();
    }

    // S3 파일 삭제 기능 (나중에 게시글 삭제 시 사용)
    public void delete(String s3Key) {
        amazonS3.deleteObject(bucket, s3Key);
    }
}