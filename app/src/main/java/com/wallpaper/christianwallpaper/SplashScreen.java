package com.wallpaper.christianwallpaper;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import com.rdev.coreutils.DevLog;
import com.rdev.coreutils.ads.DevAds;
import com.rdev.coreutils.ads.GoogleMobileAdsConsentManager;
import com.rdev.coreutils.event.DevEvent;
import com.rdev.coreutils.utils.DevUtils;
import com.rdev.coreutils.utils.NetworkUtils;
import com.wallpaper.christianwallpaper.data.ApiClient;
import com.wallpaper.christianwallpaper.data.WallpaperInterface;
import com.wallpaper.christianwallpaper.databinding.SplashScreenBinding;
import com.wallpaper.christianwallpaper.dialogs.ConfirmActionDialog;
import com.wallpaper.christianwallpaper.models.WallModel;
import com.wallpaper.christianwallpaper.utils.ApiDataManger;
import com.wallpaper.christianwallpaper.utils.AppUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SplashScreen extends AppCompatActivity {
    public static final String TAG = SplashScreen.class.getSimpleName();
    private SplashScreenBinding binding;
    private ApiDataManger _apiDataManger;

    private String _confirmDialogEventId;
    private GoogleMobileAdsConsentManager googleMobileAdsConsentManager;
    private boolean isApiCallDone = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppUtils.setStatusBarTransparent(SplashScreen.this);
        AppUtils.setNavigationBarColor(true, SplashScreen.this);

        binding = SplashScreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Create ObjectAnimator for scaleX
        ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(binding.imgAnimatedIcon, "scaleX", 1f, 1.2f, 1f);
        scaleXAnimator.setDuration(1000); // 1 second for one cycle
        scaleXAnimator.setInterpolator(new LinearInterpolator()); // Smooth scaling
        scaleXAnimator.setRepeatCount(ObjectAnimator.INFINITE); // Infinite loop
        scaleXAnimator.setRepeatMode(ObjectAnimator.RESTART); // Restart after each cycle

// Create ObjectAnimator for scaleY
        ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(binding.imgAnimatedIcon, "scaleY", 1f, 1.2f, 1f);
        scaleYAnimator.setDuration(1000); // 1 second for one cycle
        scaleYAnimator.setInterpolator(new LinearInterpolator()); // Smooth scaling
        scaleYAnimator.setRepeatCount(ObjectAnimator.INFINITE); // Infinite loop
        scaleYAnimator.setRepeatMode(ObjectAnimator.RESTART); // Restart after each cycle

// Play scaleX and scaleY animations together
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleXAnimator, scaleYAnimator);
        animatorSet.start();

        _apiDataManger = ApiDataManger.getInstance(this);
       if (_apiDataManger.isNeedToCallApi()) {
           if (NetworkUtils.getInstance().isNetworkAvailable(this)) {
               callData();
           } else {
               showNoInternetConnectionAndRetryDialog();
           }
       } else {
           isApiCallDone = true;
       }

        initializeGoogleConsentManager();
    }

    private void initializeGoogleConsentManager() {
        if (googleMobileAdsConsentManager == null) {
            googleMobileAdsConsentManager =
                    GoogleMobileAdsConsentManager.getInstance(getApplicationContext(), "", false);
        }
        googleMobileAdsConsentManager.gatherConsent(
                this,
                consentError -> {
                    if (consentError != null) {
                        // Consent not obtained in current session.
                        DevLog.w(
                                TAG,
                                String.format(
                                        "%s: %s",
                                        consentError.getErrorCode(),
                                        consentError.getMessage()));
                    }

                    if (googleMobileAdsConsentManager.canRequestAds()) {
                        ChristianApplication.isMobileAdsInitializeCalled = true;
                        // Initialize the Mobile Ads SDK.
                        DevAds.initialize(this);
                        ChristianApplication.getInstance().initializeAppOpen();
                        startMainPage();
                    }

                });
    }
    private void startMainPage() {
        if (!isApiCallDone || !ChristianApplication.isMobileAdsInitializeCalled) {
            return;
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (DevUtils.isActivityFinishing(SplashScreen.this)) {
                    return;
                }
                startActivity(new Intent(SplashScreen.this, MainActivity.class));
                finish();
            }
        }, 500);
    }
    private void callData() {
        if (!ChristianApplication.isMobileAdsInitializeCalled) {
            initializeGoogleConsentManager();
        }
        WallpaperInterface wallpaperInterface = ApiClient.getClient().create(WallpaperInterface.class);
        // Make the API call
        Call<WallModel> call = wallpaperInterface.getAppInformation(DevUtils.getKS());
        call.enqueue(new Callback<WallModel>() {
            @Override
            public void onResponse(@NonNull Call<WallModel> call, @NonNull Response<WallModel> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WallModel wallModel = response.body();
                    _apiDataManger.saveApiData(wallModel);
                    isApiCallDone = true;
                    startMainPage();
                } else {
                    showNoInternetConnectionAndRetryDialog();
                }
            }

            @Override
            public void onFailure(@NonNull Call<WallModel> call, @NonNull Throwable throwable) {
                showNoInternetConnectionAndRetryDialog();
            }
        });
    }

    private void showNoInternetConnectionAndRetryDialog() {
        _confirmDialogEventId = DevEvent.newEventId();

        DevEvent.getInstance().getEvent(_confirmDialogEventId).observe(this, new Observer<Bundle>() {
            @Override
            public void onChanged(Bundle result) {
                if (result == null) {
                    return;
                }

                int requestType = DevEvent.getInt(result);
                if (requestType == ConfirmActionDialog.RESULT_POSITIVE) {
                    callData();
                } else if (requestType == ConfirmActionDialog.RESULT_NEGATIVE) {
                    finish();
                }
            }
        });

        ConfirmActionDialog.showDialog(getSupportFragmentManager(), this, _confirmDialogEventId, getString(R.string.no_internet_title), getString(R.string.no_internet_message), getString(R.string.retry), getString(R.string.cancel), false);
    }
}