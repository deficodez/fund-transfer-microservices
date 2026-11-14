package com.defi.fundtransfer.account.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReverseRequest(@NotNull UUID transferId) {
}
