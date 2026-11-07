package com.tebesalit.browser;
import android.app.Activity;
import android.os.Bundle;
import android.os.Message;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.content.Intent;
import android.net.Uri;

public class MainActivity extends Activity {
    private WebView webView;
    private WebView popupView;

    private void setup(WebView w) {
        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(w, true);
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setAllowFileAccess(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setSupportMultipleWindows(true);
        s.setUserAgentString("Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");
    }

    // هاذي هي اللي تفتح ماسنجر في نافذة منبثقة داخل التطبيق
    private void openInPopup(String url) {
        if (popupView != null) {
            popupView.loadUrl(url);
            return;
        }
        popupView = new WebView(MainActivity.this);
        setup(popupView);
        popupView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }
        });
        popupView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onCloseWindow(WebView window) {
                closePopup();
            }
        });
        FrameLayout layout = new FrameLayout(MainActivity.this);
        layout.addView(popupView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(layout);
        popupView.loadUrl(url);
    }

    private void closePopup() {
        if (popupView != null) {
            try { ((ViewGroup) popupView.getParent()).removeView(popupView); } catch (Exception e) {}
            popupView.destroy();
            popupView = null;
            setContentView(webView);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);
        setup(webView);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                // 1. اذا ضغط على الدردشة / ماسنجر -> افتحها في نافذة منبثقة داخل التطبيق، ماشي برا
                if (url.contains("messenger.com") || url.contains("m.me") || url.contains("/messages") || url.contains("fb-messenger")) {
                    String finalUrl = url;
                    // اذا كان رابط intent:// نحولوه لـ https عادي
                    if (url.startsWith("intent://")) {
                        try {
                            Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                            String fallback = intent.getStringExtra("browser_fallback_url");
                            if (fallback != null) finalUrl = fallback;
                            else finalUrl = "https://m.facebook.com/messages/";
                        } catch (Exception e) {
                            finalUrl = "https://m.facebook.com/messages/";
                        }
                    }
                    // اذا كان رابط ماسنجر تاع التطبيق، نحولوه لنسخة الويب اللي تخدم
                    if (finalUrl.contains("fb-messenger://")) finalUrl = "https://m.facebook.com/messages/";
                    openInPopup(finalUrl);
                    return true;
                }
                return false;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                // فيسبوك يطلب نافذة جديدة -> نعطيه نافذة منبثقة داخل التطبيق
                popupView = new WebView(MainActivity.this);
                setup(popupView);
                popupView.setWebViewClient(new WebViewClient());
                popupView.setWebChromeClient(this);
                FrameLayout layout = new FrameLayout(MainActivity.this);
                layout.addView(popupView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                setContentView(layout);
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(popupView);
                resultMsg.sendToTarget();
                return true;
            }
            @Override
            public void onCloseWindow(WebView window) {
                closePopup();
            }
        });

        webView.loadUrl("https://m.facebook.com/login");
    }

    @Override
    public void onBackPressed() {
        if (popupView != null) {
            if (popupView.canGoBack()) {
                popupView.goBack();
            } else {
                closePopup();
            }
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
