package com.wallpaper.christianwallpaper.databases;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.wallpaper.christianwallpaper.models.RecentImage;

import java.util.List;

@Dao
public interface RecentImageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRecentImage(RecentImage recentImage);

    @Delete
    void deleteRecentImage(RecentImage recentImage);

    @Query("DELETE FROM recent_images WHERE image_id = :imageId")
    void deleteByImageId(String imageId);

    @Query("SELECT * FROM recent_images ORDER BY id DESC")
    LiveData<List<RecentImage>> getAllRecentImages();
}
