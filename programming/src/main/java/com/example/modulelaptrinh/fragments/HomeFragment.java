package com.example.modulelaptrinh.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.modulelaptrinh.MainActivity;
import com.example.modulelaptrinh.ProgrammingChatActivity;
import com.example.modulelaptrinh.R;
import com.example.modulelaptrinh.database.ProgrammingDbHelper;
import com.example.modulelaptrinh.utils.ProgrammingLevelCalculator;

import java.util.HashMap;
import java.util.Map;

public class HomeFragment extends Fragment {
    private LinearLayout llPython, llJava, llKotlin, llJavascript;
    private LinearLayout llCpp, llCsharp, llPhp, llSwift;
    private LinearLayout llGo, llRust, llTypescript, llRuby;
    private LinearLayout llExpand, llCollapse, row2, row3, llSendButton;
    private CardView cardExp;
    private TextView tvExpandLabel, tvLangCount;

    // EXP Card views
    private TextView tvExpLevel, tvExpTitle, tvExpTotal, tvExpProgress, tvExpPercent;
    private TextView tvQuizCount, tvAccuracy, tvTotalExpAll;
    private ProgressBar pbExp;

    private EditText etQuestionInput;
    private ProgrammingDbHelper dbHelper;

    private boolean isExpanded = false;
    private boolean isProcessing = false;

