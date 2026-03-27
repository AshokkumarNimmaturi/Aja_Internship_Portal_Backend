package com.aja.internshipportal.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.Setter;


//Our custom exception — thrown anywhere in services
//carries HTTP status so GlobalExceptionHandler returns correct code

@Getter
@Setter
public class AppException extends RuntimeException{

	private final HttpStatus status;

	public AppException(String message,HttpStatus status) {
		super();
		this.status = status;
	}
	
	//400 Bad Request - wrong input
	public static AppException badRequest(String message) {
		return new AppException(message, HttpStatus.BAD_REQUEST);
	}
	
    // 401 Unauthorized — not logged in
    public static AppException unauthorized(String message) {
        return new AppException(message, HttpStatus.UNAUTHORIZED);
    }

    // 403 Forbidden — logged in but wrong role
    public static AppException forbidden(String message) {
        return new AppException(message, HttpStatus.FORBIDDEN);
    }

    // 404 Not Found — record does not exist
    public static AppException notFound(String message) {
        return new AppException(message, HttpStatus.NOT_FOUND);
    }

    // 409 Conflict — duplicate email, already exists
    public static AppException conflict(String message) {
        return new AppException(message, HttpStatus.CONFLICT);
    }
}
