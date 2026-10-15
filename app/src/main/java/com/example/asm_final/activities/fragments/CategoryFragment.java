package com.example.asm_final.activities.fragments;

import android.os.Bundle;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import com.example.asm_final.R;
import com.example.asm_final.activities.database.DatabaseHelper;
import com.brightpath.science.api.ScienceFeature;
import com.example.mathmodule.api.MathFeature;
import com.example.modulelaptrinh.api.ProgrammingFeature;
import com.example.historymodule.api.HistoryFeature;

public class CategoryFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_category, container, false);

        CardView cardMath = view.findViewById(R.id.cardMath);
        CardView cardScience = view.findViewById(R.id.cardScience);
        CardView cardHistory = view.findViewById(R.id.cardHistory);
        CardView cardIT = view.findViewById(R.id.cardIT);

        cardMath.setOnClickListener(v -> openMathModule());
        cardScience.setOnClickListener(v -> openScienceModule());
        cardHistory.setOnClickListener(v -> openHistoryModule());
        cardIT.setOnClickListener(v -> openProgrammingModule());

        return view;
    }


    private void openMathModule() {
        Context context = requireContext();
        MathFeature.open(context, resolveCurrentUserId(context));
    }

    private void openScienceModule() {
        Context context = requireContext();
        ScienceFeature.open(context, resolveCurrentUserId(context));
    }

    private void openHistoryModule() {
        Context context = requireContext();
        HistoryFeature.open(context, resolveCurrentUserId(context));
    }

    private void openProgrammingModule() {
        Context context = requireContext();
        ProgrammingFeature.open(context, resolveCurrentUserId(context));
    }

    private int resolveCurrentUserId(Context context) {
        Bundle arguments = getArguments();
        String username = arguments == null ? null : arguments.getString("USERNAME");
        if (username == null || username.trim().isEmpty()) {
            return 0;
        }

        DatabaseHelper databaseHelper = new DatabaseHelper(context);
        return databaseHelper.getUserId(username);
    }

}