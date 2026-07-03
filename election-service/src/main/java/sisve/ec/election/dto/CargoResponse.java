package com.uce.sisve.election.dto;

public record CargoResponse(
        Long idCargo,
        Long idEleccion,
        String nombre
) {
}