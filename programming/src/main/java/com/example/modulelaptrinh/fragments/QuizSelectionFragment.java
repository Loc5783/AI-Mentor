package com.example.modulelaptrinh.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.modulelaptrinh.MainActivity;
import com.example.modulelaptrinh.R;

public class QuizSelectionFragment extends Fragment {

    private EditText edtTopic;
    private TextView tvQuestionCount;
    private ImageView ivDecrease, ivIncrease;
    private Button btnStartQuiz;

    private int questionCount = 5;
    private final int MIN_QUESTIONS = 3;
    private final int MAX_QUESTIONS = 15;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_quiz_selection, container, false);

        initViews(view);
        setupListeners();

        return view;
    }

    private void initViews(View view) {
        edtTopic = view.findViewById(R.id.edt_topic);
        tvQuestionCount = view.findViewById(R.id.tv_question_count);
        ivDecrease = view.findViewById(R.id.iv_decrease);
        ivIncrease = view.findViewById(R.id.iv_increase);
        btnStartQuiz = view.findViewById(R.id.btn_start_quiz);
    }

    private void setupListeners() {
        // Question count controls
        ivDecrease.setOnClickListener(v -> {
            if (questionCount > MIN_QUESTIONS) {
                questionCount--;
                tvQuestionCount.setText(String.valueOf(questionCount));
            } else {
                Toast.makeText(getContext(), "Tối thiểu " + MIN_QUESTIONS + " câu hỏi", Toast.LENGTH_SHORT).show();
            }
        });

        ivIncrease.setOnClickListener(v -> {
            if (questionCount < MAX_QUESTIONS) {
                questionCount++;
                tvQuestionCount.setText(String.valueOf(questionCount));
            } else {
                Toast.makeText(getContext(), "Tối đa " + MAX_QUESTIONS + " câu hỏi", Toast.LENGTH_SHORT).show();
            }
        });

        // Start quiz
        btnStartQuiz.setOnClickListener(v -> startQuiz());
    }

    private void startQuiz() {
        String topic = edtTopic.getText().toString().trim();

        if (topic.isEmpty()) {
            Toast.makeText(getContext(), R.string.quiz_topic_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        if (getActivity() instanceof MainActivity) {
            // Level mặc định là "Trung bình" vì đã bỏ chọn cấp độ
            ((MainActivity) getActivity()).openQuiz("Lập trình", topic, "Trung bình", questionCount);
        }
    }
}