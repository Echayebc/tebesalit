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
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Button;
import android.content.Intent;

public class MainActivity extends Activity {
    private WebView webView;
    private WebView popupView;
    private FrameLayout root;
    private FrameLayout popupContainer;
    private Button closeBtn;

    private void setup(WebView w) {
        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(w, true);
        w.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        w.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        w.setOverScrollMode(View.OVER_SCROLL_NEVER);
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
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setTextZoom(100);
        s.setLoadsImagesAutomatically(true);
        s.setBlockNetworkImage(false);
        s.setBlockNetworkLoads(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
        w.setInitialScale(100);
        s.setUserAgentString("Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");
    }

    private String fixIntentUrl(String url) {
        if (url.startsWith("intent://")) {
            try {
                Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                String fallback = intent.getStringExtra("browser_fallback_url");
                if (fallback != null && !fallback.isEmpty()) return fallback;
            } catch (Exception e) {}
            return "https://m.facebook.com/messages/";
        }
        if (url.startsWith("fb-messenger://") || url.startsWith("fb://")) {
            return "https://m.facebook.com/messages/";
        }
        return url;
    }

    private void showPopup() {
        popupContainer.setVisibility(View.VISIBLE);
        closeBtn.setVisibility(View.VISIBLE);
        popupContainer.bringToFront();
        closeBtn.bringToFront();
    }

    private void openInPopup(String url) {
        String cleanUrl = fixIntentUrl(url);
        if (popupView == null) {
            popupView = new WebView(this);
            setup(popupView);
            popupView.setWebViewClient(createClient(true));
            popupView.setWebChromeClient(createChrome());
            popupContainer.addView(popupView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        showPopup();
        popupView.loadUrl(cleanUrl);
    }

    private void closePopup() {
        if (popupView != null) {
            popupContainer.removeView(popupView);
            try { popupView.destroy(); } catch (Exception e) {}
            popupView = null;
        }
        popupContainer.setVisibility(View.GONE);
        closeBtn.setVisibility(View.GONE);
    }

    private WebViewClient createClient(boolean isPopup) {
        return new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("intent://") || url.startsWith("fb-messenger://") || url.startsWith("fb://")) {
                    openInPopup(fixIntentUrl(url));
                    return true;
                }
                if (!isPopup && (url.contains("messenger.com") || url.contains("m.me") || url.contains("/messages") || url.contains("facebook.com/messages"))) {
                    openInPopup(url);
                    return true;
                }
                if (url.startsWith("http://") || url.startsWith("https://")) return false;
                return true;
            }
        };
    }

    private WebChromeClient createChrome() {
        return new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                if (popupView == null) {
                    popupView = new WebView(MainActivity.this);
                    setup(popupView);
                    popupView.setWebViewClient(createClient(true));
                    popupView.setWebChromeClient(createChrome());
                    popupContainer.addView(popupView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                }
                showPopup();
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(popupView);
                resultMsg.sendToTarget();
                return true;
            }
            @Override
            public void onCloseWindow(WebView window) {
                closePopup();
            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        root = new FrameLayout(this);
        setContentView(root);
        webView = new WebView(this);
        setup(webView);
        webView.setWebViewClient(createClient(false));
        webView.setWebChromeClient(createChrome());
        root.addView(webView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        popupContainer = new FrameLayout(this);
        popupContainer.setVisibility(View.GONE);
        popupContainer.setBackgroundColor(0xFFFFFFFF);
        root.addView(popupContainer, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        closeBtn = new Button(this);
        closeBtn.setText("✕ إغلاق الدردشة");
        closeBtn.setVisibility(View.GONE);
        FrameLayout.LayoutParams btnParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnParams.topMargin = 20;
        btnParams.leftMargin = 20;
        root.addView(closeBtn, btnParams);
        closeBtn.setOnClickListener(v -> closePopup());
        webView.loadUrl("https://m.facebook.com/login");
    }

    @Override
    public void onBackPressed() {
        if (popupContainer.getVisibility() == View.VISIBLE) {
            if (popupView != null && popupView.canGoBack()) popupView.goBack();
            else closePopup();
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
