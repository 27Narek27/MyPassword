package narek.hakobyan.mypassword;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.concurrent.TimeUnit;

public class MlKitOcrExtractor implements SecureMediaVaultManager.OcrExtractor {
    private static final long OCR_TIMEOUT_SECONDS = 10;

    @SuppressWarnings("unused")
    private final Context context;

    public MlKitOcrExtractor(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public String extractText(byte[] imageBytes) throws Exception {
        if (imageBytes == null || imageBytes.length == 0) return "";

        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        if (bitmap == null) return "";

        InputImage image = InputImage.fromBitmap(bitmap, 0);
        com.google.mlkit.vision.text.TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        try {
            Text result = Tasks.await(recognizer.process(image), OCR_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return result == null ? "" : result.getText();
        } finally {
            recognizer.close();
        }
    }
}
