package com.example.project.global.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
public class OcrController {

    private final OcrClient ocrClient;

    // 1. 텍스트만 깔끔하게 받고 싶을 때
    @PostMapping("/text")
    public ResponseEntity<String> extractTextOnly(@RequestParam("file") MultipartFile file) {
        String result = ocrClient.getTextOnly(file);
        return ResponseEntity.ok(result);
    }

    // 2. 전체 데이터(좌표 포함)가 필요할 때
    @PostMapping("/all")
    public ResponseEntity<String> extractAllData(@RequestParam("file") MultipartFile file) {
        String result = ocrClient.getAllData(file);
        return ResponseEntity.ok(result);
    }
}
