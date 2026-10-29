package com.example.asm_final.accounts;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.util.Random;

public class LocalRecoveryService implements RecoveryService {
    private final Context context;
    private final SharedPreferences prefs;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    private static final String PREF_NAME = "local_recovery_prefs";
    private static final String KEY_OTP = "current_otp";
    private static final String KEY_OTP_EXPIRY = "otp_expiry_time";
    public static final String KEY_EMAIL_VERIFIED_PREFIX = "email_verified_";
    
    private static final long OTP_VALIDITY_MILLIS = 5 * 60 * 1000; // 5 minutes

    public LocalRecoveryService(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public Result requestReset(String email) {
        return generateAndSendOTP(email);
    }

    @Override
    public Result resendVerification(String email) {
        return generateAndSendOTP(email);
    }

    private Result generateAndSendOTP(String email) {
        // Sinh OTP ngẫu nhiên 6 chữ số
        String otp = String.format(java.util.Locale.US, "%06d", new Random().nextInt(999999));
        long expiryTime = System.currentTimeMillis() + OTP_VALIDITY_MILLIS;

        prefs.edit()
             .putString(KEY_OTP, otp)
             .putLong(KEY_OTP_EXPIRY, expiryTime)
             .apply();

        // Hiển thị Toast thông báo mã OTP
        mainHandler.post(() -> 
            Toast.makeText(context, 
                "AI Mentor: Mã xác nhận của bạn là " + otp + " (Hiệu lực 5 phút)", 
                Toast.LENGTH_LONG).show()
        );

        return Result.REQUEST_ACCEPTED;
    }

    @Override
    public Result verify(String token) {
        String savedOtp = prefs.getString(KEY_OTP, null);
        long expiryTime = prefs.getLong(KEY_OTP_EXPIRY, 0);

        if (savedOtp == null || !savedOtp.equals(token)) {
            return Result.INVALID_TOKEN;
        }
        
        if (System.currentTimeMillis() > expiryTime) {
            return Result.INVALID_TOKEN;
        }

        // Đánh dấu xác minh thành công. Do LocalRecoveryService không biết user hiện tại, 
        // ta giả định việc gọi service này từ AuthRecoveryActivity đã kèm theo context đăng nhập 
        // nên ta xoá token đi. Logic mark verified sẽ gọi ở ViewModel/Activity.
        prefs.edit().remove(KEY_OTP).remove(KEY_OTP_EXPIRY).apply();

        return Result.VERIFIED;
    }

    @Override
    public Result reset(String token, String password) {
        if (verify(token) == Result.VERIFIED) {
            // Demo only: We just accept the reset locally
            return Result.PASSWORD_RESET;
        }
        return Result.INVALID_TOKEN;
    }
}