package com.wallpaper.christianwallpaper.autowall;

import android.app.WallpaperManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import com.rdev.coreutils.utils.MediaUtils;
import com.wallpaper.christianwallpaper.R;
import com.wallpaper.christianwallpaper.utils.AppUtils;
import com.wallpaper.christianwallpaper.utils.DatabaseRepository;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

public class AutoLiveWallpaperService extends WallpaperService {

    private List<String> imageArraList;
    private DatabaseRepository databaseRepository;
    public AutoLiveWallpaperService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            databaseRepository = DatabaseRepository.getInstance(getApplicationContext());
            imageArraList = databaseRepository.getAllSavedImageIds();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public Engine onCreateEngine() {
        return new GalleryEngine();
    }

    public class GalleryEngine extends Engine implements OnSharedPreferenceChangeListener {

        private final Handler handler = new Handler();

        static final String SHARED_PREFS_NAME = "AMOLEDLiveWall";

        private final Random randGen = new Random();

        private final Runnable animate = new Runnable() {
            @Override
            public void run() {
                showNewImage();
            }
        };

        Rect surfaceFrame = getSurfaceHolder().getSurfaceFrame();
        private final BitmapFactory.Options options = new BitmapFactory.Options();
        private final BitmapFactory.Options optionsScale = new BitmapFactory.Options();

        private static final String TAG = "GalleryEngine";

        private Bitmap currentBitmap = null;
        private File currentFile = null;
        private long timer = 5000;
        private long timeStarted = 0;
        private final GestureDetector doubleTapDetector;

        private final Paint noImagesPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint imagePaint;

        private int xPixelOffset;
        private float xOffset;
        private float xOffsetStep;

        private boolean isRotate;
        private boolean isScrolling;
        private boolean isStretch;
        private boolean isTrim;

        private boolean allowClickToChange;

        private int desiredMinimumWidth;
        private int screenHeight;
        private int screenWidth;

        private int transition;
        private int currentAlpha;

        private boolean imageIsSetup = false;

