package com.wallpaper.christianwallpaper.databases;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.wallpaper.christianwallpaper.models.Favorite;

import java.util.List;

@Dao
public interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertFavorite(Favorite favorite);

    @Delete
    void deleteFavorite(Favorite favorite);

    @Query("DELETE FROM favorites WHERE image_id = :imageId")
    void deleteByImageId(String imageId);

    @Query("SELECT * FROM favorites ORDER BY id DESC")
    LiveData<List<Favorite>> getAllFavorites();

    @Query("SELECT image_id FROM favorites ORDER BY id DESC")
    List<String> getAllFavoriteIds();
}