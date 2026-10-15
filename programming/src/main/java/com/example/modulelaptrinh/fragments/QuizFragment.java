package com.example.modulelaptrinh.fragments;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.modulelaptrinh.MainActivity;
import com.example.modulelaptrinh.R;
import com.example.modulelaptrinh.database.ProgrammingDbHelper;
import com.example.modulelaptrinh.models.QuizQuestion;
import com.example.modulelaptrinh.models.QuizRequest;
import com.example.modulelaptrinh.models.QuizResult;
import com.example.modulelaptrinh.utils.ProgrammingLevelCalculator;
import com.example.modulelaptrinh.utils.QuizGenerator;

import java.util.ArrayList;
import java.util.List;

public class QuizFragment extends Fragment {
    private static final String ARG_SUBJECT = "subject";
    private static final String ARG_TOPIC = "topic";
    private static final String ARG_LEVEL = "level";
    private static final String ARG_QUESTION_COUNT = "question_count";

    private TextView tvProgress, tvDifficulty, tvQuestion, tvExplanation;
    private ProgressBar pbQuiz;
    private LinearLayout llOptions;
    private Button btnNext;
    private CardView cardExplanation;

    private CardView[] optionCards;
    private TextView[] optionTexts;

    private List<QuizQuestion> questions = new ArrayList<>();
    private int currentIndex = 0;
    private int correctAnswers = 0;
    private int totalExpEarned = 0;
    private boolean isAnswered = false;
    private boolean isLoading = false;
    private boolean isFinishingQuiz = false;

    private ProgrammingDbHelper dbHelper;
    private QuizGenerator quizGenerator;
    private int userId;
    private ProgressDialog progressDialog;

    public static QuizFragment newInstance(String subject, String topic, String level, int questionCount) {
        Bundle args = new Bundle();
        args.putString(ARG_SUBJECT, subject);
        args.putString(ARG_TOPIC, topic);
        args.putString(ARG_LEVEL, level);
        args.putInt(ARG_QUESTION_COUNT, questionCount);
        QuizFragment fragment = new QuizFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_quiz, container, false);
        userId = ((MainActivity) requireActivity()).getCurrentUserId();

        initViews(view);
        initQuiz();

