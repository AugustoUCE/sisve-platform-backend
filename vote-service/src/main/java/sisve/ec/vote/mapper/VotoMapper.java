package com.uce.sisve.vote.mapper;

import com.uce.sisve.vote.db.VotoEntity;
import com.uce.sisve.vote.dto.VotoResponse;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;

@ApplicationScoped
public class VotoMapper {

    public VotoEntity toEntity(Long idEleccion, String votoCifrado, String hashAnterior, String hashActual) {
        VotoEntity entity = new VotoEntity();
        entity.idEleccion = idEleccion;
        entity.votoCifrado = votoCifrado;
        entity.hashAnterior = hashAnterior;
        entity.hashActual = hashActual;
        entity.fechaRegistro = LocalDateTime.now();
        return entity;
    }

    public VotoResponse toResponse(VotoEntity entity) {
        return new VotoResponse("Voto registrado exitosamente", entity.hashActual, entity.fechaRegistro);
    }
}