package com.wallpaper.christianwallpaper;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.google.android.gms.ads.AdView;
import com.google.android.material.tabs.TabLayoutMediator;
import com.rdev.coreutils.ads.AdsUtils;
import com.rdev.coreutils.ads.DevBannerAd;
import com.rdev.coreutils.ads.DevInterstitialAdTimeInterval;
import com.rdev.coreutils.ads.GoogleMobileAdsConsentManager;
import com.rdev.coreutils.event.DevEvent;
import com.wallpaper.christianwallpaper.autowall.AutoLiveWallpaperService;
import com.wallpaper.christianwallpaper.autowall.AutoWallpaperSettingsActivity;
import com.wallpaper.christianwallpaper.databinding.ActivityMainBinding;
import com.wallpaper.christianwallpaper.dialogs.CustomInformationDialog;
import com.wallpaper.christianwallpaper.dialogs.ProcessDialogFragment;
import com.wallpaper.christianwallpaper.fragments.FavoriteImagesFragment;
import com.wallpaper.christianwallpaper.fragments.LatestImagesFragment;
import com.wallpaper.christianwallpaper.fragments.PopularImagesFragment;
import com.wallpaper.christianwallpaper.fragments.RecentsViewImagesFragment;
import com.wallpaper.christianwallpaper.fragments.SavedViewImagesFragment;
import com.wallpaper.christianwallpaper.utils.ApiDataManger;
import com.wallpaper.christianwallpaper.utils.AppUtils;
import com.wallpaper.christianwallpaper.utils.DatabaseRepository;
import com.wallpaper.christianwallpaper.utils.PermissionsUtils;

