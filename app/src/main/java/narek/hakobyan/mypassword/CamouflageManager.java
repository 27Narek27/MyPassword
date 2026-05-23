package narek.hakobyan.mypassword;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

public final class CamouflageManager {
    private CamouflageManager() {}

    public static void setCamouflageEnabled(Context context, boolean enabled) {
        PackageManager pm = context.getPackageManager();
        String pkg = context.getPackageName();

        ComponentName normalLauncher = new ComponentName(pkg, pkg + ".MainActivity");
        ComponentName camoLauncher = new ComponentName(pkg, pkg + ".HiddenModeCalculator");

        pm.setComponentEnabledSetting(normalLauncher,
                enabled ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED : PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);

        pm.setComponentEnabledSetting(camoLauncher,
                enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }
}
