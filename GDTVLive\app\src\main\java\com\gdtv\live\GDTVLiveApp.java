package com.gdtv.live;

import android.app.Application;
import android.webkit.WebView;

public class GDTVLiveApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 全局 WebView 默认设置（兼容 Android 4.4）
        try {
            WebView.setWebContentsDebuggingEnabled(true);
        } catch (Exception ignored) {
        }
    }
}
