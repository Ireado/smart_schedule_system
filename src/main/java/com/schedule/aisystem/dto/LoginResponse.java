package com.schedule.aisystem.dto;

public record LoginResponse(
        String accountNo,
        String displayName,
        String role,
        String message
) {
}

