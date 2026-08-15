package com.example.gdtvlive;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

public class MainActivity extends Activity {

    private WebView webView;
    private int currentIndex = 0;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private boolean dialogShowing = false;

    private String[] channelNames = {
        "广东卫视",
        "珠江频道",
        "体育频道",
        "新闻频道",
        "公共频道",
        "嘉佳卡通",
        "南方卫视",
        "影视频道",
        "少儿频道",
        "房产频道"
    };

    private String[] channelIds = {
        "43", "44", "45", "46", "47", "48", "49", "50", "51", "52"
    };

    private static final String BASE_URL = "https://www.gdtv.cn/tvChannelDetail/";
    private static final String REFERER = "https://www.gdtv.cn/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        webView = new WebView(this);
        setContentView(webView);

        setupWebView();
        loadChannel(0);
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        
        // JavaScript
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        
        // 存储
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAppCacheEnabled(true);
        settings.setAppCachePath(getCacheDir().getAbsolutePath());
        
        // 自动播放
        settings.setMediaPlaybackRequiresUserGesture(false);
        
        // 视图
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(false);
        
        // 文件访问
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        
        // 缓存
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        
        // User-Agent：模拟手机浏览器
        settings.setUserAgentString(
            "Mozilla/5.0 (Linux; Android 4.4.2; SmartTV Build/KOT49H) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/34.0.1847.114 " +
            "Mobile Safari/537.36"
        );
        
        // Cookie
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= 21) {
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }
        
        // WebViewClient：处理 Referer、SSL、错误
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                Toast.makeText(MainActivity.this, "加载中...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                // 注入自动播放脚本
                injectAutoPlay();
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed(); // 忽略 SSL 错误
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Toast.makeText(MainActivity.this, "加载失败: " + description, Toast.LENGTH_LONG).show();
            }
        });
        
        // WebChromeClient：处理全屏视频
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView = view;
                customViewCallback = callback;
                FrameLayout decor = (FrameLayout) getWindow().getDecorView();
                decor.addView(customView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
                webView.setVisibility(View.GONE);
            }

            @Override
            public void onHideCustomView() {
                if (customView != null) {
                    customView.setVisibility(View.GONE);
                    FrameLayout decor = (FrameLayout) getWindow().getDecorView();
                    decor.removeView(customView);
                    customView = null;
                    if (customViewCallback != null) {
                        customViewCallback.onCustomViewHidden();
                    }
                    webView.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void injectAutoPlay() {
        String js = 
            "javascript:(function() {" +
            "    function clickPlay() {" +
            "        var elements = document.querySelectorAll('[class*=play], [id*=play], .vjs-big-play-button, video');" +
            "        for (var i = 0; i < elements.length; i++) {" +
            "            try {" +
            "                elements[i].click();" +
            "            } catch(e) {}" +
            "        }" +
            "        var videos = document.querySelectorAll('video');" +
            "        for (var j = 0; j < videos.length; j++) {" +
            "            try {" +
            "                videos[j].play();" +
            "                videos[j].muted = false;" +
            "            } catch(e) {}" +
            "        }" +
            "    }" +
            "    clickPlay();" +
            "    setTimeout(clickPlay, 1000);" +
            "    setTimeout(clickPlay, 3000);" +
            "    setTimeout(clickPlay, 5000);" +
            "})();";
        
        webView.loadUrl(js);
    }

    private void loadChannel(int index) {
        if (index < 0 || index >= channelIds.length) {
            index = 0;
        }
        currentIndex = index;
        String url = BASE_URL + channelIds[index];
        
        // 添加 Referer 头
        webView.loadUrl(url);
        Toast.makeText(this, channelNames[index], Toast.LENGTH_SHORT).show();
    }

    private void showChannelList() {
        dialogShowing = true;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择频道");
        builder.setItems(channelNames, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                loadChannel(which);
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("取消", null);
        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                dialogShowing = false;
            }
        });
        dialog.show();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (dialogShowing) {
            return super.dispatchKeyEvent(event);
        }

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_CHANNEL_UP:
                    loadChannel(currentIndex - 1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_CHANNEL_DOWN:
                    loadChannel(currentIndex + 1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_MENU:
                    showChannelList();
                    return true;
                case KeyEvent.KEYCODE_BACK:
                    if (customView != null) {
                        webView.loadUrl("javascript:document.exitFullscreen();");
                        return true;
                    } else if (webView.canGoBack()) {
                        webView.goBack();
                    } else {
                        finish();
                    }
                    return true;
                case KeyEvent.KEYCODE_0:
                case KeyEvent.KEYCODE_1:
                case KeyEvent.KEYCODE_2:
                case KeyEvent.KEYCODE_3:
                case KeyEvent.KEYCODE_4:
                case KeyEvent.KEYCODE_5:
                case KeyEvent.KEYCODE_6:
                case KeyEvent.KEYCODE_7:
                case KeyEvent.KEYCODE_8:
                case KeyEvent.KEYCODE_9:
                    int num = keyCode - KeyEvent.KEYCODE_0;
                    int target = (num == 0) ? 10 : num;
                    if (target >= 1 && target <= channelIds.length) {
                        loadChannel(target - 1);
                    }
                    return true;
                default:
                    return false;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
