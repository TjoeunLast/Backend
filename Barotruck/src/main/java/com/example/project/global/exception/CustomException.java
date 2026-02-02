package com.example.project.global.exception;

public class CustomException extends RuntimeException {

    private final String code;

    public CustomException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
