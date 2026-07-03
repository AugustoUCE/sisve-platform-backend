package com.uce.sisve.election.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PadronCargaRequest(
        @NotEmpty List<Long> idsVotantes
) {
}