package com.example.historymodule.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.historymodule.HistoryMessage;
import com.example.historymodule.R;

import java.util.List;

public class HistoryChatAdapter extends RecyclerView.Adapter<HistoryChatAdapter.MyViewHolder> {

    List<HistoryMessage> messageList;

    public HistoryChatAdapter(List<HistoryMessage> messageList) {
        this.messageList = messageList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View chatView = LayoutInflater.from(parent.getContext()).inflate(R.layout.history_item_message, parent, false);
        return new MyViewHolder(chatView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        HistoryMessage message = messageList.get(position);
        if (message.getSentBy().equals(HistoryMessage.SENT_BY_ME)) {
            holder.leftChatView.setVisibility(View.GONE);
            holder.rightChatView.setVisibility(View.VISIBLE);
            holder.rightChatTextView.setText(message.getMessage());
            holder.rightChatTextView.setTextIsSelectable(true);
        } else {
            holder.rightChatView.setVisibility(View.GONE);
            holder.leftChatView.setVisibility(View.VISIBLE);
            holder.leftChatTextView.setText(message.getMessage());
            holder.leftChatTextView.setTextIsSelectable(!message.isPending());
            holder.leftChatTextView.setAlpha(message.isPending() ? 0.65f : 1f);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        LinearLayout leftChatView, rightChatView;
        TextView leftChatTextView, rightChatTextView;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            leftChatView = itemView.findViewById(R.id.left_chat_view);
            rightChatView = itemView.findViewById(R.id.right_chat_view);
            leftChatTextView = itemView.findViewById(R.id.left_chat_text_view);
            rightChatTextView = itemView.findViewById(R.id.right_chat_text_view);
        }
    }
}