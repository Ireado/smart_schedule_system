package com.schedule.aisystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
        @NotBlank
        @Pattern(regexp = "\\d{6}", message = "账号必须为 6 位数字")
        String accountNo,
        @NotBlank
        String password
) {
}

