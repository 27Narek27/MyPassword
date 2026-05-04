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

    private static final String LOGIN_SECURITY_PREFS = "login_security_prefs";
    private static final String KEY_FAILED_ATTEMPTS = "failed_master_password_attempts";
    private static final int MAX_FAILED_ATTEMPTS = 10;

    private MasterPasswordManager masterPasswordManager;
    private SharedPreferences loginSecurityPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        masterPasswordManager = new MasterPasswordManager(this);
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
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, 0);

        EditText passwordInput = new EditText(this);
        passwordInput.setHint("Create master password");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        EditText confirmInput = new EditText(this);
        confirmInput.setHint("Confirm master password");
        confirmInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        layout.addView(passwordInput);
        layout.addView(confirmInput);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Master password")
                .setMessage("Create password to protect the app")
                .setView(layout)
                .setCancelable(false)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String password = passwordInput.getText().toString().trim();
            String confirm = confirmInput.getText().toString().trim();

            if (!PasswordSecurityUtils.isValidPassword(password)) {
                Toast.makeText(this, PasswordSecurityUtils.VALIDATION_ERROR_MESSAGE, Toast.LENGTH_LONG).show();
            }

            if (!TextUtils.equals(password, confirm)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
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
        EditText passwordInput = new EditText(this);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        passwordInput.setPadding(padding, padding, padding, padding);
        passwordInput.setHint("Enter master password");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Unlock app")
                .setView(passwordInput)
                .setCancelable(false)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Unlock", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String password = passwordInput.getText().toString().trim();
            if (masterPasswordManager.verifyMasterPassword(password)) {
                resetFailedAttempts();
                dialog.dismiss();
                openPasswordScreen();
            } else {
                int failedAttempts = incrementFailedAttempts();
                if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                    performEmergencyWipe();
                    dialog.dismiss();
                    return;
                }
                Toast.makeText(this, "Wrong password", Toast.LENGTH_SHORT).show();
            }
        }));

        dialog.show();
    }

    private void openPasswordScreen() {
        Intent intent = new Intent(MainActivity.this, main_displey.class);
        startActivity(intent);
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
        // ВНИМАНИЕ: безвозвратно удаляем локальную БД с сохранёнными паролями.
        deleteDatabase("passwords.db");

        clearAllSharedPreferences();

        // ВНИМАНИЕ: удаляем ключ AES-GCM из Keystore — расшифровать старые данные больше нельзя.
        new CryptoManager().resetKeyMaterial();

        resetFailedAttempts();
        Toast.makeText(this, "Data deleted after 10 failed login attempts", Toast.LENGTH_LONG).show();
        recreate();
    }

    private void clearAllSharedPreferences() {
        java.io.File sharedPrefsDir = new java.io.File(getApplicationInfo().dataDir, "shared_prefs");
        java.io.File[] prefFiles = sharedPrefsDir.listFiles();
        if (prefFiles == null) {
            return;
        }

        for (java.io.File prefFile : prefFiles) {
            String fileName = prefFile.getName();
            if (!fileName.endsWith(".xml")) {
                continue;
            }
            String prefName = fileName.substring(0, fileName.length() - 4);
            getSharedPreferences(prefName, MODE_PRIVATE).edit().clear().apply();
        }
    }
}
