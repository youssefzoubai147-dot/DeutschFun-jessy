package com.deutschfun.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import android.view.Window;
import android.graphics.Color;
import android.speech.tts.TextToSpeech;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
    private WebView webView;
    private TextToSpeech tts;
    private SpeechRecognizer speechRecognizer;
    private static final int REQ_RECORD_AUDIO = 101;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        w.setStatusBarColor(Color.rgb(15,23,42));
        w.setNavigationBarColor(Color.rgb(15,23,42));

        tts = new TextToSpeech(this, this);
        setupSpeechRecognizer();

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
        webView.addJavascriptInterface(new AndroidTTS(), "AndroidTTS");
        webView.addJavascriptInterface(new AndroidSTT(), "AndroidSTT");
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.GERMANY);
        }
    }

    private void setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}

            @Override public void onError(int error) {
                runOnUiThread(() -> {
                    if (webView != null) {
                        webView.evaluateJavascript(
                            "window.onSpeechError && window.onSpeechError(" + error + ");", null);
                    }
                });
            }

            @Override public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String text = (matches != null && !matches.isEmpty()) ? matches.get(0) : "";
                String escaped = text.replace("\\\\", "\\\\\\\\").replace("'", "\\\\'");
                runOnUiThread(() -> {
                    if (webView != null) {
                        webView.evaluateJavascript(
                            "window.onSpeechResult && window.onSpeechResult('" + escaped + "');", null);
                    }
                });
            }

            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void actuallyStartListening() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "de-DE");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        if (speechRecognizer != null) {
            speechRecognizer.startListening(intent);
        }
    }

    public class AndroidTTS {
        @JavascriptInterface
        public void speak(String text) {
            if (tts != null) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utteranceId");
            }
        }
    }

    public class AndroidSTT {
        @JavascriptInterface
        public void startListening() {
            runOnUiThread(() -> {
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_RECORD_AUDIO);
                    return;
                }
                actuallyStartListening();
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_RECORD_AUDIO && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            actuallyStartListening();
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
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        super.onDestroy();
    }
}
