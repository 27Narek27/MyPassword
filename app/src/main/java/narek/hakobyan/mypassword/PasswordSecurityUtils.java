package narek.hakobyan.mypassword;
import java.security.SecureRandom;
public final class PasswordSecurityUtils {
    public static final int MIN_MASTER_PASSWORD_LENGTH = 16;
    public static final String MASTER_VALIDATION_ERROR_MESSAGE =
            "Master password must be at least 16 characters and contain "
                    + "uppercase letters, digits, and special characters";
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS    = "0123456789";
    private static final String SPECIAL   = "!@#$%^&*()_+-=[]{}|;:,.<>?";
    private static final String ALL_ALLOWED =
            UPPERCASE + "abcdefghijklmnopqrstuvwxyz" + DIGITS + SPECIAL;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private PasswordSecurityUtils() {}
    public static boolean isValidMasterPassword(String password) {
        if (password == null || password.length() < MIN_MASTER_PASSWORD_LENGTH) return false;

        boolean hasUpper   = false;
        boolean hasDigit   = false;
        boolean hasSpecial = false;

        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if      (Character.isUpperCase(c))   hasUpper   = true;
            else if (Character.isDigit(c))        hasDigit   = true;
            else if (SPECIAL.indexOf(c) >= 0)     hasSpecial = true;
        }

        return hasUpper && hasDigit && hasSpecial;
    }
    public static boolean isNonEmpty(String password) {
        return password != null && !password.isEmpty();
    }
    public static final String ENTRY_VALIDATION_ERROR_MESSAGE =
            "Password field cannot be empty";

    public static String generateStrongPassword(int length) {
        int resultLength = Math.max(length, MIN_MASTER_PASSWORD_LENGTH);
        char[] passwordChars = new char[resultLength];
        passwordChars[0] = randomCharFrom(UPPERCASE);
        passwordChars[1] = randomCharFrom(DIGITS);
        passwordChars[2] = randomCharFrom(SPECIAL);
        for (int i = 3; i < resultLength; i++) {
            passwordChars[i] = randomCharFrom(ALL_ALLOWED);
        }
        shuffle(passwordChars);
        return new String(passwordChars);
    }
    private static char randomCharFrom(String chars) {
        return chars.charAt(SECURE_RANDOM.nextInt(chars.length()));
    }
    private static void shuffle(char[] chars) {
        for (int i = chars.length - 1; i > 0; i--) {
            int j = SECURE_RANDOM.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
    }
}