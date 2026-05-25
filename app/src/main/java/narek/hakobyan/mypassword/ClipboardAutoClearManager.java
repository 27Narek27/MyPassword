package narek.hakobyan.mypassword;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

public class ClipboardAutoClearManager {
    private final ClipboardManager clipboardManager;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public ClipboardAutoClearManager(Context context) {
        this.clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    }

    public void copySecretWithAutoClear(String label, String secret, long delayMillis) {
        clipboardManager.setPrimaryClip(ClipData.newPlainText(label, secret));
        handler.postDelayed(() -> {
            ClipData current = clipboardManager.getPrimaryClip();
            if (current != null && current.getItemCount() > 0) {
                CharSequence value = current.getItemAt(0).getText();
                if (secret.contentEquals(value)) {
                    clipboardManager.setPrimaryClip(ClipData.newPlainText("cleared", ""));
                }
            }
        }, delayMillis);
    }
}
