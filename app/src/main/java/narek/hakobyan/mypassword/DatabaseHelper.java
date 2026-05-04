package narek.hakobyan.mypassword;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "passwords.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_NAME = "passwords";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_SITE = "site";
    public static final String COLUMN_LOGIN = "login";
    public static final String COLUMN_PASSWORD = "password";

    private final CryptoManager cryptoManager;

    private static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_SITE + " TEXT NOT NULL, " +
            COLUMN_LOGIN + " TEXT, " +
            COLUMN_PASSWORD + " TEXT" +
            ");";

    public static class PasswordEntry {
        public int id;
        public String site;
        public String login;
        public String password;

        public PasswordEntry(int id, String site, String login, String password) {
            this.id = id;
            this.site = site;
            this.login = login;
            this.password = password;
        }
    }

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
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public long insertPassword(String site, String login, String password) {
        if (!PasswordSecurityUtils.isValidPassword(password)) {
            throw new IllegalArgumentException(PasswordSecurityUtils.VALIDATION_ERROR_MESSAGE);
        }
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SITE, site);
        values.put(COLUMN_LOGIN, login);
        values.put(COLUMN_PASSWORD, cryptoManager.encrypt(password));
        long id = db.insert(TABLE_NAME, null, values);
        db.close();
        return id;
    }

    public ArrayList<PasswordEntry> getAllPasswords() {
        ArrayList<PasswordEntry> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME,
                new String[]{COLUMN_ID, COLUMN_SITE, COLUMN_LOGIN, COLUMN_PASSWORD},
                null, null, null, null, COLUMN_ID + " DESC");

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String site = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SITE));
                String login = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOGIN));
                String encryptedPassword = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD));
                list.add(new PasswordEntry(id, site, login, cryptoManager.decrypt(encryptedPassword)));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return list;
    }

    public PasswordEntry getPasswordById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME,
                new String[]{COLUMN_ID, COLUMN_SITE, COLUMN_LOGIN, COLUMN_PASSWORD},
                COLUMN_ID + "=?", new String[]{String.valueOf(id)},
                null, null, null);

        PasswordEntry entry = null;
        if (cursor.moveToFirst()) {
            entry = new PasswordEntry(
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SITE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOGIN)),
                    cryptoManager.decrypt(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD)))
            );
        }
        cursor.close();
        db.close();
        return entry;
    }

    public void deletePassword(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void updatePassword(int id, String site, String login, String password) {
        if (!PasswordSecurityUtils.isValidPassword(password)) {
            throw new IllegalArgumentException(PasswordSecurityUtils.VALIDATION_ERROR_MESSAGE);
        }
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SITE, site);
        values.put(COLUMN_LOGIN, login);
        values.put(COLUMN_PASSWORD, cryptoManager.encrypt(password));
        db.update(TABLE_NAME, values, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }
}
