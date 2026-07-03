package com.uce.sisve.vote.dto;

import java.time.LocalDateTime;

public record VotoResponse(
        String mensaje,
        String hashActual,
        LocalDateTime fechaRegistro
) {
}