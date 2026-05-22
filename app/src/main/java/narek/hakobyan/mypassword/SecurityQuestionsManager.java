package narek.hakobyan.mypassword;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages 4 security questions and their encrypted answers.
 * All answers are normalised to lower-case before storing/comparing.
 */
public class SecurityQuestionsManager {

    private static final String PREFS_NAME   = "security_questions_prefs";
    private static final String KEY_Q_PREFIX = "question_";   // question_0 … question_3
    private static final String KEY_A_PREFIX = "answer_";     // answer_0   … answer_3
    private static final int    QUESTION_COUNT = 4;

    private final SharedPreferences prefs;
    private final CryptoManager     crypto;

    public SecurityQuestionsManager(Context context) {
        prefs  = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        crypto = new CryptoManager();
    }

    /** Returns true if all 4 questions have been saved. */
    public boolean hasSecurityQuestions() {
        for (int i = 0; i < QUESTION_COUNT; i++) {
            if (!prefs.contains(KEY_A_PREFIX + i)) return false;
        }
        return true;
    }

    /**
     * Saves all 4 questions and their answers (answers are lower-cased then encrypted).
     *
     * @param questions array of exactly 4 question strings
     * @param answers   array of exactly 4 answer strings
     */
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

    /** Returns the stored question text for index 0-3. */
    public String getQuestion(int index) {
        return prefs.getString(KEY_Q_PREFIX + index, "");
    }

    /**
     * Verifies all 4 answers at once.
     *
     * @param answers array of exactly 4 answers provided by the user
     * @return true only if every answer matches
     */
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

    /** Clears all stored questions and answers (used during emergency wipe). */
    public void clear() {
        prefs.edit().clear().apply();
    }
}