        return view;
    }

    private void initViews(View view) {
        tvProgress = view.findViewById(R.id.tv_quiz_progress);
        tvDifficulty = view.findViewById(R.id.tv_question_difficulty);
        pbQuiz = view.findViewById(R.id.pb_quiz);
        tvQuestion = view.findViewById(R.id.tv_question);
        llOptions = view.findViewById(R.id.ll_options);
        tvExplanation = view.findViewById(R.id.tv_explanation);
        cardExplanation = view.findViewById(R.id.card_explanation);
        btnNext = view.findViewById(R.id.btn_next_quiz);

        optionCards = new CardView[]{
                view.findViewById(R.id.cv_option_a),
                view.findViewById(R.id.cv_option_b),
                view.findViewById(R.id.cv_option_c),
                view.findViewById(R.id.cv_option_d)
        };

        optionTexts = new TextView[]{
                view.findViewById(R.id.tv_option_a),
                view.findViewById(R.id.tv_option_b),
                view.findViewById(R.id.tv_option_c),
                view.findViewById(R.id.tv_option_d)
        };

        dbHelper = new ProgrammingDbHelper(requireContext());
        quizGenerator = new QuizGenerator();
    }

    private void initQuiz() {
        String subject = getArguments() != null ? getArguments().getString(ARG_SUBJECT, "Lập trình") : "Lập trình";
        String topic = getArguments() != null ? getArguments().getString(ARG_TOPIC, "Kiến thức lập trình") : "Kiến thức lập trình";
        String level = getArguments() != null ? getArguments().getString(ARG_LEVEL, "Trung bình") : "Trung bình";
        int questionCount = getArguments() != null ? getArguments().getInt(ARG_QUESTION_COUNT, 5) : 5;

        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Đang tạo câu hỏi...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        setOptionsEnabled(false);
        btnNext.setEnabled(false);
        tvQuestion.setText("Đang tạo câu hỏi...");
        isLoading = true;

        // Tạo request với số câu hỏi tùy chỉnh
        QuizRequest request = new QuizRequest(subject, level, topic, questionCount);

        new Thread(() -> {
            try {
                QuizResult result = quizGenerator.generateQuiz(request, userId);
                questions = result.getQuestions();

                new Handler(Looper.getMainLooper()).post(() -> {
                    isLoading = false;
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    if (!questions.isEmpty()) {
                        showQuestion(0);
                    } else {
                        Toast.makeText(getContext(), "Không thể tạo quiz, vui lòng thử lại", Toast.LENGTH_SHORT).show();
                        requireActivity().getOnBackPressedDispatcher().onBackPressed();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> {
                    isLoading = false;
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    Toast.makeText(getContext(), "Lỗi tạo quiz: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                });
            }
        }).start();

        btnNext.setOnClickListener(v -> {
            if (isLoading) return;

            if (isAnswered) {
                if (currentIndex < questions.size() - 1) {
                    currentIndex++;
                    showQuestion(currentIndex);
                } else {
                    finishQuiz();
                }
            }
        });
    }

    private void showQuestion(int index) {
        QuizQuestion question = questions.get(index);

        tvProgress.setText(getString(R.string.quiz_question_count_format, index + 1, questions.size()));
        pbQuiz.setProgress((int) Math.round(((index + 1) * 100.0) / questions.size()));
        
        String difficultyName = getString(question.getDifficulty().getDisplayNameResId());
        tvDifficulty.setText(getString(R.string.quiz_difficulty_format, difficultyName, question.getExpReward()));

        tvQuestion.setText(question.getPrompt());

        List<String> options = question.getOptions();
        for (int i = 0; i < optionTexts.length; i++) {
            if (i < options.size()) {
                optionTexts[i].setText(options.get(i));
                optionCards[i].setVisibility(View.VISIBLE);

                optionCards[i].setCardBackgroundColor(
                        getResources().getColor(R.color.bg_secondary, null)
                );
                optionTexts[i].setTextColor(
                        getResources().getColor(R.color.text_primary, null)
                );

                final int optionIndex = i;
                optionCards[i].setOnClickListener(v -> handleOptionClick(optionIndex));
            } else {
                optionCards[i].setVisibility(View.GONE);
            }
        }

        cardExplanation.setVisibility(View.GONE);
        tvExplanation.setText("");

        if (index == questions.size() - 1) {
            btnNext.setText("Xem kết quả");
        } else {
            btnNext.setText("Tiếp theo");
        }
        btnNext.setEnabled(false);

        setOptionsEnabled(true);
        isAnswered = false;
    }

    private void handleOptionClick(int index) {
        if (isAnswered || isLoading) return;

        QuizQuestion question = questions.get(currentIndex);
        String selectedOption = getOptionLetter(index);
        String correctAnswer = question.getCorrectAnswer();

        isAnswered = true;
        setOptionsEnabled(false);

        for (int i = 0; i < optionCards.length; i++) {
            String optionLetter = getOptionLetter(i);
            if (optionLetter.equals(correctAnswer)) {
                optionCards[i].setCardBackgroundColor(
                        getResources().getColor(R.color.success, null)
                );
                optionTexts[i].setTextColor(
                        getResources().getColor(R.color.programming_white, null)
                );
            } else if (i == index && !optionLetter.equals(correctAnswer)) {
                optionCards[i].setCardBackgroundColor(
                        getResources().getColor(R.color.error, null)
                );
                optionTexts[i].setTextColor(
                        getResources().getColor(R.color.programming_white, null)
                );
            }
        }

        if (selectedOption.equals(correctAnswer)) {
            correctAnswers++;
            totalExpEarned += question.getExpReward();
        }

        tvExplanation.setText(question.getExplanation());
        cardExplanation.setVisibility(View.VISIBLE);

        btnNext.setEnabled(true);
    }

    private String getOptionLetter(int index) {
        return String.valueOf((char) ('A' + index));
    }

    private void setOptionsEnabled(boolean enabled) {
        for (CardView card : optionCards) {
            card.setClickable(enabled);
            card.setFocusable(enabled);
        }
    }

    private void finishQuiz() {
        if (isFinishingQuiz || questions.isEmpty()) {
            return;
        }
        isFinishingQuiz = true;
        btnNext.setEnabled(false);
        btnNext.setText("Đang lưu kết quả...");

        String subject = getArguments() != null ? getArguments().getString(ARG_SUBJECT, "Lập trình") : "Lập trình";
        String level = getArguments() != null ? getArguments().getString(ARG_LEVEL, "Trung bình") : "Trung bình";
        String topic = getArguments() != null ? getArguments().getString(ARG_TOPIC, "Kiến thức lập trình") : "Kiến thức lập trình";

        QuizRequest request = new QuizRequest(subject, level, topic, questions.size());
        QuizResult result = new QuizResult(request, questions, correctAnswers, totalExpEarned);

        new Thread(() -> {
            long attemptId = dbHelper.saveQuizResult(userId, subject, level, topic,
                    questions.size(), correctAnswers, totalExpEarned);
            int totalExp = attemptId == -1 ? 0 : dbHelper.getTotalExp(userId, subject);

            new Handler(Looper.getMainLooper()).post(() -> {
                if (!isAdded()) {
                    return;
                }
                if (attemptId == -1) {
                    isFinishingQuiz = false;
                    btnNext.setEnabled(true);
                    btnNext.setText("Thử lưu lại");
                    Toast.makeText(requireContext(),
                            "Không thể lưu kết quả. EXP chưa được cộng.",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                showResult(result, totalExp);
            });
        }).start();
    }

    private void showResult(QuizResult result, int totalExp) {
        int level = ProgrammingLevelCalculator.getLevel(totalExp);
        String title = getString(ProgrammingLevelCalculator.getTitleResId(level));
        
        String message = getString(R.string.quiz_result_correct, result.getCorrectAnswers(), result.getTotalQuestions()) + "\n" +
                getString(R.string.quiz_result_accuracy, result.getAccuracyPercent()) + "\n" +
                getString(R.string.quiz_result_exp, result.getExpEarned()) + "\n" +
                getString(R.string.quiz_result_total_exp, totalExp) + "\n" +
                getString(R.string.quiz_result_level, level, title);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.quiz_result_dialog_title)
                .setMessage(message)
                .setPositiveButton(R.string.quiz_result_back, (dialog, which) -> {
                    requireActivity().getSupportFragmentManager().popBackStack();
                    if (getActivity() instanceof MainActivity
                            && ((MainActivity) getActivity()).getSupportActionBar() != null) {
                        ((MainActivity) getActivity()).getSupportActionBar().setTitle(R.string.nav_quiz);
                    }
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}