package com.example.modulelaptrinh.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.modulelaptrinh.R;
import com.example.modulelaptrinh.database.SessionItem;

import java.util.List;

public class ProgrammingChatHistoryAdapter extends RecyclerView.Adapter<ProgrammingChatHistoryAdapter.HistoryViewHolder> {
    private List<SessionItem> sessionList;
    private OnDeleteClickListener listener;

    public interface OnDeleteClickListener {
        void onDeleteClick(int position, SessionItem session);
        void onItemClick(int position, SessionItem session);
    }

    public ProgrammingChatHistoryAdapter(List<SessionItem> sessionList, OnDeleteClickListener listener) {
        this.sessionList = sessionList;
        this.listener = listener;
    }

    public void updateData(List<SessionItem> newList) {
        this.sessionList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        SessionItem session = sessionList.get(position);

        holder.tvChatQuestion.setText(session.getName());
        holder.tvChatTime.setText(session.getTimestamp());
        holder.tvChatAnswer.setVisibility(View.GONE);

        holder.ivDeleteSession.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(position, session);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(position, session);
            }
        });
    }

    @Override
    public int getItemCount() {
        return sessionList != null ? sessionList.size() : 0;
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvChatQuestion, tvChatAnswer, tvChatTime;
        ImageView ivDeleteSession;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChatQuestion = itemView.findViewById(R.id.tv_chat_question);
            tvChatAnswer = itemView.findViewById(R.id.tv_chat_answer);
            tvChatTime = itemView.findViewById(R.id.tv_chat_time);
            ivDeleteSession = itemView.findViewById(R.id.iv_delete_session);
        }
    }
}