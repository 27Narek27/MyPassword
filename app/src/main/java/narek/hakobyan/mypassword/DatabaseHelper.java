package narek.hakobyan.mypassword;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME    = "passwords.db";
    private static final int    DATABASE_VERSION = 2;

    public static final String TABLE_NAME        = "passwords";
    public static final String COLUMN_ID         = "id";
    public static final String COLUMN_SITE       = "site";
    public static final String COLUMN_LOGIN      = "login";
    public static final String COLUMN_PASSWORD   = "password";
    public static final String COLUMN_WEBSITE_URL = "website_url";

    private static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " ("
                    + COLUMN_ID          + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_SITE        + " TEXT NOT NULL, "
                    + COLUMN_LOGIN       + " TEXT, "
                    + COLUMN_PASSWORD    + " TEXT, "
                    + COLUMN_WEBSITE_URL + " TEXT"
                    + ");";
    public static class PasswordEntry {
        public int    id;
        public String site;
        public String login;
        public String password;
        public String websiteUrl;

        public PasswordEntry(int id, String site, String login,
                             String password, String websiteUrl) {
            this.id         = id;
            this.site       = site;
            this.login      = login;
            this.password   = password;
            this.websiteUrl = websiteUrl;
        }
    }

    private final CryptoManager cryptoManager;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        cryptoManager = new CryptoManager();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_NAME
                    + " ADD COLUMN " + COLUMN_WEBSITE_URL + " TEXT");
        }
    }

    public long insertPassword(String site, String login,
                               String password, String websiteUrl) {
        if (!PasswordSecurityUtils.isNonEmpty(password)) {
            throw new IllegalArgumentException(
                    PasswordSecurityUtils.ENTRY_VALIDATION_ERROR_MESSAGE);
        }

        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SITE,        site);
        values.put(COLUMN_LOGIN,       login);
        values.put(COLUMN_PASSWORD,    cryptoManager.encrypt(password));
        values.put(COLUMN_WEBSITE_URL, websiteUrl != null ? websiteUrl.trim() : "");

        long id = db.insert(TABLE_NAME, null, values);
        db.close();
        return id;
    }

    public ArrayList<PasswordEntry> getAllPasswords() {
        ArrayList<PasswordEntry> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(TABLE_NAME,
                new String[]{COLUMN_ID, COLUMN_SITE, COLUMN_LOGIN,
                        COLUMN_PASSWORD, COLUMN_WEBSITE_URL},
                null, null, null, null, COLUMN_ID + " DESC");

        if (cursor.moveToFirst()) {
            do {
                list.add(rowToEntry(cursor));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return list;
    }

    public PasswordEntry getPasswordById(int id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME,
                new String[]{COLUMN_ID, COLUMN_SITE, COLUMN_LOGIN,
                        COLUMN_PASSWORD, COLUMN_WEBSITE_URL},
                COLUMN_ID + "=?", new String[]{String.valueOf(id)},
                null, null, null);

        PasswordEntry entry = null;
        if (cursor.moveToFirst()) {
            entry = rowToEntry(cursor);
        }
        cursor.close();
        db.close();
        return entry;
    }


    public void updatePassword(int id, String site, String login,
                               String password, String websiteUrl) {
        if (!PasswordSecurityUtils.isNonEmpty(password)) {
            throw new IllegalArgumentException(
                    PasswordSecurityUtils.ENTRY_VALIDATION_ERROR_MESSAGE);
        }

        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SITE,        site);
        values.put(COLUMN_LOGIN,       login);
        values.put(COLUMN_PASSWORD,    cryptoManager.encrypt(password));
        values.put(COLUMN_WEBSITE_URL, websiteUrl != null ? websiteUrl.trim() : "");

        db.update(TABLE_NAME, values, COLUMN_ID + "=?",
                new String[]{String.valueOf(id)});
        db.close();
    }

    public void deletePassword(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_NAME, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    private PasswordEntry rowToEntry(Cursor cursor) {
        int    id         = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
        String site       = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SITE));
        String login      = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOGIN));
        String encPass    = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD));
        String websiteUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WEBSITE_URL));

        return new PasswordEntry(id, site, login,
                cryptoManager.decrypt(encPass),
                websiteUrl != null ? websiteUrl : "");
    }
}