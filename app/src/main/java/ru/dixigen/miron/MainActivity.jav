package ru.dixigen.miron;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.PermissionRequest;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String START_URL = "https://miron.dixigen.ru/agent/";
    private static final int PERM_CODE = 1;

    private WebView web;
    private FrameLayout root;
    private int renderCrashes = 0;

    /* Самописец: любое непойманное падение пишет себя в Download/miron_crash.txt */
    private final Thread.UncaughtExceptionHandler oldHandler =
            Thread.getDefaultUncaughtExceptionHandler();

    private final Thread.UncaughtExceptionHandler crashWriter =
            new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    try {
                        StringWriter sw = new StringWriter();
                        e.printStackTrace(new PrintWriter(sw));
                        File dir = new File(getExternalFilesDir(null), "Download");
                        if (!dir.exists()) dir.mkdirs();
                        File f = new File(dir, "miron_crash.txt");
                        FileWriter fw = new FileWriter(f, true);
                        fw.write("=== CRASH " + new SimpleDateFormat(
                                "yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()) + " ===\n");
                        fw.write(sw.toString());
                        fw.write("\n\n");
                        fw.close();
                    } catch (Throwable ignored) { }
                    if (oldHandler != null) oldHandler.uncaughtException(t, e);
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Thread.setDefaultUncaughtExceptionHandler(crashWriter);

        requestPermissions(new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
        }, PERM_CODE);

        root = new FrameLayout(this);
        setContentView(root);

        buildWebView();
        web.loadUrl(START_URL);
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
                renderCrashes++;
                logToFile("RENDERER GONE, раз #" + renderCrashes);
                if (renderCrashes <= 2) {
                    root.removeAllViews();
                    buildWebView();
                    web.loadUrl(START_URL);
                } else {
                    showFallback();
                }
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
            @Override
            public void onPermissionRequestCanceled(PermissionRequest request) { }
        });

        root.addView(web, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void logToFile(String line){
        try {
            File dir = new File(getExternalFilesDir(null), "Download");
            if (!dir.exists()) dir.mkdirs();
            File f = new File(dir, "miron_crash.txt");
            FileWriter fw = new FileWriter(f, true);
            fw.write("=== LOG " + new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()) + " ===\n" + line + "\n\n");
            fw.close();
        } catch (Throwable ignored) { }
    }

    private void showFallback(){
        root.removeAllViews();
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(40, 40, 40, 40);

        TextView t = new TextView(this);
        t.setText("Веб-движок этого телефона не тянет МИРОНа.\nОткрой агента в браузере — там всё работает.");
        t.setTextSize(16);
        t.setGravity(Gravity.CENTER);
        box.addView(t);

        Button b = new Button(this);
        b.setText("Открыть в браузере");
        b.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(START_URL)));
            } catch (Exception e) { }
            finish();
        });
        box.addView(b);

        root.addView(box);
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
