package com.gathin.dormir;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;

/**
 * Dark screen for sleeping with music on. While it is in front the screen stays on, so the Fire TV never goes
 * to sleep (and never tells the TV to switch off over HDMI-CEC); music started in Spotify keeps playing behind it.
 * The picture is a dim clock drifting slowly over black. A plain black frame (or a tiny dot) reads as "no signal"
 * to many TVs, which then switch themselves off, so the clock is big enough to lift the average brightness a little.
 * Up/Down set how bright it is (remembered), to find the darkest level a given TV still accepts. Back leaves.
 */
public class DormirActivity extends Activity {

    // Grey levels for the clock, darkest first. 0x38 (22%) is the default; the TV decides how low it can go.
    static final int[] LEVELS = {0x1E, 0x2A, 0x38, 0x48, 0x5C, 0x74};
    private static final int DEFAULT_LEVEL = 2;
    private DriftView view;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        prefs = getSharedPreferences("dormir", MODE_PRIVATE);
        view = new DriftView(this, prefs.getInt("nivel", DEFAULT_LEVEL));
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setContentView(view);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        int d = keyCode == KeyEvent.KEYCODE_DPAD_UP ? 1 : keyCode == KeyEvent.KEYCODE_DPAD_DOWN ? -1 : 0;
        if (d == 0) return super.onKeyDown(keyCode, event);
        int level = clampLevel(view.level + d);
        prefs.edit().putInt("nivel", level).apply();
        view.setLevel(level);
        return true;
    }

    static int clampLevel(int level) {
        return Math.max(0, Math.min(LEVELS.length - 1, level));
    }

    /** Next position on one axis: moves by {@code v[0]} and bounces inside [0, max]. Changes {@code v[0]} on a bounce. */
    static float step(float pos, float[] v, float max) {
        if (max <= 0) return 0;
        float p = pos + v[0];
        if (p < 0) { p = -p; v[0] = -v[0]; }
        if (p > max) { p = 2 * max - p; v[0] = -v[0]; }
        return Math.max(0, Math.min(max, p));
    }

    static final class DriftView extends View {
        private static final long FRAME_MS = 1000;      // once a second: the clock ticks and moves, near-zero CPU
        private static final long HINT_MS = 8000;       // how long the help line shows after opening or a change
        private final Paint clock = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint hint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Rect bounds = new Rect();
        private final float[] vx, vy;
        private long hintUntil = SystemClock.uptimeMillis() + HINT_MS;
        private float x = -1, y;
        int level;

        DriftView(Context c, int level) {
            super(c);
            float d = c.getResources().getDisplayMetrics().density;
            vx = new float[]{3 * d};
            vy = new float[]{2 * d};
            clock.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
            clock.setTextSize(96 * d);
            hint.setColor(0xFF2E2E2E);
            hint.setTextSize(16 * d);
            hint.setTextAlign(Paint.Align.CENTER);
            setBackgroundColor(Color.BLACK);
            setKeepScreenOn(true);
            setLevel(clampLevel(level));
        }

        void setLevel(int level) {
            this.level = level;
            int g = LEVELS[level];
            clock.setColor(Color.rgb(g, g, g));
            hintUntil = SystemClock.uptimeMillis() + HINT_MS;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas c) {
            String time = DateFormat.getTimeFormat(getContext()).format(new java.util.Date());
            clock.getTextBounds(time, 0, time.length(), bounds);
            float w = getWidth() - bounds.width(), h = getHeight() - bounds.height();
            if (x < 0) { x = w * 0.4f; y = h * 0.45f; }
            x = step(x, vx, w);
            y = step(y, vy, h);
            c.drawText(time, x - bounds.left, y - bounds.top, clock);
            if (SystemClock.uptimeMillis() < hintUntil) {
                c.drawText("Brilho " + (level + 1) + "/" + LEVELS.length + " · Cima/Baixo ajusta · Voltar sai",
                        getWidth() / 2f, getHeight() * 0.93f, hint);
            }
            postInvalidateDelayed(FRAME_MS);
        }
    }
}
