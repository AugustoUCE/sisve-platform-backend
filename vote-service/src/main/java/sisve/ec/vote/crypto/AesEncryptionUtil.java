package com.uce.sisve.vote.crypto;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@ApplicationScoped
public class AesEncryptionUtil {

    private static final int IV_LENGTH = 16;

    @ConfigProperty(name = "vote.aes.secret.key")
    String aesKeyBase64;

    private final SecureRandom secureRandom = new SecureRandom();
    private SecretKey key;

    @PostConstruct
    void init() {
        if (aesKeyBase64 == null || aesKeyBase64.isBlank()) {
            key = null;
            return;
        }
        byte[] decoded = Base64.getDecoder().decode(aesKeyBase64);
        key = new SecretKeySpec(decoded, "AES");
    }

    public boolean estaConfigurado() {
        return key != null;
    }

    public String cifrar(String texto) {
        validarConfiguracion();
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
            byte[] encrypted = cipher.doFinal(texto.getBytes(StandardCharsets.UTF_8));

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(iv);
            outputStream.write(encrypted);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception exception) {
            throw new IllegalStateException("No fue posible cifrar el voto", exception);
        }
    }

    public String descifrar(String textoCifrado) {
        validarConfiguracion();
        try {
            byte[] data = Base64.getDecoder().decode(textoCifrado);
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[data.length - IV_LENGTH];
            System.arraycopy(data, 0, iv, 0, IV_LENGTH);
            System.arraycopy(data, IV_LENGTH, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("No fue posible descifrar el voto", exception);
        }
    }

    private void validarConfiguracion() {
        if (key == null) {
            throw new IllegalStateException("La clave AES no fue configurada");
        }
    }
}