package narek.hakobyan.mypassword;

import android.util.Base64;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class PasswordHashManager {
    private static final int SALT_SIZE = 16;
    private static final int ITERATIONS = 120000;
    private static final int KEY_LENGTH = 256;

    public String hashPassword(String plainPassword) {
        byte[] salt = new byte[SALT_SIZE];
        new SecureRandom().nextBytes(salt);
        byte[] hash = hash(plainPassword.toCharArray(), salt);
        return encode(salt) + ":" + encode(hash);
    }

    public boolean verifyPassword(String plainPassword, String storedValue) {
        if (storedValue == null || !storedValue.contains(":")) {
            return false;
        }

        String[] parts = storedValue.split(":", 2);
        byte[] salt = decode(parts[0]);
        byte[] expectedHash = decode(parts[1]);
        byte[] actualHash = hash(plainPassword.toCharArray(), salt);
        return constantTimeEquals(expectedHash, actualHash);
    }

    private byte[] hash(char[] password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Hashing failed", e);
        }
    }

    private String encode(byte[] data) {
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }

    private byte[] decode(String value) {
        return Base64.decode(value, Base64.NO_WRAP);
    }

    private boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
}
