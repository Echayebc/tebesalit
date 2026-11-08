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

    // هذا هو اللي يصلح خطأ الصورة - يحول intent:// الى https
    private String fixIntentUrl(String url) {
        if (url.startsWith("intent://")) {
            try {
                Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                String fallback = intent.getStringExtra("browser_fallback_url");
                if (fallback != null && !fallback.isEmpty()) return fallback;
                // اذا ماكانش fallback، نرجعوه لرسائل فيسبوك مباشرة
                if (url.contains("threads")) return "https://m.facebook.com/messages/";
                return "https://m.facebook.com/messages/";
            } catch (Exception e) {
                return "https://m.facebook.com/messages/";
            }
        }
        if (url.startsWith("fb-messenger://") || url.startsWith("fb://")) {
            return "https://m.facebook.com/messages/";
        }
        return url;
    }

    private void openInPopup(String url) {
        String cleanUrl = fixIntentUrl(url);
        if (popupView != null) {
            popupView.loadUrl(cleanUrl);
            return;
        }
        popupView = new WebView(MainActivity.this);
        setup(popupView);
        popupView.setWebViewClient(createClient(true));
        popupView.setWebChromeClient(createChrome());
        FrameLayout layout = new FrameLayout(MainActivity.this);
        layout.addView(popupView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(layout);
        popupView.loadUrl(cleanUrl);
    }

    private void closePopup() {
        if (popupView != null) {
            try { ((ViewGroup) popupView.getParent()).removeView(popupView); } catch (Exception e) {}
            try { popupView.destroy(); } catch (Exception e) {}
            popupView = null;
            setContentView(webView);
        }
    }

    // نفس العميل لكل النوافذ - باش مايتعاودش الخطأ في النافذة المنبثقة
    private WebViewClient createClient(boolean isPopup) {
        return new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                // 1. اذا كان intent:// او fb-messenger:// - مانخليوهش يتحمل كصفحة، نحولوه
                if (url.startsWith("intent://") || url.startsWith("fb-messenger://") || url.startsWith("fb://")) {
                    openInPopup(fixIntentUrl(url));
                    return true;
                }
                // 2. اذا كان ماسنجر / رسائل و رانا في النافذة الرئيسية -> افتح في نافذة منبثقة
                if (!isPopup && (url.contains("messenger.com") || url.contains("m.me") || url.contains("/messages"))) {
                    openInPopup(url);
                    return true;
                }
                // 3. اذا كان رابط عادي http/https خليه يكمل
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    return false;
                }
                // 4. اي رابط غريب آخر - حاول تفتحو برا، و اذا مانجحش افتح الرسائل
                try {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                } catch (Exception e) {
                    openInPopup("https://m.facebook.com/messages/");
                }
                return true;
            }
        };
    }

    private WebChromeClient createChrome() {
        return new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                popupView = new WebView(MainActivity.this);
                setup(popupView);
                popupView.setWebViewClient(createClient(true));
                popupView.setWebChromeClient(createChrome());
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
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);
        setup(webView);
        webView.setWebViewClient(createClient(false));
        webView.setWebChromeClient(createChrome());
        webView.loadUrl("https://m.facebook.com/login");
    }

    @Override
    public void onBackPressed() {
        if (popupView != null) {
            if (popupView.canGoBack()) popupView.goBack();
            else closePopup();
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
