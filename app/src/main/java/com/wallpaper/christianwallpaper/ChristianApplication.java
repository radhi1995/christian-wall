package com.wallpaper.christianwallpaper;

import android.app.Application;

import com.onesignal.OneSignal;
import com.rdev.coreutils.DevLog;
import com.rdev.coreutils.ads.DevAppOpenAd;

public class ChristianApplication extends Application {
    public static ChristianApplication _instance;
    private DevAppOpenAd _appOpenAds;
    public static boolean isMobileAdsInitializeCalled = false;

    @Override
    public void onCreate() {
        super.onCreate();
        _instance = this;
        DevLog.init(BuildConfig.DEBUG);
//        DevAds.initialize(this);
//
//        initializeAppOpen();

        OneSignal.initWithContext(this, "7b6919b5-32af-4c3b-aba5-4abb3b55c5d5");
    }

    public static ChristianApplication getInstance() {
        return _instance;
    }

    public void initializeAppOpen() {
        if (_appOpenAds == null) {
            String[] classname = new String[]{MainActivity.class.getSimpleName()};
            _appOpenAds = DevAppOpenAd.getInstance(this, getString(R.string.admob_app_open), classname);
        }
    }

    public void updateOpenAdFlag() {
        if (_appOpenAds != null) {
            _appOpenAds.noNeedToShowOpenAds();
        }
    }
}
