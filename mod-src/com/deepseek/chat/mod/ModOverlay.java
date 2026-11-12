package com.deepseek.chat.mod;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.content.Intent;

public final class ModOverlay {
    private static final String TAG = "dplab.mod.overlay";

    private ModOverlay() {}

    public static void attach(Object target) {
        if (!(target instanceof Activity)) return;
        Activity activity = (Activity) target;
        if (activity.isFinishing() || activity.isDestroyed()) return;
        View decor = activity.getWindow().getDecorView();
        if (!(decor instanceof ViewGroup)) return;
        ViewGroup root = (ViewGroup) decor;
        if (root.findViewWithTag(TAG) != null) return;

        TextView button = new TextView(activity);
        button.setTag(TAG);
        button.setText("dp");
        button.setTextColor(Color.rgb(12, 16, 12));
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setElevation(dp(activity, 12));
        button.setContentDescription("Открыть dpLab");
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(216, 255, 98));
        background.setCornerRadius(dp(activity, 17));
        background.setStroke(dp(activity, 1), Color.rgb(246, 255, 212));
        button.setBackground(background);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                dp(activity, 50), dp(activity, 50), Gravity.BOTTOM | Gravity.START);
        params.setMargins(dp(activity, 16), 0, 0, dp(activity, 118));
        button.setOnClickListener(view -> {
            try {
                Intent intent = new Intent();
                intent.setClassName(activity, "com.deepseek.chat.mod.ModToolsActivity");
                activity.startActivity(intent);
            } catch (Exception ignored) {
            }
        });
        root.addView(button, params);
    }

    private static int dp(Activity activity, int value) {
        return (int) (value * activity.getResources().getDisplayMetrics().density + 0.5f);
    }
}
