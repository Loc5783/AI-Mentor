package com.example.modulelaptrinh.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.modulelaptrinh.ProgrammingChatActivity;
import com.example.modulelaptrinh.R;
import com.example.modulelaptrinh.adapters.ProgrammingChatHistoryAdapter;
import com.example.modulelaptrinh.database.ProgrammingDbHelper;
import com.example.modulelaptrinh.database.SessionItem;

import java.util.List;

public class ChatHistoryFragment extends Fragment {
    private RecyclerView rvChatHistory;
    private TextView tvEmptyHistory;
    private ImageView ivDeleteAll;
    private ProgrammingChatHistoryAdapter adapter;
    private ProgrammingDbHelper dbHelper;
    private List<SessionItem> sessionList;
    private int userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_history, container, false);
        userId = ((com.example.modulelaptrinh.MainActivity) requireActivity()).getCurrentUserId();

        rvChatHistory = view.findViewById(R.id.rv_chat_history);
        tvEmptyHistory = view.findViewById(R.id.tv_empty_history);
        ivDeleteAll = view.findViewById(R.id.iv_delete_all);

        try {
            dbHelper = new ProgrammingDbHelper(getContext());
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Lỗi khởi tạo database", Toast.LENGTH_SHORT).show();
        }

        setupRecyclerView();
        loadHistory();

        ivDeleteAll.setOnClickListener(v -> {
            if (dbHelper != null) {
                dbHelper.deleteAllSessions(userId);
                loadHistory();
                Toast.makeText(getContext(), "Đã xóa tất cả lịch sử", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void setupRecyclerView() {
        adapter = new ProgrammingChatHistoryAdapter(null, new ProgrammingChatHistoryAdapter.OnDeleteClickListener() {
            @Override
            public void onDeleteClick(int position, SessionItem session) {
                if (dbHelper != null && session != null) {
                    dbHelper.deleteSession(session.getId(), userId);
                    loadHistory();
                    Toast.makeText(getContext(), "Đã xóa cuộc hội thoại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onItemClick(int position, SessionItem session) {
                if (getActivity() == null || session == null) return;

                try {
                    Intent intent = new Intent(getActivity(), ProgrammingChatActivity.class);
                    intent.putExtra("SESSION_ID", session.getId());
                    intent.putExtra(com.example.modulelaptrinh.MainActivity.EXTRA_USER_ID, userId);
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Lỗi mở chat", Toast.LENGTH_SHORT).show();
                }
            }
        });
        rvChatHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        rvChatHistory.setAdapter(adapter);
    }

    private void loadHistory() {
        if (dbHelper == null) {
            try {
                dbHelper = new ProgrammingDbHelper(getContext());
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        try {
            sessionList = dbHelper.getAllSessions(userId);

            for (SessionItem session : sessionList) {
                if (session.getName().equals("Cuộc trò chuyện mới")) {
                    try {
                        List<com.example.modulelaptrinh.ProgrammingMessage> messages = dbHelper.getMessagesBySession(session.getId());
                        if (!messages.isEmpty()) {
                            String firstMessage = messages.get(0).getContent();
                            if (firstMessage != null && !firstMessage.isEmpty()) {
                                if (firstMessage.length() > 30) {
                                    firstMessage = firstMessage.substring(0, 30) + "...";
                                }
                                session.setName(firstMessage);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            if (sessionList.isEmpty()) {
                tvEmptyHistory.setVisibility(View.VISIBLE);
                rvChatHistory.setVisibility(View.GONE);
                ivDeleteAll.setVisibility(View.GONE);
            } else {
                tvEmptyHistory.setVisibility(View.GONE);
                rvChatHistory.setVisibility(View.VISIBLE);
                ivDeleteAll.setVisibility(View.VISIBLE);
                adapter.updateData(sessionList);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Lỗi tải lịch sử", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        if (dbHelper != null) {
            loadHistory();
        }
    }

}