package narek.hakobyan.mypassword;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "passwords.db";
    private static final int DATABASE_VERSION = 3;

    public static final String TABLE_NAME = "passwords";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_SITE = "site";
    public static final String COLUMN_LOGIN = "login";
    public static final String COLUMN_PASSWORD = "password";
    public static final String COLUMN_WEBSITE_URL = "website_url";
    public static final String COLUMN_CATEGORY = "category";
    public static final String COLUMN_CATEGORY_COLOR = "category_color";
    public static final String COLUMN_IS_FAVORITE = "is_favorite";
    public static final String COLUMN_LAST_CHANGED_AT = "last_changed_at";

    public static final String TABLE_HISTORY = "password_history";

    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " ("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_SITE + " TEXT NOT NULL, "
            + COLUMN_LOGIN + " TEXT, "
            + COLUMN_PASSWORD + " TEXT, "
            + COLUMN_WEBSITE_URL + " TEXT, "
            + COLUMN_CATEGORY + " TEXT DEFAULT 'Общее', "
            + COLUMN_CATEGORY_COLOR + " TEXT DEFAULT '#6B9E5A', "
            + COLUMN_IS_FAVORITE + " INTEGER DEFAULT 0, "
            + COLUMN_LAST_CHANGED_AT + " INTEGER DEFAULT 0"
            + ");";

    private static final String CREATE_HISTORY = "CREATE TABLE " + TABLE_HISTORY + " ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "password_id INTEGER NOT NULL, "
            + "old_password TEXT NOT NULL, "
            + "changed_at INTEGER NOT NULL"
            + ");";

    public static class PasswordEntry {
        public int id;
        public String site;
        public String login;
        public String password;
        public String websiteUrl;
        public String category;
        public String categoryColor;
        public boolean isFavorite;
        public long lastChangedAt;
    }

    public static class PasswordHealthStats {
        public int weakPasswords;
        public int duplicatePasswords;
        public int stalePasswords;
    }

    private final CryptoManager cryptoManager;

    public DatabaseHelper(Context context) { super(context, DATABASE_NAME, null, DATABASE_VERSION); cryptoManager = new CryptoManager(); }
    @Override public void onCreate(SQLiteDatabase db) { db.execSQL(CREATE_TABLE); db.execSQL(CREATE_HISTORY); }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COLUMN_WEBSITE_URL + " TEXT");
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COLUMN_CATEGORY + " TEXT DEFAULT 'Общее'");
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COLUMN_CATEGORY_COLOR + " TEXT DEFAULT '#6B9E5A'");
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COLUMN_IS_FAVORITE + " INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COLUMN_LAST_CHANGED_AT + " INTEGER DEFAULT 0");
            db.execSQL(CREATE_HISTORY);
        }
    }

    public long insertPassword(String site, String login, String password, String websiteUrl, String category, String categoryColor, boolean isFavorite) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SITE, site); values.put(COLUMN_LOGIN, login); values.put(COLUMN_PASSWORD, cryptoManager.encrypt(password));
        values.put(COLUMN_WEBSITE_URL, websiteUrl != null ? websiteUrl.trim() : "");
        values.put(COLUMN_CATEGORY, category); values.put(COLUMN_CATEGORY_COLOR, categoryColor); values.put(COLUMN_IS_FAVORITE, isFavorite ? 1 : 0);
        values.put(COLUMN_LAST_CHANGED_AT, System.currentTimeMillis());
        long id = db.insert(TABLE_NAME, null, values); db.close(); return id;
    }
    public long insertPassword(String site, String login, String password, String websiteUrl) { return insertPassword(site, login, password, websiteUrl, "Общее", "#6B9E5A", false); }

    public ArrayList<PasswordEntry> getAllPasswords() {
        ArrayList<PasswordEntry> list = new ArrayList<>(); SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_NAME, null, null, null, null, null, COLUMN_IS_FAVORITE + " DESC, " + COLUMN_ID + " DESC");
        if (c.moveToFirst()) do list.add(rowToEntry(c)); while (c.moveToNext()); c.close(); db.close(); return list;
    }

    public PasswordEntry getPasswordById(int id) {
        SQLiteDatabase db = getReadableDatabase(); Cursor c = db.query(TABLE_NAME, null, COLUMN_ID + "=?", new String[]{String.valueOf(id)}, null, null, null);
        PasswordEntry e = null; if (c.moveToFirst()) e = rowToEntry(c); c.close(); db.close(); return e;
    }

    public void updatePassword(int id, String site, String login, String password, String websiteUrl) {
        PasswordEntry old = getPasswordById(id); SQLiteDatabase db = getWritableDatabase();
        if (old != null && !old.password.equals(password)) {
            ContentValues h = new ContentValues(); h.put("password_id", id); h.put("old_password", cryptoManager.encrypt(old.password)); h.put("changed_at", System.currentTimeMillis()); db.insert(TABLE_HISTORY, null, h);
        }
        ContentValues values = new ContentValues();
        values.put(COLUMN_SITE, site); values.put(COLUMN_LOGIN, login); values.put(COLUMN_PASSWORD, cryptoManager.encrypt(password)); values.put(COLUMN_WEBSITE_URL, websiteUrl != null ? websiteUrl.trim() : "");
        values.put(COLUMN_LAST_CHANGED_AT, System.currentTimeMillis());
        db.update(TABLE_NAME, values, COLUMN_ID + "=?", new String[]{String.valueOf(id)}); db.close();
    }

    public void toggleFavorite(int id, boolean isFavorite) { SQLiteDatabase db = getWritableDatabase(); ContentValues v = new ContentValues(); v.put(COLUMN_IS_FAVORITE, isFavorite ? 1 : 0); db.update(TABLE_NAME, v, COLUMN_ID + "=?", new String[]{String.valueOf(id)}); db.close(); }

    public ArrayList<String> getPasswordHistory(int id) {
        ArrayList<String> out = new ArrayList<>(); SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_HISTORY, new String[]{"old_password","changed_at"}, "password_id=?", new String[]{String.valueOf(id)}, null, null, "changed_at DESC");
        if (c.moveToFirst()) do { out.add(cryptoManager.decrypt(c.getString(0)) + " • " + new java.util.Date(c.getLong(1))); } while (c.moveToNext());
        c.close(); db.close(); return out;
    }

    public PasswordHealthStats getHealthStats() {
        PasswordHealthStats s = new PasswordHealthStats();
        ArrayList<PasswordEntry> entries = getAllPasswords();
        java.util.HashMap<String, Integer> freq = new java.util.HashMap<>();
        long ninetyDays = 90L * 24 * 60 * 60 * 1000;
        for (PasswordEntry e: entries) {
            if (PasswordSecurityUtils.calculateStrengthScore(e.password) < 50) s.weakPasswords++;
            freq.put(e.password, freq.getOrDefault(e.password, 0) + 1);
            if (System.currentTimeMillis() - e.lastChangedAt > ninetyDays) s.stalePasswords++;
        }
        for (int n: freq.values()) if (n > 1) s.duplicatePasswords += n;
        return s;
    }

    public void deletePassword(int id) { SQLiteDatabase db = getWritableDatabase(); db.delete(TABLE_NAME, COLUMN_ID + "=?", new String[]{String.valueOf(id)}); db.close(); }

    private PasswordEntry rowToEntry(Cursor c) {
        PasswordEntry e = new PasswordEntry();
        e.id = c.getInt(c.getColumnIndexOrThrow(COLUMN_ID)); e.site = c.getString(c.getColumnIndexOrThrow(COLUMN_SITE)); e.login = c.getString(c.getColumnIndexOrThrow(COLUMN_LOGIN));
        e.password = cryptoManager.decrypt(c.getString(c.getColumnIndexOrThrow(COLUMN_PASSWORD))); e.websiteUrl = c.getString(c.getColumnIndexOrThrow(COLUMN_WEBSITE_URL));
        e.category = c.getString(c.getColumnIndexOrThrow(COLUMN_CATEGORY)); e.categoryColor = c.getString(c.getColumnIndexOrThrow(COLUMN_CATEGORY_COLOR));
        e.isFavorite = c.getInt(c.getColumnIndexOrThrow(COLUMN_IS_FAVORITE)) == 1; e.lastChangedAt = c.getLong(c.getColumnIndexOrThrow(COLUMN_LAST_CHANGED_AT));
        return e;
    }
}
