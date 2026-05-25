package narek.hakobyan.mypassword;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class SecureSharingManager {
    private static final long MIN_TTL_MILLIS = 10_000L;
    private static final long MAX_TTL_MILLIS = 7L * 24 * 60 * 60 * 1000;

    private final DatabaseHelper dbHelper;
    private final CryptoManager cryptoManager;
    private final String deepLinkBase;

    public SecureSharingManager(DatabaseHelper dbHelper, String deepLinkBase) {
        this.dbHelper = dbHelper;
        this.cryptoManager = new CryptoManager();
        this.deepLinkBase = deepLinkBase;
    }

    public String createOneTimeLink(DatabaseHelper.PasswordEntry entry, long ttlMillis) {
        if (entry == null) throw new IllegalArgumentException("Entry must not be null");
        if (ttlMillis < MIN_TTL_MILLIS || ttlMillis > MAX_TTL_MILLIS) {
            throw new IllegalArgumentException("TTL must be between 10 seconds and 7 days");
        }

        purgeExpiredShares();
        String token = randomToken();
        String tokenHash = sha256(token);
        long now = System.currentTimeMillis();
        long expiresAt = now + ttlMillis;

        String payload = entry.site + "\n" + entry.login + "\n" + entry.password;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("token_hash", tokenHash);
        values.put("password_id", entry.id);
        values.put("encrypted_payload", cryptoManager.encrypt(payload));
        values.put("created_at", now);
        values.put("expires_at", expiresAt);
        values.put("max_views", 1);
        db.insert(DatabaseHelper.TABLE_SECURE_SHARES, null, values);
        db.close();

        return Uri.parse(deepLinkBase).buildUpon().appendQueryParameter("token", token).build().toString();
    }

    public String consumeShare(String token) {
        if (token == null || token.trim().isEmpty()) return null;
        String tokenHash = sha256(token);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        Cursor c = db.query(DatabaseHelper.TABLE_SECURE_SHARES, null,
                "token_hash=?", new String[]{tokenHash}, null, null, null, "1");

        if (!c.moveToFirst()) {
            c.close();
            db.endTransaction();
            db.close();
            return null;
        }

        long expiresAt = c.getLong(c.getColumnIndexOrThrow("expires_at"));
        String encryptedPayload = c.getString(c.getColumnIndexOrThrow("encrypted_payload"));
        int id = c.getInt(c.getColumnIndexOrThrow("id"));
        c.close();

        if (System.currentTimeMillis() > expiresAt) {
            db.delete(DatabaseHelper.TABLE_SECURE_SHARES, "id=?", new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
            db.endTransaction();
            db.close();
            return null;
        }

        db.delete(DatabaseHelper.TABLE_SECURE_SHARES, "id=?", new String[]{String.valueOf(id)});
        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
        return cryptoManager.decrypt(encryptedPayload);
    }

    public int purgeExpiredShares() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int deleted = db.delete(DatabaseHelper.TABLE_SECURE_SHARES, "expires_at <= ?", new String[]{String.valueOf(System.currentTimeMillis())});
        db.close();
        return deleted;
    }

    private String randomToken() {
        byte[] token = new byte[24];
        new SecureRandom().nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
