package narek.hakobyan.mypassword;

import android.app.Activity;
import android.os.Build;
import android.view.WindowManager;

public final class SecureScreenUtils {
    private SecureScreenUtils() {}
    public static void apply(Activity activity) {
        activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.setRecentsScreenshotEnabled(false);
        }
    }
}
