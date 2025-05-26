package com.wallpaper.christianwallpaper.utils;

import android.content.Context;

import com.google.gson.Gson;
import com.rdev.coreutils.DevPrefManager;
import com.rdev.coreutils.utils.AESNameEncryption;
import com.rdev.coreutils.utils.DevUtils;
import com.wallpaper.christianwallpaper.ChristianApplication;
import com.wallpaper.christianwallpaper.models.ImageItemModel;
import com.wallpaper.christianwallpaper.models.WallModel;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApiDataManger {
    public static final String TAG = ApiDataManger.class.getSimpleName();
    public static final String WALL_MODEL_KEY = "api_data";
    private static final int LOAD_BATCH_SIZE = 27; // Number of items to load per batch
    private static ApiDataManger _instance;
    private DevPrefManager prefManager;
    private Gson gson;
    private WallModel _wallModel;
    private Context _context;
    private LinkedList<ImageItemModel> allDataItems = new LinkedList<>();
    private int currentPopularLoadIndex = 0; // Tracks the current load position
    private int currentLatestLoadIndex = 0; // Tracks the current load position
    public ApiDataManger(Context context) {
        this._context = context.getApplicationContext();
        prefManager = DevPrefManager.getInstance(context);
        gson = new Gson();

        String json = prefManager.getString(WALL_MODEL_KEY, null);
        if (json != null && !json.isEmpty()) {
            _wallModel = gson.fromJson(json, WallModel.class);
        }

        prepareImageDataItems();
    }

    public static ApiDataManger getInstance(Context context) {
        if (_instance == null) {
            _instance = new ApiDataManger(context);
        }
        return _instance;
    }

    public boolean isNeedToCallApi() {
        // Check if wall model is null
        if (_wallModel == null || _wallModel.lastSystemMilliseconds == 0) {
            return true; // Call API if wall data is null
        }

        // Get the last call time from the model
        long lastCallTime = _wallModel.lastSystemMilliseconds;
        long currentTime = System.currentTimeMillis();

        // Check if more than 24 hours have passed
        // 24 hours in milliseconds
        // Return true if either condition is met
        return (currentTime - lastCallTime > 24 * 60 * 60 * 1000);
    }

    public void saveApiData(WallModel wallModel) {
        wallModel.lastSystemMilliseconds = System.currentTimeMillis();
        this._wallModel = wallModel;

        String json = gson.toJson(_wallModel);
       prefManager.putString(WALL_MODEL_KEY, json);
        prepareImageDataItems();
    }

    public WallModel getWallModelData() {
        return _wallModel;
    }

    public String getPrivacyPolicy() {
        return _wallModel != null? _wallModel.privacy : "";
    }

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private void prepareImageDataItems() {
        executorService.execute(() -> {
            if (_wallModel == null) {
                return;
            }

            int imageCount = _wallModel.wallCount;
            String wallRawDirectory = DevUtils.getDomain() + _wallModel.raw_dir;
            allDataItems.clear();
            for (int i = 1; i <= imageCount; i++) {
                String keyEncrypted = AESNameEncryption.encryptKey(String.valueOf(i));
                String imageWithPath = wallRawDirectory + keyEncrypted;
                allDataItems.add(new ImageItemModel(String.valueOf(i), keyEncrypted, imageWithPath));
            }
        });
    }

//    public LinkedList<ImageItemModel> getPopularList() {
//        return popularList;
//    }
//
//    public LinkedList<ImageItemModel> getLatestList() {
//        return latestList;
//    }

    public LinkedList<ImageItemModel> getLoadedPopularItems() {
        if (currentPopularLoadIndex >= allDataItems.size()) {
            return allDataItems;
        }

        int start = 0;
        int end = currentPopularLoadIndex;
        if (end == 0) {
            end = Math.min(currentPopularLoadIndex + LOAD_BATCH_SIZE, allDataItems.size());
        }

        // Add next batch of data
        currentPopularLoadIndex = end;

        return new LinkedList<>(
                new LinkedList<>(allDataItems).subList(start, end));
    }
    public LinkedList<ImageItemModel> getNextPopularData() {
        if (currentPopularLoadIndex >= allDataItems.size()) {
            return allDataItems;
        }
        int start = currentPopularLoadIndex;
        int end = Math.min(currentPopularLoadIndex + LOAD_BATCH_SIZE, allDataItems.size());

        // Add next batch of data
        currentPopularLoadIndex = end;

        return new LinkedList<>(
                new LinkedList<>(allDataItems).subList(start, end));
    }

    public LinkedList<ImageItemModel> getLoadedLatestItems() {
        LinkedList<ImageItemModel> reverseData = new LinkedList<>(allDataItems);
        Collections.reverse(reverseData);
        if (currentLatestLoadIndex >= allDataItems.size()) {
            return reverseData;
        }

        int start = 0;
        int end = currentLatestLoadIndex;
        if (end == 0) {
            end = Math.min(currentLatestLoadIndex + LOAD_BATCH_SIZE, reverseData.size());
        }

        // Add next batch of data
        currentLatestLoadIndex = end;

        return new LinkedList<>(
                new LinkedList<>(reverseData).subList(start, end));
    }
    public LinkedList<ImageItemModel> getNextLatestData() {
        LinkedList<ImageItemModel> reverseData = new LinkedList<>(allDataItems);
        Collections.reverse(reverseData);
        if (currentLatestLoadIndex >= allDataItems.size()) {
            return reverseData;
        }
        int start = currentLatestLoadIndex;
        int end = Math.min(currentLatestLoadIndex + LOAD_BATCH_SIZE, reverseData.size());

        // Add next batch of data
        currentLatestLoadIndex = end;
        return new LinkedList<>(
                new LinkedList<>(reverseData).subList(start, end));
    }

    public ArrayList<String> moveToFirstList(List<ImageItemModel> list, String imageId) {
        ArrayList<String> moveFirstList = new ArrayList<>();
        if (list == null || imageId == null || list.isEmpty()) {
            return moveFirstList; // Return the original list if it's null, empty, or the imageId is null
        }

        ImageItemModel targetItem = null;

        // Find the item with the matching imageId
        for (ImageItemModel item : list) {
            if (imageId.equals(item.getImageKey())) {
                targetItem = item;
            } else {
                moveFirstList.add(item.getImageKey());
            }
        }

        // If the target item is found, rearrange the list
        if (targetItem != null) {
            moveFirstList.add(0, targetItem.getImageKey());
        }

        return moveFirstList;
    }

    public String getImagePath(String keyEncrypted) {
        String wallRawDirectory = DevUtils.getDomain() + _wallModel.raw_dir;
        return wallRawDirectory + keyEncrypted;
    }

    public String getLocalImagePath(String imageId) {
        if (_context == null) {
            _context = ChristianApplication.getInstance();
        }
        File filePath = new File(AppUtils.getAppSpecificDirectoryWithAppDir(_context), imageId + AppUtils.IMAGE_EXTENSION);
        return filePath.getAbsolutePath();
    }

    public long getInterstitialMilli() {
//        return 1 * 30 * 1000;
        if (_wallModel == null || _wallModel.adsInterval == 0) {
            return 2 * 60 * 1000;
        }

        return (long) _wallModel.adsInterval * 60 * 1000;
    }

    public boolean isAdsEnabled() {
        return ChristianApplication.isMobileAdsInitializeCalled && _wallModel != null && _wallModel.ads_enable;
    }
}
