package narek.hakobyan.mypassword;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class PasswordDetailActivity extends AppCompatActivity {

    DatabaseHelper dbHelper;
    int entryId;
    DatabaseHelper.PasswordEntry entry;

    TextView tvSite, tvLogin, tvPassword;
    Button btnShowPassword, btnEdit, btnDelete;

    boolean passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_detail);

        dbHelper = new DatabaseHelper(this);
        entryId = getIntent().getIntExtra("id", -1);

        tvSite = findViewById(R.id.tvSite);
        tvLogin = findViewById(R.id.tvLogin);
        tvPassword = findViewById(R.id.tvPassword);
        btnShowPassword = findViewById(R.id.btnShowPassword);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);

        loadEntry();

        btnShowPassword.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            if (passwordVisible) {
                tvPassword.setText(entry.password);
                btnShowPassword.setText("Hide password");
            } else {
                tvPassword.setText("••••••••");
                btnShowPassword.setText("Show password");
            }
        });

        btnEdit.setOnClickListener(v -> showEditDialog());

        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete")
                    .setMessage("Delete entry for " + entry.site + "?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        dbHelper.deletePassword(entryId);
                        Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void loadEntry() {
        entry = dbHelper.getPasswordById(entryId);
        if (entry != null) {
            tvSite.setText(entry.site);
            tvLogin.setText(entry.login);
            tvPassword.setText("••••••••");
            passwordVisible = false;
            btnShowPassword.setText("Show password");
        }
    }

    private void showEditDialog() {
        View redactor = getLayoutInflater().inflate(R.layout.redactor, null);
        EditText etSite = redactor.findViewById(R.id.etSite);
        EditText etLogin = redactor.findViewById(R.id.etLogin);
        EditText etPassword = redactor.findViewById(R.id.etPassword);

        etSite.setText(entry.site);
        etLogin.setText(entry.login);
        etPassword.setText(entry.password);

        new AlertDialog.Builder(this)
                .setTitle("Edit")
                .setView(redactor)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newSite = etSite.getText().toString().trim();
                    String newLogin = etLogin.getText().toString().trim();
                    String newPassword = etPassword.getText().toString().trim();

                    if (newSite.isEmpty()) {
                        Toast.makeText(this, "Enter site", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!PasswordSecurityUtils.isValidPassword(newPassword)) {
                        Toast.makeText(this, PasswordSecurityUtils.VALIDATION_ERROR_MESSAGE, Toast.LENGTH_LONG).show();
                        return;
                    }

                    dbHelper.updatePassword(entryId, newSite, newLogin, newPassword);
                    Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
                    loadEntry();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