import java.util.LinkedList;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = MainActivity.class.getSimpleName();
    public static final String POPULAR_FRAGMENT_TITLE = "Popular";
    public static final String LATEST_FRAGMENT_TITLE = "Latest";
    public static final String RECENT_FRAGMENT_TITLE = "Recent";
    public static final String FAVORITE_FRAGMENT_TITLE = "Favourites";
    public static final String SAVED_FRAGMENT_TITLE = "Saved";
    private String _customInfoDialogEventId;



    private LinkedList<String> fragmentTitleList = new LinkedList<>();
    private ActivityMainBinding _binding;
    private ApiDataManger _apiDataManager;

    private AdView devBannerView;
    private DevInterstitialAdTimeInterval _interstitialAdManager;
    private GoogleMobileAdsConsentManager googleMobileAdsConsentManager;
    public class ViewPagerAdapter extends FragmentStateAdapter {

        public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            String fragmentTitle = fragmentTitleList.get(position);
            if (fragmentTitle.equalsIgnoreCase(POPULAR_FRAGMENT_TITLE)) {
                return PopularImagesFragment.newInstance();
            } else if (fragmentTitle.equalsIgnoreCase(LATEST_FRAGMENT_TITLE)) {
                return LatestImagesFragment.newInstance();
            } else if (fragmentTitle.equalsIgnoreCase(RECENT_FRAGMENT_TITLE)) {
                return RecentsViewImagesFragment.newInstance();
            } else if (fragmentTitle.equalsIgnoreCase(FAVORITE_FRAGMENT_TITLE)) {
                return FavoriteImagesFragment.newInstance();
            } else if (fragmentTitle.equalsIgnoreCase(SAVED_FRAGMENT_TITLE)) {
                return SavedViewImagesFragment.newInstance();
            }
            return null;
        }

        @Override
        public int getItemCount() {
            return fragmentTitleList.size();
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppUtils.setStatusBarTransparent(MainActivity.this);
        AppUtils.setNavigationBarColor(true, MainActivity.this);

        _binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(_binding.getRoot());

        AppUtils.setSpaceOnSystemBar(_binding.main);

        _apiDataManager = ApiDataManger.getInstance(this);
        fragmentTitleList.add(POPULAR_FRAGMENT_TITLE);
        fragmentTitleList.add(LATEST_FRAGMENT_TITLE);
        fragmentTitleList.add(RECENT_FRAGMENT_TITLE);
        fragmentTitleList.add(FAVORITE_FRAGMENT_TITLE);
        fragmentTitleList.add(SAVED_FRAGMENT_TITLE);

        // Create and set the adapter
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        _binding.pager.setAdapter(adapter);

        // Set up TabLayout with ViewPager2
        new TabLayoutMediator(_binding.tablayout, _binding.pager, (tab, position) -> {
            tab.setText(fragmentTitleList.get(position));
        }).attach();


        _binding.imgSettings.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(this, v);
            popupMenu.getMenuInflater().inflate(R.menu.app_menu, popupMenu.getMenu());
            MenuItem menuItemPrivacySettings = popupMenu.getMenu().findItem(R.id.privacySetting);
            if (menuItemPrivacySettings != null && !googleMobileAdsConsentManager.isPrivacyOptionsRequired()) {
                menuItemPrivacySettings.setVisible(false);
            }
            popupMenu.show();
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (item.getItemId() == R.id.autoWallpaper) {
                        if (Build.VERSION.SDK_INT < 23) {
                            //Do not need to check the permission
                            autoChangeWallpapers();
                        } else {
                            if (checkAndRequestPermissions()) {
                                //If you have already permitted the permission
                                autoChangeWallpapers();
                            }
                        }
                    } else if (item.getItemId() == R.id.autoWallpaperSettings) {
                        Intent intent = new Intent(MainActivity.this, AutoWallpaperSettingsActivity.class);
                        startActivity(intent);
                    } else if (item.getItemId() == R.id.shareApp) {
                        ChristianApplication.getInstance().updateOpenAdFlag();
                        String shareLink = "Hey " + getString(R.string.app_name) + " App Install from here " + "https://play.google.com/store/apps/details?id=" + getPackageName();

                        Intent sendIntent = new Intent();
                        sendIntent.setAction(Intent.ACTION_SEND);
                        sendIntent.putExtra(Intent.EXTRA_TEXT, shareLink);
                        sendIntent.setType("text/plain");
                        if (AppUtils.isAvailable(sendIntent, MainActivity.this)) {
                            startActivity(sendIntent);
                        } else {
                            Toast.makeText(MainActivity.this, "There is no app available for this task", Toast.LENGTH_SHORT).show();
                        }
                    } else if (item.getItemId() == R.id.rateUs) {
                        ChristianApplication.getInstance().updateOpenAdFlag();
                        String urlStrRateUs = "https://play.google.com/store/apps/details?id=" + getPackageName();
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(urlStrRateUs)));
                    } else if (item.getItemId() == R.id.privacy) {
                        ChristianApplication.getInstance().updateOpenAdFlag();
                        AppUtils.safeStartBrowser(MainActivity.this, _apiDataManager.getPrivacyPolicy());
                        return true;
                    } else if (item.getItemId() == R.id.privacySetting) {
                        googleMobileAdsConsentManager.showPrivacyOptionsForm(
                                MainActivity.this,
                                formError -> {
                                    if (formError != null) {
                                        Toast.makeText(
                                                MainActivity.this,
                                                formError.getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                        return true;
                    }
                    return false;
                }
            });
        });


        ProcessDialogFragment.showDialog(getSupportFragmentManager(), this, getString(R.string.loading), false, false);

        googleMobileAdsConsentManager =
                GoogleMobileAdsConsentManager.getInstance(getApplicationContext(), "", false);

        initializeAds();
    }

    private void initializeAds() {
        if (!_apiDataManager.isAdsEnabled()) {
            return;
        }

        DevBannerAd.getAdStatusLiveData().observe(this, new Observer<AdsUtils.AD_STATUS>() {
            @Override
            public void onChanged(AdsUtils.AD_STATUS adStatus) {
                if (adStatus == null) {
                    return;
                }

                if (adStatus == AdsUtils.AD_STATUS.FAILED) {
                    devBannerView = null;
                }
            }
        });

        _interstitialAdManager = DevInterstitialAdTimeInterval.getInstance(this, getString(R.string.admob_interstitial), _apiDataManager.getInterstitialMilli());
        getLifecycle().addObserver(_interstitialAdManager.lifecycleObserver);
    }


    private void loadBannerAd() {
        // load banner ads
        ViewGroup adContainerBanner = _binding.adContainerBanner;
        if (!_apiDataManager.isAdsEnabled()) {
            adContainerBanner.setVisibility(GONE);
            return;
        }

        adContainerBanner.removeAllViews();

        View adLoadingView = LayoutInflater.from(MainActivity.this).inflate(R.layout.loading_ad_view, null);

        adContainerBanner.setVisibility(VISIBLE);
        String bannerAdId = getString(R.string.admob_banner);

        if (!TextUtils.isEmpty(bannerAdId)) {
            devBannerView = DevBannerAd.newInstance(this, bannerAdId, adContainerBanner, adLoadingView);
        } else {
            adContainerBanner.setVisibility(GONE);
        }
    }

    public void autoChangeWallpapers() {
        try {
            if (DatabaseRepository.getInstance(this).getAllSavedImageIds().size() > 1) {
                ProcessDialogFragment.showDialog(getSupportFragmentManager(), this, "Preparing", false, false);
                ChristianApplication.getInstance().updateOpenAdFlag();
                WallpaperManager.getInstance(MainActivity.this).clear();
                Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
                intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        new ComponentName(MainActivity.this, AutoLiveWallpaperService.class));
                startActivity(intent);

            } else {
                showCustomInformationDialog();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean checkAndRequestPermissions() {
        boolean isAllGrant = true;
        for (String mustHavePermission : AppUtils.saveImagePermissions) {
            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    mustHavePermission) != PackageManager.PERMISSION_GRANTED) {
                isAllGrant = false;
            }
        }
        if (!isAllGrant) {
            requestPermissionLauncher.launch(AppUtils.saveImagePermissions);
            return false;
        }
        return true;
    }

    ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            new ActivityResultCallback<Map<String, Boolean>>() {
                @Override
                public void onActivityResult(Map<String, Boolean> result) {

                    if (PermissionsUtils.arePermissionsGranted(MainActivity.this, AppUtils.saveImagePermissions)) {
                        autoChangeWallpapers();
                        //Permission Granted Successfully. Write working code here.
                        Toast.makeText(MainActivity.this, "Permission granted", Toast.LENGTH_SHORT).show();
                    } else {
                        //You did not accept the request can not use the functionality.
                        Toast.makeText(MainActivity.this, "Permission not granted", Toast.LENGTH_SHORT).show();
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
                        alertDialog.setTitle("Allow permission");
                        alertDialog.setMessage("Using this " + getString(R.string.app_name) + " app need storage permission is necessary.\nPlease allow this permission.");
                        alertDialog.setCancelable(false);
                        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
//                                requestPermissionLauncher.launch(mustHavePermissions);
                            }
                        });
                        alertDialog.setNegativeButton("Setting", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                            }
                        });
                        alertDialog.show();
                    }

                }
            }
    );

    private void showCustomInformationDialog() {
        _customInfoDialogEventId = DevEvent.newEventId();

        DevEvent.getInstance().getEvent(_customInfoDialogEventId).observe(this, new Observer<Bundle>() {
            @Override
            public void onChanged(Bundle result) {
                if (result == null) {
                    return;
                }

            }
        });

        CustomInformationDialog.showDialog(getSupportFragmentManager(), this, _customInfoDialogEventId, "Required!", "For Auto change wallpaper you need to saved more than 1 image to your device from app.", true);
    }

    @Override
    public void onPause() {
        if (devBannerView != null) {
            devBannerView.pause();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (devBannerView != null) {
            devBannerView.resume();
        } else {
            loadBannerAd();
        }

        ProcessDialogFragment.dismissDialog(getSupportFragmentManager(), this);
    }

    @Override
    public void onDestroy() {
        if (devBannerView != null) {
            devBannerView.destroy();
        }
        super.onDestroy();
    }
}