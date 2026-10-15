package com.example.modulelaptrinh.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.modulelaptrinh.models.QuizRequest;
import com.example.modulelaptrinh.models.QuizResult;

import java.util.ArrayList;
import java.util.List;

public class QuizRepository {
    private final ProgrammingDbHelper dbHelper;

    public QuizRepository(ProgrammingDbHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    // Lưu kết quả quiz
    public long saveQuizResult(int userId, QuizResult result) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(QuizDbContract.QuizAttempt.USER_ID, userId);
        values.put(QuizDbContract.QuizAttempt.SUBJECT, result.getRequest().getSubject());
        values.put(QuizDbContract.QuizAttempt.LEVEL, result.getRequest().getLevel());
        values.put(QuizDbContract.QuizAttempt.TOPIC, result.getRequest().getTopic());
        values.put(QuizDbContract.QuizAttempt.TOTAL, result.getTotalQuestions());
        values.put(QuizDbContract.QuizAttempt.CORRECT, result.getCorrectAnswers());
        values.put(QuizDbContract.QuizAttempt.EXP_EARNED, result.getExpEarned());
        values.put(QuizDbContract.QuizAttempt.CREATED_AT, System.currentTimeMillis());

        long id = db.insert(QuizDbContract.QuizAttempt.TABLE, null, values);

        // Cập nhật EXP cho user
        updateUserExp(userId, result.getRequest().getSubject(), result.getExpEarned());

        return id;
    }

    // Cập nhật EXP của user
    private void updateUserExp(int userId, String subject, int expEarned) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Kiểm tra xem đã có record chưa
        Cursor cursor = db.query(
                QuizDbContract.UserProgress.TABLE,
                new String[]{QuizDbContract.UserProgress.TOTAL_EXP},
                QuizDbContract.UserProgress.USER_ID + "=? AND "
                        + QuizDbContract.UserProgress.SUBJECT + "=?",
                new String[]{String.valueOf(userId), subject},
                null, null, null
        );

        ContentValues values = new ContentValues();
        values.put(QuizDbContract.UserProgress.UPDATED_AT, System.currentTimeMillis());

        if (cursor.moveToFirst()) {
            int currentExp = cursor.getInt(0);
            values.put(QuizDbContract.UserProgress.TOTAL_EXP, currentExp + expEarned);
            db.update(QuizDbContract.UserProgress.TABLE, values,
                    QuizDbContract.UserProgress.USER_ID + "=? AND "
                            + QuizDbContract.UserProgress.SUBJECT + "=?",
                    new String[]{String.valueOf(userId), subject});
        } else {
            values.put(QuizDbContract.UserProgress.USER_ID, userId);
            values.put(QuizDbContract.UserProgress.SUBJECT, subject);
            values.put(QuizDbContract.UserProgress.TOTAL_EXP, expEarned);
            db.insert(QuizDbContract.UserProgress.TABLE, null, values);
        }
        cursor.close();
    }

    // Lấy tổng EXP của user theo subject
    public int getTotalExp(int userId, String subject) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                QuizDbContract.UserProgress.TABLE,
                new String[]{QuizDbContract.UserProgress.TOTAL_EXP},
                QuizDbContract.UserProgress.USER_ID + "=? AND "
                        + QuizDbContract.UserProgress.SUBJECT + "=?",
                new String[]{String.valueOf(userId), subject},
                null, null, null
        );
        int exp = 0;
        if (cursor.moveToFirst()) {
            exp = cursor.getInt(0);
        }
        cursor.close();
        return exp;
    }

    // Lấy lịch sử quiz gần đây
    public List<QuizResult> getRecentQuizResults(int userId, int limit) {
        List<QuizResult> results = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(
                QuizDbContract.QuizAttempt.TABLE,
                null,
                QuizDbContract.QuizAttempt.USER_ID + "=?",
                new String[]{String.valueOf(userId)},
                null, null,
                QuizDbContract.QuizAttempt.CREATED_AT + " DESC",
                String.valueOf(limit)
        );

        while (cursor.moveToNext()) {
            // Parse và tạo QuizResult từ cursor
            // (Cần lưu thêm questions vào database hoặc parse từ JSON)
        }
        cursor.close();
        return results;
    }

    // Tính độ chính xác trung bình
    public double getAverageAccuracy(int userId, String subject) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT COALESCE(SUM(" + QuizDbContract.QuizAttempt.CORRECT + "),0), "
                + "COALESCE(SUM(" + QuizDbContract.QuizAttempt.TOTAL + "),0) FROM "
                + QuizDbContract.QuizAttempt.TABLE + " WHERE "
                + QuizDbContract.QuizAttempt.USER_ID + "=? AND "
                + QuizDbContract.QuizAttempt.SUBJECT + "=?";

        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(userId), subject});
        double accuracy = 0;
        if (cursor.moveToFirst()) {
            int correct = cursor.getInt(0);
            int total = cursor.getInt(1);
            accuracy = total == 0 ? 0 : (correct * 100.0) / total;
        }
        cursor.close();
        return accuracy;
    }
}