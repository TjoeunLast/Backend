package com.example.project.global.config;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class OcrClient {

    @Value("${clova.ocr.invoke-url}")
    private String invokeUrl;

    @Value("${clova.ocr.secret-key}")
    private String secretKey;

    // 버전 1: 순수하게 추출된 텍스트(문장)만 반환
    public String getTextOnly(MultipartFile file) {
        String fullJsonResponse = getRawOcrResult(file);
        if (fullJsonResponse.startsWith("Error:")) return fullJsonResponse;

        JSONObject responseJson = new JSONObject(fullJsonResponse);
        JSONArray imagesArray = responseJson.getJSONArray("images");
        StringBuilder extractedText = new StringBuilder();

        if (imagesArray.length() > 0) {
            JSONArray fields = imagesArray.getJSONObject(0).getJSONArray("fields");
            for (int i = 0; i < fields.length(); i++) {
                String text = fields.getJSONObject(i).getString("inferText");
                extractedText.append(text).append(" ");
            }
        }
        return extractedText.toString().trim();
    }

    // 버전 2: 텍스트 + 원본 JSON 전체를 합쳐서 반환
    public String getAllData(MultipartFile file) {
        String fullJsonResponse = getRawOcrResult(file);
        if (fullJsonResponse.startsWith("Error:")) return fullJsonResponse;

        JSONObject rawResponse = new JSONObject(fullJsonResponse);
        
        // 텍스트 추출 부분
        StringBuilder extractedText = new StringBuilder();
        JSONArray imagesArray = rawResponse.getJSONArray("images");
        if (imagesArray.length() > 0) {
            JSONArray fields = imagesArray.getJSONObject(0).getJSONArray("fields");
            for (int i = 0; i < fields.length(); i++) {
                extractedText.append(fields.getJSONObject(i).getString("inferText")).append(" ");
            }
        }

        // 결과 합치기
        JSONObject finalResult = new JSONObject();
        finalResult.put("extractedText", extractedText.toString().trim());
        finalResult.put("original", rawResponse); // 전체 데이터 포함

        return finalResult.toString();
    }

    // [공통 내부 메소드] Clova API와 직접 통신하여 원본 JSON 문자열을 가져옴
    private String getRawOcrResult(MultipartFile file) {
        try {
            URL url = new URL(invokeUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setConnectTimeout(5000); // 연결 시도 시간 5초
            con.setReadTimeout(30000);    // 데이터 읽기 시간 30초 (OCR은 처리가 느릴 수 있음)
            con.setUseCaches(false);
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            con.setRequestProperty("X-OCR-SECRET", secretKey);

            byte[] fileBytes = file.getBytes();
            String encodeFile = Base64.getEncoder().encodeToString(fileBytes);

            JSONObject json = new JSONObject();
            json.put("version", "V2");
            json.put("requestId", UUID.randomUUID().toString());
            json.put("timestamp", System.currentTimeMillis());

            JSONObject image = new JSONObject();
            image.put("format", getFileExtension(file.getOriginalFilename()));
            image.put("data", encodeFile);
            image.put("name", "ocr_image");

            JSONArray images = new JSONArray();
            images.put(image);
            json.put("images", images);

            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.write(json.toString().getBytes("UTF-8"));
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();
            BufferedReader br = new BufferedReader(new InputStreamReader(
                (responseCode == 200) ? con.getInputStream() : con.getErrorStream()
            ));

            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = br.readLine()) != null) {
                response.append(inputLine);
            }
            br.close();

            return response.toString();

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        }
        return "jpg";
    }
}