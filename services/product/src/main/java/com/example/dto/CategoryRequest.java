package com.example.dto;

import jakarta.validation.constraints.NotNull;

public record CategoryRequest(
        @NotNull(message = "Category name is required")
        String name,
        @NotNull(message = "Category description is required")
        String description
) {
}

