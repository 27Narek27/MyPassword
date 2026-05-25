package narek.hakobyan.mypassword;

import android.net.Uri;
import android.text.TextUtils;
import android.webkit.URLUtil;

import org.json.JSONObject;

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

    public Verdict verifyAutofillSafety(String currentUrl, String savedUrl, String domSignalsJson) {
        if (!URLUtil.isHttpsUrl(currentUrl)) return new Verdict(false, "Autofill blocked: non-HTTPS page");
        if (TextUtils.isEmpty(savedUrl)) return new Verdict(false, "Autofill blocked: no trusted URL in vault");

        String currentHost = host(currentUrl);
        String savedHost = host(savedUrl);
        if (currentHost == null || savedHost == null) return new Verdict(false, "Autofill blocked: malformed URL");

        if (!sameRegistrableDomain(currentHost, savedHost)) {
            return new Verdict(false, "Autofill blocked: domain mismatch");
        }

        DomSignals s = parseSignals(domSignalsJson);
        if (s.hasHiddenPasswordField) {
            return new Verdict(false, "Autofill blocked: hidden password field detected");
        }
        if (s.passwordFieldCount == 0) {
            return new Verdict(false, "Autofill blocked: no password field on page");
        }
        if (s.hasIframePasswordField) {
            return new Verdict(false, "Autofill blocked: password field in iframe");
        }
        if (s.hasSuspiciousKeywords) {
            return new Verdict(false, "Autofill blocked: suspicious phishing keywords found");
        }
        if (s.formActionHost != null && !sameRegistrableDomain(s.formActionHost, savedHost)) {
            return new Verdict(false, "Autofill blocked: form action domain mismatch");
        }

        return new Verdict(true, "Autofill allowed");
    }

    private DomSignals parseSignals(String json) {
        DomSignals out = new DomSignals();
        try {
            if (json == null || json.isEmpty()) return out;
            JSONObject o = new JSONObject(json);
            out.hasHiddenPasswordField = o.optBoolean("hasHiddenPasswordField", false);
            out.passwordFieldCount = o.optInt("passwordFieldCount", 0);
            out.hasIframePasswordField = o.optBoolean("hasIframePasswordField", false);
            out.hasSuspiciousKeywords = o.optBoolean("hasSuspiciousKeywords", false);
            String host = o.optString("formActionHost", "").toLowerCase(Locale.ROOT).trim();
            out.formActionHost = host.isEmpty() ? null : host;
        } catch (Exception ignore) {
            // Invalid JSON -> keep defaults and let upper checks block missing password fields.
        }
        return out;
    }

    private static class DomSignals {
        boolean hasHiddenPasswordField;
        int passwordFieldCount;
        boolean hasIframePasswordField;
        boolean hasSuspiciousKeywords;
        String formActionHost;
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
