package com.orbis.stream.record;

import jakarta.validation.constraints.NotNull;

public record LoginRecord(@NotNull(message = "not.valid.input") String email, @NotNull(message = "not.valid.input") String password) {
}
