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
        // رجعنا متصفح هاتف - ثابت، ماشي كمبيوتر
        s.setUserAgentString("Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");
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
                // ماسنجر يفتح في نافذة أخرى - تطبيق ماسنجر اذا كان موجود، ولا متصفح خارجي
                if (url.contains("messenger.com") || url.contains("fb-messenger") || url.startsWith("intent://")) {
                    try {
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                    } catch (Exception e) {
                        // اذا ماكانش ماسنجر، افتحه في متصفح خارجي
                        try {
                            Intent i2 = new Intent(Intent.ACTION_VIEW, Uri.parse("https://m.me/"));
                            startActivity(i2);
                        } catch (Exception e2) {}
                    }
                    return true;
                }
                return false;
            }
        });

        // نافذة أخرى ثابتة للماسنجر و النوافذ المنبثقة
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
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
                if (popupView != null) {
                    ((ViewGroup) popupView.getParent()).removeView(popupView);
                    popupView.destroy();
                    popupView = null;
                    setContentView(webView);
                }
            }
        });

        // التكيف الآلي: كل مرة يجيب آخر نسخة من فيسبوك
        // و البناء الآلي يبقى يخدم من GitHub Actions
        webView.loadUrl("https://m.facebook.com/login");
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
