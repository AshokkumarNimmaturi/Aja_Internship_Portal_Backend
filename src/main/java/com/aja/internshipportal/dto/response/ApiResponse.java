package com.aja.internshipportal.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

//Generic wrapper used for simple success/failure messages
//e.g. { "success": true, "message": "Password changed successfully" }
@Getter @Setter @Builder @AllArgsConstructor
public class ApiResponse {

 private boolean success;
 private String message;

 // Quick static factory methods
 public static ApiResponse success(String message) {
     return new ApiResponse(true, message);
 }

 public static ApiResponse failure(String message) {
     return new ApiResponse(false, message);
 }
}
