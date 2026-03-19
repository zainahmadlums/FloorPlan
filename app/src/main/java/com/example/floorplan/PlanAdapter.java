package com.example.floorplan;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.List;
import com.bumptech.glide.Glide;

public class PlanAdapter extends RecyclerView.Adapter<PlanAdapter.PlanViewHolder> {

    private List<File> planFiles;
    private Context context;

    public PlanAdapter(Context context, List<File> planFiles) {
        this.context = context;
        this.planFiles = planFiles;
    }

    @NonNull
    @Override
    public PlanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_floor_plan, parent, false);
        return new PlanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlanViewHolder holder, int position) {
        File file = planFiles.get(position);

        Glide.with(context)
                .load(file)
                .into(holder.imageView);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, FullScreenActivity.class);
            intent.putExtra("filePath", file.getAbsolutePath());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return planFiles.size();
    }

    public static class PlanViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        public PlanViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageThumbnail);
        }
    }
}