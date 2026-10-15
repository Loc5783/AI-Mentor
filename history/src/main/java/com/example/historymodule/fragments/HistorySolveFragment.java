package com.example.historymodule.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.historymodule.HistoryChatActivity;
import com.example.historymodule.R;
import com.example.historymodule.adapter.HistoryHistoryAdapter;
import com.example.historymodule.databases.HistoryHistoryItem;
import com.example.historymodule.databases.SqliteDbHelper;
import com.example.historymodule.util.HistoryUserSession;

import java.util.ArrayList;
import java.util.List;

public final class HistorySolveFragment extends Fragment {
    private final List<HistoryHistoryItem> recentList = new ArrayList<>();
    private HistoryHistoryAdapter adapter;
    private SqliteDbHelper dbHelper;
    private TextView emptyView;
    private int userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.history_fragment_solver, container, false);
        userId = HistoryUserSession.getUserId(requireContext());
        dbHelper = new SqliteDbHelper(requireContext());
        emptyView = view.findViewById(R.id.tvRecentEmpty);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_recent_solver);
        adapter = new HistoryHistoryAdapter(recentList, new HistoryHistoryAdapter.Listener() {
            @Override
            public void onClick(HistoryHistoryItem item) {
                openConversation(item.getReferenceId());
            }

            @Override
            public void onLongClick(HistoryHistoryItem item) {
                confirmDelete(item);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.setNestedScrollingEnabled(false);

        view.findViewById(R.id.card_ai_intro)
                .setOnClickListener(v -> openChat(false, false));
        view.findViewById(R.id.btn_camera_card)
                .setOnClickListener(v -> openChat(true, false));
        view.findViewById(R.id.btn_input_card)
                .setOnClickListener(v -> openChat(false, true));

        loadRecentConversations();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (dbHelper != null && adapter != null) loadRecentConversations();
    }

    private void openChat(boolean openCamera, boolean focusInput) {
        Intent intent = new Intent(requireContext(), HistoryChatActivity.class);
        intent.putExtra(HistoryChatActivity.EXTRA_OPEN_CAMERA, openCamera);
        intent.putExtra(HistoryChatActivity.EXTRA_FOCUS_INPUT, focusInput);
        startActivity(intent);
    }

    private void openConversation(String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()) return;
        Intent intent = new Intent(requireContext(), HistoryChatActivity.class);
        intent.putExtra(HistoryChatActivity.EXTRA_CONVERSATION_ID, conversationId);
        startActivity(intent);
    }

    private void confirmDelete(HistoryHistoryItem item) {
        if (item.getReferenceId() == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.history_solver_delete_title)
                .setMessage(R.string.history_solver_delete_msg)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    dbHelper.deleteConversation(userId, item.getReferenceId());
                    loadRecentConversations();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void loadRecentConversations() {
        recentList.clear();
        recentList.addAll(dbHelper.loadConversationSummaries(userId, 0L, 0L, 5));
        adapter.notifyDataSetChanged();
        emptyView.setVisibility(recentList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        if (dbHelper != null) dbHelper.close();
        dbHelper = null;
        adapter = null;
        emptyView = null;
        super.onDestroyView();
    }
}
