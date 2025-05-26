package com.wallpaper.christianwallpaper.autowall;

import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;

public class DoubleTapGestureListener extends SimpleOnGestureListener {

    private final AutoLiveWallpaperService.GalleryEngine galleryEngine;

    DoubleTapGestureListener(AutoLiveWallpaperService.GalleryEngine galleryEngine) {
        this.galleryEngine = galleryEngine;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        if (this.galleryEngine.allowClickToChange()) {
            galleryEngine.showNewImage();
        }
        return true;
    }


}
