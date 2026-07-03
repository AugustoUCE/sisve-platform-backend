package com.uce.sisve.vote.dto;

import jakarta.validation.constraints.NotNull;

public record VotoRequest(
        @NotNull Long idEleccion,
        @NotNull Long idCandidato,
        @NotNull Long idVotante
) {
}