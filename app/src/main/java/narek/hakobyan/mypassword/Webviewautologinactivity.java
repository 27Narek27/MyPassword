package narek.hakobyan.mypassword;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
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
    private String login;
    private String password;

    public static void launch(Context context, String url, String login, String password) {
        Intent intent = new Intent(context, Webviewautologinactivity.class);
        intent.putExtra(EXTRA_URL,      normaliseUrl(url));
        intent.putExtra(EXTRA_LOGIN,    login);
        intent.putExtra(EXTRA_PASSWORD, password);
        context.startActivity(intent);
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview_autologin);

        String url = getIntent().getStringExtra(EXTRA_URL);
        login    = getIntent().getStringExtra(EXTRA_LOGIN);
        password = getIntent().getStringExtra(EXTRA_PASSWORD);

        if (url == null || url.isEmpty()) {
            Toast.makeText(this, "No URL provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressBar = findViewById(R.id.webViewProgress);
        webView     = findViewById(R.id.webView);

        configureWebView();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView != null && webView.canGoBack()) {
                    webView.goBack();
                } else {
                    finish();
                }
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
        login    = null;
        password = null;
        super.onDestroy();
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);


        webView.addJavascriptInterface(new AutoFillBridge(), "__AutoFillBridge__");
        webView.setWebViewClient(new AutoLoginWebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                progressBar.setVisibility(newProgress < 100 ? View.VISIBLE : View.GONE);
            }
        });
    }

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
                        + "    var nativeInputValueSetter = Object.getOwnPropertyDescriptor(\n"
                        + "      window.HTMLInputElement.prototype, 'value').set;\n"
                        + "    nativeInputValueSetter.call(field, value);\n"
                        + "    field.dispatchEvent(new Event('input',  { bubbles: true }));\n"
                        + "    field.dispatchEvent(new Event('change', { bubbles: true }));\n"
                        + "  }\n"
                        + "  fillField(usernameField, '" + safeLogin    + "');\n"
                        + "  fillField(passwordField, '" + safePassword + "');\n"
                        + "  var submitBtn = document.querySelector(\n"
                        + "    'button[type=\"submit\"], input[type=\"submit\"]');\n"
                        + "  if (submitBtn) submitBtn.focus();\n"
                        + "  return 'ok';\n"
                        + "})();";

        webView.evaluateJavascript(js, result -> {});
    }

    private static String escapeForJs(String raw) {
        return raw
                .replace("\\", "\\\\")
                .replace("'",  "\\'")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("</", "<\\/");
    }

    private static String normaliseUrl(String url) {
        if (url == null || url.isEmpty()) return "";
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "https://" + url;
        }
        return url;
    }

    private class AutoLoginWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            progressBar.setVisibility(View.GONE);
            injectAutoFillScript();
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return false;
        }
    }
    private static final class AutoFillBridge {


        @JavascriptInterface
        public String toString() {
            return "AutoFillBridge";
        }
    }
}