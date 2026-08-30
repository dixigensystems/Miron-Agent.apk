package ru.dixigen.miron;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    private static final String START_URL = "https://miron.dixigen.ru/agent/";
    private WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildWebView();
        web.loadUrl(START_URL);
        setContentView(web);
    }

    private void buildWebView(){
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);

        web.setWebViewClient(new WebViewClient(){
            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail){
                /* Рендерер WebView упал — НЕ даём приложению умереть.
                   Пересоздаём окно целиком: пользователь видит перезагрузку агента. */
                recreate();
                return true;
            }
        });

        web.setWebChromeClient(new WebChromeClient(){
            @Override
            public void onPermissionRequest(final PermissionRequest request){
                runOnUiThread(new Runnable(){
                    @Override public void run(){
                        request.grant(request.getResources());
                    }
                });
            }
        });
    }

    @Override
    public void onBackPressed(){
        if (web != null && web.canGoBack()) web.goBack(); else super.onBackPressed();
    }

    @Override
    protected void onDestroy(){
        if (web != null) web.destroy();
        super.onDestroy();
    }
}
