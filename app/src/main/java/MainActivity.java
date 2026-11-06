package com.duma.browser;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private WebView webView;
    private EditText addressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildBrowser();
        showHome();
    }

    private void buildBrowser() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(12), dp(10), dp(12), dp(10));
        header.setBackgroundColor(Color.rgb(18, 18, 18));

        TextView brand = new TextView(this);
        brand.setText("DUMA");
        brand.setTextColor(Color.WHITE);
        brand.setTextSize(21);
        brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        brand.setPadding(0, 0, dp(10), 0);
        header.addView(brand);

        addressBar = new EditText(this);
        addressBar.setSingleLine(true);
        addressBar.setTextSize(14);
        addressBar.setHint("Search or enter website");
        addressBar.setHintTextColor(Color.LTGRAY);
        addressBar.setTextColor(Color.WHITE);
        addressBar.setSelectAllOnFocus(true);
        addressBar.setPadding(dp(12), 0, dp(12), 0);
        addressBar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(210, 210, 210)));
        addressBar.setImeOptions(EditorInfo.IME_ACTION_GO);
        header.addView(addressBar, new LinearLayout.LayoutParams(0, dp(44), 1));

        Button go = new Button(this);
        go.setText("GO");
        go.setTextColor(Color.WHITE);
        go.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(190, 30, 45)));
        header.addView(go);
        go.setOnClickListener(v -> navigate(addressBar.getText().toString()));
        addressBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEARCH) {
                navigate(addressBar.getText().toString());
                return true;
            }
            return false;
        });

        root.addView(header);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER);
        toolbar.setBackgroundColor(Color.rgb(242, 242, 242));
        addTool(toolbar, "HOME", this::showHome);
        addTool(toolbar, "BACK", () -> { if (webView.canGoBack()) webView.goBack(); });
        addTool(toolbar, "FORWARD", () -> { if (webView.canGoForward()) webView.goForward(); });
        addTool(toolbar, "RELOAD", () -> webView.reload());
        root.addView(toolbar);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.WHITE);
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                if (url != null && !url.startsWith("file:") && !url.startsWith("data:")) addressBar.setText(url);
            }
        });
        webView.setWebChromeClient(new WebChromeClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setLoadsImagesAutomatically(true);
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        root.addView(webView, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private void addTool(LinearLayout bar, String label, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(11);
        button.setTextColor(Color.rgb(35, 35, 35));
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(242, 242, 242)));
        bar.addView(button, new LinearLayout.LayoutParams(0, dp(46), 1));
        button.setOnClickListener(v -> action.run());
    }

    private void navigate(String input) {
        String value = input == null ? "" : input.trim();
        if (value.isEmpty()) return;
        String target;
        if (value.matches("(?i)^https?://.*")) target = value;
        else if (value.matches("(?i)^[a-z0-9.-]+\\.[a-z]{2,}(/.*)?$")) target = "https://" + value;
        else target = "https://www.google.com/search?q=" + android.net.Uri.encode(value);
        webView.loadUrl(target);
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
            .hideSoftInputFromWindow(addressBar.getWindowToken(), 0);
        addressBar.clearFocus();
    }

    private void showHome() {
        addressBar.setText("");
        String home = "<html><head><meta name='viewport' content='width=device-width, initial-scale=1'><style>"
            + "body{margin:0;background:#f5f5f5;color:#171717;font-family:Arial;text-align:center}"
            + ".top{background:#111;color:white;padding:48px 16px 38px}.logo{font-size:48px;font-weight:900;letter-spacing:3px}"
            + ".logo span{color:#c51f32}.tag{color:#ddd;margin-top:8px;font-size:14px}"
            + ".search{display:flex;margin:28px 18px 18px}input{flex:1;min-width:0;padding:15px;border:1px solid #ccc;border-radius:8px 0 0 8px;font-size:16px}"
            + "button{padding:0 18px;border:0;background:#c51f32;color:white;border-radius:0 8px 8px 0;font-weight:bold}"
            + ".links{margin:28px auto;display:flex;justify-content:center;gap:12px;flex-wrap:wrap}.links a{color:#222;text-decoration:none;background:white;padding:12px 16px;border-radius:8px;border:1px solid #eee}"
            + "footer{color:#777;font-size:12px;margin-top:55px}</style></head><body>"
            + "<div class='top'><div class='logo'>DU<span>MA</span></div><div class='tag'>BROWSE THE AFRICAN WAY</div></div>"
            + "<form class='search' onsubmit='event.preventDefault();AndroidSearch();'><input id='q' placeholder='Search the web or enter a website'><button type='submit'>SEARCH</button></form>"
            + "<div class='links'><a href='https://www.google.com'>Google</a><a href='https://www.youtube.com'>YouTube</a><a href='https://www.facebook.com'>Facebook</a><a href='https://www.wikipedia.org'>Wikipedia</a></div>"
            + "<footer>DUMA BROWSER · MADE FOR EVERYDAY BROWSING</footer>"
            + "<script>function AndroidSearch(){var q=document.getElementById('q').value;window.location='https://www.google.com/search?q='+encodeURIComponent(q);}</script>"
            + "</body></html>";
        webView.loadDataWithBaseURL("https://duma.local/", home, "text/html", "UTF-8", null);
    }

    private int dp(int value) { return (int)(value * getResources().getDisplayMetrics().density + 0.5f); }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else showHome();
    }
}
