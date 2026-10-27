package com.duma.browser;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.EditText;
import android.widget.Button;

public class MainActivity extends Activity {
    WebView webView;
    EditText urlBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        urlBar = new EditText(this);
        urlBar.setHint("Browse The African Way - Enter URL");
        urlBar.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        urlBar.setText("https://www.google.com");
        Button goBtn = new Button(this);
        goBtn.setText("GO");
        top.addView(urlBar);
        top.addView(goBtn);
        
        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("https://www.google.com");
        
        goBtn.setOnClickListener(v -> {
            String url = urlBar.getText().toString();
            if(!url.startsWith("http")) url = "https://" + url;
            webView.loadUrl(url);
        });
        
        root.addView(top);
        root.addView(webView, new LinearLayout.LayoutParams(-1, -1, 1));
        setContentView(root);
    }
    @Override
    public void onBackPressed() {
        if(webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}