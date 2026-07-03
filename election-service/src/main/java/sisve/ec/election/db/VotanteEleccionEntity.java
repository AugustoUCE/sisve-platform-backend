package com.uce.sisve.election.db;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "votante_eleccion", schema = "election_schema")
public class VotanteEleccionEntity extends PanacheEntityBase {

    @EmbeddedId
    public VotanteEleccionId id;

    @Column(name = "ha_votado", nullable = false)
    public Boolean haVotado = false;

    @Column(name = "fecha_participacion")
    public LocalDateTime fechaParticipacion;

    @Embeddable
    public static class VotanteEleccionId implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Column(name = "id_votante", nullable = false)
        public Long idVotante;

        @Column(name = "id_eleccion", nullable = false)
        public Long idEleccion;

        public VotanteEleccionId() {
        }

        public VotanteEleccionId(Long idVotante, Long idEleccion) {
            this.idVotante = idVotante;
            this.idEleccion = idEleccion;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            VotanteEleccionId that = (VotanteEleccionId) o;
            return Objects.equals(idVotante, that.idVotante) && Objects.equals(idEleccion, that.idEleccion);
        }

        @Override
        public int hashCode() {
            return Objects.hash(idVotante, idEleccion);
        }
    }
}