        GalleryEngine() {
            allowClickToChange = false;
            currentAlpha = 0;
            imagePaint = new Paint();
            imagePaint.setAlpha(255);
            options.inTempStorage = new byte[16 * 1024];

            optionsScale.inTempStorage = options.inTempStorage;
            optionsScale.inSampleSize = 4;

            noImagesPaint.setTextAlign(Paint.Align.CENTER);
            noImagesPaint.setColor(Color.GRAY);
            noImagesPaint.setTextSize(24);
            // register the listener to detect preference changes
            SharedPreferences prefs = getSharedPreferences(SHARED_PREFS_NAME, 0);
            prefs.registerOnSharedPreferenceChangeListener(this);
            // initialize the starting preferences
            onSharedPreferenceChanged(prefs, null);
            setTouchEventsEnabled(true);
            doubleTapDetector = new GestureDetector(getApplicationContext(), new DoubleTapGestureListener(this));

            getScreenSize();
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            handler.removeCallbacks(fadeAnimate);
            handler.removeCallbacks(animate);
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep,
                                     float yOffsetStep, int xPixelOffset, int yPixelOffset) {

            if (this.xPixelOffset != xPixelOffset * -1 || this.xOffset != xOffset
                    || this.xOffsetStep != xOffsetStep) {
                this.xPixelOffset = xPixelOffset * -1;
                this.xOffset = xOffset;
                this.xOffsetStep = xOffsetStep;
                drawBitmap(currentBitmap);
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            handler.removeCallbacks(fadeAnimate);
            handler.removeCallbacks(animate);
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            this.desiredMinimumWidth = getDesiredMinimumWidth();
            // this.desiredMinimumHeight = getDesiredMinimumHeight();
            showNewImage();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            getScreenSize(); // onVisibilityChanged can be called on screen rotation.
            if (visible) {
                // if there is a bitmap with time left to keep around, redraw it
                if (currentFile != null && systemTime() - timeStarted + 100 < timer) {
                    // for some reason, it's sometimes recycled!
                    if (currentBitmap != null) {
                        if (currentBitmap.isRecycled()) {
                            currentBitmap = BitmapFactory.decodeFile(currentFile.getAbsolutePath());
                            imageIsSetup = false;
                        }
                        drawBitmap(currentBitmap);
                    } else {
                        WallpaperManager myWallpaperManager
                                = WallpaperManager.getInstance(getApplicationContext());
                        try {
                            myWallpaperManager.clear();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    // left over timer
                    handler.postDelayed(animate, timer - (systemTime() - timeStarted));
                }
                // otherwise draw a new one since it's time for a new one
                else {
                    showNewImage();
                }
            } else {
                handler.removeCallbacks(fadeAnimate);
                handler.removeCallbacks(animate);
            }
        }

        @Override
        public void onDesiredSizeChanged(int desiredWidth, int desiredHeight) {
            super.onDesiredSizeChanged(desiredWidth, desiredHeight);
            Log.v(TAG, "onDesiredSizeChanged");
            getScreenSize();
            this.desiredMinimumWidth = desiredWidth;

            drawBitmap(currentBitmap);
        }

        private void getScreenSize() {
            DisplayMetrics metrics = new DisplayMetrics();
            Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
            display.getMetrics(metrics);

            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                this.screenWidth = metrics.widthPixels;
                this.screenHeight = metrics.heightPixels;
            }
            this.screenHeight = metrics.heightPixels;
            this.screenWidth = metrics.widthPixels;
        }

        private void drawBitmap(Bitmap b) {
            if (b == null) {
                Log.d(TAG, "b == null!");
                /*
                 * { try { throw new RuntimeException(); } catch (Exception e) {
                 * e.printStackTrace(); } }
                 */
                return;
            }

            final SurfaceHolder holder = getSurfaceHolder();

            int virtualWidth = this.desiredMinimumWidth;

            // shouldn't happen
            if (surfaceFrame == null) {
                Log.d(TAG, "surfaceFrame == null!");
            }

            Rect window;
            Rect dstWindow = surfaceFrame;
            if (!isScrolling) {
                // virtual width becomes screen width
                virtualWidth = screenWidth;
                xPixelOffset = 0;
            }

            // not sure why but it happens
            if (virtualWidth == 0) {
                Log.d(TAG, "virtualWidth == 0 !!");
                return;
            }

            int virtualHeight = screenHeight;
            Log.d(TAG, "width = " + virtualWidth + " height = " + virtualHeight);

            // we only need to convert the image once, then we can reuse it
            if (b == currentBitmap && !imageIsSetup) {
                System.gc();

                float screenRatio = (float) virtualWidth / (float) virtualHeight;
                float bitmapRatio = (float) b.getWidth() / (float) b.getHeight();

                if (isTrim) {

                    // downscale the bitmap to make the trim detection faster
                    Matrix dmatrix = new Matrix();
                    float dscale = (float) 50 / b.getWidth();
                    dmatrix.postScale(dscale, dscale * bitmapRatio);
                    Bitmap c = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), dmatrix, true);
                    int trimLeft = (int) (BitmapHelper.sizeTrimmableLeft(c) * b.getWidth());
                    int trimRight = (int) (BitmapHelper.sizeTrimmableRight(c) * b.getWidth());
                    int trimTop = (int) (BitmapHelper.sizeTrimmableTop(c) * b.getHeight());
                    int trimBottom = (int) (BitmapHelper.sizeTrimmableBottom(c) * b.getHeight());
                    c.recycle();

                    // b = BitmapHelper.doTrim(b, trimLeft, trimRight, trimTop,
                    // trimBottom);

                    // we calculate the aspect ratio of the important part of the picture
                    bitmapRatio = (float) (b.getWidth() - trimLeft - trimRight)
                            / (float) (b.getHeight() - trimTop - trimBottom);
                }

                // rotate the bitmap if rotation is enabled and it would fit the screen
                // better
                if (isRotate
                        && ((screenRatio > 1 && bitmapRatio < 1) || (screenRatio < 1 && bitmapRatio > 1))) {
                    b = BitmapHelper.doRotate(b, 90);
                    // get the ratio again
                    bitmapRatio = (float) b.getWidth() / (float) b.getHeight();
                }

                if (isTrim) {
                    Matrix dmatrix = new Matrix();

                    int downScaleRes = 100;

                    float dscale = (float) downScaleRes / b.getWidth();
                    dmatrix.postScale(dscale, dscale * bitmapRatio);
                    Bitmap c = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), dmatrix, true);

                    final int extraBorder = 10;

                    int trimLeft = (int) (BitmapHelper.sizeTrimmableLeft(c) * b.getWidth()) - extraBorder;
                    int trimRight = (int) (BitmapHelper.sizeTrimmableRight(c) * b.getWidth()) - extraBorder;
                    int trimTop = (int) (BitmapHelper.sizeTrimmableTop(c) * b.getHeight()) - extraBorder;
                    int trimBottom = (int) (BitmapHelper.sizeTrimmableBottom(c) * b.getHeight())
                            - extraBorder;
                    c.recycle();

                    if (trimLeft < 0)
                        trimLeft = 0;
                    if (trimRight < 0)
                        trimRight = 0;
                    if (trimTop < 0)
                        trimTop = 0;
                    if (trimBottom < 0)
                        trimBottom = 0;

                    while (true) {
                        bitmapRatio = (float) (b.getWidth() - trimLeft - trimRight)
                                / (float) (b.getHeight() - trimTop - trimBottom);

                        if (trimLeft == 0 && trimRight == 0 && trimTop == 0 && trimBottom == 0
                                || Math.abs(screenRatio - bitmapRatio) < 0.01)
                            break;

                        if (bitmapRatio < screenRatio) {
                            // horizontal
                            if (trimLeft == 0 && trimRight == 0)
                                break;
                            if (trimLeft != 0)
                                trimLeft--;
                            if (trimRight != 0)
                                trimRight--;
                        } else {
                            // vertical
                            if (trimTop == 0 && trimBottom == 0)
                                break;
                            if (trimTop != 0)
                                trimTop--;
                            if (trimBottom != 0)
                                trimBottom--;
                        }
                    }
                    b = BitmapHelper.doTrim(b, trimLeft, trimRight, trimTop, trimBottom);

                }

