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
        String savedPassword = preferences.getString(KEY_PASSWORD, null);
        return savedPassword != null && !savedPassword.isEmpty();
    }

    public void saveMasterPassword(String password) {
        if (!PasswordSecurityUtils.isValidPassword(password)) {
            throw new IllegalArgumentException(PasswordSecurityUtils.VALIDATION_ERROR_MESSAGE);
        }
        preferences.edit()
                .putString(KEY_PASSWORD, password)
                .apply();
    }

    public boolean verifyMasterPassword(String password) {
        String savedPassword = preferences.getString(KEY_PASSWORD, null);
        return savedPassword != null && savedPassword.equals(password);
    }
}
