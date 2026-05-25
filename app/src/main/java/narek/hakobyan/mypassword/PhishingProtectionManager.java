package narek.hakobyan.mypassword;

import android.net.Uri;
import android.text.TextUtils;
import android.webkit.URLUtil;

import java.util.Locale;

public class PhishingProtectionManager {

    public static class Verdict {
        public final boolean allowAutofill;
        public final String reason;

        public Verdict(boolean allowAutofill, String reason) {
            this.allowAutofill = allowAutofill;
            this.reason = reason;
        }
    }

    public Verdict verifyAutofillSafety(String currentUrl, String savedUrl, String pageHtml) {
        if (!URLUtil.isHttpsUrl(currentUrl)) return new Verdict(false, "Autofill blocked: non-HTTPS page");
        if (TextUtils.isEmpty(savedUrl)) return new Verdict(false, "Autofill blocked: no trusted URL in vault");

        String currentHost = host(currentUrl);
        String savedHost = host(savedUrl);
        if (currentHost == null || savedHost == null) return new Verdict(false, "Autofill blocked: malformed URL");

        if (!sameRegistrableDomain(currentHost, savedHost)) {
            return new Verdict(false, "Autofill blocked: domain mismatch");
        }

        String lowHtml = pageHtml == null ? "" : pageHtml.toLowerCase(Locale.ROOT);
        if (lowHtml.contains("password") && (lowHtml.contains("display:none") || lowHtml.contains("opacity:0"))) {
            return new Verdict(false, "Autofill blocked: hidden password fields detected");
        }

        return new Verdict(true, "Autofill allowed");
    }

    private String host(String url) {
        return Uri.parse(url).getHost();
    }

    private boolean sameRegistrableDomain(String left, String right) {
        String l = left.toLowerCase(Locale.ROOT);
        String r = right.toLowerCase(Locale.ROOT);
        return l.equals(r) || l.endsWith("." + r) || r.endsWith("." + l);
    }
}