                // scale the bitmap without distortion or cropping to fit the screen
                // as well as possible
                if (isStretch) {
                    Log.d(TAG, "pic dimensions: " + b.getWidth() + "x" + b.getHeight());
                    float scale;
                    if (virtualHeight - b.getHeight() < virtualWidth - b.getWidth()) {
                        // vertically
                        scale = (float) virtualHeight / b.getHeight();
                        Log.d(TAG, "vertical scale: " + scale);
                    } else {
                        // horizontally
                        scale = (float) virtualWidth / b.getWidth();
                        Log.d(TAG, "horizontal scale: " + scale);
                    }

                    b = BitmapHelper.doScale(b, scale);
                }

                b = centerCropBitmap(b, screenWidth, screenHeight);

                imageIsSetup = true;
                currentBitmap = b;
                System.gc();
            }

            int vertMargin = (b.getHeight() - virtualHeight) / 2;

            if (b.getWidth() >= virtualWidth && b.getHeight() >= virtualHeight) {
                int pictureHorizOffset = xPixelOffset + (b.getWidth() - virtualWidth) / 2;
                window = new Rect(pictureHorizOffset, vertMargin, pictureHorizOffset + surfaceFrame.right,
                        b.getHeight() - vertMargin);
            } else {
                int pictureHorizOffset = xPixelOffset + (b.getWidth() - virtualWidth) / 2;

                window = new Rect(0, 0, b.getWidth(), b.getHeight());
                dstWindow = new Rect(surfaceFrame);
                dstWindow.top = -vertMargin;
                dstWindow.bottom = b.getHeight() - vertMargin;
                dstWindow.left = -pictureHorizOffset;
                dstWindow.right = -pictureHorizOffset + b.getWidth();

//                dstWindow = new Rect(surfaceFrame);
//                dstWindow.top = 0;
//                dstWindow.bottom = holder.lockCanvas().getHeight();
//                dstWindow.left = 0;
//                dstWindow.right = holder.lockCanvas().getWidth();
            }

