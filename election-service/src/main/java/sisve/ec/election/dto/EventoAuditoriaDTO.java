package com.uce.sisve.election.dto;

public record EventoAuditoriaDTO(
        String tipoEvento,
        String descripcion,
        String servicioOrigen
) {
}