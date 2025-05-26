package com.wallpaper.christianwallpaper.utils;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;

public class PermissionsUtils {
    public static boolean arePermissionsGranted(Context context, String[] permissions) {
        for (String permission : permissions) {
            if (!isPermissionGranted(context, permission)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isPermissionGranted(Context context, String permission) {

        int result = ContextCompat.checkSelfPermission(context, permission);
        return (result == PackageManager.PERMISSION_GRANTED);
    }


}
