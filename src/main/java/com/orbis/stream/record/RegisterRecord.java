package com.orbis.stream.record;


import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record RegisterRecord(
        @NotNull(message = "not.valid.input") String nickName,
        @NotNull(message = "not.valid.input") String email,
        @NotNull(message = "not.valid.input") @NotEmpty(message = "not.valid.input") String password) {
}
