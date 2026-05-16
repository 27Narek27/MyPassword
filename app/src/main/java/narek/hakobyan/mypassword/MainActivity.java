package narek.hakobyan.mypassword;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String LOGIN_SECURITY_PREFS  = "login_security_prefs";
    private static final String KEY_FAILED_ATTEMPTS   = "failed_master_password_attempts";
    private static final int    MAX_FAILED_ATTEMPTS   = 10;

    private MasterPasswordManager masterPasswordManager;
    private SharedPreferences     loginSecurityPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        masterPasswordManager    = new MasterPasswordManager(this);
        loginSecurityPreferences = getSharedPreferences(LOGIN_SECURITY_PREFS, MODE_PRIVATE);

        Button open = findViewById(R.id.btnOpen);
        open.setOnClickListener(v -> {
            if (masterPasswordManager.hasMasterPassword()) {
                showUnlockDialog();
            } else {
                showCreateMasterPasswordDialog();
            }
        });
    }


    private void showCreateMasterPasswordDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int dp16 = dp(16);
        layout.setPadding(dp16, dp16, dp16, 0);

        EditText passwordInput = makePasswordInput("Create master password");
        EditText confirmInput  = makePasswordInput("Confirm master password");
        layout.addView(passwordInput);
        layout.addView(confirmInput);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Master password")
                .setMessage("Create a password to protect the app.\n\n"
                        + "Requirements: ≥16 characters, uppercase, digit, special character.")
                .setView(layout)
                .setCancelable(false)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save",   null)
                .create();

        dialog.setOnShowListener(d ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String password = passwordInput.getText().toString().trim();
                    String confirm  = confirmInput.getText().toString().trim();

                    // Strict validation for the MASTER password only
                    if (!PasswordSecurityUtils.isValidMasterPassword(password)) {
                        passwordInput.setError(
                                PasswordSecurityUtils.MASTER_VALIDATION_ERROR_MESSAGE);
                        return;
                    }
                    if (!TextUtils.equals(password, confirm)) {
                        confirmInput.setError("Passwords do not match");
                        confirmInput.requestFocus();
                        return;
                    }

                    try {
                        masterPasswordManager.saveMasterPassword(password);
                    } catch (IllegalArgumentException e) {
                        passwordInput.setError(e.getMessage());
                        return;
                    }

                    resetFailedAttempts();
                    dialog.dismiss();
                    openPasswordScreen();
                }));

        dialog.show();
    }
    private void showUnlockDialog() {
        EditText passwordInput = makePasswordInput("Enter master password");
        int dp16 = dp(16);
        passwordInput.setPadding(dp16, dp16, dp16, dp16);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Unlock app")
                .setView(passwordInput)
                .setCancelable(false)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Unlock", null)
                .create();
        dialog.setOnShowListener(d ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String password = passwordInput.getText().toString().trim();
                    if (masterPasswordManager.verifyMasterPassword(password)) {
                        resetFailedAttempts();
                        dialog.dismiss();
                        openPasswordScreen();
                    } else {
                        int failed = incrementFailedAttempts();
                        if (failed >= MAX_FAILED_ATTEMPTS) {
                            performEmergencyWipe();
                            dialog.dismiss();
                            return;
                        }
                        passwordInput.setError("Wrong password (" + failed
                                + "/" + MAX_FAILED_ATTEMPTS + " attempts)");
                    }
                }));

        dialog.show();
    }
    private void openPasswordScreen() {
        startActivity(new Intent(this, main_displey.class));
    }


    private int incrementFailedAttempts() {
        int attempts = loginSecurityPreferences.getInt(KEY_FAILED_ATTEMPTS, 0) + 1;
        loginSecurityPreferences.edit().putInt(KEY_FAILED_ATTEMPTS, attempts).apply();
        return attempts;
    }

    private void resetFailedAttempts() {
        loginSecurityPreferences.edit().putInt(KEY_FAILED_ATTEMPTS, 0).apply();
    }


    private void performEmergencyWipe() {
        deleteDatabase("passwords.db");
        clearAllSharedPreferences();
        new CryptoManager().resetKeyMaterial();
        resetFailedAttempts();
        Toast.makeText(this,
                "Data deleted after " + MAX_FAILED_ATTEMPTS + " failed login attempts",
                Toast.LENGTH_LONG).show();
        recreate();
    }
    private void clearAllSharedPreferences() {
        java.io.File dir   = new java.io.File(getApplicationInfo().dataDir, "shared_prefs");
        java.io.File[] files = dir.listFiles();
        if (files == null) return;
        for (java.io.File f : files) {
            String name = f.getName();
            if (!name.endsWith(".xml")) continue;
            getSharedPreferences(name.substring(0, name.length() - 4), MODE_PRIVATE)
                    .edit().clear().apply();
        }
    }
    private EditText makePasswordInput(String hint) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        return et;
    }
    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}