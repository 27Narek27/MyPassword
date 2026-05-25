package narek.hakobyan.mypassword;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

public class SecureMediaVaultService {
    private static final int MAX_IMAGE_BYTES = 8 * 1024 * 1024;

    private final Context context;
    private final SecureMediaVaultManager vaultManager;
    private final SecureMediaVaultManager.OcrExtractor ocrExtractor;

    public SecureMediaVaultService(Context context, DatabaseHelper dbHelper) {
        this.context = context.getApplicationContext();
        this.vaultManager = new SecureMediaVaultManager(dbHelper);
        this.ocrExtractor = new MlKitOcrExtractor(this.context);
    }

    public long importImageToVault(int passwordId, Uri imageUri) throws Exception {
        byte[] bytes = readImageBytes(imageUri);
        String mimeType = context.getContentResolver().getType(imageUri);
        if (mimeType == null || mimeType.trim().isEmpty()) mimeType = "image/*";
        return vaultManager.storeEncryptedImage(passwordId, bytes, mimeType, ocrExtractor);
    }

    public ArrayList<Integer> searchMediaByText(String query) {
        return vaultManager.searchByOcrKeyword(query);
    }

    private byte[] readImageBytes(Uri imageUri) throws Exception {
        ContentResolver resolver = context.getContentResolver();
        try (InputStream in = resolver.openInputStream(imageUri);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (in == null) throw new IllegalStateException("Cannot open image stream");

            byte[] buffer = new byte[4096];
            int read;
            int total = 0;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > MAX_IMAGE_BYTES) throw new IllegalArgumentException("Image is too large (max 8MB)");
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }
}