            Canvas c = null;
            try {
                c = holder.lockCanvas();
                if (c != null) {
                    c.drawColor(Color.BLACK);
//                    Rect srcRect = new Rect(0, 0, b.getWidth(), b.getHeight());
                    Rect dstRect = new Rect(0, 0, c.getWidth(), c.getHeight());
                    c.drawBitmap(b, window, dstRect, imagePaint);
//                    c.drawBitmap(centerCropBitmap(b, screenWidth, screenHeight), window, dstRect, imagePaint);
//                    c.drawBitmap(b, window, dstWindow, imagePaint);

                }
            } finally {
                if (c != null)
                    holder.unlockCanvasAndPost(c);
            }

        }

        private Bitmap centerCropBitmap(Bitmap srcBitmap, int targetWidth, int targetHeight) {
            if (srcBitmap == null) {
                return null;
            }

            float scale;
            float dx = 0, dy = 0;

            int srcWidth = srcBitmap.getWidth();
            int srcHeight = srcBitmap.getHeight();

            // Calculate scale to fit the target dimensions while maintaining aspect ratio
            if (srcWidth * targetHeight > targetWidth * srcHeight) {
                scale = (float) targetHeight / (float) srcHeight;
                dx = (targetWidth - srcWidth * scale) * 0.5f;
            } else {
                scale = (float) targetWidth / (float) srcWidth;
                dy = (targetHeight - srcHeight * scale) * 0.5f;
            }

            Matrix matrix = new Matrix();
            matrix.setScale(scale, scale);
            matrix.postTranslate(dx, dy);

            // Create a new bitmap with the cropped and scaled dimensions
            Bitmap croppedBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(croppedBitmap);
            Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
            canvas.drawBitmap(srcBitmap, matrix, paint);

            return croppedBitmap;
        }

        @Override
        public void onTouchEvent(MotionEvent event) {
            super.onTouchEvent(event);
            this.doubleTapDetector.onTouchEvent(event);
        }

        void showNewImage() {
            Log.d(TAG, "showNewImage");
            try {

                if (imageArraList != null && !imageArraList.isEmpty()) {
                    String fileName = imageArraList.get(randGen.nextInt(imageArraList.size())) + AppUtils.IMAGE_EXTENSION;

                    String filePath = MediaUtils.getImageFile(getApplicationContext(), getString(R.string.app_name), fileName);

                    File image = new File(filePath);
                    if (currentBitmap != null)
                        currentBitmap.recycle();
                    currentBitmap = null;
                    System.gc();

                    try {
                        currentFile = image;
                        currentBitmap = BitmapFactory.decodeFile(image.getAbsolutePath(), options);
                        imageIsSetup = false;

                        if (transition == AutoWallpaperSettingsActivity.FADE_TRANSITION) {
                            fadeTransition(currentBitmap, 0);
                        } else {
                            drawBitmap(currentBitmap);
                        }
                    } catch (OutOfMemoryError e) {
                        try {
                            System.gc();
                            Log.i(TAG, "Image too big, attempting to scale.");
                            currentBitmap = BitmapFactory.decodeFile(image.getAbsolutePath(), optionsScale);
                            drawBitmap(currentBitmap);
                            Log.i(TAG, "Scale successful.");
                        } catch (OutOfMemoryError e2) {
                            Log.e(TAG, "Scale failed: " + image.getAbsolutePath());
                            // skip to next image.
                            showNewImage();
                            return;
                        }
                    }
                } else {
//                drawTextHelper(getString(R.string.no_images_found));
                    WallpaperManager myWallpaperManager
                            = WallpaperManager.getInstance(getApplicationContext());
                    try {
                        myWallpaperManager.clear();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            } catch (Exception e) {
                WallpaperManager myWallpaperManager
                        = WallpaperManager.getInstance(getApplicationContext());
                try {
                    myWallpaperManager.clear();

                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            handler.removeCallbacks(animate);
            if (isVisible()) {
                handler.postDelayed(animate, timer);
                timeStarted = systemTime();
            }
        }

        private final Runnable fadeAnimate = new Runnable() {
            public void run() {
                fadeTransition(currentBitmap, currentAlpha);
            }
        };

        private void fadeTransition(Bitmap b, int alpha) {
            currentAlpha = alpha;
            currentAlpha += 255 / 25;
            if (currentAlpha > 255) {
                currentAlpha = 255;
            }
            // Log.v(TAG, "alpha " + currentAlpha);
            imagePaint.setAlpha(currentAlpha);
            drawBitmap(b);

            /*
             * This is how it animates. After drawing a frame, ask it to draw another
             * one.
             */
            handler.removeCallbacks(fadeAnimate);
            if (isVisible() && currentAlpha < 255) // stop when at full opacity
            {
                handler.post(fadeAnimate);
            }
        }

        private void drawTextHelper(String line) {
            final SurfaceHolder holder = getSurfaceHolder();

            Canvas c = null;
            try {
                c = holder.lockCanvas();
                if (c != null) {
                    c.drawColor(Color.BLACK);
                    c.drawText(line, surfaceFrame.right / 2, surfaceFrame.bottom / 2, noImagesPaint);
                }
            } finally {
                if (c != null)
                    holder.unlockCanvasAndPost(c);
            }
        }

        private long systemTime() {
            return System.nanoTime() / 1000000;
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPrefs, String keyChanged) {
            timer = Integer.parseInt(sharedPrefs.getString(AutoWallpaperSettingsActivity.TIMER_KEY, "5000"));
            isRotate = sharedPrefs.getBoolean(AutoWallpaperSettingsActivity.ROTATE_KEY, true);
            isScrolling = sharedPrefs.getBoolean(AutoWallpaperSettingsActivity.SCROLLING_KEY, true);
            isStretch = sharedPrefs.getBoolean(AutoWallpaperSettingsActivity.STRETCHING_KEY, false);
            isTrim = sharedPrefs.getBoolean(AutoWallpaperSettingsActivity.TRIM_KEY, true);
            allowClickToChange = sharedPrefs.getBoolean(AutoWallpaperSettingsActivity.CLICK_TO_CHANGE_KEY,
                    false);
            transition = Integer.parseInt(sharedPrefs.getString(AutoWallpaperSettingsActivity.TRANSITION_KEY, "0"));
            if (transition != AutoWallpaperSettingsActivity.FADE_TRANSITION) {
                imagePaint.setAlpha(255);
            }
        }

        boolean allowClickToChange() {
            return allowClickToChange;
        }
    }
}
