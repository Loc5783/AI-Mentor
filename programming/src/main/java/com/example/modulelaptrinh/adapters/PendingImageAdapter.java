package com.example.modulelaptrinh.adapters;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.modulelaptrinh.R;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class PendingImageAdapter extends RecyclerView.Adapter<PendingImageAdapter.PendingImageViewHolder> {

    private List<Uri> imageUris;
    private OnImageActionListener listener;
    private static final int MAX_IMAGES = 5;

    public interface OnImageActionListener {
        void onImageClick(int position, Uri uri);
        void onRemoveClick(int position);
    }

    public PendingImageAdapter(OnImageActionListener listener) {
        this.imageUris = new ArrayList<>();
        this.listener = listener;
    }

    public boolean canAddMore() {
        return imageUris.size() < MAX_IMAGES;
    }

    public int getRemainingSlots() {
        return MAX_IMAGES - imageUris.size();
    }

    public void addImages(List<Uri> uris) {
        int remaining = getRemainingSlots();
        int countToAdd = Math.min(uris.size(), remaining);

        if (countToAdd <= 0) return;

        List<Uri> urisToAdd = new ArrayList<>(uris.subList(0, countToAdd));
        int startPos = imageUris.size();
        imageUris.addAll(urisToAdd);
        notifyItemRangeInserted(startPos, urisToAdd.size());
    }

    public void removeImage(int position) {
        if (position >= 0 && position < imageUris.size()) {
            imageUris.remove(position);
            if (imageUris.isEmpty()) {
                notifyDataSetChanged();
            } else {
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, imageUris.size() - position);
            }
        }
    }

    public void clearAll() {
        int size = imageUris.size();
        if (size > 0) {
            imageUris.clear();
            notifyDataSetChanged();
        }
    }

    public List<Uri> getImageUris() {
        return new ArrayList<>(imageUris);
    }

    public int getImageCount() {
        return imageUris.size();
    }

    public boolean isFull() {
        return imageUris.size() >= MAX_IMAGES;
    }

    public boolean isEmpty() {
        return imageUris.isEmpty();
    }

    @NonNull
    @Override
    public PendingImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pending_image, parent, false);
        return new PendingImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PendingImageViewHolder holder, int position) {
        Uri uri = imageUris.get(position);

        Glide.with(holder.itemView.getContext())
                .load(uri)
                .centerCrop()
                .into(holder.ivImage);

        holder.ivRemove.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRemoveClick(position);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onImageClick(position, uri);
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageUris.size();
    }

    public static class PendingImageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage, ivRemove;

        public PendingImageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_pending_image);
            ivRemove = itemView.findViewById(R.id.iv_remove_pending_image);
        }
    }
}