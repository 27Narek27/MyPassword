package narek.hakobyan.mypassword;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME    = "passwords.db";
    private static final int    DATABASE_VERSION = 5;

    public static final String TABLE_NAME            = "passwords";
    public static final String COLUMN_ID             = "id";
    public static final String COLUMN_SITE           = "site";
    public static final String COLUMN_LOGIN          = "login";
    public static final String COLUMN_PASSWORD       = "password";
    public static final String COLUMN_WEBSITE_URL    = "website_url";
    public static final String COLUMN_CATEGORY       = "category";
    public static final String COLUMN_CATEGORY_COLOR = "category_color";
    public static final String COLUMN_IS_FAVORITE    = "is_favorite";
    public static final String COLUMN_LAST_CHANGED_AT = "last_changed_at";

    public static final String TABLE_HISTORY      = "password_history";
    public static final String TABLE_SECURE_SHARES = "secure_shares";
    public static final String TABLE_MEDIA_VAULT  = "media_vault";

    private static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " ("
                    + COLUMN_ID             + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_SITE           + " TEXT NOT NULL, "
                    + COLUMN_LOGIN          + " TEXT, "
                    + COLUMN_PASSWORD       + " TEXT, "
                    + COLUMN_WEBSITE_URL    + " TEXT, "
                    + COLUMN_CATEGORY       + " TEXT DEFAULT 'Общее', "
                    + COLUMN_CATEGORY_COLOR + " TEXT DEFAULT '#6B9E5A', "
                    + COLUMN_IS_FAVORITE    + " INTEGER DEFAULT 0, "
                    + COLUMN_LAST_CHANGED_AT + " INTEGER DEFAULT 0"
                    + ");";

    private static final String CREATE_HISTORY =
            "CREATE TABLE " + TABLE_HISTORY + " ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "password_id INTEGER NOT NULL, "
                    + "old_password TEXT NOT NULL, "
                    + "changed_at INTEGER NOT NULL"
                    + ");";

    private static final String CREATE_SECURE_SHARES =
            "CREATE TABLE " + TABLE_SECURE_SHARES + " ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "token_hash TEXT NOT NULL UNIQUE, "
                    + "password_id INTEGER NOT NULL, "
                    + "encrypted_payload TEXT NOT NULL, "
                    + "created_at INTEGER NOT NULL, "
                    + "expires_at INTEGER NOT NULL, "
                    + "max_views INTEGER NOT NULL DEFAULT 1"
                    + ");";

    private static final String CREATE_MEDIA_VAULT =
            "CREATE TABLE " + TABLE_MEDIA_VAULT + " ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "password_id INTEGER, "
                    + "encrypted_blob BLOB NOT NULL, "
                    + "encrypted_ocr_index TEXT, "
                    + "mime_type TEXT, "
                    + "created_at INTEGER NOT NULL"
                    + ");";



    public static class PasswordEntry {
        public int     id;
        public String  site;
        public String  login;
        public String  password;
        public String  websiteUrl;
        public String  category;
        public String  categoryColor;
        public boolean isFavorite;
        public long    lastChangedAt;
    }

    public static class PasswordHealthStats {
        public int weakPasswords;
        public int duplicatePasswords;
        public int stalePasswords;
    }

    public static class PasswordVersion {
        public int    historyId;
        public String password;
        public long   changedAt;
    }


    private final CryptoManager cryptoManager;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        cryptoManager = new CryptoManager();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
        db.execSQL(CREATE_HISTORY);
        db.execSQL(CREATE_SECURE_SHARES);
        db.execSQL(CREATE_MEDIA_VAULT);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2)
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COLUMN_WEBSITE_URL + " TEXT");
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COLUMN_CATEGORY       + " TEXT DEFAULT 'Общее'");
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COLUMN_CATEGORY_COLOR + " TEXT DEFAULT '#6B9E5A'");
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COLUMN_IS_FAVORITE    + " INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COLUMN_LAST_CHANGED_AT + " INTEGER DEFAULT 0");
            db.execSQL(CREATE_HISTORY);
        }
        if (oldVersion < 4) {
            db.execSQL(CREATE_SECURE_SHARES);
            db.execSQL(CREATE_MEDIA_VAULT);
        }
        if (oldVersion < 5) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_SECURE_SHARES);
            db.execSQL(CREATE_SECURE_SHARES);
        }
    }



    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.enableWriteAheadLogging();
    }



    public long insertPassword(String site, String login, String password,
                               String websiteUrl, String category,
                               String categoryColor, boolean isFavorite) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COLUMN_SITE,            site);
        v.put(COLUMN_LOGIN,           login);
        v.put(COLUMN_PASSWORD,        cryptoManager.encrypt(password));
        v.put(COLUMN_WEBSITE_URL,     websiteUrl != null ? websiteUrl.trim() : "");
        v.put(COLUMN_CATEGORY,        category);
        v.put(COLUMN_CATEGORY_COLOR,  categoryColor);
        v.put(COLUMN_IS_FAVORITE,     isFavorite ? 1 : 0);
        v.put(COLUMN_LAST_CHANGED_AT, System.currentTimeMillis());
        return db.insert(TABLE_NAME, null, v);
    }

    public long insertPassword(String site, String login, String password, String websiteUrl) {
        return insertPassword(site, login, password, websiteUrl, "Общее", "#6B9E5A", false);
    }



    public ArrayList<PasswordEntry> getAllPasswords() {
        ArrayList<PasswordEntry> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_NAME, null, null, null, null, null,
                COLUMN_IS_FAVORITE + " DESC, " + COLUMN_ID + " DESC");
        if (c.moveToFirst()) do { list.add(rowToEntry(c)); } while (c.moveToNext());
        c.close();
        return list;
    }

    public ArrayList<PasswordEntry> getVisiblePasswords() {
        ArrayList<PasswordEntry> out = new ArrayList<>();
        for (PasswordEntry e : getAllPasswords()) {
            if (!"Honeytoken".equalsIgnoreCase(e.category)) out.add(e);
        }
        return out;
    }

    public PasswordEntry getPasswordById(int id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_NAME, null,
                COLUMN_ID + "=?", new String[]{String.valueOf(id)},
                null, null, null);
        PasswordEntry e = null;
        if (c.moveToFirst()) e = rowToEntry(c);
        c.close();
        return e;
    }



    public void updatePassword(int id, String site, String login,
                               String password, String websiteUrl) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {

            String currentEncrypted = null;
            Cursor c = db.query(TABLE_NAME,
                    new String[]{COLUMN_PASSWORD},
                    COLUMN_ID + "=?", new String[]{String.valueOf(id)},
                    null, null, null);
            if (c.moveToFirst()) currentEncrypted = c.getString(0);
            c.close();

            if (currentEncrypted != null) {
                String currentPlain = cryptoManager.decrypt(currentEncrypted);
                // Only save history if password actually changed
                if (!currentPlain.equals(password)) {
                    ContentValues h = new ContentValues();
                    h.put("password_id", id);
                    h.put("old_password", currentEncrypted); // already encrypted
                    h.put("changed_at",   System.currentTimeMillis());
                    db.insert(TABLE_HISTORY, null, h);
                }
            }

            ContentValues v = new ContentValues();
            v.put(COLUMN_SITE,            site);
            v.put(COLUMN_LOGIN,           login);
            v.put(COLUMN_PASSWORD,        cryptoManager.encrypt(password));
            v.put(COLUMN_WEBSITE_URL,     websiteUrl != null ? websiteUrl.trim() : "");
            v.put(COLUMN_LAST_CHANGED_AT, System.currentTimeMillis());
            db.update(TABLE_NAME, v, COLUMN_ID + "=?", new String[]{String.valueOf(id)});

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void toggleFavorite(int id, boolean isFavorite) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COLUMN_IS_FAVORITE, isFavorite ? 1 : 0);
        db.update(TABLE_NAME, v, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
    }



    public ArrayList<PasswordVersion> getPasswordVersions(int passwordId) {
        ArrayList<PasswordVersion> out = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_HISTORY,
                new String[]{"id", "old_password", "changed_at"},
                "password_id=?", new String[]{String.valueOf(passwordId)},
                null, null, "changed_at DESC");
        while (c.moveToNext()) {
            PasswordVersion v = new PasswordVersion();
            v.historyId = c.getInt(0);
            v.password  = cryptoManager.decrypt(c.getString(1));
            v.changedAt = c.getLong(2);
            out.add(v);
        }
        c.close();
        return out;
    }


    public String rollbackToVersion(int passwordId, int historyId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {

            Cursor hc = db.query(TABLE_HISTORY,
                    new String[]{"old_password"},
                    "id=? AND password_id=?",
                    new String[]{String.valueOf(historyId), String.valueOf(passwordId)},
                    null, null, null, "1");
            if (!hc.moveToFirst()) {
                hc.close();
                return null;
            }
            String encryptedRollback = hc.getString(0);
            hc.close();

            String rollbackPassword = cryptoManager.decrypt(encryptedRollback);


            Cursor ec = db.query(TABLE_NAME,
                    new String[]{COLUMN_PASSWORD},
                    COLUMN_ID + "=?", new String[]{String.valueOf(passwordId)},
                    null, null, null);
            if (ec.moveToFirst()) {
                String currentEncrypted = ec.getString(0);
                String currentPlain     = cryptoManager.decrypt(currentEncrypted);

                if (!currentPlain.equals(rollbackPassword)) {
                    ContentValues backup = new ContentValues();
                    backup.put("password_id", passwordId);
                    backup.put("old_password", currentEncrypted);
                    backup.put("changed_at",   System.currentTimeMillis());
                    db.insert(TABLE_HISTORY, null, backup);
                }
            }
            ec.close();


            ContentValues update = new ContentValues();
            update.put(COLUMN_PASSWORD,        encryptedRollback);
            update.put(COLUMN_LAST_CHANGED_AT, System.currentTimeMillis());
            db.update(TABLE_NAME, update,
                    COLUMN_ID + "=?", new String[]{String.valueOf(passwordId)});


            db.delete(TABLE_HISTORY,
                    "id=?", new String[]{String.valueOf(historyId)});

            db.setTransactionSuccessful();
            return rollbackPassword;

        } finally {

            db.endTransaction();
        }
    }


    public PasswordHealthStats getHealthStats() {
        PasswordHealthStats s = new PasswordHealthStats();
        ArrayList<PasswordEntry> entries = getAllPasswords();
        java.util.HashMap<String, Integer> freq = new java.util.HashMap<>();
        long ninetyDays = 90L * 24 * 60 * 60 * 1000;
        for (PasswordEntry e : entries) {
            if (PasswordSecurityUtils.calculateStrengthScore(e.password) < 50) s.weakPasswords++;
            freq.put(e.password, freq.getOrDefault(e.password, 0) + 1);
            if (System.currentTimeMillis() - e.lastChangedAt > ninetyDays) s.stalePasswords++;
        }
        for (int n : freq.values()) if (n > 1) s.duplicatePasswords += n;
        return s;
    }

    public int insertHoneytokenData() {
        String[][] fake = {
                {"Swiss National Vault",    "ceo@vault-secure.com",    "S3cret!Vault2026",  "https://vault-secure.com"},
                {"Offshore Reserve Bank",   "director@orb-finance.com","Ocean#Reserve77",   "https://orb-finance.com"},
                {"BlackCard Concierge",     "vip@blackcard.world",     "V1P-Concierge$",    "https://blackcard.world"},
                {"Crypto Whale Desk",       "ops@whale-trade.io",      "WhaleTrade@@1",     "https://whale-trade.io"},
                {"Quantum Payroll",         "admin@q-payroll.ai",      "Qpay!2026#",        "https://q-payroll.ai"},
                {"Falcon Defense CRM",      "agent@falcon-def.net",    "Falcon*Secure88",   "https://falcon-def.net"},
                {"Private Jet Club",        "owner@jetclub.aero",      "JetClub#Sky55",     "https://jetclub.aero"},
                {"Crown Jewels Ledger",     "ledger@crown-safe.org",   "CrownSafe$2026",    "https://crown-safe.org"},
                {"Platinum Family Office",  "office@platinum-fo.com",  "PlatinumFO!9",      "https://platinum-fo.com"},
                {"Embassy Secure Mail",     "attache@embassy-mail.gov","EmbassyMail@X1",    "https://embassy-mail.gov"}
        };
        int inserted = 0;
        for (String[] row : fake) {
            if (insertPassword(row[0], row[1], row[2], row[3], "Honeytoken", "#B06CFF", false) > 0)
                inserted++;
        }
        return inserted;
    }


    public void deletePassword(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE_HISTORY, "password_id=?", new String[]{String.valueOf(id)});
            db.delete(TABLE_NAME,    COLUMN_ID + "=?", new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }


    private PasswordEntry rowToEntry(Cursor c) {
        PasswordEntry e = new PasswordEntry();
        e.id            = c.getInt(c.getColumnIndexOrThrow(COLUMN_ID));
        e.site          = c.getString(c.getColumnIndexOrThrow(COLUMN_SITE));
        e.login         = c.getString(c.getColumnIndexOrThrow(COLUMN_LOGIN));
        e.password      = cryptoManager.decrypt(c.getString(c.getColumnIndexOrThrow(COLUMN_PASSWORD)));
        e.websiteUrl    = c.getString(c.getColumnIndexOrThrow(COLUMN_WEBSITE_URL));
        e.category      = c.getString(c.getColumnIndexOrThrow(COLUMN_CATEGORY));
        e.categoryColor = c.getString(c.getColumnIndexOrThrow(COLUMN_CATEGORY_COLOR));
        e.isFavorite    = c.getInt(c.getColumnIndexOrThrow(COLUMN_IS_FAVORITE)) == 1;
        e.lastChangedAt = c.getLong(c.getColumnIndexOrThrow(COLUMN_LAST_CHANGED_AT));
        return e;
    }
}