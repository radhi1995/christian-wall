package com.wallpaper.christianwallpaper.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
@Entity(
        tableName = "saved_images",
        indices = {@Index(value = "image_id", unique = true)}
)
public class SavedImage {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    public String image_id;
}
