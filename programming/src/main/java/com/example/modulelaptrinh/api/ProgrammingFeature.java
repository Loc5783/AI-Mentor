package com.example.modulelaptrinh.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.example.modulelaptrinh.MainActivity;

public final class ProgrammingFeature {
    private ProgrammingFeature() {
    }

    public static void open(Context context, int userId) {
        if (context == null) {
            return;
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_USER_ID, Math.max(0, userId));
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }
}
