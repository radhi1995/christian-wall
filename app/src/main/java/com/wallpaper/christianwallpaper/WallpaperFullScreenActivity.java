package com.wallpaper.christianwallpaper;

import static com.wallpaper.christianwallpaper.utils.AppUtils.SHOW_ADS_DIALOG_DELAY;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.rdev.coreutils.ads.DevInterstitialAdTimeInterval;
import com.rdev.coreutils.event.DevEvent;
import com.rdev.coreutils.utils.DevUtils;
import com.rdev.coreutils.utils.MediaUtils;
import com.wallpaper.christianwallpaper.databinding.ActivityWallpaperFullScreenBinding;
import com.wallpaper.christianwallpaper.databinding.PagerListItemBinding;
import com.wallpaper.christianwallpaper.dialogs.ConfirmActionDialog;
import com.wallpaper.christianwallpaper.dialogs.CustomInformationDialog;
import com.wallpaper.christianwallpaper.dialogs.ProcessDialogFragment;
import com.wallpaper.christianwallpaper.dialogs.SetWallpaperOptionDialog;
import com.wallpaper.christianwallpaper.utils.ApiDataManger;
import com.wallpaper.christianwallpaper.utils.AppUtils;
import com.wallpaper.christianwallpaper.utils.DatabaseRepository;
import com.wallpaper.christianwallpaper.utils.PermissionsUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WallpaperFullScreenActivity extends AppCompatActivity {
    public static final String TAG = WallpaperFullScreenActivity.class.getSimpleName();
    public static final String ARG_IMAGE_LIST = "image_list";
    public static final String ARG_FROM_SAVED = "is_from_saved";
    private ActivityWallpaperFullScreenBinding _binding;
    private List<String> _imageIds = new ArrayList<>();
    private ApiDataManger _apiDataManger;
    private DatabaseRepository _databaseRepository;

    private String _setAsWallpaperEventId;
    private String _filePath;
    private ImagePagerAdapter imageModelAdapter;
    private boolean isSavedImage;
    private String _confirmDeleteActionDialogEventId;
    private String _customInfoDialogEventId;
    private DevInterstitialAdTimeInterval _interstitialAdManager;

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        PagerListItemBinding itemBinding;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            itemBinding = PagerListItemBinding.bind(itemView);
        }
    }

    public class ImagePagerAdapter extends RecyclerView.Adapter<ImageViewHolder> {

        @NonNull
        @Override
        public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(WallpaperFullScreenActivity.this).inflate(R.layout.pager_list_item, parent, false);
            return new ImageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
            String fileName = _imageIds.get(position) + AppUtils.IMAGE_EXTENSION;
            final Uri fileUri;
            if (!isSavedImage) {
                fileUri = Uri.parse(_apiDataManger.getImagePath(_imageIds.get(position)));
            } else {
                fileUri = Uri.fromFile(new File(MediaUtils.getImageFile(WallpaperFullScreenActivity.this, getString(R.string.app_name), fileName)));
            }

            Glide.with(WallpaperFullScreenActivity.this)
                    .load(fileUri)
                    .centerCrop()
                    .placeholder(R.drawable.place_holder)
                    .into(holder.itemBinding.imgFullScreen);

            holder.itemBinding.imgFullScreen.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    gestureDetector.onTouchEvent(event);
                    return true;
                }

                private final GestureDetector gestureDetector = new GestureDetector(WallpaperFullScreenActivity.this, new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDoubleTap(@NonNull MotionEvent e) {
                        if (!isSavedImage) {

                            Animation pulse_fade = AnimationUtils.loadAnimation(WallpaperFullScreenActivity.this, R.anim.pulse_fade_in);
                            pulse_fade.setAnimationListener(new Animation.AnimationListener() {
                                @Override
                                public void onAnimationStart(Animation animation) {
                                    _binding.imgZoomFavorite.setVisibility(View.VISIBLE);
                                }

                                @Override
                                public void onAnimationEnd(Animation animation) {
                                    _binding.imgZoomFavorite.setVisibility(View.GONE);
                                }

                                @Override
                                public void onAnimationRepeat(Animation animation) {

                                }
                            });
                            _binding.imgZoomFavorite.startAnimation(pulse_fade);

                            String imageId = _imageIds.get(_binding.pagerImages.getCurrentItem());
                            boolean isLike = _databaseRepository.isFavorite(imageId);
                            if (isLike) {
                                _binding.imgFavorite.setIcon(AppCompatResources.getDrawable(WallpaperFullScreenActivity.this, R.drawable.ic_favorite_off));
                                _binding.imgZoomFavorite.setImageResource(R.drawable.ic_favorite_off);
                            } else {
                                _binding.imgFavorite.setIcon(AppCompatResources.getDrawable(WallpaperFullScreenActivity.this, R.drawable.ic_favorite_on));
                                _binding.imgZoomFavorite.setImageResource(R.drawable.ic_favorite_on);
                            }
                            _databaseRepository.toggleFavoriteImage(imageId);
                        }
                        return super.onDoubleTap(e);

                    }

                    @Override
                    public boolean onSingleTapUp(@NonNull MotionEvent e) {
                        showHideView();
                        return super.onSingleTapUp(e);
                    }
                });
            });

        }

        @Override
        public int getItemCount() {
            return _imageIds.size();
        }
    }

    ViewPager2.OnPageChangeCallback onPageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);

            if (position != 0 && _interstitialAdManager != null && _interstitialAdManager.isThisClickShowAds()) {
                ProcessDialogFragment.showDialog(getSupportFragmentManager(), WallpaperFullScreenActivity.this, getString(R.string.ads_loading), false, false);
                new Handler().postDelayed(() -> {
                    if (DevUtils.isActivityFinishing(WallpaperFullScreenActivity.this)) {
                        return;
                    }
                    ProcessDialogFragment.dismissDialog(getSupportFragmentManager(), WallpaperFullScreenActivity.this);
                    _interstitialAdManager.showAdOrContinue(WallpaperFullScreenActivity.this, new Runnable() {
                        @Override
                        public void run() {
                            changePage(position);
                        }
                    });
                }, SHOW_ADS_DIALOG_DELAY);
            } else {
                changePage(position);
            }
        }
    };

    private void changePage(int position) {
        if (_imageIds.isEmpty()) {
            return;
        }

        String imageId = _imageIds.get(position);
        _filePath = _apiDataManger.getImagePath(imageId);
        boolean isLike = _databaseRepository.isFavorite(imageId);

        if (isLike) {
            _binding.imgFavorite.setIcon(AppCompatResources.getDrawable(WallpaperFullScreenActivity.this, R.drawable.ic_favorite_on));
            _binding.imgZoomFavorite.setImageResource(R.drawable.ic_favorite_on);
        } else {
            _binding.imgFavorite.setIcon(AppCompatResources.getDrawable(WallpaperFullScreenActivity.this, R.drawable.ic_favorite_off));
            _binding.imgZoomFavorite.setImageResource(R.drawable.ic_favorite_off);
        }

        if (!isSavedImage) {
            _binding.buttonDelete.setVisibility(View.GONE);
            _binding.imgSaved.setVisibility(View.VISIBLE);
            _binding.imgFavorite.setVisibility(View.VISIBLE);
            _databaseRepository.addToRecentViewImage(imageId);
        } else {
            _binding.buttonDelete.setVisibility(View.VISIBLE);
            _binding.imgSaved.setVisibility(View.GONE);
            _binding.imgFavorite.setVisibility(View.GONE);
        }
    }

    void showHideView() {
        if (_binding.main.getVisibility() == View.VISIBLE) {
            _binding.main.setVisibility(View.GONE);
        } else {
            _binding.main.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppUtils.setStatusBarTransparent(WallpaperFullScreenActivity.this);
        AppUtils.setNavigationBarColor(true, WallpaperFullScreenActivity.this);

        _binding = ActivityWallpaperFullScreenBinding.inflate(getLayoutInflater());
        setContentView(_binding.getRoot());

        AppUtils.setSpaceOnSystemBar(_binding.main);

        _imageIds = getIntent().getStringArrayListExtra(ARG_IMAGE_LIST);
        isSavedImage = getIntent().getBooleanExtra(ARG_FROM_SAVED, false);
        if (_imageIds == null || _imageIds.isEmpty()) {
            finish();
        }
        _apiDataManger = ApiDataManger.getInstance(this);
        _databaseRepository = DatabaseRepository.getInstance(this);
        if (_apiDataManger.isAdsEnabled()) {
            _interstitialAdManager = DevInterstitialAdTimeInterval.getInstance(this, getString(R.string.admob_interstitial), _apiDataManger.getInterstitialMilli());
        }

        _binding.imgFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                favoriteFunction();
            }
        });

        _binding.buttonSetWallpaper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!PermissionsUtils.arePermissionsGranted(WallpaperFullScreenActivity.this, AppUtils.setWallpaperPermissions)) {
                    requestMultiplePermissionForSetWallpaper(AppUtils.setWallpaperPermissions);
                } else {
                    performSetWallpaperAfterPermission();
                }
            }
        });

        _binding.imgSaved.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!PermissionsUtils.arePermissionsGranted(WallpaperFullScreenActivity.this, AppUtils.saveImagePermissions)) {
                    requestMultiplePermissionForStorage(AppUtils.saveImagePermissions);
                } else {
                    showAdsOrContinue();
                }
            }
        });

        _binding.imgShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fileName = _imageIds.get(_binding.pagerImages.getCurrentItem()) + AppUtils.IMAGE_EXTENSION;
                String filePath = MediaUtils.getImageFile(WallpaperFullScreenActivity.this, getString(R.string.app_name), fileName);

                File file = new File(filePath);
                if (file.exists()) {
                    shareImagePath();
                } else {
                    Glide.with(WallpaperFullScreenActivity.this)
                            .asBitmap()
                            .load(_filePath)
                            .into(new CustomTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                    MediaUtils.saveBitmapAsJPGToAppSpecific(WallpaperFullScreenActivity.this, resource, getString(R.string.app_name), fileName).observe(WallpaperFullScreenActivity.this, isSaved -> {
                                        if (!isSaved) {
                                            return;
                                        }
                                        _databaseRepository.addToSavedImage(_imageIds.get(_binding.pagerImages.getCurrentItem()));
                                        shareImagePath();
                                    });


                                }

                                @Override
                                public void onLoadCleared(@Nullable Drawable placeholder) {

                                }
                            });


                }
            }
        });

        _binding.buttonDelete.setOnClickListener(v -> {
            showDeleteConfirmDialog();
        });

        _binding.imgBack.setOnClickListener(v -> {
            Intent intent = new Intent();
            setResult(RESULT_OK, intent);
            finish();
        });

        imageModelAdapter = new ImagePagerAdapter();
        _binding.pagerImages.setAdapter(imageModelAdapter);
        onPageChangeCallback.onPageSelected(0);

        // Add the callback to the back button dispatcher
        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    private void shareImagePath() {
        String fileName = _imageIds.get(_binding.pagerImages.getCurrentItem()) + AppUtils.IMAGE_EXTENSION;
        // Create the intent with the image file URI
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/*");

        String filePath = MediaUtils.getImageFile(this, getString(R.string.app_name), fileName);

        // Get the URI of the image file
        Uri imageUri = FileProvider.getUriForFile(
                this,
                getPackageName(),
                new File(filePath));

        // Set the URI and other extras (description and app link) for the intent
        intent.putExtra(Intent.EXTRA_STREAM, imageUri);
        intent.putExtra(Intent.EXTRA_TEXT, AppUtils.getPlayStoreLink(this, getPackageName()));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // Start the sharing activity
        startActivity(Intent.createChooser(intent, "Share Image"));
    }

    public void requestMultiplePermissionForStorage(String[] permissions) {
        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(WallpaperFullScreenActivity.this, permission)) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            showRationaleDialog(getString(R.string.permissions_required), getString(R.string.missing_permission_dialog, getString(R.string.app_name)));
        } else {
            requestMultiplePermissionsForStorage.launch(permissions);
        }
    }

    private final ActivityResultLauncher<String[]> requestMultiplePermissionsForStorage =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissionsResult -> {
                // Check if all permissions are granted
                boolean allPermissionsGranted = PermissionsUtils.arePermissionsGranted(WallpaperFullScreenActivity.this, AppUtils.saveImagePermissions);

                if (allPermissionsGranted) {
                    performSaveImageToGalleryAfterPermission();
                } else {
                    // At least one permission denied, inform the user or handle the denial gracefully
                    // You can also iterate through permissionsResult to find out which specific permissions were denied
                    for (Map.Entry<String, Boolean> permission : permissionsResult.entrySet()) {
                        if (!permission.getValue()) {
                            break;
                        }
                    }
                }
            });

    private final ActivityResultLauncher<String[]> requestMultiplePermissionsForSetWallpaper =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissionsResult -> {
                // Check if all permissions are granted
                boolean allPermissionsGranted = PermissionsUtils.arePermissionsGranted(WallpaperFullScreenActivity.this, AppUtils.setWallpaperPermissions);

                if (allPermissionsGranted) {
                    performSetWallpaperAfterPermission();
                } else {
                    // At least one permission denied, inform the user or handle the denial gracefully
                    // You can also iterate through permissionsResult to find out which specific permissions were denied
                    for (Map.Entry<String, Boolean> permission : permissionsResult.entrySet()) {
                        if (!permission.getValue()) {
                            break;
                        }
                    }
                }
            });

    public void requestMultiplePermissionForSetWallpaper(String[] permissions) {
        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(WallpaperFullScreenActivity.this, permission)) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            showRationaleDialog(getString(R.string.permissions_required), getString(R.string.missing_permission_dialog, getString(R.string.app_name)));
        } else {
            requestMultiplePermissionsForSetWallpaper.launch(permissions);
        }
    }

    private void performSetWallpaperAfterPermission() {
        String fileName = _imageIds.get(_binding.pagerImages.getCurrentItem()) + AppUtils.IMAGE_EXTENSION;

        String filePath = MediaUtils.getImageFile(this, getString(R.string.app_name), fileName);
        File file = new File(filePath);
        if (file.exists()) {
            showSetAsWallpaperDialog();
        } else {
            Glide.with(this)
                    .asBitmap()
                    .load(_filePath)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            MediaUtils.saveBitmapAsJPGToAppSpecific(WallpaperFullScreenActivity.this, resource, getString(R.string.app_name), fileName).observe(WallpaperFullScreenActivity.this, isSaved -> {
                                if (!isSaved) {
                                    return;
                                }
                                _databaseRepository.addToSavedImage(_imageIds.get(_binding.pagerImages.getCurrentItem()));
                                showSetAsWallpaperDialog();
                            });


                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });
        }
    }

    private void showRationaleDialog(String tittle, String message) {
        new AlertDialog.Builder(WallpaperFullScreenActivity.this)
                .setTitle(tittle)
                .setMessage(message)
                .setPositiveButton(getString(R.string.common_action_allow), (dialog, which) -> {
                    // Open app settings to allow the user to enable the permission manually
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton(getString(R.string.common_action_deny), (dialog, which) -> {
                    // Handle denial gracefully, if needed
                })
                .show();
    }


    private void showSetAsWallpaperDialog() {
        _setAsWallpaperEventId = DevEvent.newEventId();
        DevEvent.getInstance().getEvent(_setAsWallpaperEventId).observe(this, result -> {
            if (result == null) {
                return;
            }

            int res = DevEvent.getInt(result);
            if (res == SetWallpaperOptionDialog.RESULT_HOME_SCREEN) {
                setDeviceWallpaper(SetWallpaperOptionDialog.RESULT_HOME_SCREEN);
            } else if (res == SetWallpaperOptionDialog.RESULT_LOCK_SCREEN) {
                setDeviceWallpaper(SetWallpaperOptionDialog.RESULT_LOCK_SCREEN);
            } else if (res == SetWallpaperOptionDialog.RESULT_BOTH) {
                setDeviceWallpaper(SetWallpaperOptionDialog.RESULT_BOTH);
            }
        });
        if (!getSupportFragmentManager().isDestroyed()) {
            SetWallpaperOptionDialog.showDialog(getSupportFragmentManager(), _setAsWallpaperEventId);
        }
    }

    private void setDeviceWallpaper(int where) {
        String fileName = _imageIds.get(_binding.pagerImages.getCurrentItem()) + AppUtils.IMAGE_EXTENSION;
        AppUtils.setWallpaperByWhere(this, fileName, where).observe(this, isSuccess -> {
            if (isSuccess == null || !isSuccess) {
                Toast.makeText(this, getString(R.string.error_set_wallpaper), Toast.LENGTH_SHORT).show();
                return;
            }

//            Toast.makeText(this, getString(R.string.set_wallpaper_successfully_message), Toast.LENGTH_SHORT).show();
            showCustomInformationDialog("Success", getString(R.string.set_wallpaper_successfully_message), true);
        });


    }

    private void showAdsOrContinue() {
        if (_interstitialAdManager != null &&_interstitialAdManager.isThisClickShowAds()) {
            ProcessDialogFragment.showDialog(getSupportFragmentManager(), this, getString(R.string.ads_loading), false, false);
            new Handler().postDelayed(() -> {
                if (DevUtils.isActivityFinishing(WallpaperFullScreenActivity.this)) {
                    return;
                }
                ProcessDialogFragment.dismissDialog(getSupportFragmentManager(), WallpaperFullScreenActivity.this);
                _interstitialAdManager.showAdOrContinue(WallpaperFullScreenActivity.this, new Runnable() {
                    @Override
                    public void run() {
                        performSaveImageToGalleryAfterPermission();
                    }
                });
            }, SHOW_ADS_DIALOG_DELAY);
        } else {
            performSaveImageToGalleryAfterPermission();
        }
    }
    private void performSaveImageToGalleryAfterPermission() {
        String fileName = _imageIds.get(_binding.pagerImages.getCurrentItem()) + AppUtils.IMAGE_EXTENSION;
        String filePath = MediaUtils.getImageFile(this, getString(R.string.app_name), fileName);
        File file = new File(filePath);
        if (file.exists()) {
            _databaseRepository.addToSavedImage(_imageIds.get(_binding.pagerImages.getCurrentItem()));
            startCopyFileToStorage();
        } else {
            Glide.with(this)
                    .asBitmap()
                    .load(_filePath)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            MediaUtils.saveBitmapAsJPGToAppSpecific(WallpaperFullScreenActivity.this, resource, getString(R.string.app_name), fileName).observe(WallpaperFullScreenActivity.this, isSaved -> {
                                if (!isSaved) {
                                    return;
                                }
                                _databaseRepository.addToSavedImage(_imageIds.get(_binding.pagerImages.getCurrentItem()));
                                startCopyFileToStorage();
                            });


                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });
        }
    }

    private void startCopyFileToStorage() {
        String fileName = _imageIds.get(_binding.pagerImages.getCurrentItem()) + AppUtils.IMAGE_EXTENSION;
        String galleryImageFileName = System.currentTimeMillis() + AppUtils.IMAGE_EXTENSION;
        String filePath = MediaUtils.getImageFile(this, getString(R.string.app_name), fileName);
        MediaUtils.copySourceImageToGallery(this, filePath, galleryImageFileName).observe(this, saveFinished -> {
            if (saveFinished == null) {
                return;
            }
            if (saveFinished) {
                showCustomInformationDialog("Success", getString(R.string.save_successfully_message), true);
//                Toast.makeText(WallpaperFullScreenActivity.this, getString(R.string.save_successfully_message), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(WallpaperFullScreenActivity.this, getString(R.string.error_wallpaper_saved_message), Toast.LENGTH_SHORT).show();
            }
        });
    }

    void favoriteFunction() {
        if (isSavedImage) {
            return;
        }
        String imageId = _imageIds.get(_binding.pagerImages.getCurrentItem());
        boolean isLike = _databaseRepository.isFavorite(imageId);

        Animation pulse_fade = AnimationUtils.loadAnimation(WallpaperFullScreenActivity.this, R.anim.pulse_fade_in);
        pulse_fade.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                _binding.imgZoomFavorite.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                _binding.imgZoomFavorite.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        _binding.imgZoomFavorite.startAnimation(pulse_fade);
        if (isLike) {
            _binding.imgFavorite.setIcon(AppCompatResources.getDrawable(WallpaperFullScreenActivity.this, R.drawable.ic_favorite_off));
            _binding.imgZoomFavorite.setImageResource(R.drawable.ic_favorite_off);

        } else {
            _binding.imgFavorite.setIcon(AppCompatResources.getDrawable(WallpaperFullScreenActivity.this, R.drawable.ic_favorite_on));
            _binding.imgZoomFavorite.setImageResource(R.drawable.ic_favorite_on);

        }

        _databaseRepository.toggleFavoriteImage(imageId);
    }


    private void showDeleteConfirmDialog() {
        _confirmDeleteActionDialogEventId = DevEvent.newEventId();

        DevEvent.getInstance().getEvent(_confirmDeleteActionDialogEventId).observe(this, new Observer<Bundle>() {
            @Override
            public void onChanged(Bundle result) {
                if (result == null) {
                    return;
                }

                int requestType = DevEvent.getInt(result);
                if (requestType == ConfirmActionDialog.RESULT_POSITIVE) {
                    String fileName = _imageIds.get(_binding.pagerImages.getCurrentItem()) + AppUtils.IMAGE_EXTENSION;
                    String filePath = MediaUtils.getImageFile(WallpaperFullScreenActivity.this, getString(R.string.app_name), fileName);

                    File file = new File(filePath);
                    if (file.delete()) {
                        _imageIds.remove(_binding.pagerImages.getCurrentItem());
                        imageModelAdapter.notifyDataSetChanged();
                        Toast.makeText(WallpaperFullScreenActivity.this, "Image deleted successfully", Toast.LENGTH_SHORT).show();

                        if (_imageIds.isEmpty()) {
                            Intent intent = new Intent();
                            setResult(RESULT_OK, intent);
                            finish();
                        }
                    }
                } else if (requestType == ConfirmActionDialog.RESULT_NEGATIVE) {

                }
            }
        });

        ConfirmActionDialog.showDialog(getSupportFragmentManager(), this, _confirmDeleteActionDialogEventId, "Delete Image?", "Are you sure you want to delete this image? This action cannot be undone.", "Delete", "Cancel", true);
    }

    private void showCustomInformationDialog(String title, String message, boolean isAdsEnable) {
        _customInfoDialogEventId = DevEvent.newEventId();

        DevEvent.getInstance().getEvent(_customInfoDialogEventId).observe(this, new Observer<Bundle>() {
            @Override
            public void onChanged(Bundle result) {
                if (result == null) {
                    return;
                }

            }
        });

        CustomInformationDialog.showDialog(getSupportFragmentManager(), this, _customInfoDialogEventId, title, message, isAdsEnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        _binding.pagerImages.unregisterOnPageChangeCallback(onPageChangeCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        _binding.pagerImages.registerOnPageChangeCallback(onPageChangeCallback);
    }
}