package com.wallpaper.christianwallpaper.utils;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.wallpaper.christianwallpaper.databases.AppDatabase;
import com.wallpaper.christianwallpaper.databases.FavoriteDao;
import com.wallpaper.christianwallpaper.databases.RecentImageDao;
import com.wallpaper.christianwallpaper.databases.SavedImageDao;
import com.wallpaper.christianwallpaper.models.Favorite;
import com.wallpaper.christianwallpaper.models.RecentImage;
import com.wallpaper.christianwallpaper.models.SavedImage;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseRepository {
    private static DatabaseRepository instance;
    private final FavoriteDao favoriteDao;
    private final SavedImageDao savedImageDao;
    private final RecentImageDao recentImageDao;
    private final ExecutorService executorService;
    private Set<String> favoriteIds = new HashSet<>();
    private Set<String> savedImageIds = new HashSet<>();
    private MutableLiveData<SavedImage> savedImageMutableLiveData = new MutableLiveData<>();
    private Context _context;
    public DatabaseRepository(Context context) {
        this._context = context.getApplicationContext();
        // Initialize the database and DAO
        AppDatabase database = AppDatabase.getInstance(context);
        favoriteDao = database.favoriteDao();
        savedImageDao = database.savedImageDao();
        recentImageDao = database.recentImageDao();

        // Executor for background tasks
        executorService = Executors.newSingleThreadExecutor();

        executorService.execute(() -> {
            favoriteIds = new HashSet<>(favoriteDao.getAllFavoriteIds());
        });

        syncSavedImages();
    }

    public static DatabaseRepository getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseRepository(context);
        }
        return instance;
    }

    public LifecycleObserver lifecycleObserver = new DefaultLifecycleObserver() {
        public void onCreate(@NonNull LifecycleOwner lifecycleOwner) {
        }

        public void onResume(@NonNull LifecycleOwner lifecycleOwner) {
            syncSavedImages();
        }

        @Override
        public void onDestroy(@NonNull LifecycleOwner owner) {
        }
    };
    public LiveData<List<SavedImage>> getAllSavedImages() {
        return savedImageDao.getAllSavedImages();
    }

    public List<String> getAllSavedImageIds() {
        return new LinkedList<>(savedImageIds);
    }


    public LiveData<List<RecentImage>> getAllRecentViewImages() {
        return recentImageDao.getAllRecentImages();
    }

    public LiveData<List<Favorite>> getAllFavoriteImages() {
        return favoriteDao.getAllFavorites();
    }

    public void addToRecentViewImage(String imageId) {
        executorService.execute(() -> {
            RecentImage recentImage = new RecentImage();
            recentImage.image_id = imageId;
            recentImageDao.insertRecentImage(recentImage);
        });
    }

    public void syncSavedImages() {
        executorService.execute(() -> {
            List<String> savedImageList = savedImageDao.getAllSavedImageIds();

            String appSpecificDir = AppUtils.getAppSpecificDirectoryWithAppDir(_context);
            savedImageIds = new HashSet<>();
            for (String imageId: savedImageList) {
                File file = new File(appSpecificDir, imageId + AppUtils.IMAGE_EXTENSION);
                if (!file.exists()) {
                    savedImageDao.deleteByImageId(imageId);
                } else {
                    savedImageIds.add(imageId);
                }
            }

        });
    }
    public void addToSavedImage(String imageId) {
        executorService.execute(() -> {
            SavedImage savedImage = new SavedImage();
            savedImage.image_id = imageId;
            savedImageDao.insertSavedImage(savedImage);
            savedImageIds.add(imageId);
        });
    }

    public void deleteSavedImage(String imageId) {
        executorService.execute(() -> {
            savedImageDao.deleteByImageId(imageId);
            savedImageIds.remove(imageId);
        });
    }

    public void toggleFavoriteImage(String imageId) {
        executorService.execute(() -> {
            if (favoriteIds.contains(imageId)) {
                // Remove from favorites
                favoriteDao.deleteByImageId(imageId);
                favoriteIds.remove(imageId);
            } else {
                // Add to favorites
                Favorite favorite = new Favorite();
                favorite.image_id = imageId;
                favoriteDao.insertFavorite(favorite);
                favoriteIds.add(imageId);
            }
        });
    }

    public boolean isFavorite(String imageId) {
        return favoriteIds.contains(imageId);
    }
}
