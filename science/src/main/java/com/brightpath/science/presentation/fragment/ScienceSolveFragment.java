package com.brightpath.science.presentation.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.brightpath.science.R;
import com.brightpath.science.di.ScienceModule;
import com.brightpath.science.domain.model.ScienceHistory;
import com.brightpath.science.presentation.ScienceActivity;
import com.brightpath.science.presentation.ScienceMainActivity;
import com.brightpath.science.presentation.adapter.ScienceModernAdapters;
import com.brightpath.science.util.ScienceUserSession;

import java.util.List;

public final class ScienceSolveFragment extends Fragment {
    public ScienceSolveFragment() { super(R.layout.fragment_science_solve); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.buttonScienceSolveBack).setOnClickListener(v -> requireActivity().finish());
        view.findViewById(R.id.buttonScienceSolveAction).setOnClickListener(v -> main().selectTab(R.id.navScienceHistory));
        view.findViewById(R.id.buttonScienceViewAllHistory).setOnClickListener(v -> main().selectTab(R.id.navScienceHistory));
        view.findViewById(R.id.cardScienceCamera).setOnClickListener(v -> openChat(true));
        view.findViewById(R.id.cardScienceTextInput).setOnClickListener(v -> openChat(false));

        RecyclerView recycler = view.findViewById(R.id.recyclerScienceRecentQuestions);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        int userId = new ScienceUserSession(requireContext()).getUserId();
        List<ScienceHistory> items = ScienceModule.get(requireContext()).historyRepository().search(userId, null, "");
        if (items.size() > 5) items = items.subList(0, 5);
        recycler.setAdapter(new ScienceModernAdapters.HistoryAdapter(items, true,
                item -> startActivity(ScienceActivity.createHistoryIntent(requireContext(), item))));
    }

    private void openChat(boolean camera) {
        Intent intent = new Intent(requireContext(), ScienceActivity.class);
        intent.putExtra(ScienceActivity.EXTRA_OPEN_CAMERA, camera);
        intent.putExtra(ScienceActivity.EXTRA_FOCUS_INPUT, !camera);
        startActivity(intent);
    }

    private ScienceMainActivity main() { return (ScienceMainActivity) requireActivity(); }
}
