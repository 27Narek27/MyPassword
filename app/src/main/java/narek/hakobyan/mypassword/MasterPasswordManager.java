package narek.hakobyan.mypassword;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Stores the master password as a PBKDF2-SHA256 hash (120 000 iterations, 256-bit key).
 * The plaintext password is NEVER persisted — only the salted hash.
 *
 * Migration: if an old plaintext value is found on first open, it is re-hashed
 * transparently so existing users don't need to reset their password.
 */
public class MasterPasswordManager {

    private static final String PREFS_NAME  = "master_password_prefs";
    private static final String KEY_HASH    = "master_password_hash";   // PBKDF2 hash
    // Legacy key — only read during one-time migration, then deleted
    private static final String KEY_LEGACY  = "master_password_value";

    private final SharedPreferences  prefs;
    private final PasswordHashManager hasher;

    public MasterPasswordManager(Context context) {
        prefs  = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        hasher = new PasswordHashManager();
        migrateLegacyIfNeeded();
    }

    // ── public API ──────────────────────────────────────────────────────────

    /** @return true if a master password hash has been saved. */
    public boolean hasMasterPassword() {
        return prefs.contains(KEY_HASH);
    }

    /**
     * Hash and persist the master password.
     * @throws IllegalArgumentException if the password fails complexity rules
     */
    public void saveMasterPassword(String password) {
        if (!PasswordSecurityUtils.isValidMasterPassword(password)) {
            throw new IllegalArgumentException(
                    PasswordSecurityUtils.MASTER_VALIDATION_ERROR_MESSAGE);
        }
        String hash = hasher.hashPassword(password);
        prefs.edit()
                .putString(KEY_HASH, hash)
                .remove(KEY_LEGACY)   // remove plaintext if somehow still present
                .apply();
    }

    /**
     * Verify a candidate password against the stored hash.
     * Uses constant-time comparison internally (see PasswordHashManager).
     */
    public boolean verifyMasterPassword(String password) {
        String storedHash = prefs.getString(KEY_HASH, null);
        if (storedHash == null) return false;
        return hasher.verifyPassword(password, storedHash);
    }

    // ── migration ───────────────────────────────────────────────────────────

    /**
     * One-time migration: if the old plaintext key exists and no hash exists yet,
     * re-hash the plaintext and delete it.
     */
    private void migrateLegacyIfNeeded() {
        if (prefs.contains(KEY_HASH)) return;           // already migrated
        String legacy = prefs.getString(KEY_LEGACY, null);
        if (legacy == null || legacy.isEmpty()) return; // nothing to migrate

        String hash = hasher.hashPassword(legacy);
        prefs.edit()
                .putString(KEY_HASH, hash)
                .remove(KEY_LEGACY)
                .apply();
    }
}