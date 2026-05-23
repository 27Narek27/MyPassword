package narek.hakobyan.mypassword;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class PasswordDetailActivity extends AppCompatActivity {

    DatabaseHelper              dbHelper;
    int                         entryId;
    DatabaseHelper.PasswordEntry entry;

    TextView       tvSite, tvLogin, tvPassword, tvWebsiteUrl;
    MaterialButton btnShowPassword, btnEdit, btnDelete, btnAutoLogin;

    boolean passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SecureScreenUtils.apply(this);
        setContentView(R.layout.activity_password_detail);

        dbHelper = new DatabaseHelper(this);
        entryId  = getIntent().getIntExtra("id", -1);

        tvSite         = findViewById(R.id.tvSite);
        tvWebsiteUrl   = findViewById(R.id.tvWebsiteUrl);   // NEW
        tvLogin        = findViewById(R.id.tvLogin);
        tvPassword     = findViewById(R.id.tvPassword);
        btnShowPassword = findViewById(R.id.btnShowPassword);
        btnEdit        = findViewById(R.id.btnEdit);
        btnDelete      = findViewById(R.id.btnDelete);
        btnAutoLogin   = findViewById(R.id.btnAutoLogin);   // NEW

        loadEntry();
        bindButtons();
    }
    private void loadEntry() {
        entry = dbHelper.getPasswordById(entryId);
        if (entry == null) {
            Toast.makeText(this, "Запись не найдена", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvSite.setText(entry.site);
        tvLogin.setText(entry.login);
        tvPassword.setText("••••••••");
        passwordVisible = false;
        btnShowPassword.setText("Показать пароль");

        boolean hasUrl = !TextUtils.isEmpty(entry.websiteUrl);
        if (hasUrl) {
            tvWebsiteUrl.setText(entry.websiteUrl);

            tvWebsiteUrl.setPaintFlags(
                    tvWebsiteUrl.getPaintFlags()
                            | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
            tvWebsiteUrl.setOnClickListener(v ->
                    Webviewautologinactivity.launch(
                            this, entry.websiteUrl, entry.login, entry.password));
            btnAutoLogin.setVisibility(View.VISIBLE);
        } else {
            tvWebsiteUrl.setText("—");
            tvWebsiteUrl.setOnClickListener(null);
            btnAutoLogin.setVisibility(View.GONE);
        }
    }

    private void bindButtons() {
        btnShowPassword.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            tvPassword.setText(passwordVisible ? entry.password : "••••••••");
            btnShowPassword.setText(passwordVisible ? "Скрыть пароль" : "Показать пароль");
        });
        btnAutoLogin.setOnClickListener(v -> {
            if (TextUtils.isEmpty(entry.websiteUrl)) {
                Toast.makeText(this, "Для этой записи не сохранён URL",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            Webviewautologinactivity.launch(
                    this, entry.websiteUrl, entry.login, entry.password);
        });

        btnEdit.setOnClickListener(v -> showEditDialog());
        btnDelete.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Удалить запись")
                        .setMessage("Удалить запись для \"" + entry.site + "\"?")
                        .setPositiveButton("Удалить", (dialog, which) -> {
                            dbHelper.deletePassword(entryId);
                            Toast.makeText(this, "Удалено", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .setNegativeButton("Отмена", null)
                        .show());
    }

    private void showEditDialog() {
        View redactor   = getLayoutInflater().inflate(R.layout.redactor, null);
        EditText etSite = redactor.findViewById(R.id.etSite);
        EditText etUrl  = redactor.findViewById(R.id.etWebsiteUrl);
        EditText etLogin = redactor.findViewById(R.id.etLogin);
        EditText etPass  = redactor.findViewById(R.id.etPassword);

        etSite.setText(entry.site);
        etUrl.setText(entry.websiteUrl);
        etLogin.setText(entry.login);
        etPass.setText(entry.password);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Редактировать запись")
                .setView(redactor)
                .setPositiveButton("Сохранить",   null)
                .setNegativeButton("Отмена", null)
                .create();

        dialog.setOnShowListener(d ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {

                    String newSite  = etSite.getText().toString().trim();
                    String newUrl   = etUrl.getText().toString().trim();
                    String newLogin = etLogin.getText().toString().trim();
                    String newPass  = etPass.getText().toString();

                    if (TextUtils.isEmpty(newSite)) {
                        etSite.setError("Введите название или сайт");
                        etSite.requestFocus();
                        return;
                    }
                    if (TextUtils.isEmpty(newLogin)) {
                        etLogin.setError("Введите логин / имя пользователя");
                        etLogin.requestFocus();
                        return;
                    }
                    if (!PasswordSecurityUtils.isNonEmpty(newPass)) {
                        etPass.setError(PasswordSecurityUtils.ENTRY_VALIDATION_ERROR_MESSAGE);
                        etPass.requestFocus();
                        return;
                    }
                    try {
                        dbHelper.updatePassword(entryId, newSite, newLogin, newPass, newUrl);
                    } catch (IllegalArgumentException e) {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadEntry();
                }));

        dialog.show();
    }
}