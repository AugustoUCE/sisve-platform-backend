package com.uce.sisve.vote.crypto;

import com.uce.sisve.vote.db.VotoEntity;
import jakarta.enterprise.context.ApplicationScoped;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@ApplicationScoped
public class Sha256ChainUtil {

    public static final String HASH_SEMILLA = "0000000000000000000000000000000000000000000000000000000000000000";

    public String calcularHash(String votoCifrado, String hashAnterior) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((votoCifrado + hashAnterior).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value & 0xff));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("No fue posible calcular el hash del voto", exception);
        }
    }

    public boolean verificarCadena(List<VotoEntity> votos) {
        String hashAnteriorEsperado = HASH_SEMILLA;
        for (VotoEntity voto : votos) {
            if (!hashAnteriorEsperado.equals(voto.hashAnterior)) {
                return false;
            }
            String hashCalculado = calcularHash(voto.votoCifrado, voto.hashAnterior);
            if (!hashCalculado.equals(voto.hashActual)) {
                return false;
            }
            hashAnteriorEsperado = voto.hashActual;
        }
        return true;
    }
}