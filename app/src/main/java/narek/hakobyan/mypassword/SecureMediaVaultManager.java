package narek.hakobyan.mypassword;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

public class SecureMediaVaultManager {
    public interface OcrExtractor {
        String extractText(byte[] imageBytes) throws Exception;
    }

    private final DatabaseHelper dbHelper;
    private final CryptoManager cryptoManager;

    public SecureMediaVaultManager(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
        this.cryptoManager = new CryptoManager();
    }

    public long storeEncryptedImage(int passwordId, byte[] imageBytes, String mimeType, OcrExtractor extractor) throws Exception {
        String ocrText = extractor != null ? extractor.extractText(imageBytes) : "";

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("password_id", passwordId);
        values.put("encrypted_blob", cryptoManager.encryptBytes(imageBytes));
        values.put("encrypted_ocr_index", cryptoManager.encrypt(normalize(ocrText)));
        values.put("mime_type", mimeType);
        values.put("created_at", System.currentTimeMillis());
        long id = db.insert(DatabaseHelper.TABLE_MEDIA_VAULT, null, values);
        db.close();
        return id;
    }

    public ArrayList<Integer> searchByOcrKeyword(String keyword) {
        ArrayList<Integer> out = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TABLE_MEDIA_VAULT, new String[]{"id", "encrypted_ocr_index"}, null, null, null, null, "created_at DESC");
        String needle = normalize(keyword);
        while (c.moveToNext()) {
            String plain = cryptoManager.decrypt(c.getString(1));
            if (plain.contains(needle)) out.add(c.getInt(0));
        }
        c.close();
        db.close();
        return out;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase().replaceAll("\\s+", " ").trim();
    }
}
