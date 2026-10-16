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
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String DEFAULT_URL = "https://script.google.com/macros/s/AKfycbw_6aLH5qbvE6zWMpbAj2BgISczuzo07RYvU-E06nD9iUpZH5jTaJMet1617v_ZxRh5OA/exec";
    private static final String PREFS = "treino_hibrido_prefs";
    private static final String PREF_URL = "webapp_url";
    private static final String PREF_WARM_CACHE = "warm_web_cache";

    private WebView webView;
    private ProgressBar progressBar;
    private FrameLayout loadingCover;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        getWindow().setStatusBarColor(Color.rgb(9, 14, 26));
        getWindow().setNavigationBarColor(Color.rgb(9, 14, 26));

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(9, 14, 26));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(9, 14, 26));
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

        loadingCover = createLoadingCover();
        root.addView(loadingCover, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        ImageButton settings = new ImageButton(this);
        settings.setImageResource(R.drawable.ic_settings);
        settings.setColorFilter(Color.WHITE);
        settings.setPadding(dp(10), dp(10), dp(10), dp(10));
        settings.setContentDescription("Configurações");
        GradientDrawable settingsBg = new GradientDrawable();
        settingsBg.setShape(GradientDrawable.OVAL);
        settingsBg.setColor(Color.argb(225, 34, 22, 63));
        settingsBg.setStroke(dp(1), Color.argb(160, 124, 58, 237));
        settings.setBackground(settingsBg);
        settings.setElevation(dp(6));
        settings.setOnClickListener(v -> showSettings());

        FrameLayout.LayoutParams settingsParams = new FrameLayout.LayoutParams(dp(42), dp(42));
        settingsParams.gravity = Gravity.TOP | Gravity.END;
        settingsParams.topMargin = dp(10);
        settingsParams.rightMargin = dp(10);
        root.addView(settings, settingsParams);

        setContentView(root);

        if (savedInstanceState == null || webView.restoreState(savedInstanceState) == null) {
            loadConfiguredUrl();
        } else {
            hideLoadingCover();
        }
    }

    private FrameLayout createLoadingCover() {
        FrameLayout cover = new FrameLayout(this);
        cover.setBackgroundColor(Color.rgb(9, 14, 26));
        cover.setClickable(true);

        LinearLayout center = new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER_HORIZONTAL);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_launcher_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        center.addView(logo, new LinearLayout.LayoutParams(dp(112), dp(112)));

        TextView label = new TextView(this);
        label.setText("Treino Híbrido");
        label.setTextColor(Color.WHITE);
        label.setTextSize(20);
        label.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        labelParams.topMargin = dp(14);
        center.addView(label, labelParams);

        TextView hint = new TextView(this);
        hint.setText("Carregando treino…");
        hint.setTextColor(Color.rgb(161, 161, 170));
        hint.setTextSize(13);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        hintParams.topMargin = dp(6);
        center.addView(hint, hintParams);

        FrameLayout.LayoutParams centerParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        centerParams.gravity = Gravity.CENTER;
        cover.addView(center, centerParams);
        return cover;
    }

    private void showLoadingCover() {
        if (loadingCover == null) return;
        loadingCover.animate().cancel();
        loadingCover.setAlpha(1f);
        loadingCover.setVisibility(View.VISIBLE);
    }

    private void hideLoadingCover() {
        if (loadingCover == null || loadingCover.getVisibility() != View.VISIBLE) return;
        loadingCover.animate()
                .alpha(0f)
                .setDuration(180)
                .withEndAction(() -> loadingCover.setVisibility(View.GONE))
                .start();
    }

    private void configureWebView(WebView view) {
        WebSettings s = view.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setCacheMode(prefs.getBoolean(PREF_WARM_CACHE, false)
                ? WebSettings.LOAD_CACHE_ELSE_NETWORK
                : WebSettings.LOAD_DEFAULT);
        s.setLoadsImagesAutomatically(true);
        s.setMediaPlaybackRequiresUserGesture(true);
        s.setSupportMultipleWindows(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setTextZoom(100);
        s.setOffscreenPreRaster(true);

        view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        view.setVerticalScrollBarEnabled(false);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(view, true);

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
                Uri uri = request.getUrl();
                String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
                if ("http".equals(scheme) || "https".equals(scheme)) {
                    return false;
                }
                openExternal(uri);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectPersistenceBridge();
                CookieManager.getInstance().flush();
                prefs.edit().putBoolean(PREF_WARM_CACHE, true).apply();
                view.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
                hideLoadingCover();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) {
                    hideLoadingCover();
                    Toast.makeText(MainActivity.this,
                            "Não consegui carregar o treino. Verifique sua conexão.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadConfiguredUrl() {
        showLoadingCover();
        boolean warm = prefs.getBoolean(PREF_WARM_CACHE, false);
        webView.getSettings().setCacheMode(warm
                ? WebSettings.LOAD_CACHE_ELSE_NETWORK
                : WebSettings.LOAD_DEFAULT);
        webView.loadUrl(getConfiguredUrl());
    }

    private void loadFresh(String url) {
        prefs.edit().putBoolean(PREF_WARM_CACHE, false).apply();
        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        showLoadingCover();
        webView.stopLoading();
        webView.loadUrl(url);
    }

    private void openExternal(Uri uri) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Não encontrei um app para abrir esse link.", Toast.LENGTH_SHORT).show();
        }
    }

    private String getConfiguredUrl() {
        String saved = prefs.getString(PREF_URL, DEFAULT_URL);
        return saved == null || saved.trim().isEmpty() ? DEFAULT_URL : saved.trim();
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
        box.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Treino Híbrido")
                .setMessage("Troque a URL somente se você publicar o Web App em outro deployment.")
                .setView(box)
                .setPositiveButton("Salvar e recarregar", null)
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
                loadFresh(url);
                Toast.makeText(this, "URL salva", Toast.LENGTH_SHORT).show();
            });
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                prefs.edit().remove(PREF_URL).apply();
                input.setText(DEFAULT_URL);
                loadFresh(DEFAULT_URL);
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
                "if(window.__treinoNativePersistV3)return;window.__treinoNativePersistV3=true;" +
                "function nk(id){return 'field:'+id;}" +
                "function bind(el){if(!el||!el.id||el.dataset.nativePersist==='1')return;" +
                "el.dataset.nativePersist='1';" +
                "try{var v=NativeCache.get(nk(el.id));if((el.value===''||el.value==null)&&v!=='')el.value=v;}catch(e){}" +
                "function save(){try{NativeCache.put(nk(el.id),String(el.value==null?'':el.value));}catch(e){}}" +
                "el.addEventListener('input',save);el.addEventListener('change',save);el.addEventListener('blur',save);" +
                "}" +
                "function fromSheets(){try{" +
                "if(typeof STATE==='undefined'||!STATE.data||!STATE.currentSession||!STATE.currentWorkout||typeof safeId!=='function')return;" +
                "var logs=STATE.data.logs||[],exs=(STATE.data.exercises||[]).filter(function(e){return e.workout_id===STATE.currentWorkout.workout_id;});" +
                "exs.forEach(function(ex){var sets=Math.max(1,parseInt(ex.sets||'1',10)||1);for(var i=1;i<=sets;i++){" +
                "var key=safeId(STATE.currentSession.session_key+'-'+ex.exercise_order+'-'+i);" +
                "var log=logs.find(function(l){return String(l.week_key||'')===String(STATE.weekKey||'')&&String(l.session_key||'')===String(STATE.currentSession.session_key||'')&&String(l.workout_id||'')===String(STATE.currentWorkout.workout_id||'')&&String(l.exercise_name||'')===String(ex.name||'')&&Number(l.set_number||0)===Number(i);});" +
                "if(log){[['-kg','load_kg'],['-reps','reps'],['-rir','rir']].forEach(function(p){var el=document.getElementById(key+p[0]);if(el&&(el.value===''||el.value==null)){var nv='';try{nv=NativeCache.get(nk(el.id));}catch(e){}if(nv!=='')el.value=nv;else if(log[p[1]]!==null&&log[p[1]]!==undefined)el.value=log[p[1]];}});var b=document.getElementById(key+'-save');if(b)b.classList.add('saved');}" +
                "}});" +
                "}catch(e){}}" +
                "function scan(){document.querySelectorAll('input,textarea,select').forEach(bind);fromSheets();}" +
                "scan();new MutationObserver(scan).observe(document.documentElement,{childList:true,subtree:true});setInterval(scan,1000);" +
                "})();";
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        CookieManager.getInstance().flush();
        webView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.loadUrl("about:blank");
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
