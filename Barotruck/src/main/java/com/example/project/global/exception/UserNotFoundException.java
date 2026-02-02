package com.example.project.global.exception;

public class UserNotFoundException extends CustomException {

    public UserNotFoundException() {
        super("User not found", "USER_NOT_FOUND");
    }
}
