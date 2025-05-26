package com.wallpaper.christianwallpaper.autowall;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.rdev.coreutils.ads.AdsUtils;
import com.rdev.coreutils.ads.DevReloadNativeAd;
import com.wallpaper.christianwallpaper.R;
import com.wallpaper.christianwallpaper.databinding.ActivityWallpaperSettingBinding;
import com.wallpaper.christianwallpaper.utils.ApiDataManger;
import com.wallpaper.christianwallpaper.utils.AppUtils;

import java.util.HashMap;

public class AutoWallpaperSettingsActivity extends AppCompatActivity {
    public static final String ROTATE_KEY = "rotate";
    public static final String TIMER_KEY = "timer";
    public static final String SCROLLING_KEY = "scrolling";
    public static final String STRETCHING_KEY = "stretching";
    public static final String TRIM_KEY = "trim";
    public static final String CLICK_TO_CHANGE_KEY = "clickToChange";
    public static final String TRANSITION_KEY = "transition";
    public static final int FADE_TRANSITION = 1;
    private ActivityWallpaperSettingBinding _binding;
    private AdView devBannerView;
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        AppUtils.setStatusBarTransparent(AutoWallpaperSettingsActivity.this);
        AppUtils.setNavigationBarColor(true, AutoWallpaperSettingsActivity.this);

        _binding = ActivityWallpaperSettingBinding.inflate(getLayoutInflater());
        setContentView(_binding.getRoot());

        AppUtils.setSpaceOnSystemBar(_binding.main);

        _binding.toolbarWallByCat.setTitle("Setting");
        setSupportActionBar(_binding.toolbarWallByCat);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.setting_content, new SettingsFragment())
                .commit();


        if (ApiDataManger.getInstance(this).isAdsEnabled()) {
            loadNativeAds();
        } else {
            _binding.adContainerNative.setVisibility(GONE);
        }

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(menuItem);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements
            SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            getPreferenceManager().setSharedPreferencesName(AutoLiveWallpaperService.GalleryEngine.SHARED_PREFS_NAME);
            addPreferencesFromResource(R.xml.auto_wallpaper_settings);
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {

        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        }

        @Override
        public void onDestroy() {
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            super.onDestroy();
        }
    }

    private void loadNativeAds() {
        DevReloadNativeAd _reloadedNativeAd = DevReloadNativeAd.getInstance(this, getString(R.string.admob_native));
        _reloadedNativeAd.loadAd().observe(this, new Observer<HashMap<AdsUtils.AD_STATUS, NativeAd>>() {
            @Override
            public void onChanged(HashMap<AdsUtils.AD_STATUS, NativeAd> adStatusNativeAd) {
                if (adStatusNativeAd == null) {
                    return;
                }

                for (AdsUtils.AD_STATUS key : adStatusNativeAd.keySet()) {
                    if (key == AdsUtils.AD_STATUS.LOADING) {
                        _binding.adContainerNative.setVisibility(VISIBLE);

                        _binding.loadingNativeAd.loadingAdView.setVisibility(VISIBLE);
                        _binding.nativeAdTemplate.setVisibility(View.INVISIBLE);
                    } else if (key == AdsUtils.AD_STATUS.FAILED) {
                        _binding.adContainerNative.setVisibility(GONE);
                    } else if (key == AdsUtils.AD_STATUS.SUCCESS) {
                        _binding.adContainerNative.setVisibility(VISIBLE);

                        _binding.loadingNativeAd.loadingAdView.setVisibility(GONE);
                        _binding.nativeAdTemplate.setVisibility(VISIBLE);


                        NativeAd nativeAd = adStatusNativeAd.get(key);

                        _binding.nativeAdTemplate.setNativeAd(nativeAd);
                        _binding.nativeAdTemplate.setStyles(AppUtils.getNativeAdsCommonStyle(AutoWallpaperSettingsActivity.this));
                    }
                }

            }
        });
    }
}