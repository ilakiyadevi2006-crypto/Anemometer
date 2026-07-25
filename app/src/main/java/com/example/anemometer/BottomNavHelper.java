package com.example.anemometer;

import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

public class BottomNavHelper {

    public static void applyHoverAnimation(View view) {
        if (view == null) return;
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate()
                            .scaleX(0.92f)
                            .scaleY(0.92f)
                            .setDuration(100)
                            .setInterpolator(new AccelerateDecelerateInterpolator())
                            .start();
                    break;
                case MotionEvent.ACTION_UP:
                    v.performClick();
                case MotionEvent.ACTION_CANCEL:
                    v.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(150)
                            .setInterpolator(new AccelerateDecelerateInterpolator())
                            .start();
                    break;
            }
            return true; 
        });
    }
}
