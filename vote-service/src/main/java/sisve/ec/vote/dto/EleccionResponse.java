package com.uce.sisve.vote.dto;

import java.time.LocalDateTime;

public record EleccionResponse(
        Long idEleccion,
        String nombre,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        String estado
) {
}