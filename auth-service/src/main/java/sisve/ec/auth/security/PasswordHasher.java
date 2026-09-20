package sisve.ec.auth.security;

import jakarta.enterprise.context.ApplicationScoped;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.HexFormat;

@ApplicationScoped
public class PasswordHasher {
    private static final int ITERATIONS = 120000;
    private static final int KEY_LENGTH = 256;
    private final SecureRandom secureRandom = new SecureRandom();

    public String newSalt() {
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        return HexFormat.of().formatHex(salt);
    }

    public String hash(String password, String saltHex) {
        try {
            byte[] salt = HexFormat.of().parseHex(saltHex);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return HexFormat.of().formatHex(hash);
        } catch (Exception exception) {
            throw new IllegalStateException("No fue posible verificar la credencial", exception);
        }
    }

    public boolean matches(String password, String saltHex, String expectedHash) {
        return java.security.MessageDigest.isEqual(
                hash(password, saltHex).getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                expectedHash.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }
}
