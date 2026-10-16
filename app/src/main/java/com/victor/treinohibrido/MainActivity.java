package com.victor.treinohibrido;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String DEFAULT_URL = "https://script.google.com/macros/s/AKfycbw_6aLH5qbvE6zWMpbAj2BgISczuzo07RYvU-E06nD9iUpZH5jTaJMet1617v_ZxRh5OA/exec";
    private static final String PREFS = "treino_hibrido_prefs";
    private static final String PREF_URL = "webapp_url";

    private WebView webView;
    private ProgressBar progressBar;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        getWindow().setStatusBarColor(Color.rgb(9, 14, 26));
        getWindow().setNavigationBarColor(Color.rgb(9, 14, 26));

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(9, 14, 26));

        webView = new WebView(this);
        configureWebView(webView);
        root.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(3));
        progressParams.gravity = Gravity.TOP;
        root.addView(progressBar, progressParams);

        Button settings = new Button(this);
        settings.setText("⚙");
        settings.setTextSize(20);
        settings.setTextColor(Color.WHITE);
        settings.setAllCaps(false);
        settings.setPadding(0, 0, 0, 2);
        GradientDrawable gearBg = new GradientDrawable();
        gearBg.setColor(Color.argb(215, 76, 29, 149));
        gearBg.setShape(GradientDrawable.OVAL);
        settings.setBackground(gearBg);
        settings.setContentDescription("Configurações");
        settings.setOnClickListener(v -> showSettings());
        FrameLayout.LayoutParams gearParams = new FrameLayout.LayoutParams(dp(44), dp(44));
        gearParams.gravity = Gravity.TOP | Gravity.END;
        gearParams.topMargin = dp(10);
        gearParams.rightMargin = dp(10);
        root.addView(settings, gearParams);

        setContentView(root);

        if (savedInstanceState == null || webView.restoreState(savedInstanceState) == null) {
            webView.loadUrl(getConfiguredUrl());
        }
    }

    private void configureWebView(WebView view) {
        WebSettings s = view.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setLoadsImagesAutomatically(true);
        s.setMediaPlaybackRequiresUserGesture(true);
        s.setSupportMultipleWindows(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(view, true);

        view.addJavascriptInterface(new NativeCache(this), "NativeCache");

        view.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                progressBar.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }
        });

        view.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return false;
                Uri uri = request.getUrl();
                String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
                if (host.endsWith("script.google.com") ||
                    host.endsWith("script.googleusercontent.com") ||
                    host.endsWith("googleusercontent.com")) {
                    return false;
                }
                openExternal(uri);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectPersistenceBridge();
            }
        });
    }

    private void openExternal(Uri uri) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Não encontrei app para abrir o link.", Toast.LENGTH_SHORT).show();
        }
    }

    private String getConfiguredUrl() {
        return prefs.getString(PREF_URL, DEFAULT_URL);
    }

    private void showSettings() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        box.setPadding(pad, dp(8), pad, 0);

        TextView label = new TextView(this);
        label.setText("URL do Web App");
        label.setTextSize(14);
        label.setTextColor(Color.DKGRAY);
        box.addView(label);

        EditText input = new EditText(this);
        input.setSingleLine(false);
        input.setMaxLines(4);
        input.setText(getConfiguredUrl());
        input.setSelectAllOnFocus(false);
        box.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Configurações")
                .setView(box)
                .setPositiveButton("Salvar e abrir", null)
                .setNeutralButton("Restaurar padrão", null)
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String url = input.getText().toString().trim();
                if (!isValidWebUrl(url)) {
                    input.setError("Use uma URL https:// válida");
                    return;
                }
                prefs.edit().putString(PREF_URL, url).apply();
                dialog.dismiss();
                webView.loadUrl(url);
                Toast.makeText(this, "URL salva", Toast.LENGTH_SHORT).show();
            });
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                prefs.edit().remove(PREF_URL).apply();
                input.setText(DEFAULT_URL);
                webView.loadUrl(DEFAULT_URL);
                Toast.makeText(this, "URL padrão restaurada", Toast.LENGTH_SHORT).show();
            });
        });
        dialog.show();
    }

    private boolean isValidWebUrl(String value) {
        try {
            Uri uri = Uri.parse(value);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null;
        } catch (Exception e) {
            return false;
        }
    }

    private void injectPersistenceBridge() {
        webView.evaluateJavascript(buildPersistenceScript(), null);
    }

    private String buildPersistenceScript() {
        return "(function(){" +
            "if(window.__treinoNativePersistV2)return;window.__treinoNativePersistV2=true;" +
            "function nk(id){return 'field:'+id;}" +
            "function bind(el){if(!el||!el.id||el.dataset.nativePersist==='1')return;" +
                "el.dataset.nativePersist='1';" +
                "try{var v=NativeCache.get(nk(el.id));if((el.value===''||el.value==null)&&v!=='')el.value=v;}catch(e){}" +
                "el.addEventListener('input',function(){try{NativeCache.put(nk(el.id),String(el.value==null?'':el.value));}catch(e){}});" +
            "}" +
            "function fromSheets(){try{" +
                "if(typeof STATE==='undefined'||!STATE.data||!STATE.currentSession||!STATE.currentWorkout||typeof safeId!=='function')return;" +
                "var logs=STATE.data.logs||[], exs=(STATE.data.exercises||[]).filter(function(e){return e.workout_id===STATE.currentWorkout.workout_id;});" +
                "exs.forEach(function(ex){var sets=Math.max(1,parseInt(ex.sets||'1',10)||1);for(var i=1;i<=sets;i++){" +
                    "var key=safeId(STATE.currentSession.session_key+'-'+ex.exercise_order+'-'+i);" +
                    "var log=logs.find(function(l){return String(l.week_key||'')===String(STATE.weekKey||'')&&String(l.session_key||'')===String(STATE.currentSession.session_key||'')&&String(l.workout_id||'')===String(STATE.currentWorkout.workout_id||'')&&String(l.exercise_name||'')===String(ex.name||'')&&Number(l.set_number||0)===Number(i);});" +
                    "if(log){[['-kg','load_kg'],['-reps','reps'],['-rir','rir']].forEach(function(p){var el=document.getElementById(key+p[0]);if(el&&(el.value===''||el.value==null)){var nv='';try{nv=NativeCache.get(nk(el.id));}catch(e){}if(nv!=='')el.value=nv;else if(log[p[1]]!==null&&log[p[1]]!==undefined)el.value=log[p[1]];}});var b=document.getElementById(key+'-save');if(b)b.classList.add('saved');}" +
                "}});" +
            "}catch(e){}}" +
            "function scan(){document.querySelectorAll('input,textarea,select').forEach(bind);fromSheets();}" +
            "scan();new MutationObserver(scan).observe(document.documentElement,{childList:true,subtree:true});setInterval(scan,1200);" +
        "})();";
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        CookieManager.getInstance().flush();
        webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    public static class NativeCache {
        private final SharedPreferences prefs;
        NativeCache(Context context) {
            prefs = context.getSharedPreferences("treino_field_cache", Context.MODE_PRIVATE);
        }
        @JavascriptInterface
        public void put(String key, String value) {
            prefs.edit().putString(key, value == null ? "" : value).apply();
        }
        @JavascriptInterface
        public String get(String key) {
            return prefs.getString(key, "");
        }
        @JavascriptInterface
        public void remove(String key) {
            prefs.edit().remove(key).apply();
        }
    }
}
