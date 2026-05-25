package narek.hakobyan.mypassword;

import java.security.SecureRandom;

public class AutoPasswordChangeManager {
    public interface SitePasswordChanger {
        boolean changePassword(String websiteUrl, String login, String oldPassword, String newPassword) throws Exception;
    }

    private final DatabaseHelper dbHelper;

    public AutoPasswordChangeManager(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public String rotatePassword(int passwordId, SitePasswordChanger changer) throws Exception {
        DatabaseHelper.PasswordEntry entry = dbHelper.getPasswordById(passwordId);
        if (entry == null) throw new IllegalArgumentException("Entry not found");

        String newPassword = generateStrongPassword(20);
        boolean changed = changer.changePassword(entry.websiteUrl, entry.login, entry.password, newPassword);
        if (!changed) throw new IllegalStateException("Remote password change failed");

        dbHelper.updatePassword(entry.id, entry.site, entry.login, newPassword, entry.websiteUrl);
        return newPassword;
    }

    private String generateStrongPassword(int length) {
        final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%^&*";
        StringBuilder out = new StringBuilder();
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < length; i++) out.append(alphabet.charAt(random.nextInt(alphabet.length())));
        return out.toString();
    }
}
