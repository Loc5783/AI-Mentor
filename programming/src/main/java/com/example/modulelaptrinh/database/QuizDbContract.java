package com.example.modulelaptrinh.database;

public final class QuizDbContract {
    private QuizDbContract() { }

    public static final class QuizAttempt {
        public static final String TABLE = "programming_quiz_attempts";
        public static final String ID = "id";
        public static final String USER_ID = "user_id";
        public static final String SUBJECT = "subject";
        public static final String LEVEL = "level";
        public static final String TOPIC = "topic";
        public static final String TOTAL = "total_questions";
        public static final String CORRECT = "correct_answers";
        public static final String EXP_EARNED = "exp_earned";
        public static final String CREATED_AT = "created_at";
    }

    public static final class UserProgress {
        public static final String TABLE = "user_progress";
        public static final String USER_ID = "user_id";
        public static final String TOTAL_EXP = "total_exp";
        public static final String SUBJECT = "subject";
        public static final String UPDATED_AT = "updated_at";
    }

    public static String createQuizAttemptTable() {
        return "CREATE TABLE IF NOT EXISTS " + QuizAttempt.TABLE + " ("
                + QuizAttempt.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + QuizAttempt.USER_ID + " INTEGER NOT NULL, "
                + QuizAttempt.SUBJECT + " TEXT NOT NULL, "
                + QuizAttempt.LEVEL + " TEXT NOT NULL, "
                + QuizAttempt.TOPIC + " TEXT, "
                + QuizAttempt.TOTAL + " INTEGER NOT NULL, "
                + QuizAttempt.CORRECT + " INTEGER NOT NULL, "
                + QuizAttempt.EXP_EARNED + " INTEGER NOT NULL DEFAULT 0, "
                + QuizAttempt.CREATED_AT + " INTEGER NOT NULL)";
    }

    public static String createUserProgressTable() {
        return "CREATE TABLE IF NOT EXISTS " + UserProgress.TABLE + " ("
                + UserProgress.USER_ID + " INTEGER PRIMARY KEY, "
                + UserProgress.TOTAL_EXP + " INTEGER NOT NULL DEFAULT 0, "
                + UserProgress.SUBJECT + " TEXT NOT NULL, "
                + UserProgress.UPDATED_AT + " INTEGER NOT NULL)";
    }
}