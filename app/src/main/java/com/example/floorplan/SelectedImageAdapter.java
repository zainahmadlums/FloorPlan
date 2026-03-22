package com.example.floorplan;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class SelectedImageAdapter extends RecyclerView.Adapter<SelectedImageAdapter.ImageViewHolder> {

    private final Context context;
    private final List<String> imageUris;
    private final OnImageClickListener clickListener;

    // Interface to handle clicks back in the Activity
    public interface OnImageClickListener {
        void onImageClick(String uri, int position);
    }

    public SelectedImageAdapter(Context context, List<String> imageUris, OnImageClickListener listener) {
        this.context = context;
        this.imageUris = imageUris;
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_selected_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String uriString = imageUris.get(position);

        Glide.with(context)
                .load(uriString)
                .into(holder.imageView);

        // Notify activity when clicked
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onImageClick(uriString, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageUris.size();
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageThumbnail);
        }
    }
}