package com.example.taskmanagement.dto;

public record TokenResponse(String accessToken, String tokenType, long expiresInMs) {
}
