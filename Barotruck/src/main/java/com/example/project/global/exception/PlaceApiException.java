package com.example.project.global.exception;

/**
 * PlaceApiException
 * --------------------------------------------------
 * Place 도메인에서 외부 API(Google Places 등)
 * 호출 실패 시 사용하는 커스텀 런타임 예외.
 *
 * 목적:
 *  - 외부 API 예외를 그대로 노출하지 않고
 *  - 우리 서비스 도메인 예외로 추상화
 *
 * 사용 예:
 *  - Google Place Details 응답 null
 *  - 필수 좌표(lat/lng) 누락
 *  - 외부 API 호출 실패
 */
public class PlaceApiException extends RuntimeException {

    public PlaceApiException(String message) {
        super(message);
    }

    public PlaceApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
