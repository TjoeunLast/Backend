package com.example.project.global.exception;

public class AlreadyLikedException extends CustomException {

    public AlreadyLikedException() {
        super("Already liked this route", "LIKE_DUPLICATE");
    }
}
