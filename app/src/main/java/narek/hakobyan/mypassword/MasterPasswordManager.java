package narek.hakobyan.mypassword;

import android.content.Context;
import android.content.SharedPreferences;

public class MasterPasswordManager {

    private static final String PREFS_NAME  = "master_password_prefs";
    private static final String KEY_HASH    = "master_password_hash";
    private static final String KEY_LEGACY  = "master_password_value";

    private final SharedPreferences  prefs;
    private final PasswordHashManager hasher;

    public MasterPasswordManager(Context context) {
        prefs  = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        hasher = new PasswordHashManager();
        migrateLegacyIfNeeded();
    }


    public boolean hasMasterPassword() {
        return prefs.contains(KEY_HASH);
    }

    public void saveMasterPassword(String password) {
        if (!PasswordSecurityUtils.isValidMasterPassword(password)) {
            throw new IllegalArgumentException(
                    PasswordSecurityUtils.MASTER_VALIDATION_ERROR_MESSAGE);
        }
        String hash = hasher.hashPassword(password);
        prefs.edit()
                .putString(KEY_HASH, hash)
                .remove(KEY_LEGACY)
                .apply();
    }

    public boolean verifyMasterPassword(String password) {
        String storedHash = prefs.getString(KEY_HASH, null);
        if (storedHash == null) return false;
        return hasher.verifyPassword(password, storedHash);
    }


    private void migrateLegacyIfNeeded() {
        if (prefs.contains(KEY_HASH)) return;
        String legacy = prefs.getString(KEY_LEGACY, null);
        if (legacy == null || legacy.isEmpty()) return;

        String hash = hasher.hashPassword(legacy);
        prefs.edit()
                .putString(KEY_HASH, hash)
                .remove(KEY_LEGACY)
                .apply();
    }
}