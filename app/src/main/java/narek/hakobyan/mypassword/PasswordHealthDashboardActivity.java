package narek.hakobyan.mypassword;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class PasswordHealthDashboardActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;

import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class PasswordHealthDashboardActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SecureScreenUtils.apply(this);
        setContentView(R.layout.activity_password_health_dashboard);

        dbHelper = new DatabaseHelper(this);

        TextView tvWeak = findViewById(R.id.tvWeak);
        TextView tvDuplicates = findViewById(R.id.tvDuplicates);
        TextView tvStale = findViewById(R.id.tvStale);
        LinearLayout cardWeak = findViewById(R.id.cardWeak);
        LinearLayout cardDuplicates = findViewById(R.id.cardDuplicates);
        LinearLayout cardStale = findViewById(R.id.cardStale);

        DatabaseHelper.PasswordHealthStats stats = dbHelper.getHealthStats();
        tvWeak.setText(String.valueOf(stats.weakPasswords));
        tvDuplicates.setText(String.valueOf(stats.duplicatePasswords));
        tvStale.setText(String.valueOf(stats.stalePasswords));

        cardWeak.setOnClickListener(v -> showItemsDialog("Слабые пароли", collectWeakPasswords()));
        cardDuplicates.setOnClickListener(v -> showItemsDialog("Дубликаты паролей", collectDuplicatePasswords()));
        cardStale.setOnClickListener(v -> showItemsDialog("Давно не менялись", collectStalePasswords()));
    }

    private ArrayList<String> collectWeakPasswords() {
        ArrayList<String> result = new ArrayList<>();
        for (DatabaseHelper.PasswordEntry e : dbHelper.getAllPasswords()) {
            int score = PasswordSecurityUtils.calculateStrengthScore(e.password);
            if (score < 50) {
                result.add(e.site + " • " + e.login + " (" + score + "%)");
            }
        }
        return result;
    }

    private ArrayList<String> collectDuplicatePasswords() {
        ArrayList<DatabaseHelper.PasswordEntry> entries = dbHelper.getAllPasswords();
        HashMap<String, ArrayList<DatabaseHelper.PasswordEntry>> grouped = new HashMap<>();

        for (DatabaseHelper.PasswordEntry e : entries) {
            grouped.computeIfAbsent(e.password, k -> new ArrayList<>()).add(e);
        }

        ArrayList<String> result = new ArrayList<>();
        for (ArrayList<DatabaseHelper.PasswordEntry> group : grouped.values()) {
            if (group.size() > 1) {
                StringBuilder line = new StringBuilder("Одинаковый пароль: ");
                for (int i = 0; i < group.size(); i++) {
                    DatabaseHelper.PasswordEntry entry = group.get(i);
                    line.append(entry.site).append(" (").append(entry.login).append(")");
                    if (i < group.size() - 1) line.append(", ");
                }
                result.add(line.toString());
            }
        }
        return result;
    }

    private ArrayList<String> collectStalePasswords() {
        ArrayList<String> result = new ArrayList<>();
        long now = System.currentTimeMillis();
        long ninetyDays = 90L * 24 * 60 * 60 * 1000;
        SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

        for (DatabaseHelper.PasswordEntry e : dbHelper.getAllPasswords()) {
            if (now - e.lastChangedAt > ninetyDays) {
                String dateText = e.lastChangedAt > 0 ? df.format(new Date(e.lastChangedAt)) : "неизвестно";
                result.add(e.site + " • " + e.login + " (посл. смена: " + dateText + ")");
            }
        }
        return result;
    }

    private void showItemsDialog(String title, ArrayList<String> items) {
        if (items.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage("Ничего не найдено")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        String[] arr = items.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setItems(arr, null)
                .setPositiveButton("Закрыть", null)
                .show();
    }
}
