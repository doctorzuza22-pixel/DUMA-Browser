package com.duma.browser;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.webkit.*;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private WebView webView;
    private EditText addressBar;
    private ProgressBar progress;
    private TextView status;
    private SharedPreferences prefs;
    private boolean dataSaver;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("duma", MODE_PRIVATE);
        dataSaver = prefs.getBoolean("data_saver", false);
        buildBrowser();
        showHome();
    }

    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }

    private void buildBrowser() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(245,234,210));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(10),dp(7),dp(10),dp(7));
        header.setBackgroundColor(Color.rgb(18,60,43));

        TextView brand = new TextView(this);
        brand.setText("DUMA");
        brand.setTextColor(Color.WHITE);
        brand.setTextSize(20);
        brand.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        brand.setPadding(0,0,dp(8),0);
        header.addView(brand);

        addressBar = new EditText(this);
        addressBar.setSingleLine(true);
        addressBar.setTextSize(14);
        addressBar.setHint("Search Africa... or the whole world");
        addressBar.setHintTextColor(Color.LTGRAY);
        addressBar.setTextColor(Color.WHITE);
        addressBar.setSelectAllOnFocus(true);
        addressBar.setPadding(dp(12),0,dp(12),0);
        addressBar.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(224,173,53)));
        addressBar.setImeOptions(EditorInfo.IME_ACTION_GO);
        header.addView(addressBar,new LinearLayout.LayoutParams(0,dp(42),1));
        Button go = new Button(this); go.setText("GO"); go.setTextSize(12);
        go.setTextColor(Color.WHITE); go.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(183,43,53)));
        header.addView(go);
        go.setOnClickListener(v -> navigate(addressBar.getText().toString()));
        addressBar.setOnEditorActionListener((v,id,event)->{ if(id==EditorInfo.IME_ACTION_GO||id==EditorInfo.IME_ACTION_SEARCH){navigate(addressBar.getText().toString());return true;}return false;});
        root.addView(header);

        progress = new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100); progress.setProgress(0);
        progress.setProgressTintList(ColorStateList.valueOf(Color.rgb(183,43,53)));
        progress.setVisibility(View.GONE);
        root.addView(progress,new LinearLayout.LayoutParams(-1,dp(3)));

        status = new TextView(this); status.setText("DUMA is ready"); status.setTextSize(10);
        status.setTextColor(Color.rgb(183,43,53)); status.setPadding(dp(10),dp(2),dp(10),dp(2));
        status.setVisibility(View.GONE); root.addView(status);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(245,234,210));
        WebSettings s=webView.getSettings();
        s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setLoadsImagesAutomatically(!dataSaver);
        s.setSupportZoom(true); s.setBuiltInZoomControls(true); s.setDisplayZoomControls(false);
        s.setMediaPlaybackRequiresUserGesture(true);
        webView.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri u=req.getUrl();
                if("file".equals(u.getScheme())) return false;
                webView.removeJavascriptInterface("Android");
                return false;
            }
            @Override public void onPageStarted(WebView view,String url,Bitmap icon){
                progress.setVisibility(View.VISIBLE); progress.setProgress(8); status.setText("DUMA is fetching..."); status.setVisibility(View.VISIBLE);
                if(url!=null && !url.startsWith("file:") && !url.startsWith("data:")) addressBar.setText(cleanUrl(url));
            }
            @Override public void onPageFinished(WebView view,String url){
                progress.setProgress(100); progress.setVisibility(View.GONE); status.setVisibility(View.GONE);
                if(url!=null && url.startsWith("file:///android_asset/")) attachBridge();
                else if(url!=null && !url.startsWith("data:")) addressBar.setText(cleanUrl(url));
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request){
                if(dataSaver){
                    String u=request.getUrl().toString().toLowerCase();
                    if(u.matches(".*\\.(mp4|webm|m3u8|mov|avi)(\\?.*)?$"))
                        return new WebResourceResponse("text/plain","utf-8",new java.io.ByteArrayInputStream(new byte[0]));
                }
                return super.shouldInterceptRequest(view,request);
            }
        });
        webView.setWebChromeClient(new WebChromeClient(){
            @Override public void onProgressChanged(WebView view,int p){progress.setProgress(p); if(p>0&&p<100){status.setText("DUMA is fetching...");status.setVisibility(View.VISIBLE);}else if(p==100){progress.setVisibility(View.GONE);status.setVisibility(View.GONE);}}
        });
        root.addView(webView,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout bottom=new LinearLayout(this); bottom.setGravity(Gravity.CENTER); bottom.setBackgroundColor(Color.rgb(18,60,43));
        addBottom(bottom,"HOME",this::showHome); addBottom(bottom,"AI",this::showAi); addBottom(bottom,"NEW JOURNEY",this::newJourney); addBottom(bottom,"SAVED",this::showSavedMenu); addBottom(bottom,"MENU",this::showMenu);
        root.addView(bottom);
        setContentView(root);
    }

    private void addBottom(LinearLayout bar,String label,Runnable action){
        Button b=new Button(this);b.setText(label);b.setTextSize(10);b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(18,60,43)));
        bar.addView(b,new LinearLayout.LayoutParams(0,dp(52),1));b.setOnClickListener(v->action.run());
    }
    private String cleanUrl(String raw){try{Uri u=Uri.parse(raw);String h=u.getHost();return h==null?raw:h+(u.getPath()!=null&&!u.getPath().equals("/")?u.getPath():"");}catch(Exception e){return raw;}}
    private void attachBridge(){webView.addJavascriptInterface(new DumaBridge(),"Android");}
    private void showHome(){attachBridge();addressBar.setText("");webView.loadUrl("file:///android_asset/home.html");}
    private void newJourney(){showHome();}
    private void navigate(String input){
        String v=input==null?"":input.trim();if(v.isEmpty())return;
        String target;
        if(v.matches("(?i)^https?://.*"))target=v;
        else if(v.matches("(?i)^[a-z0-9.-]+\\.[a-z]{2,}(/.*)?$"))target="https://"+v;
        else target="https://duckduckgo.com/?q="+Uri.encode(v);
        webView.removeJavascriptInterface("Android");
        webView.loadUrl(target);
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(addressBar.getWindowToken(),0);
        addressBar.clearFocus();
    }
    private void showAi(){
        if(webView.getUrl()!=null && webView.getUrl().startsWith("file:")){new AlertDialog.Builder(this).setTitle("DUMA AI").setMessage("Open an article first, then use DUMA AI to summarise or translate it. For questions, use the search bar.").setPositiveButton("OK",null).show();return;}
        new AlertDialog.Builder(this).setTitle("DUMA AI").setItems(new String[]{"Summarise this page","Translate this page","Ask a question"},(d,w)->{
            if(w==0)summarisePage();
            if(w==1)chooseTranslation();
            if(w==2){EditText q=new EditText(this);q.setHint("Ask DUMA...");new AlertDialog.Builder(this).setTitle("Ask DUMA").setView(q).setPositiveButton("Search",(x,y)->navigate(q.getText().toString())).setNegativeButton("Cancel",null).show();}
        }).show();
    }
    private void summarisePage(){
        webView.evaluateJavascript("(function(){var t=(document.body&&document.body.innerText)||'';return JSON.stringify({title:document.title,text:t.slice(0,12000)});})()",value->{
            try{String decoded=org.json.JSONTokener.stringToValue(value).toString();JSONObject o=new JSONObject(decoded);String text=o.optString("text");String[] sentences=text.split("(?<=[.!?])\\s+");StringBuilder sum=new StringBuilder();for(int i=0;i<Math.min(4,sentences.length);i++){if(sentences[i].trim().length()>25)sum.append(sentences[i].trim()).append("\n\n");}
                new AlertDialog.Builder(this).setTitle("DUMA AI · Quick summary").setMessage(sum.length()==0?"There isn't enough readable text on this page to summarise.":sum.toString()).setPositiveButton("Done",null).show();
            }catch(Exception e){new AlertDialog.Builder(this).setTitle("DUMA AI").setMessage("Could not read this page.").setPositiveButton("OK",null).show();}
        });
    }
    private void chooseTranslation(){
        String[] names={"isiZulu","isiXhosa","Sesotho","Setswana","Sepedi","Afrikaans","Shona","Swahili","Yoruba","French"};
        String[] codes={"zu","xh","st","tn","nso","af","sn","sw","yo","fr"};
        new AlertDialog.Builder(this).setTitle("Translate page").setItems(names,(d,w)->{
            String url="https://translate.google.com/translate?sl=auto&tl="+codes[w]+"&u="+Uri.encode(webView.getUrl());
            webView.removeJavascriptInterface("Android");webView.loadUrl(url);
        }).show();
    }
    private void showMenu(){
        String[] items={"Back","Forward","Reload","Data Saver: "+(dataSaver?"ON":"OFF"),"Loadshedding Mode","Settings"};
        new AlertDialog.Builder(this).setTitle("DUMA Menu").setItems(items,(d,w)->{
            if(w==0&&webView.canGoBack())webView.goBack();
            if(w==1&&webView.canGoForward())webView.goForward();
            if(w==2)webView.reload();
            if(w==3){dataSaver=!dataSaver;prefs.edit().putBoolean("data_saver",dataSaver).apply();webView.getSettings().setLoadsImagesAutomatically(!dataSaver);new AlertDialog.Builder(this).setMessage("Data Saver "+(dataSaver?"enabled":"disabled")+". Large video files are blocked and automatic images are disabled when enabled.").setPositiveButton("OK",null).show();}
            if(w==4)webView.evaluateJavascript("document.body.dataset.theme='night';localStorage.setItem('theme','night')",null);
            if(w==5)showHome();
        }).show();
    }
    private void showSavedMenu(){
        String[] options={"Save this page for offline reading","Open saved articles"};
        new AlertDialog.Builder(this).setTitle("DUMA Saved").setItems(options,(d,w)->{if(w==0)saveCurrentPage();else showSaved();}).show();
    }
    private void showSaved(){
        attachBridge();
        webView.loadDataWithBaseURL("file:///android_asset/",savedHtml(),"text/html","UTF-8",null);
    }
    private String savedHtml(){
        JSONArray a=savedArray();StringBuilder h=new StringBuilder("<html><meta name='viewport' content='width=device-width, initial-scale=1'><body style='font-family:Arial;background:#f5ead2;padding:18px;color:#222'><h2>📚 DUMA SAVED</h2><p>Your saved reading, available offline.</p>");
        if(a.length()==0)h.append("<p>No saved articles yet. Open an article and tap SAVED.</p>");
        for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);h.append("<p style='background:#fff8e9;padding:14px;border-radius:12px'><b>").append(escape(o.optString("title"))).append("</b><br><small>").append(escape(o.optString("url"))).append("</small><br><a href='#' onclick='Android.openSaved(").append(i).append(");return false;'>Read offline</a> · <a href='#' onclick='Android.deleteSaved(").append(i).append(");return false;'>Delete</a></p>");}
        h.append("<p><a href='file:///android_asset/home.html'>Home</a></p></body></html>");return h.toString();
    }
    private String escape(String s){return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
    private JSONArray savedArray(){try{return new JSONArray(prefs.getString("saved","[]"));}catch(Exception e){return new JSONArray();}}
    private void saveCurrentPage(){
        if(webView.getUrl()==null||webView.getUrl().startsWith("file:")){new AlertDialog.Builder(this).setMessage("Open an article first, then tap SAVED.").setPositiveButton("OK",null).show();return;}
        webView.evaluateJavascript("(function(){return JSON.stringify({title:document.title,url:location.href,text:(document.body&&document.body.innerText||'').slice(0,20000)});})()",value->{
            try{String raw=org.json.JSONTokener.stringToValue(value).toString();JSONObject item=new JSONObject(raw);JSONArray a=savedArray();a.put(item);prefs.edit().putString("saved",a.toString()).apply();new AlertDialog.Builder(this).setTitle("Saved offline").setMessage("This article text is saved on this device and can be read without data.").setPositiveButton("OK",null).show();}
            catch(Exception e){new AlertDialog.Builder(this).setMessage("Could not save this page.").setPositiveButton("OK",null).show();}
        });
    }
    private class DumaBridge{
        @JavascriptInterface public void openUrl(String url){runOnUiThread(()->navigate(url));}
        @JavascriptInterface public void setDataSaver(boolean on){runOnUiThread(()->{dataSaver=on;prefs.edit().putBoolean("data_saver",on).apply();webView.getSettings().setLoadsImagesAutomatically(!on);});}
        @JavascriptInterface public String getSavedArticles(){return savedArray().toString();}
        @JavascriptInterface public void openSaved(int index){runOnUiThread(()->openSavedArticle(index));}
        @JavascriptInterface public void deleteSaved(int index){runOnUiThread(()->{JSONArray old=savedArray();JSONArray next=new JSONArray();for(int i=0;i<old.length();i++)if(i!=index)next.put(old.opt(i));prefs.edit().putString("saved",next.toString()).apply();showSaved();});}
        @JavascriptInterface public void summarisePage(){runOnUiThread(()->summarisePage());}
        @JavascriptInterface public void translateCurrentPage(String lang){runOnUiThread(()->{String u="https://translate.google.com/translate?sl=auto&tl="+Uri.encode(lang)+"&u="+Uri.encode(webView.getUrl());navigate(u);});}
    }
    private void openSavedArticle(int index){
        JSONArray a=savedArray();JSONObject o=a.optJSONObject(index);if(o==null)return;
        String title=escape(o.optString("title"));String text=escape(o.optString("text")).replace("\n","<br>");
        String page="<html><meta name='viewport' content='width=device-width, initial-scale=1'><body style='font:16px/1.6 Arial;background:#fff8e9;color:#222;padding:20px'><small> DUMA · SAVED OFFLINE</small><h2>"+title+"</h2><p>"+text+"</p></body></html>";
        webView.loadDataWithBaseURL(null,page,"text/html","UTF-8",null);
    }
    @Override public void onBackPressed(){if(webView!=null&&webView.canGoBack())webView.goBack();else showHome();}
}