package narek.hakobyan.mypassword;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

public class Webviewautologinactivity extends AppCompatActivity {

    public static final String EXTRA_URL      = "extra_url";
    public static final String EXTRA_LOGIN    = "extra_login";
    public static final String EXTRA_PASSWORD = "extra_password";

    private WebView     webView;
    private ProgressBar progressBar;
    private String      login;
    private String      password;
    private String      trustedUrl;

    private final PhishingProtectionManager phishingProtectionManager =
            new PhishingProtectionManager();

    public static void launch(Context context, String url, String login, String password) {
        Intent intent = new Intent(context, Webviewautologinactivity.class);
        intent.putExtra(EXTRA_URL,      normaliseUrl(url));
        intent.putExtra(EXTRA_LOGIN,    login);
        intent.putExtra(EXTRA_PASSWORD, password);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SecureScreenUtils.apply(this);
        setContentView(R.layout.activity_webview_autologin);

        String url = getIntent().getStringExtra(EXTRA_URL);
        trustedUrl = url;
        login      = getIntent().getStringExtra(EXTRA_LOGIN);
        password   = getIntent().getStringExtra(EXTRA_PASSWORD);

        if (url == null || url.isEmpty()) {
            Toast.makeText(this, "URL не указан", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressBar = findViewById(R.id.webViewProgress);
        webView     = findViewById(R.id.webView);

        configureWebView();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (webView != null && webView.canGoBack()) webView.goBack();
                else finish();
            }
        });

        webView.loadUrl(url);
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
        // Zero-out secrets in memory
        login    = null;
        password = null;
        super.onDestroy();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        // SECURITY: never allow mixed content (HTTP resources on an HTTPS page).
        // MIXED_CONTENT_ALWAYS_ALLOW was removed — it leaks credentials over HTTP.
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        // Disable saving form data / passwords in the WebView itself
        settings.setSaveFormData(false);
        settings.setSavePassword(false);

        webView.setWebViewClient(new AutoLoginWebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                progressBar.setVisibility(newProgress < 100 ? View.VISIBLE : View.GONE);
            }
        });
    }

    // ── autofill ────────────────────────────────────────────────────────────

    private void injectAutoFillScript() {
        String safeLogin    = escapeForJs(login    != null ? login    : "");
        String safePassword = escapeForJs(password != null ? password : "");

        String js =
                "(function() {\n"
                        + "  var usernameSelectors = [\n"
                        + "    'input[autocomplete=\"username\"]',\n"
                        + "    'input[autocomplete=\"email\"]',\n"
                        + "    'input[type=\"email\"]',\n"
                        + "    'input[name*=\"user\"]',\n"
                        + "    'input[name*=\"email\"]',\n"
                        + "    'input[name*=\"login\"]',\n"
                        + "    'input[id*=\"user\"]',\n"
                        + "    'input[id*=\"email\"]',\n"
                        + "    'input[id*=\"login\"]',\n"
                        + "    'input[type=\"text\"]'\n"
                        + "  ];\n"
                        + "  var usernameField = null;\n"
                        + "  for (var i = 0; i < usernameSelectors.length; i++) {\n"
                        + "    var el = document.querySelector(usernameSelectors[i]);\n"
                        + "    if (el) { usernameField = el; break; }\n"
                        + "  }\n"
                        + "  var passwordField = document.querySelector('input[type=\"password\"]');\n"
                        + "  function fillField(field, value) {\n"
                        + "    if (!field) return;\n"
                        + "    var setter = Object.getOwnPropertyDescriptor(\n"
                        + "      window.HTMLInputElement.prototype, 'value').set;\n"
                        + "    setter.call(field, value);\n"
                        + "    field.dispatchEvent(new Event('input',  { bubbles: true }));\n"
                        + "    field.dispatchEvent(new Event('change', { bubbles: true }));\n"
                        + "  }\n"
                        + "  fillField(usernameField, '" + safeLogin    + "');\n"
                        + "  fillField(passwordField, '" + safePassword + "');\n"
                        + "  var btn = document.querySelector(\n"
                        + "    'button[type=\"submit\"], input[type=\"submit\"]');\n"
                        + "  if (btn) btn.focus();\n"
                        + "  return 'ok';\n"
                        + "})();";

        webView.evaluateJavascript(js, result -> {});
    }

    private void verifyAndAutofill(String currentUrl) {
        String domProbeJs =
                "(function(){"
                        + "var p=document.querySelectorAll('input[type=\\\"password\\\"]');"
                        + "var hidden=false;"
                        + "for(var i=0;i<p.length;i++){var s=getComputedStyle(p[i]);"
                        + "if(s.display==='none'||s.visibility==='hidden'||s.opacity==='0'"
                        + "||p[i].offsetParent===null){hidden=true;break;}}"
                        + "var ifr=false;try{ifr=window.top!==window.self;}catch(e){ifr=true;}"
                        + "var t=(document.body&&document.body.innerText?document.body.innerText:'').toLowerCase();"
                        + "var suspicious=/verify.{0,20}account|urgent|suspend|confirm.{0,20}password|wallet|seed phrase/.test(t);"
                        + "var actionHost='';"
                        + "if(document.forms.length>0&&document.forms[0].action){"
                        + "try{actionHost=(new URL(document.forms[0].action,location.href)).host.toLowerCase();}catch(e){}}"
                        + "return JSON.stringify({"
                        + "passwordFieldCount:p.length,"
                        + "hasHiddenPasswordField:hidden,"
                        + "hasIframePasswordField:ifr,"
                        + "hasSuspiciousKeywords:suspicious,"
                        + "formActionHost:actionHost});"
                        + "})();";

        webView.evaluateJavascript(domProbeJs, raw -> {
            String domSignals = unquoteJsResult(raw);
            PhishingProtectionManager.Verdict verdict =
                    phishingProtectionManager.verifyAutofillSafety(
                            currentUrl, trustedUrl, domSignals);
            if (!verdict.allowAutofill) {
                Toast.makeText(this, verdict.reason, Toast.LENGTH_LONG).show();
                return;
            }
            injectAutoFillScript();
        });
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private String unquoteJsResult(String raw) {
        if (raw == null) return "";
        String value = raw;
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        return value.replace("\\\\", "\\").replace("\\\"", "\"");
    }

    private static String escapeForJs(String raw) {
        return raw
                .replace("\\",  "\\\\")
                .replace("'",   "\\'")
                .replace("\"",  "\\\"")
                .replace("\n",  "\\n")
                .replace("\r",  "\\r")
                .replace("</",  "<\\/");
    }

    private static String normaliseUrl(String url) {
        if (url == null || url.isEmpty()) return "";
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "https://" + url;
        }
        return url;
    }

    // ── WebViewClient ────────────────────────────────────────────────────────

    private class AutoLoginWebViewClient extends WebViewClient {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            progressBar.setVisibility(View.GONE);
            verifyAndAutofill(url);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            // Let the WebView handle all navigation internally
            return false;
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            // Cancel SSL errors — never proceed on a broken certificate
            handler.cancel();
            Toast.makeText(Webviewautologinactivity.this,
                    "Автозаполнение заблокировано: ошибка SSL-сертификата",
                    Toast.LENGTH_LONG).show();
        }
    }
}