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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);

        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(webView, true);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setSupportMultipleWindows(true);
        // الحل هنا: نقولولو رانا كمبيوتر، ماشي تيليفون
        // هكذا فيسبوك يفتح الدردشة بلا ماسنجر
        s.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                // اذا حاول يفتح ماسنجر، نرجعوه لـ mbasic اللي يخدم فيه الشات
                if (url.contains("messenger.com") || url.contains("m.facebook.com/messages")) {
                    // mbasic هو النسخة القديمة اللي مازال فيها الدردشة تخدم بلا ماسنجر
                    if (!url.contains("mbasic")) {
                        view.loadUrl("https://mbasic.facebook.com/messages/");
                        return true;
                    }
                }
                // اذا حاول يفتح تطبيق ماسنجر، نفتحوه اذا كان مثبت
                if (url.startsWith("intent://") || url.startsWith("fb-messenger://")) {
                    try {
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        startActivity(intent);
                    } catch (Exception e) {
                        view.loadUrl("https://mbasic.facebook.com/messages/");
                    }
                    return true;
                }
                return false;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                popupView = new WebView(MainActivity.this);
                popupView.getSettings().setJavaScriptEnabled(true);
                popupView.getSettings().setDomStorageEnabled(true);
                popupView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
                popupView.getSettings().setSupportMultipleWindows(true);
                popupView.getSettings().setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
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
                if (popupView != null) {
                    ((ViewGroup) popupView.getParent()).removeView(popupView);
                    popupView.destroy();
                    popupView = null;
                    setContentView(webView);
                }
            }
        });

        // نبدأ بـ mbasic - هي اللي فيها الدردشة تخدم
        webView.loadUrl("https://mbasic.facebook.com/login");
    }

    @Override
    public void onBackPressed() {
        if (popupView != null) {
            ((ViewGroup) popupView.getParent()).removeView(popupView);
            popupView.destroy();
            popupView = null;
            setContentView(webView);
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
