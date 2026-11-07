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

public class MainActivity extends Activity {
    private WebView webView;
    private WebView popupView;

    // هذا هو الحل: كود يخلي الدردشة على طول الهاتف كامل
    private String FULL_CHAT_JS = "javascript:(function(){"
        + "function fix(){"
        + "var all=document.querySelectorAll('div');"
        + "for(var i=0;i<all.length;i++){"
        + "var e=all[i];"
        + "var s=e.getAttribute('style')||'';"
        + "if(s.indexOf('height')!==-1 || e.getAttribute('role')==='dialog'){"
        + "e.style.setProperty('height','100vh','important');"
        + "e.style.setProperty('max-height','100vh','important');"
        + "e.style.setProperty('width','100%','important');"
        + "e.style.setProperty('max-width','100%','important');"
        + "e.style.setProperty('position','fixed','important');"
        + "e.style.setProperty('top','0','important');"
        + "e.style.setProperty('left','0','important');"
        + "e.style.setProperty('right','0','important');"
        + "e.style.setProperty('bottom','0','important');"
        + "e.style.setProperty('z-index','9999','important');"
        + "}"
        + "}"
        + "var chats=document.querySelectorAll('[aria-label*=\"Chat\"],[data-testid*=\"chat\"]');"
        + "for(var j=0;j<chats.length;j++){"
        + "chats[j].style.setProperty('height','100vh','important');"
        + "chats[j].style.setProperty('width','100vw','important');"
        + "chats[j].style.setProperty('position','fixed','important');"
        + "chats[j].style.setProperty('top','0','important');"
        + "chats[j].style.setProperty('left','0','important');"
        + "}"
        + "}"
        + "fix(); setInterval(fix, 800);"
        + "})()";

    private void setupWebView(WebView w) {
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
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setSupportMultipleWindows(true);
        s.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);
        setupWebView(webView);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // كل ما تفتح صفحة، نحقن الكود باش الدردشة تولي على طول الهاتف
                view.evaluateJavascript(FULL_CHAT_JS, null);
            }
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                // اذا ضغطت على دردشة، افتحها كاملة في نفس الصفحة
                if (url.contains("/messages/t/") || url.contains("messenger.com/t/")) {
                    view.loadUrl(url);
                    return true;
                }
                if (url.contains("messenger.com") || url.contains("m.facebook.com/messages")) {
                    view.loadUrl("https://mbasic.facebook.com/messages/");
                    return true;
                }
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
                setupWebView(popupView);
                popupView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView v, String url) {
                        super.onPageFinished(v, url);
                        v.evaluateJavascript(FULL_CHAT_JS, null);
                    }
                });
                popupView.setWebChromeClient(this);
                // نافذة كاملة 100% طول و عرض
                FrameLayout layout = new FrameLayout(MainActivity.this);
                FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                layout.addView(popupView, p);
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
