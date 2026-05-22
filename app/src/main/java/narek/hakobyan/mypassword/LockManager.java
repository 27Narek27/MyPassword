package narek.hakobyan.mypassword;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Tracks login-failure state and lock-out logic:
 *
 *  Phase 1 – master password attempts
 *    Up to MAX_MASTER_ATTEMPTS (10) wrong passwords.
 *    On reaching the limit → app locks, asks security questions.
 *
 *  Phase 2 – security-question attempts
 *    Up to MAX_QUESTION_ATTEMPTS (10) wrong answers.
 *    On reaching the limit → TIMED LOCK for LOCK_DURATION_MS (24 h).
 *    After the timed lock expires → 5 more attempts (FINAL_ATTEMPTS).
 *    If those 5 are exhausted → WIPE.
 */
public class LockManager {

    /* ── constants ─────────────────────────────────────────────────── */
    public static final int  MAX_MASTER_ATTEMPTS  = 10;
    public static final int  MAX_QUESTION_ATTEMPTS = 10;
    public static final int  FINAL_ATTEMPTS        = 5;
    public static final long LOCK_DURATION_MS      = 5L * 60 * 1000; // 5 минут

    /* ── prefs keys ─────────────────────────────────────────────────── */
    private static final String PREFS              = "lock_manager_prefs";
    private static final String KEY_MASTER_FAILS   = "master_failed";
    private static final String KEY_QUESTION_FAILS = "question_failed";
    private static final String KEY_LOCKED          = "is_locked";          // awaiting security Qs
    private static final String KEY_TIMED_LOCK_UNTIL = "timed_lock_until";  // epoch ms, 0 = none
    private static final String KEY_IN_FINAL_PHASE  = "in_final_phase";

    private final SharedPreferences prefs;

    public LockManager(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /* ════════════════════════════════════════════════════════════════
       Master-password phase
       ════════════════════════════════════════════════════════════════ */

    public int getMasterFails()  { return prefs.getInt(KEY_MASTER_FAILS, 0); }

    /**
     * Call when the master password is wrong.
     * @return new fail count
     */
    public int incrementMasterFails() {
        int n = getMasterFails() + 1;
        prefs.edit().putInt(KEY_MASTER_FAILS, n).apply();
        return n;
    }

    public void resetMasterFails() {
        prefs.edit().putInt(KEY_MASTER_FAILS, 0).apply();
    }

    /** Call when the master password is correct – resets everything. */
    public void onSuccessfulUnlock() {
        prefs.edit()
                .putInt(KEY_MASTER_FAILS,    0)
                .putInt(KEY_QUESTION_FAILS,  0)
                .putBoolean(KEY_LOCKED,       false)
                .putLong(KEY_TIMED_LOCK_UNTIL, 0L)
                .putBoolean(KEY_IN_FINAL_PHASE, false)
                .apply();
    }

    /* ════════════════════════════════════════════════════════════════
       Security-questions phase
       ════════════════════════════════════════════════════════════════ */

    /** True when master-password limit was hit and the app is locked. */
    public boolean isLocked() { return prefs.getBoolean(KEY_LOCKED, false); }

    /** Lock the app (transition to security-questions phase). */
    public void lock() {
        prefs.edit()
                .putBoolean(KEY_LOCKED, true)
                .putInt(KEY_QUESTION_FAILS, 0)
                .apply();
    }

    /* ── timed lock ─────────────────────────────────────────────── */

    /** True while the 24-hour timed lock is active. */
    public boolean isTimedLockActive() {
        long until = prefs.getLong(KEY_TIMED_LOCK_UNTIL, 0L);
        return until > 0 && System.currentTimeMillis() < until;
    }

    /** Milliseconds remaining in the timed lock (0 if not active). */
    public long timedLockRemainingMs() {
        long until = prefs.getLong(KEY_TIMED_LOCK_UNTIL, 0L);
        long remaining = until - System.currentTimeMillis();
        return Math.max(remaining, 0L);
    }

    /** Start the 24-hour timed lock and switch to final phase. */
    public void startTimedLock() {
        prefs.edit()
                .putLong(KEY_TIMED_LOCK_UNTIL, System.currentTimeMillis() + LOCK_DURATION_MS)
                .putBoolean(KEY_IN_FINAL_PHASE, false)  // final phase begins AFTER timed lock
                .putInt(KEY_QUESTION_FAILS, 0)
                .apply();
    }

    /** Called when the timed lock has expired – enables the final 5-attempt phase. */
    public void onTimedLockExpired() {
        prefs.edit()
                .putLong(KEY_TIMED_LOCK_UNTIL, 0L)
                .putBoolean(KEY_IN_FINAL_PHASE, true)
                .putInt(KEY_QUESTION_FAILS, 0)
                .apply();
    }

    public boolean isInFinalPhase() { return prefs.getBoolean(KEY_IN_FINAL_PHASE, false); }

    /* ── question-fail counter ──────────────────────────────────── */

    public int getQuestionFails() { return prefs.getInt(KEY_QUESTION_FAILS, 0); }

    /** @return new fail count */
    public int incrementQuestionFails() {
        int n = getQuestionFails() + 1;
        prefs.edit().putInt(KEY_QUESTION_FAILS, n).apply();
        return n;
    }

    public void resetQuestionFails() {
        prefs.edit().putInt(KEY_QUESTION_FAILS, 0).apply();
    }

    /** How many question attempts are allowed in the current phase. */
    public int maxQuestionAttempts() {
        return isInFinalPhase() ? FINAL_ATTEMPTS : MAX_QUESTION_ATTEMPTS;
    }
}