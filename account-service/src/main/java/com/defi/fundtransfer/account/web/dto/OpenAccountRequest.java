package com.defi.fundtransfer.account.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OpenAccountRequest(
        @NotBlank @Size(max = 120) String ownerName,
        @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "must be a 3-letter ISO currency code") String currency,
        @Min(0) long initialBalanceMinor) {
}
