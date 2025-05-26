package com.wallpaper.christianwallpaper.data;

import com.wallpaper.christianwallpaper.models.WallModel;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface WallpaperInterface {
    @POST("christianApi")
    @FormUrlEncoded
    Call<WallModel> getAppInformation(@Field("app") String app);
}
