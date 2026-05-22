package narek.hakobyan.mypassword;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private MasterPasswordManager  masterPasswordManager;
    private SecurityQuestionsManager sqManager;
    private LockManager            lockManager;

    /* Predefined question pool that the user picks from */
    private static final String[] QUESTION_POOL = {
            "Имя вашего первого питомца?",
            "Девичья фамилия матери?",
            "Название школы, в которой вы учились?",
            "Город, в котором вы родились?",
            "Любимая книга в детстве?",
            "Имя вашего лучшего друга в детстве?",
            "Марка вашего первого автомобиля?",
            "Любимое блюдо вашей бабушки?"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        masterPasswordManager = new MasterPasswordManager(this);
        sqManager             = new SecurityQuestionsManager(this);
        lockManager           = new LockManager(this);

        Button open = findViewById(R.id.btnOpen);
        open.setOnClickListener(v -> handleOpenClick());
    }

    /* ──────────────────────────────────────────────────────────────────
       Entry point
       ────────────────────────────────────────────────────────────────── */

    private void handleOpenClick() {
        // 1. Timed lock active?
        if (lockManager.isTimedLockActive()) {
            showTimedLockMessage();
            return;
        }
        // 2. Timed lock just expired → enter final phase
        if (lockManager.isInFinalPhase() == false
                && lockManager.timedLockRemainingMs() == 0
                && lockManager.isLocked()
                && lockManager.getQuestionFails() >= LockManager.MAX_QUESTION_ATTEMPTS) {
            lockManager.onTimedLockExpired();
        }
        // 3. App locked → show security questions
        if (lockManager.isLocked()) {
            if (sqManager.hasSecurityQuestions()) {
                showSecurityQuestionsDialog();
            } else {
                showTimedLockMessage(); // no questions saved → full lock (edge-case)
            }
            return;
        }
        // 4. Normal flow
        if (!masterPasswordManager.hasMasterPassword()) {
            showCreateMasterPasswordDialog();
        } else {
            showUnlockDialog();
        }
    }

    /* ──────────────────────────────────────────────────────────────────
       Create master password (first launch)
       ────────────────────────────────────────────────────────────────── */

    private void showCreateMasterPasswordDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int dp16 = dp(16);
        layout.setPadding(dp16, dp16, dp16, 0);

        EditText passwordInput = makePasswordInput("Создать мастер-пароль");
        EditText confirmInput  = makePasswordInput("Подтвердить мастер-пароль");
        layout.addView(passwordInput);
        layout.addView(confirmInput);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Мастер-пароль")
                .setMessage("Создайте пароль для защиты приложения.\n\n"
                        + "Требования: ≥16 символов, заглавная буква, цифра, спецсимвол.")
                .setView(layout)
                .setCancelable(false)
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Далее", null)
                .create();

        dialog.setOnShowListener(d ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String password = passwordInput.getText().toString().trim();
                    String confirm  = confirmInput.getText().toString().trim();

                    if (!PasswordSecurityUtils.isValidMasterPassword(password)) {
                        passwordInput.setError(PasswordSecurityUtils.MASTER_VALIDATION_ERROR_MESSAGE);
                        return;
                    }
                    if (!TextUtils.equals(password, confirm)) {
                        confirmInput.setError("Пароли не совпадают");
                        confirmInput.requestFocus();
                        return;
                    }
                    try {
                        masterPasswordManager.saveMasterPassword(password);
                    } catch (IllegalArgumentException e) {
                        passwordInput.setError(e.getMessage());
                        return;
                    }
                    dialog.dismiss();
                    showSetupSecurityQuestionsDialog();
                }));

        dialog.show();
    }

    /* ──────────────────────────────────────────────────────────────────
       Setup security questions (shown once after master password creation)
       ────────────────────────────────────────────────────────────────── */

    private void showSetupSecurityQuestionsDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_security_questions_setup, null);

        // 4 question spinners + answer fields wired in the layout via ids:
        // tvQ1..tvQ4 (TextViews showing the question), etA1..etA4 (EditTexts for answers)
        // We assign questions from the pool sequentially (first 4)
        TextView[] tvQ = new TextView[4];
        EditText[] etA = new EditText[4];
        for (int i = 0; i < 4; i++) {
            int tvId = getResources().getIdentifier("tvSecQ" + (i + 1), "id", getPackageName());
            int etId = getResources().getIdentifier("etSecA" + (i + 1), "id", getPackageName());
            tvQ[i] = view.findViewById(tvId);
            etA[i] = view.findViewById(etId);
            tvQ[i].setText((i + 1) + ". " + QUESTION_POOL[i]);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Секретные вопросы")
                .setMessage("Ответьте на 4 вопроса. Они помогут восстановить доступ.\n"
                        + "Ответы нечувствительны к регистру.")
                .setView(view)
                .setCancelable(false)
                .setPositiveButton("Сохранить", null)
                .create();

        dialog.setOnShowListener(d ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String[] answers = new String[4];
                    for (int i = 0; i < 4; i++) {
                        answers[i] = etA[i].getText().toString().trim();
                        if (answers[i].isEmpty()) {
                            etA[i].setError("Введите ответ");
                            etA[i].requestFocus();
                            return;
                        }
                    }
                    String[] questions = new String[4];
                    for (int i = 0; i < 4; i++) questions[i] = QUESTION_POOL[i];

                    sqManager.saveQuestions(questions, answers);
                    lockManager.onSuccessfulUnlock();
                    dialog.dismiss();
                    openPasswordScreen();
                }));

        dialog.show();
    }

    /* ──────────────────────────────────────────────────────────────────
       Unlock dialog (normal)
       ────────────────────────────────────────────────────────────────── */

    private void showUnlockDialog() {
        EditText passwordInput = makePasswordInput("Введите мастер-пароль");
        passwordInput.setPadding(dp(16), dp(16), dp(16), dp(16));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Разблокировать приложение")
                .setView(passwordInput)
                .setCancelable(false)
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Войти", null)
                .create();

        dialog.setOnShowListener(d ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String password = passwordInput.getText().toString().trim();
                    if (masterPasswordManager.verifyMasterPassword(password)) {
                        lockManager.onSuccessfulUnlock();
                        dialog.dismiss();
                        openPasswordScreen();
                    } else {
                        int failed = lockManager.incrementMasterFails();
                        if (failed >= LockManager.MAX_MASTER_ATTEMPTS) {
                            lockManager.lock();
                            dialog.dismiss();
                            Toast.makeText(this,
                                    "Слишком много попыток. Требуется ответ на секретные вопросы.",
                                    Toast.LENGTH_LONG).show();
                            showSecurityQuestionsDialog();
                        } else {
                            int remaining = LockManager.MAX_MASTER_ATTEMPTS - failed;
                            passwordInput.setError("Неверный пароль. Осталось попыток: " + remaining);
                        }
                    }
                }));

        dialog.show();
    }

    /* ──────────────────────────────────────────────────────────────────
       Security-questions unlock dialog
       ────────────────────────────────────────────────────────────────── */

    private void showSecurityQuestionsDialog() {
        // Check timed lock again (edge case: dialog re-opened)
        if (lockManager.isTimedLockActive()) {
            showTimedLockMessage();
            return;
        }

        boolean isFinal = lockManager.isInFinalPhase();
        int maxAttempts = lockManager.maxQuestionAttempts();
        int usedAttempts = lockManager.getQuestionFails();

        View view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_security_questions_verify, null);

        EditText[] etA = new EditText[4];
        for (int i = 0; i < 4; i++) {
            int tvId = getResources().getIdentifier("tvSecQ" + (i + 1), "id", getPackageName());
            int etId = getResources().getIdentifier("etSecA" + (i + 1), "id", getPackageName());
            TextView tvQ = view.findViewById(tvId);
            etA[i] = view.findViewById(etId);
            tvQ.setText((i + 1) + ". " + sqManager.getQuestion(i));
        }

        String title   = isFinal ? "⚠️ Последний шанс" : "Секретные вопросы";
        String message = isFinal
                ? "Осталось попыток: " + (maxAttempts - usedAttempts)
                + "\nПосле их исчерпания все данные будут УДАЛЕНЫ."
                : "Введите ответы на все 4 вопроса.\n"
                + "Осталось попыток: " + (maxAttempts - usedAttempts);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setView(view)
                .setCancelable(false)
                .setPositiveButton("Проверить", null)
                .create();

        dialog.setOnShowListener(d ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String[] answers = new String[4];
                    for (int i = 0; i < 4; i++) {
                        answers[i] = etA[i].getText().toString().trim();
                        if (answers[i].isEmpty()) {
                            etA[i].setError("Введите ответ");
                            etA[i].requestFocus();
                            return;
                        }
                    }

                    if (sqManager.verifyAllAnswers(answers)) {
                        lockManager.onSuccessfulUnlock();
                        dialog.dismiss();
                        openPasswordScreen();
                    } else {
                        int fails    = lockManager.incrementQuestionFails();
                        int maxAtts  = lockManager.maxQuestionAttempts();
                        int remaining = maxAtts - fails;

                        if (fails >= maxAtts) {
                            dialog.dismiss();
                            if (lockManager.isInFinalPhase()) {
                                // WIPE
                                performEmergencyWipe();
                            } else {
                                // Start 24-hour timed lock
                                lockManager.startTimedLock();
                                showTimedLockMessage();
                            }
                        } else {
                            // Update message
                            TextView tvMsg = dialog.findViewById(android.R.id.message);
                            if (tvMsg != null) {
                                tvMsg.setText("Неверный ответ. Осталось попыток: " + remaining);
                            }
                            Toast.makeText(this,
                                    "Неверно. Осталось попыток: " + remaining,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }));

        dialog.show();
    }

    /* ──────────────────────────────────────────────────────────────────
       Timed-lock screen
       ────────────────────────────────────────────────────────────────── */

    private void showTimedLockMessage() {
        long ms      = lockManager.timedLockRemainingMs();
        long minutes = TimeUnit.MILLISECONDS.toMinutes(ms);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60;

        String timeLeft = String.format(Locale.getDefault(), "%d мин %02d сек", minutes, seconds);

        new AlertDialog.Builder(this)
                .setTitle("Приложение заблокировано")
                .setMessage("Вы исчерпали все попытки ввода секретных ответов.\n\n"
                        + "Следующая попытка доступна через: " + timeLeft + "\n\n"
                        + "⚠️ После разблокировки у вас будет ещё 5 попыток. "
                        + "Если они будут неверными — все данные будут удалены без возможности восстановления.")
                .setPositiveButton("ОК", null)
                .setCancelable(false)
                .show();
    }

    /* ──────────────────────────────────────────────────────────────────
       Emergency wipe
       ────────────────────────────────────────────────────────────────── */

    private void performEmergencyWipe() {
        deleteDatabase("passwords.db");
        clearAllSharedPreferences();
        new CryptoManager().resetKeyMaterial();
        sqManager.clear();
        Toast.makeText(this, "Все данные удалены после исчерпания попыток.", Toast.LENGTH_LONG).show();
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

    /* ──────────────────────────────────────────────────────────────────
       Helpers
       ────────────────────────────────────────────────────────────────── */

    private void openPasswordScreen() {
        startActivity(new Intent(this, main_displey.class));
    }

    private EditText makePasswordInput(String hint) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        return et;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}