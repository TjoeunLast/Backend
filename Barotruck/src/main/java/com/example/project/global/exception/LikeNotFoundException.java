package com.example.project.global.exception;

public class LikeNotFoundException extends CustomException {

    public LikeNotFoundException() {
        super("Like record not found", "LIKE_NOT_FOUND");
    }
}
