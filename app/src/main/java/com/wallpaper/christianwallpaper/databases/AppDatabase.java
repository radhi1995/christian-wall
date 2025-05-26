package com.wallpaper.christianwallpaper.databases;


import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.wallpaper.christianwallpaper.models.Favorite;
import com.wallpaper.christianwallpaper.models.RecentImage;
import com.wallpaper.christianwallpaper.models.SavedImage;

@Database(entities = {Favorite.class, SavedImage.class, RecentImage.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract FavoriteDao favoriteDao();
    public abstract SavedImageDao savedImageDao();
    public abstract RecentImageDao recentImageDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "app_database")
                    .fallbackToDestructiveMigration(true)
                    .build();
        }
        return instance;
    }
}
