package narek.hakobyan.mypassword;

import android.content.Context;
import android.content.SharedPreferences;

public class SecurityQuestionsManager {

    private static final String PREFS_NAME   = "security_questions_prefs";
    private static final String KEY_Q_PREFIX = "question_";
    private static final String KEY_A_PREFIX = "answer_";
    private static final int    QUESTION_COUNT = 4;

    private final SharedPreferences prefs;
    private final CryptoManager     crypto;

    public SecurityQuestionsManager(Context context) {
        prefs  = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        crypto = new CryptoManager();
    }

    public boolean hasSecurityQuestions() {
        for (int i = 0; i < QUESTION_COUNT; i++) {
            if (!prefs.contains(KEY_A_PREFIX + i)) return false;
        }
        return true;
    }

    public void saveQuestions(String[] questions, String[] answers) {
        if (questions.length != QUESTION_COUNT || answers.length != QUESTION_COUNT) {
            throw new IllegalArgumentException("Exactly 4 questions and answers required");
        }
        SharedPreferences.Editor editor = prefs.edit();
        for (int i = 0; i < QUESTION_COUNT; i++) {
            String normAnswer = answers[i].trim().toLowerCase();
            editor.putString(KEY_Q_PREFIX + i, questions[i]);
            editor.putString(KEY_A_PREFIX + i, crypto.encrypt(normAnswer));
        }
        editor.apply();
    }

    public String getQuestion(int index) {
        return prefs.getString(KEY_Q_PREFIX + index, "");
    }

    public boolean verifyAllAnswers(String[] answers) {
        if (answers.length != QUESTION_COUNT) return false;
        for (int i = 0; i < QUESTION_COUNT; i++) {
            String stored    = prefs.getString(KEY_A_PREFIX + i, null);
            if (stored == null) return false;
            String decrypted = crypto.decrypt(stored).trim().toLowerCase();
            String provided  = answers[i].trim().toLowerCase();
            if (!decrypted.equals(provided)) return false;
        }
        return true;
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}