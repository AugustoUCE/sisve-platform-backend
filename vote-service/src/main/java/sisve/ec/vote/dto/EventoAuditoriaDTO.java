package com.uce.sisve.vote.dto;

public record EventoAuditoriaDTO(
        String tipoEvento,
        String descripcion,
        String servicioOrigen
) {
}