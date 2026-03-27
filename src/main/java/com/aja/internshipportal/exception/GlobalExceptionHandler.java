package com.aja.internshipportal.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.aja.internshipportal.dto.response.ApiResponse;

//Catches every exception thrown anywhere in the app
//Returns clean JSON instead of ugly Spring error pages
@RestControllerAdvice
public class GlobalExceptionHandler {

 // Our custom AppException
 @ExceptionHandler(AppException.class)
 public ResponseEntity<ApiResponse> handleAppException(AppException ex) {
     return ResponseEntity
             .status(ex.getStatus())
             .body(ApiResponse.failure(ex.getMessage()));
 }

 // Validation errors — @NotBlank, @Email, @Size etc.
 // Returns map of field → error message
 @ExceptionHandler(MethodArgumentNotValidException.class)
 public ResponseEntity<Map<String, String>> handleValidationException(
         MethodArgumentNotValidException ex) {

     Map<String, String> errors = new HashMap<>();
     ex.getBindingResult().getAllErrors().forEach(error -> {
         String field = ((FieldError) error).getField();
         String message = error.getDefaultMessage();
         errors.put(field, message);
     });

     return ResponseEntity
             .status(HttpStatus.BAD_REQUEST)
             .body(errors);
 }

 // Wrong email or password during login
 @ExceptionHandler(BadCredentialsException.class)
 public ResponseEntity<ApiResponse> handleBadCredentials(
         BadCredentialsException ex) {
     return ResponseEntity
             .status(HttpStatus.UNAUTHORIZED)
             .body(ApiResponse.failure("Invalid email or password"));
 }

 // Account deactivated by admin
 @ExceptionHandler(DisabledException.class)
 public ResponseEntity<ApiResponse> handleDisabled(DisabledException ex) {
     return ResponseEntity
             .status(HttpStatus.FORBIDDEN)
             .body(ApiResponse.failure("Your account has been deactivated"));
 }

 // Trying to access route without required role
 @ExceptionHandler(AccessDeniedException.class)
 public ResponseEntity<ApiResponse> handleAccessDenied(
         AccessDeniedException ex) {
     return ResponseEntity
             .status(HttpStatus.FORBIDDEN)
             .body(ApiResponse.failure("You do not have permission to access this resource"));
 }

 // Catch-all — any unexpected exception
 @ExceptionHandler(Exception.class)
 public ResponseEntity<ApiResponse> handleGenericException(Exception ex) {
     return ResponseEntity
             .status(HttpStatus.INTERNAL_SERVER_ERROR)
             .body(ApiResponse.failure("Something went wrong. Please try again."));
 }
}
