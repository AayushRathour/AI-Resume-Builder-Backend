package com.resumeai.auth.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentOrderRequest {

    @Min(value = 1, message = "Amount must be at least 1")
    private long amount;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotBlank(message = "Plan is required")
    private String plan;
}
