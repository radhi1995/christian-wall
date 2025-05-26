package com.wallpaper.christianwallpaper.utils;

import android.Manifest;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.rdev.coreutils.DevLog;
import com.rdev.coreutils.utils.MediaUtils;
import com.rdev.coreutils.widget.NativeTemplateStyle;
import com.wallpaper.christianwallpaper.ChristianApplication;
import com.wallpaper.christianwallpaper.R;
import com.wallpaper.christianwallpaper.dialogs.SetWallpaperOptionDialog;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class AppUtils {
    public static final int SHOW_ADS_DIALOG_DELAY = 500;
    public static final String IMAGE_EXTENSION = ".jpg";
    public static String[] notificationPermission;

    public static String[] saveImagePermissions;
    public static String[] setWallpaperPermissions;

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission = new String[] {Manifest.permission.POST_NOTIFICATIONS};
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            notificationPermission = new String[] {};
        } else {
            notificationPermission = new String[] {};
        }
    }
    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            saveImagePermissions = new String[] {};
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            saveImagePermissions = new String[] {Manifest.permission.READ_EXTERNAL_STORAGE};
        } else {
            saveImagePermissions = new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        }
    }

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setWallpaperPermissions = new String[] {Manifest.permission.SET_WALLPAPER};
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setWallpaperPermissions = new String[] {Manifest.permission.SET_WALLPAPER};
        } else {
            setWallpaperPermissions = new String[] {Manifest.permission.SET_WALLPAPER};
        }
    }



    public static void setSystemBarTextColor(boolean isBlack, AppCompatActivity activity) {
        View decorView = activity.getWindow().getDecorView();
        WindowInsetsControllerCompat windowInsetsController = ViewCompat.getWindowInsetsController(decorView);
        if (windowInsetsController != null)
            windowInsetsController.setAppearanceLightStatusBars(isBlack);
    }

    public static void setNavigationBarColor(boolean isBlack, AppCompatActivity activity) {
        Window window = activity.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        if (isBlack)
            window.setNavigationBarColor(Color.BLACK);
        else
            window.setNavigationBarColor(Color.WHITE);
    }

    public static void setNavigationBarTransparent(AppCompatActivity activity) {
        Window window = activity.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setNavigationBarColor(Color.TRANSPARENT);
    }

    public static void setStatusBarTransparent(AppCompatActivity activity) {
        Window window = activity.getWindow();
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        window.setStatusBarColor(Color.TRANSPARENT);
    }

    public static void setSystemUiVisibility(Context context) {
        Window window = ((AppCompatActivity) context).getWindow();
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        window.setStatusBarColor(Color.TRANSPARENT);
    }

    public static void setSpaceOnSystemBar(View view) {
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public static void setPaddingOnStatusBar(View view) {
        int statusBarHeight = getStatusBarHeight(view);
        view.setPadding(0, statusBarHeight, 0, 0);
    }

    public static int getStatusBarHeight(View view) {
        int result = 0;
        int resourceId = view.getContext().getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = view.getContext().getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static void safeStartBrowser(Context context, String url) {
        if (url == null || url.isEmpty()) {
            Toast.makeText(context, context.getString(R.string.err_empty_url), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            if (!(context instanceof Activity)) {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(i);
        } catch (ActivityNotFoundException activityNotFoundException) {
            DevLog.i(activityNotFoundException.toString());
            Toast.makeText(context, context.getString(R.string.err_no_browser_open_url), Toast.LENGTH_SHORT).show();
        }
    }

    public static boolean isAvailable(Intent intent, Context context) {
        final PackageManager mgr = context.getPackageManager();
        List<ResolveInfo> list =
                mgr.queryIntentActivities(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);
        return !list.isEmpty();
    }

    public static String getFilePath(Context context) {

        String imagesDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES).toString() + File.separator + context.getString(R.string.app_name) + File.separator;

        File file = new File(imagesDir);

        if (!file.exists()) {
            file.mkdirs();
        }
        return file.getAbsolutePath();
    }


    public static boolean isActivityFinishing(Activity activity) {
        return activity.isFinishing() || activity.isDestroyed();
    }

    public static void runOnUiThread(Runnable action, long delayMillis) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.postDelayed(action, delayMillis);
        } else if (action != null) {
            action.run();
        }
    }

    public static String getDeviceInfo() {
        String deviceInfo;
        deviceInfo = "Device Info :\nAndroid SDK: " + Build.VERSION.SDK_INT + ", "
                + "\nRelease: " + Build.VERSION.RELEASE + ", "
                + "\nBrand: " + Build.BRAND + ", "
                + "\nDevice: " + Build.DEVICE + ", "
                + "\nId: " + Build.ID + ", "
                + "\nHardware: " + Build.HARDWARE + ", "
                + "\nManufacturer: " + Build.MANUFACTURER + ", "
                + "\nModel: " + Build.MODEL + ", "
                + "\nProduct: " + Build.PRODUCT;
        return deviceInfo;
    }

    public static String getPlayStoreLink(Context context, String packageName) {
        return String.format(context.getString(R.string.merge_two_urls), "https://play.google.com/store/apps/details?id=", packageName);
    }
    public static LiveData<Boolean> setWallpaperByWhere(Context context, String fileName, int where) {
        MutableLiveData<Boolean> saveFinished = new MutableLiveData<>();

        Runnable saveWorker = () -> {

            boolean isDone = setAsWallpaper(context, fileName, where);
            saveFinished.postValue(true);
        };

        Thread thread = new Thread(saveWorker);
        thread.start();
        return saveFinished;
    }

    private static boolean setAsWallpaper(Context context, String fileName, int whereSet) {
        String filePath = MediaUtils.getImageFile(context, context.getString(R.string.app_name), fileName);

        Bitmap bitmap = BitmapFactory.decodeFile(filePath);

        // Set wallpaper using WallpaperManager
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
        wallpaperManager.setWallpaperOffsetSteps(1, 1);
        DisplayMetrics metrics = context.getApplicationContext().getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        wallpaperManager.suggestDesiredDimensions(width, height);

        try {

            if (whereSet == SetWallpaperOptionDialog.RESULT_HOME_SCREEN) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM);
                } else {
                    wallpaperManager.setBitmap(bitmap);
                }
            } else if (whereSet == SetWallpaperOptionDialog.RESULT_LOCK_SCREEN) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK);
                } else {
                    wallpaperManager.setBitmap(bitmap);
                }
            } else {
                wallpaperManager.setBitmap(bitmap);
            }
            return true;
            // Wallpaper set successfully
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String getAppSpecificDirectoryWithAppDir(Context context) {
        if (context == null) {
            context = ChristianApplication.getInstance();
        }
        File appSpecificDir = new File(MediaUtils.getAppSpecificTopDir(context), context.getString(R.string.app_name));
        if (!appSpecificDir.exists()) {
            boolean success = appSpecificDir.mkdir();
        }

        return appSpecificDir.getAbsolutePath();
    }

    public static NativeTemplateStyle getNativeAdsCommonStyle(Context context) {
        return new
                NativeTemplateStyle.Builder()
                .withPrimaryTextTypefaceColor(Color.WHITE)
                .withAdsAttribBackground(R.drawable.ad_attrib_bg) // background color
                .withAdsAttribTextTypefaceColor(Color.BLACK)
                .withCallToActionBackgroundColor(new ColorDrawable(ContextCompat.getColor(context, R.color.ads_accent_color)))
                .withCallToActionTypefaceColor(Color.WHITE)
                .withAdsRatingColor(ContextCompat.getColor(context, R.color.ads_accent_color), ContextCompat.getColor(context, R.color.ads_accent_color), 0)
                .build();
    }
}
