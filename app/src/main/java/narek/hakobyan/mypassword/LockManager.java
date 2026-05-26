package narek.hakobyan.mypassword;

import android.content.Context;
import android.content.SharedPreferences;

public class LockManager {

    public static final int  MAX_MASTER_ATTEMPTS   = 10;
    public static final int  MAX_QUESTION_ATTEMPTS = 10;
    public static final int  FINAL_ATTEMPTS        = 5;
    public static final long LOCK_DURATION_MS      = 24L * 60 * 60 * 1000;

    private static final String PREFS               = "lock_manager_prefs";
    private static final String KEY_MASTER_FAILS    = "master_failed";
    private static final String KEY_QUESTION_FAILS  = "question_failed";
    private static final String KEY_LOCKED          = "is_locked";
    private static final String KEY_TIMED_LOCK_UNTIL = "timed_lock_until";
    private static final String KEY_IN_FINAL_PHASE  = "in_final_phase";

    private final SharedPreferences prefs;

    public LockManager(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }


    public int getMasterFails() { return prefs.getInt(KEY_MASTER_FAILS, 0); }

    public int incrementMasterFails() {
        int n = getMasterFails() + 1;
        prefs.edit().putInt(KEY_MASTER_FAILS, n).apply();
        return n;
    }

    public void resetMasterFails() {
        prefs.edit().putInt(KEY_MASTER_FAILS, 0).apply();
    }

    public void onSuccessfulUnlock() {
        prefs.edit()
                .putInt(KEY_MASTER_FAILS,     0)
                .putInt(KEY_QUESTION_FAILS,   0)
                .putBoolean(KEY_LOCKED,        false)
                .putLong(KEY_TIMED_LOCK_UNTIL, 0L)
                .putBoolean(KEY_IN_FINAL_PHASE, false)
                .apply();
    }


    public boolean isLocked() { return prefs.getBoolean(KEY_LOCKED, false); }

    public void lock() {
        prefs.edit()
                .putBoolean(KEY_LOCKED, true)
                .putInt(KEY_QUESTION_FAILS, 0)
                .apply();
    }


    public boolean isTimedLockActive() {
        long until = prefs.getLong(KEY_TIMED_LOCK_UNTIL, 0L);
        return until > 0 && System.currentTimeMillis() < until;
    }

    public long timedLockRemainingMs() {
        long until   = prefs.getLong(KEY_TIMED_LOCK_UNTIL, 0L);
        long remaining = until - System.currentTimeMillis();
        return Math.max(remaining, 0L);
    }

    public void startTimedLock() {
        prefs.edit()
                .putLong(KEY_TIMED_LOCK_UNTIL,
                        System.currentTimeMillis() + LOCK_DURATION_MS)
                .putBoolean(KEY_IN_FINAL_PHASE, false)
                .putInt(KEY_QUESTION_FAILS, 0)
                .apply();
    }

    public void onTimedLockExpired() {
        prefs.edit()
                .putLong(KEY_TIMED_LOCK_UNTIL, 0L)
                .putBoolean(KEY_IN_FINAL_PHASE, true)
                .putInt(KEY_QUESTION_FAILS, 0)
                .apply();
    }

    public boolean isInFinalPhase() { return prefs.getBoolean(KEY_IN_FINAL_PHASE, false); }


    public int getQuestionFails() { return prefs.getInt(KEY_QUESTION_FAILS, 0); }

    public int incrementQuestionFails() {
        int n = getQuestionFails() + 1;
        prefs.edit().putInt(KEY_QUESTION_FAILS, n).apply();
        return n;
    }

    public void resetQuestionFails() {
        prefs.edit().putInt(KEY_QUESTION_FAILS, 0).apply();
    }

    public int maxQuestionAttempts() {
        return isInFinalPhase() ? FINAL_ATTEMPTS : MAX_QUESTION_ATTEMPTS;
    }
}