    private Map<String, String> languageQuestions = new HashMap<>();
    private int totalLanguages = 12;
    private int completedLanguages = 4;
    private int userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_programming_home, container, false);
        userId = ((MainActivity) requireActivity()).getCurrentUserId();

        try {
            dbHelper = new ProgrammingDbHelper(getContext());
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Lỗi khởi tạo database", Toast.LENGTH_SHORT).show();
        }

        languageQuestions.put("Python", "Tôi muốn học Python từ cơ bản đến nâng cao.");
        languageQuestions.put("Java", "Tôi muốn học Java từ cơ bản đến nâng cao.");
        languageQuestions.put("Kotlin", "Tôi muốn học Kotlin để phát triển Android.");
        languageQuestions.put("JavaScript", "Tôi muốn học JavaScript từ cơ bản đến nâng cao.");
        languageQuestions.put("C++", "Tôi muốn học C++ từ cơ bản đến nâng cao.");
        languageQuestions.put("C#", "Tôi muốn học C# từ cơ bản đến nâng cao.");
        languageQuestions.put("PHP", "Tôi muốn học PHP từ cơ bản đến nâng cao.");
        languageQuestions.put("Swift", "Tôi muốn học Swift để phát triển iOS.");
        languageQuestions.put("Go", "Tôi muốn học Go từ cơ bản đến nâng cao.");
        languageQuestions.put("Rust", "Tôi muốn học Rust từ cơ bản đến nâng cao.");
        languageQuestions.put("TypeScript", "Tôi muốn học TypeScript từ cơ bản đến nâng cao.");
        languageQuestions.put("Ruby", "Tôi muốn học Ruby từ cơ bản đến nâng cao.");

        // Language views
        llPython = view.findViewById(R.id.ll_python);
        llJava = view.findViewById(R.id.ll_java);
        llKotlin = view.findViewById(R.id.ll_kotlin);
        llJavascript = view.findViewById(R.id.ll_javascript);
        llCpp = view.findViewById(R.id.ll_cpp);
        llCsharp = view.findViewById(R.id.ll_csharp);
        llPhp = view.findViewById(R.id.ll_php);
        llSwift = view.findViewById(R.id.ll_swift);
        llGo = view.findViewById(R.id.ll_go);
        llRust = view.findViewById(R.id.ll_rust);
        llTypescript = view.findViewById(R.id.ll_typescript);
        llRuby = view.findViewById(R.id.ll_ruby);
        row2 = view.findViewById(R.id.row_2);
        row3 = view.findViewById(R.id.row_3);
        llExpand = view.findViewById(R.id.ll_expand);
        llCollapse = view.findViewById(R.id.ll_collapse);
        tvExpandLabel = view.findViewById(R.id.tv_expand_label);
        tvLangCount = view.findViewById(R.id.tv_lang_count);
        etQuestionInput = view.findViewById(R.id.et_question_input);
        llSendButton = view.findViewById(R.id.ll_send_button);

        // EXP Card views - SỬA: dùng CardView
        cardExp = view.findViewById(R.id.card_exp);
        tvExpLevel = view.findViewById(R.id.tv_exp_level);
        tvExpTitle = view.findViewById(R.id.tv_exp_title);
        tvExpTotal = view.findViewById(R.id.tv_exp_total);
        tvExpProgress = view.findViewById(R.id.tv_exp_progress);
        tvExpPercent = view.findViewById(R.id.tv_exp_percent);
        pbExp = view.findViewById(R.id.pb_exp);
        tvQuizCount = view.findViewById(R.id.tv_quiz_count);
        tvAccuracy = view.findViewById(R.id.tv_accuracy);
        tvTotalExpAll = view.findViewById(R.id.tv_total_exp_all);

        updateLangCount();
        updateExpData();
        row2.setVisibility(View.GONE);
        row3.setVisibility(View.GONE);
        llCollapse.setVisibility(View.GONE);

        setupExpandCollapse();
        setupLanguageClicks();
        setupChatInput();
        setupExpCardClick();

        return view;
    }

    private void updateExpData() {
        if (dbHelper == null) return;

        int totalExp = dbHelper.getTotalExpAllSubjects(userId);
        int level = ProgrammingLevelCalculator.getLevel(totalExp);
        String title = getString(ProgrammingLevelCalculator.getTitleResId(level));
        int progressPercent = ProgrammingLevelCalculator.getProgressPercent(totalExp);
        int expToNext = ProgrammingLevelCalculator.getExpToNextLevel(level);
        int expInLevel = ProgrammingLevelCalculator.getExpInsideCurrentLevel(totalExp);

        int quizCount = dbHelper.getQuizAttemptCount(userId, "Lập trình");
        double accuracy = dbHelper.getAverageAccuracy(userId, "Lập trình");
        int totalExpAll = dbHelper.getTotalExpAllSubjects(userId);

        tvExpLevel.setText(getString(R.string.prog_level_format, level));
        tvExpTitle.setText(title);
        tvExpTotal.setText(getString(R.string.prog_exp_format, totalExp));
        tvExpProgress.setText(getString(R.string.prog_exp_detail_format, expInLevel, expToNext));
        tvExpPercent.setText(getString(R.string.prog_percent_format, progressPercent));
        pbExp.setProgress(progressPercent);

        tvQuizCount.setText(String.valueOf(quizCount));
        tvAccuracy.setText(accuracy > 0 ? getString(R.string.prog_percent_format, (int)accuracy) : "0%");
        tvTotalExpAll.setText(String.valueOf(totalExpAll));
    }

    private void setupExpCardClick() {
        cardExp.setOnClickListener(v -> {
            ProgressDetailFragment fragment = new ProgressDetailFragment();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack("progress")
                    .commit();

            if (getActivity() != null && getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).getSupportActionBar().setTitle(R.string.prog_quiz_progress_title);
            }
        });
    }

    private void updateLangCount() {
        if (isExpanded) {
            tvLangCount.setText(getString(R.string.prog_lang_count_format, totalLanguages, totalLanguages));
        } else {
            tvLangCount.setText(getString(R.string.prog_lang_count_format, completedLanguages, totalLanguages));
        }
    }

    private void setupExpandCollapse() {
        llExpand.setOnClickListener(v -> {
            row2.setVisibility(View.VISIBLE);
            row3.setVisibility(View.VISIBLE);
            llExpand.setVisibility(View.GONE);
            llCollapse.setVisibility(View.VISIBLE);
            tvExpandLabel.setText(R.string.collapse);
            isExpanded = true;
            updateLangCount();
        });

        llCollapse.setOnClickListener(v -> {
            row2.setVisibility(View.GONE);
            row3.setVisibility(View.GONE);
            llCollapse.setVisibility(View.GONE);
            llExpand.setVisibility(View.VISIBLE);
            tvExpandLabel.setText(R.string.see_more);
            isExpanded = false;
            updateLangCount();
        });
    }

    private void setupLanguageClicks() {
        View.OnClickListener listener = v -> {
            if (isProcessing) {
                return;
            }

            if (getActivity() == null || getContext() == null) {
                return;
            }

            isProcessing = true;

            String lang = "";
            if (v == llPython) lang = "Python";
            else if (v == llJava) lang = "Java";
            else if (v == llKotlin) lang = "Kotlin";
            else if (v == llJavascript) lang = "JavaScript";
            else if (v == llCpp) lang = "C++";
            else if (v == llCsharp) lang = "C#";
            else if (v == llPhp) lang = "PHP";
            else if (v == llSwift) lang = "Swift";
            else if (v == llGo) lang = "Go";
            else if (v == llRust) lang = "Rust";
            else if (v == llTypescript) lang = "TypeScript";
            else if (v == llRuby) lang = "Ruby";

            if (!lang.isEmpty()) {
                try {
                    String question = languageQuestions.getOrDefault(lang, "I want to learn " + lang + " from basic to advanced.");

                    if (dbHelper == null) {
                        dbHelper = new ProgrammingDbHelper(getContext());
                    }

                    long sessionId = dbHelper.createNewSession(userId);

                    if (sessionId == -1) {
                        Toast.makeText(getContext(), R.string.prog_chat_error_create_session, Toast.LENGTH_SHORT).show();
                        isProcessing = false;
                        return;
                    }

                    String welcomeMessage = getString(R.string.prog_chat_welcome_format, lang);
                    dbHelper.insertMessage(sessionId, welcomeMessage, false, null);

                    Intent intent = new Intent(getActivity(), ProgrammingChatActivity.class);
                    intent.putExtra("SESSION_ID", sessionId);
                    intent.putExtra("QUESTION", question);
                    intent.putExtra("LANGUAGE", lang);
                    intent.putExtra(MainActivity.EXTRA_USER_ID, userId);
                    startActivity(intent);

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            isProcessing = false;
        };

        llPython.setOnClickListener(listener);
        llJava.setOnClickListener(listener);
        llKotlin.setOnClickListener(listener);
        llJavascript.setOnClickListener(listener);
        llCpp.setOnClickListener(listener);
        llCsharp.setOnClickListener(listener);
        llPhp.setOnClickListener(listener);
        llSwift.setOnClickListener(listener);
        llGo.setOnClickListener(listener);
        llRust.setOnClickListener(listener);
        llTypescript.setOnClickListener(listener);
        llRuby.setOnClickListener(listener);
    }

    private void setupChatInput() {
        etQuestionInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (llSendButton != null) {
                    llSendButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                }
            }
        });

        llSendButton.setOnClickListener(v -> {
            if (isProcessing) return;

            if (getActivity() == null || getContext() == null) {
                return;
            }

            String question = etQuestionInput.getText().toString().trim();
            if (!question.isEmpty()) {
                isProcessing = true;
                try {
                    if (dbHelper == null) {
                        dbHelper = new ProgrammingDbHelper(getContext());
                    }

                    long sessionId = dbHelper.createNewSession(userId);

                    if (sessionId == -1) {
                        Toast.makeText(getContext(), "Lỗi tạo phiên chat", Toast.LENGTH_SHORT).show();
                        isProcessing = false;
                        return;
                    }

                    String welcomeMessage = "Xin chào! Tôi là AI Mentor Lập trình. Bạn có câu hỏi nào không?";
                    dbHelper.insertMessage(sessionId, welcomeMessage, false, null);

                    Intent intent = new Intent(getActivity(), ProgrammingChatActivity.class);
                    intent.putExtra("SESSION_ID", sessionId);
                    intent.putExtra("QUESTION", question);
                    intent.putExtra("LANGUAGE", "Lập trình");
                    intent.putExtra(MainActivity.EXTRA_USER_ID, userId);
                    startActivity(intent);
                    etQuestionInput.setText("");

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                isProcessing = false;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        updateExpData();
    }
}