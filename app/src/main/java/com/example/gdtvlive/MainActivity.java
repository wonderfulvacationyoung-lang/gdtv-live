package com.example.gdtvlive;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private WebView webView;
    private TextView channelLabel;
    private int currentIndex = 0;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private boolean dialogShowing = false;

    // 频道名称和对应ID
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

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        webView = (WebView) findViewById(R.id.webview);
        channelLabel = (TextView) findViewById(R.id.channel_label);

        setupWebView();
        loadChannel(currentIndex);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        
        // 基础设置
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAppCacheEnabled(true);
        settings.setAppCachePath(getApplicationContext().getCacheDir().getAbsolutePath());
        
        // 允许自动播放
        settings.setMediaPlaybackRequiresUserGesture(false);
        
        // 视图设置
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        
        // 文件访问
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        
        // 缓存设置
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        
        // 关键：设置 User-Agent 模拟电视浏览器
        String ua = "Mozilla/5.0 (Linux; Android 4.4.2; " + Build.MODEL + " Build/KOT49H) " +
                   "AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 " +
                   "Mobile Safari/537.36";
        settings.setUserAgentString(ua);
        
        // 启用 Cookie
        CookieManager.getInstance().setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= 21) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        }
        
        // 设置 WebViewClient
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                channelLabel.setText("正在加载: " + channelNames[currentIndex]);
                channelLabel.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                channelLabel.setText("正在播放: " + channelNames[currentIndex]);
                channelLabel.setVisibility(View.VISIBLE);
                
                // 注入自动播放脚本
                injectPlayScript();
                
                // 3秒后隐藏标签
                view.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        channelLabel.setVisibility(View.GONE);
                    }
                }, 3000);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                channelLabel.setText("加载失败: " + description);
                channelLabel.setVisibility(View.VISIBLE);
                Toast.makeText(MainActivity.this, "加载失败，请检查网络", Toast.LENGTH_SHORT).show();
            }
        });
        
        // 设置 WebChromeClient
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
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
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

    private void injectPlayScript() {
        String js = 
            "javascript:(function() {" +
            "    function tryPlay() {" +
            "        var videos = document.getElementsByTagName('video');" +
            "        if (videos.length > 0) {" +
            "            var v = videos[0];" +
            "            v.play();" +
            "            v.muted = false;" +
            "            v.controls = true;" +
            "            return true;" +
            "        }" +
            "        return false;" +
            "    }" +
            "    tryPlay();" +
            "    setTimeout(tryPlay, 2000);" +
            "    setTimeout(tryPlay, 5000);" +
            "})();";
        
        webView.loadUrl(js);
    }

    private void loadChannel(int index) {
        if (index < 0 || index >= channelIds.length) {
            index = 0;
        }
        currentIndex = index;
        String url = BASE_URL + channelIds[index];
        webView.loadUrl(url);
        channelLabel.setText("正在加载: " + channelNames[index]);
        channelLabel.setVisibility(View.VISIBLE);
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

    private void switchChannel(int newIndex) {
        if (newIndex < 0) {
            newIndex = channelIds.length - 1;
        } else if (newIndex >= channelIds.length) {
            newIndex = 0;
        }
        if (newIndex != currentIndex) {
            loadChannel(newIndex);
        } else {
            Toast.makeText(this, channelNames[currentIndex], Toast.LENGTH_SHORT).show();
        }
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
                    switchChannel(currentIndex - 1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_CHANNEL_DOWN:
                    switchChannel(currentIndex + 1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_MENU:
                    showChannelList();
                    return true;
                case KeyEvent.KEYCODE_BACK:
                    if (customView != null) {
                        // 退出全屏
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
                        switchChannel(target - 1);
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
        if (webView != null) {
            webView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
