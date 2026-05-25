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
    private final DatabaseHelper dbHelper;
    private final CryptoManager cryptoManager;
    private final String deepLinkBase;

    public SecureSharingManager(DatabaseHelper dbHelper, String deepLinkBase) {
        this.dbHelper = dbHelper;
        this.cryptoManager = new CryptoManager();
        this.deepLinkBase = deepLinkBase;
    }

    public String createOneTimeLink(DatabaseHelper.PasswordEntry entry, long ttlMillis) {
        String token = randomToken();
        String tokenHash = sha256(token);
        long expiresAt = System.currentTimeMillis() + ttlMillis;

        String payload = entry.site + "\n" + entry.login + "\n" + entry.password;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("token_hash", tokenHash);
        values.put("password_id", entry.id);
        values.put("encrypted_payload", cryptoManager.encrypt(payload));
        values.put("expires_at", expiresAt);
        values.put("views_left", 1);
        db.insert(DatabaseHelper.TABLE_SECURE_SHARES, null, values);
        db.close();

        return Uri.parse(deepLinkBase).buildUpon().appendQueryParameter("token", token).build().toString();
    }

    public String consumeShare(String token) {
        String tokenHash = sha256(token);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor c = db.query(DatabaseHelper.TABLE_SECURE_SHARES, null,
                "token_hash=? AND is_revoked=0", new String[]{tokenHash}, null, null, null, "1");

        if (!c.moveToFirst()) {
            c.close(); db.close();
            return null;
        }

        long expiresAt = c.getLong(c.getColumnIndexOrThrow("expires_at"));
        int viewsLeft = c.getInt(c.getColumnIndexOrThrow("views_left"));
        String encryptedPayload = c.getString(c.getColumnIndexOrThrow("encrypted_payload"));

        if (System.currentTimeMillis() > expiresAt || viewsLeft <= 0) {
            c.close();
            db.delete(DatabaseHelper.TABLE_SECURE_SHARES, "token_hash=?", new String[]{tokenHash});
            db.close();
            return null;
        }

        ContentValues values = new ContentValues();
        values.put("views_left", viewsLeft - 1);
        if (viewsLeft - 1 <= 0) values.put("is_revoked", 1);
        db.update(DatabaseHelper.TABLE_SECURE_SHARES, values, "token_hash=?", new String[]{tokenHash});

        c.close(); db.close();
        return cryptoManager.decrypt(encryptedPayload);
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
