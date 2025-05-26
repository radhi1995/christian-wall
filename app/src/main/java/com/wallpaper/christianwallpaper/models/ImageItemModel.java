package com.wallpaper.christianwallpaper.models;

public class ImageItemModel {
    private String decryptName;
    private String imageKey;
    private String imagePath;

    public ImageItemModel(String decryptName, String imageKey, String imagePath) {
        this.decryptName = decryptName;
        this.imageKey = imageKey;
        this.imagePath = imagePath;
    }

    public String getDecryptName() {
        return decryptName;
    }

    public void setDecryptName(String decryptName) {
        this.decryptName = decryptName;
    }

    public String getImageKey() {
        return imageKey;
    }

    public void setImageKey(String imageKey) {
        this.imageKey = imageKey;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
}
