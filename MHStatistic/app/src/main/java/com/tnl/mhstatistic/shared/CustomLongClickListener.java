package com.tnl.mhstatistic.shared;

import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;

public class CustomLongClickListener implements View.OnTouchListener {

    private static final int LONG_CLICK_DURATION_MS = 500; // Desired long click duration in milliseconds (2 seconds)
    private Handler handler;
    private Runnable longClickRunnable;
    private View.OnLongClickListener onLongClickListener;
    private boolean isLongClicked = false;

    public CustomLongClickListener(View.OnLongClickListener onLongClickListener) {
        this.onLongClickListener = onLongClickListener;
        handler = new Handler();
        longClickRunnable = () -> {
            if (!isLongClicked) {
                isLongClicked = true;
                onLongClickListener.onLongClick(null);
            }
        };
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isLongClicked = false;
                handler.postDelayed(longClickRunnable, LONG_CLICK_DURATION_MS);
                return true; // Return true to indicate the touch event is being handled
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(longClickRunnable);
                if (!isLongClicked) {
                    // Handle click event if not a long click
                    v.performClick();
                }
                return true; // Return true to indicate the touch event is being handled
            default:
                return false; // Default return false to let other touch events pass through
        }
    }
}

