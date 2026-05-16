package narek.hakobyan.mypassword;

import android.content.Context;
import android.content.SharedPreferences;

public class MasterPasswordManager {

    private static final String PREFS_NAME = "master_password_prefs";
    private static final String KEY_PASSWORD = "master_password_value";

    private final SharedPreferences preferences;

    public MasterPasswordManager(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean hasMasterPassword() {
        String saved = preferences.getString(KEY_PASSWORD, null);
        return saved != null && !saved.isEmpty();
    }

    /**
     * Persists the master password after strict validation.
     * Throws {@link IllegalArgumentException} if the password does not meet policy.
     */
    public void saveMasterPassword(String password) {
        if (!PasswordSecurityUtils.isValidMasterPassword(password)) {
            throw new IllegalArgumentException(
                    PasswordSecurityUtils.MASTER_VALIDATION_ERROR_MESSAGE);
        }
        preferences.edit().putString(KEY_PASSWORD, password).apply();
    }

    public boolean verifyMasterPassword(String password) {
        String saved = preferences.getString(KEY_PASSWORD, null);
        return saved != null && saved.equals(password);
    }
}