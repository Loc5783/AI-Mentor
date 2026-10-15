package com.example.modulelaptrinh.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.modulelaptrinh.R;
import com.example.modulelaptrinh.models.ChatMessage;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ChatHistoryAdapter extends RecyclerView.Adapter<ChatHistoryAdapter.ChatViewHolder> {

    private List<ChatMessage> messages;
    private OnMessageClickListener listener;

    public interface OnMessageClickListener {
        void onMessageClick(ChatMessage message);
    }

    public ChatHistoryAdapter(List<ChatMessage> messages, OnMessageClickListener listener) {
        this.messages = messages;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_history, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        holder.tvChatQuestion.setText(message.getQuestion());
        holder.tvChatAnswer.setText(message.getAnswer());

        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());
        holder.tvChatTime.setText(dateFormat.format(message.getTimestamp()));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMessageClick(message);
            }
        });
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void updateMessages(List<ChatMessage> newMessages) {
        this.messages = newMessages;
        notifyDataSetChanged();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvChatQuestion, tvChatAnswer, tvChatTime;

        ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChatQuestion = itemView.findViewById(R.id.tv_chat_question);
            tvChatAnswer = itemView.findViewById(R.id.tv_chat_answer);
            tvChatTime = itemView.findViewById(R.id.tv_chat_time);
        }
    }
}