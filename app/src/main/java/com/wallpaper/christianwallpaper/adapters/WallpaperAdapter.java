package com.wallpaper.christianwallpaper.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.wallpaper.christianwallpaper.ImageItemClicked;
import com.wallpaper.christianwallpaper.R;
import com.wallpaper.christianwallpaper.databinding.ImageListItemBinding;
import com.wallpaper.christianwallpaper.models.ImageItemModel;

import java.util.LinkedList;

public class WallpaperAdapter extends RecyclerView.Adapter<WallpaperAdapter.ImageViewHolder> {

    private Context _context;
    private LinkedList<ImageItemModel> _imageList;
    private ImageItemClicked _listener;

    public WallpaperAdapter(Context context, LinkedList<ImageItemModel> imageList, ImageItemClicked listener) {
        this._context = context;
        this._imageList = imageList;
        this._listener = listener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.image_list_item, viewGroup, false);

        return new ImageViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        ImageItemModel imageItemModel = _imageList.get(position);

        Glide.with(_context).load(imageItemModel.getImagePath())
                .placeholder(R.drawable.place_holder)
                .centerCrop()
                .error(R.drawable.place_holder)
                .into(holder.itemBinding.imgDisplay);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (_listener != null) {
                    _listener.onItemClicked(imageItemModel);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return _imageList.size();
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageListItemBinding itemBinding;

        ImageViewHolder(View view) {
            super(view);
            itemBinding = ImageListItemBinding.bind(view);
        }
    }
}
