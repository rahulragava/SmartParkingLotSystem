package com.smartparking.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CheckOutRequest(@NotBlank String ticketNumber) {
}
