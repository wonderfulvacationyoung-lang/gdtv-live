package com.example.gdtvlive;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
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
    private boolean isVideoPlaying = false;

    // 广东台频道列表（根据实际频道ID调整）
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

    // JS 注入脚本：自动播放视频
    private static final String AUTO_PLAY_JS = 
        "javascript:(function() {" +
        "    function tryPlayVideo() {" +
        "        var videos = document.querySelectorAll('video');" +
        "        if (videos.length > 0) {" +
        "            var video = videos[0];" +
        "            video.muted = false;" +
        "            video.autoplay = true;" +
        "            video.controls = true;" +
        "            video.setAttribute('playsinline', 'true');" +
        "            video.setAttribute('webkit-playsinline', 'true');" +
        "            video.play().then(function() {" +
        "                if (video.requestFullscreen) {" +
        "                    video.requestFullscreen();" +
        "                } else if (video.webkitRequestFullscreen) {" +
        "                    video.webkitRequestFullscreen();" +
        "                }" +
        "            }).catch(function(e) {" +
        "                console.log('Play failed: ' + e);" +
        "            });" +
        "            return true;" +
        "        }" +
        "        return false;" +
        "    }" +
        "    " +
        "    // 立即尝试播放" +
        "    if (!tryPlayVideo()) {" +
        "        // 如果没找到 video，等待 DOM 加载" +
        "        var observer = new MutationObserver(function(mutations) {" +
        "            if (tryPlayVideo()) {" +
        "                observer.disconnect();" +
        "            }" +
        "        });" +
        "        observer.observe(document.body, { childList: true, subtree: true });" +
        "        " +
        "        // 5秒后再次尝试" +
        "        setTimeout(function() {" +
        "            tryPlayVideo();" +
        "        }, 5000);" +
        "    }" +
        "})();";

    // JS 注入脚本：点击播放按钮
    private static final String CLICK_PLAY_JS = 
        "javascript:(function() {" +
        "    var playButtons = document.querySelectorAll('.play-btn, .play-button, .vjs-big-play-button, [class*=play], [id*=play]');" +
        "    for (var i = 0; i < playButtons.length; i++) {" +
        "        playButtons[i].click();" +
        "    }" +
        "    var videos = document.querySelectorAll('video');" +
        "    for (var j = 0; j < videos.length; j++) {" +
        "        videos[j].play();" +
        "    }" +
        "})();";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        webView = (WebView) findViewById(R.id.webview);
        channelLabel = (TextView) findViewById(R.id.channel_label);

        configureWebView();

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
                
                // 页面加载完成后，延迟执行自动播放脚本
                view.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        injectAutoPlayScript();
                    }
                }, 2000);
                
                view.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        injectAutoPlayScript();
                    }
                }, 5000);
                
                // 隐藏频道标签
                view.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        channelLabel.setVisibility(View.GONE);
                    }
                }, 3000);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                channelLabel.setText("加载失败，请检查网络");
                channelLabel.setVisibility(View.VISIBLE);
                Toast.makeText(MainActivity.this, "加载失败: " + description, Toast.LENGTH_SHORT).show();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView = view;
                customViewCallback = callback;
                FrameLayout decor = (FrameLayout) getWindow().getDecorView();
                decor.addView(customView,
                        new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT));
                webView.setVisibility(View.GONE);
                isVideoPlaying = true;
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
                    isVideoPlaying = false;
                }
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100 && !isVideoPlaying) {
                    channelLabel.setText("加载中... " + newProgress + "%");
                    channelLabel.setVisibility(View.VISIBLE);
                }
            }
        });

        loadChannel(currentIndex);
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAppCacheEnabled(true);
        settings.setAppCachePath(getApplicationContext().getCacheDir().getAbsolutePath());
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setPluginState(WebSettings.PluginState.ON);
        
        // 关键：模拟 TV 浏览器 UA，绕过移动端适配
        settings.setUserAgentString(
            "Mozilla/5.0 (Linux; Android 4.4.2; SmartTV; Build/KOT49H) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/34.0.1847.114 " +
            "Safari/537.36 TVBrowser/1.0"
        );
    }

    private void injectAutoPlayScript() {
        if (webView != null) {
            // 注入自动播放脚本
            webView.loadUrl(AUTO_PLAY_JS);
            // 延迟后注入点击播放脚本
            webView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    webView.loadUrl(CLICK_PLAY_JS);
                }
            }, 1000);
        }
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
        builder.setTitle("选择频道（当前: " + channelNames[currentIndex] + "）");
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
                case KeyEvent.KEYCODE_BACK:
                    if (isVideoPlaying) {
                        // 如果视频全屏，先退出全屏
                        webView.loadUrl("javascript:document.exitFullscreen();");
                        return true;
                    } else if (webView.canGoBack()) {
                        webView.goBack();
                    } else {
                        finish();
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
