package com.example.modulelaptrinh.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.modulelaptrinh.ProgrammingMessage;
import com.example.modulelaptrinh.R;

import java.util.List;

import io.noties.markwon.Markwon;

public class ProgrammingChatAdapter extends RecyclerView.Adapter<ProgrammingChatAdapter.MessageViewHolder> {

    private List<ProgrammingMessage> messageList;
    private Markwon markwon;
    private OnImageClickListener imageClickListener;
    private Context context;

    public interface OnImageClickListener {
        void onImageClick(String imageBase64);
    }

    public ProgrammingChatAdapter(List<ProgrammingMessage> messageList, OnImageClickListener listener) {
        this.messageList = messageList;
        this.imageClickListener = listener;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_message, parent, false);
        markwon = Markwon.create(context);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ProgrammingMessage message = messageList.get(position);

        holder.llUserMessage.setVisibility(View.GONE);
        holder.llAiMessage.setVisibility(View.GONE);
        holder.tvUserMessage.setVisibility(View.GONE);
        holder.rvUserImages.setVisibility(View.GONE);
        holder.tvImageCount.setVisibility(View.GONE);

        if (message.isUser()) {
            holder.llUserMessage.setVisibility(View.VISIBLE);

            String content = message.getContent();
            List<String> images = message.getImageBase64List();

            if (content != null && !content.isEmpty()) {
                holder.tvUserMessage.setVisibility(View.VISIBLE);
                holder.tvUserMessage.setText(content);
            } else {
                holder.tvUserMessage.setVisibility(View.GONE);
            }

            if (images != null && !images.isEmpty()) {
                holder.rvUserImages.setVisibility(View.VISIBLE);

                if (images.size() > 1) {
                    holder.tvImageCount.setVisibility(View.VISIBLE);
                    holder.tvImageCount.setText(holder.itemView.getContext().getString(R.string.prog_chat_image_count_format, images.size()));
                }

                if (holder.rvUserImages.getLayoutManager() == null) {
                    LinearLayoutManager layoutManager = new LinearLayoutManager(
                            context, LinearLayoutManager.HORIZONTAL, false
                    );
                    holder.rvUserImages.setLayoutManager(layoutManager);
                }

                if (holder.imageAdapter == null) {
                    holder.imageAdapter = new ImagePreviewAdapter(imageClickListener);
                    holder.rvUserImages.setAdapter(holder.imageAdapter);
                }
                holder.imageAdapter.submitList(images);
            } else {
                holder.rvUserImages.setVisibility(View.GONE);
                if (holder.imageAdapter != null) {
                    holder.imageAdapter.submitList(null);
                }
            }
        } else {
            holder.llAiMessage.setVisibility(View.VISIBLE);
            if (message.getContent() != null) {
                markwon.setMarkdown(holder.tvAiMessage, message.getContent());
            }
        }
    }

    @Override
    public int getItemCount() {
        return messageList != null ? messageList.size() : 0;
    }

    private static class ImagePreviewAdapter extends ListAdapter<String, ImagePreviewAdapter.ImageViewHolder> {
        private OnImageClickListener listener;
        private int imageSize;

        ImagePreviewAdapter(OnImageClickListener listener) {
            super(new DiffUtil.ItemCallback<String>() {
                @Override
                public boolean areItemsTheSame(@NonNull String oldItem, @NonNull String newItem) {
                    return oldItem.hashCode() == newItem.hashCode();
                }

                @Override
                public boolean areContentsTheSame(@NonNull String oldItem, @NonNull String newItem) {
                    return oldItem.equals(newItem);
                }
            });
            this.listener = listener;
            setHasStableIds(true);
        }

        @Override
        public long getItemId(int position) {
            String item = getItem(position);
            return item != null ? item.hashCode() : position;
        }

        @NonNull
        @Override
        public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (imageSize == 0) {
                imageSize = (int) (80 * parent.getContext().getResources().getDisplayMetrics().density);
            }

            ImageView imageView = new ImageView(parent.getContext());
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(imageSize, imageSize);
            params.setMarginEnd((int) (8 * parent.getContext().getResources().getDisplayMetrics().density));
            imageView.setLayoutParams(params);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setBackgroundResource(R.drawable.bg_chat_input_dark);
            imageView.setPadding(4, 4, 4, 4);
            imageView.setClipToOutline(true);
            return new ImageViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
            String imageBase64 = getItem(position);
            if (imageBase64 == null) return;

            try {
                byte[] decodedBytes = Base64.decode(imageBase64, Base64.DEFAULT);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2;
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length, options);

                if (bitmap != null) {
                    holder.imageView.setImageBitmap(bitmap);
                }

                holder.imageView.setOnClickListener(v -> {
                    if (listener != null) {
                        String image = getItem(position);
                        if (image != null) {
                            listener.onImageClick(image);
                        }
                    }
                });
            } catch (OutOfMemoryError e) {
                Log.e("ImagePreviewAdapter", "OOM", e);
            } catch (Exception e) {
                Log.e("ImagePreviewAdapter", "Error", e);
            }
        }

        static class ImageViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            ImageViewHolder(@NonNull ImageView itemView) {
                super(itemView);
                this.imageView = itemView;
            }
        }
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        LinearLayout llUserMessage, llAiMessage;
        TextView tvUserMessage, tvAiMessage, tvUserTime, tvAiTime, tvImageCount;
        ImageView ivUserAvatar, ivAiAvatar;
        RecyclerView rvUserImages;
        ImagePreviewAdapter imageAdapter;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            llUserMessage = itemView.findViewById(R.id.ll_user_message);
            llAiMessage = itemView.findViewById(R.id.ll_ai_message);
            tvUserMessage = itemView.findViewById(R.id.tv_user_message);
            tvAiMessage = itemView.findViewById(R.id.tv_ai_message);
            tvUserTime = itemView.findViewById(R.id.tv_user_time);
            tvAiTime = itemView.findViewById(R.id.tv_ai_time);
            ivUserAvatar = itemView.findViewById(R.id.iv_user_avatar);
            ivAiAvatar = itemView.findViewById(R.id.iv_ai_avatar);
            rvUserImages = itemView.findViewById(R.id.rv_user_images);
            tvImageCount = itemView.findViewById(R.id.tv_image_count);
            imageAdapter = null;
        }
    }
}