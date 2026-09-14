package com.deutschfun.app;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.Window;
import android.graphics.Color;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

public class MainActivity extends Activity {
    private WebView webView;
    private TextToSpeech tts;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        w.setStatusBarColor(Color.rgb(15,23,42));
        w.setNavigationBarColor(Color.rgb(15,23,42));

        // تهيئة محرك النطق الأصلي لأندرويد باللغة الألمانية
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    tts.setLanguage(Locale.GERMANY);
                    tts.setSpeechRate(0.9f);
                }
            }
        });

        webView = new WebView(this);
        setContentView(webView);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setLoadWithOverviewMode(false);
        s.setUseWideViewPort(false);

        // ربط جافا سكريبت بمحرك النطق الأصلي عبر اسم AndroidTTS
        webView.addJavascriptInterface(new WebAppInterface(), "AndroidTTS");

        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("file:///android_asset/index.html");
    }

    // الكلاس الذي يستقبل النداءات من JavaScript
    public class WebAppInterface {
        @JavascriptInterface
        public void speak(String text) {
            if (tts != null) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ttsID");
            }
        }
    }

    @Override public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
