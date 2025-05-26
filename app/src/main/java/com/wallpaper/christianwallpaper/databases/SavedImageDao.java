package com.wallpaper.christianwallpaper.databases;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.wallpaper.christianwallpaper.models.SavedImage;

import java.util.List;

@Dao
public interface SavedImageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSavedImage(SavedImage savedImage);

    @Delete
    void deleteSavedImage(SavedImage savedImage);

    @Query("DELETE FROM saved_images WHERE image_id = :imageId")
    void deleteByImageId(String imageId);

    @Query("SELECT * FROM saved_images ORDER BY id DESC")
    LiveData<List<SavedImage>> getAllSavedImages();

    @Query("SELECT image_id FROM saved_images ORDER BY id DESC")
    List<String> getAllSavedImageIds();
}
