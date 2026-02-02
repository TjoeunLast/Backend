package com.example.project.global.exception;

public class RouteNotFoundException extends CustomException {

    public RouteNotFoundException() {
        super("Route not found", "ROUTE_NOT_FOUND");
    }
}
