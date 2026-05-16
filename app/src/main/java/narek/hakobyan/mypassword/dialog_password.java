package narek.hakobyan.mypassword;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class dialog_password extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog_password);

        TextInputEditText etSite       = findViewById(R.id.etSite);
        TextInputEditText etWebsiteUrl = findViewById(R.id.etWebsiteUrl);
        TextInputEditText etLogin      = findViewById(R.id.etLogin);
        TextInputEditText etPassword   = findViewById(R.id.etPassword);
        MaterialButton    btnGenerate  = findViewById(R.id.btnGeneratePassword);
        MaterialButton    btnSave      = findViewById(R.id.btnSave);

        btnGenerate.setOnClickListener(v ->
                etPassword.setText(PasswordSecurityUtils.generateStrongPassword(20)));

        btnSave.setOnClickListener(v -> {
            String site       = text(etSite);
            String websiteUrl = text(etWebsiteUrl);   // NEW
            String login      = text(etLogin);
            String password   = etPassword.getText() != null
                    ? etPassword.getText().toString() : "";
            if (TextUtils.isEmpty(site)) {
                etSite.setError("Enter a title or site name");
                etSite.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(login)) {
                etLogin.setError("Enter a login / username");
                etLogin.requestFocus();
                return;
            }
            if (!PasswordSecurityUtils.isNonEmpty(password)) {
                etPassword.setError(PasswordSecurityUtils.ENTRY_VALIDATION_ERROR_MESSAGE);
                etPassword.requestFocus();
                return;
            }
            try {
                DatabaseHelper db = new DatabaseHelper(this);
                db.insertPassword(site, login, password, websiteUrl);
            } catch (IllegalArgumentException e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }

            finish();
        });
    }

    private String text(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}