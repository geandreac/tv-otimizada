package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import java.util.Calendar;
import java.util.Random;

/**
 * The backdrop: a sky whose colours follow the real time of day (night, dawn, day, dusk), with soft blobs
 * of light drifting slowly, the focused app's colour rising from behind the dock, and a fixed film grain.
 * Rendered on the CPU into a 128x72 bitmap and drawn scaled up with bilinear filtering, which is both the
 * blur and the cheap part; ~15 fps since nothing moves fast, and only while Home is in front.
 * Ported from Fireglass (github.com/bhaweshverma50/fireglass, SkyView), MIT, (c) 2026 Mihir Gupta;
 * its wallpaper and fixed looks are left out.
 */
public class SkyView extends View {
    private static final int W = 128, H = 72;

    //                                   base        blob 1      blob 2      blob 3
    private static final int[] NIGHT = {0xFF070B1F, 0xFF2E3A8C, 0xFF6E3FB8, 0xFF1B6F8A};
    private static final int[] DAWN = {0xFF2A2350, 0xFFF6B49A, 0xFFC9A3E8, 0xFF5B6CC9};
    private static final int[] DAY = {0xFF0E2A5A, 0xFF3E7BFA, 0xFF3FB8C9, 0xFF5CC9A0};
    private static final int[] DUSK = {0xFF2B1240, 0xFFFF8A5B, 0xFFE2508F, 0xFF6A3FD1};
    private static final float[] HOURS = {0f, 5.5f, 8f, 11f, 16f, 18.5f, 20.5f, 24f};
    private static final int[][] KEYS = {NIGHT, NIGHT, DAWN, DAY, DAY, DUSK, NIGHT, NIGHT};

    // Per blob: x/y drift speeds (rad/s), phases, reach, strength. Slow and out of step.
    private static final double[][] BLOBS = {
            {0.031, 0.023, 0.0, 1.7, 0.55, 0.80},
            {0.019, 0.029, 2.1, 0.4, 0.50, 0.70},
            {0.026, 0.017, 4.2, 3.3, 0.45, 0.65},
    };

    private final Bitmap sky = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
    private final int[] px = new int[W * H];
    private final float[] pal = new float[12];
    private final float[] bx = new float[4], by = new float[4], reach = new float[4], str = new float[4];
    private final Paint skyPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint grain = new Paint();
    private final Rect dst = new Rect();
    private boolean running;
    // Still sky: blobs frozen at one moment, frames only while the accent eases (Fire TV Stick, to spare its CPU).
    private boolean still;
    private static final double STILL_SECONDS = 40.0;   // the frozen moment: a pleasant spread of the blobs

    // Accent: the focused app's colour and how strongly it shows, eased over ACCENT_MS.
    private static final long ACCENT_MS = 700;
    private int accentFrom = 0xFF3A3A3A, accentTo = 0xFF3A3A3A;
    private float levelFrom, levelTo;
    private long accentAt;
    // While the remote is in use the sky holds still, so focus movement gets the whole GPU.
    private static final long HOLD_MS = 450;
    private long holdUntil;

    public SkyView(Context c, AttributeSet attrs) {
        super(c, attrs);
        sky.setHasAlpha(false);   // opaque: drawing it needs no blending with what's under it
        grain.setShader(new BitmapShader(noise(), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
    }

    /** Animate only while Home is in front; a paused sky costs nothing. */
    void setRunning(boolean run) {
        if (running == run) return;
        running = run;
        if (run) invalidate();
    }

    void setStill(boolean still) {
        this.still = still;
        invalidate();
    }

    /** Remote in use: pause the drift for a moment. */
    void hold() {
        holdUntil = SystemClock.uptimeMillis() + HOLD_MS;
    }

    /** Ease the accent to {@code color} at {@code level} (0..1). */
    void setAccent(int color, float level) {
        long now = SystemClock.uptimeMillis();
        float t = progress(now);
        accentFrom = mix(accentFrom, accentTo, t);
        levelFrom = levelFrom + (levelTo - levelFrom) * t;
        accentTo = color;
        levelTo = level;
        accentAt = Math.max(now, holdUntil);   // the light catches up once the remote goes quiet
        invalidate();
    }

    private float progress(long now) {
        if (accentAt == 0) return 1f;
        float t = Math.max(0f, Math.min(1f, (now - accentAt) / (float) ACCENT_MS));
        return t * t * (3 - 2 * t);
    }

    @Override protected void onDetachedFromWindow() {
        running = false;
        super.onDetachedFromWindow();
    }

    @Override protected void onDraw(Canvas c) {
        long now = SystemClock.uptimeMillis();
        if (now >= holdUntil) render(now);
        dst.set(0, 0, getWidth(), getHeight());
        c.drawBitmap(sky, null, dst, skyPaint);
        c.drawPaint(grain);
        if (!running) return;
        boolean easing = Math.abs(now - accentAt) < ACCENT_MS;
        if (still) {
            if (now < holdUntil) postInvalidateDelayed(holdUntil - now);
            else if (easing) postInvalidateDelayed(33);
            return;   // nothing else changes: no more frames until the next accent
        }
        if (now < holdUntil) postInvalidateDelayed(holdUntil - now);
        else postInvalidateDelayed(easing ? 33 : 66);
    }

    private void render(long now) {
        palette();
        float t = progress(now);
        int accent = mix(accentFrom, accentTo, t);
        float level = levelFrom + (levelTo - levelFrom) * t;
        double sec = still ? STILL_SECONDS : System.currentTimeMillis() / 1000.0;

        for (int i = 0; i < 3; i++) {
            double[] b = BLOBS[i];
            bx[i] = (float) (0.5 + 0.42 * Math.sin(sec * b[0] + b[2]));
            by[i] = (float) (0.42 + 0.34 * Math.sin(sec * b[1] + b[3]));
            reach[i] = (float) b[4];
            str[i] = (float) b[5];
        }
        bx[3] = (float) (0.5 + 0.12 * Math.sin(sec * 0.05));
        by[3] = 1.05f;
        reach[3] = 0.62f;
        str[3] = level;
        float ar = Color.red(accent), ag = Color.green(accent), ab = Color.blue(accent);

        for (int y = 0; y < H; y++) {
            float fy = y / (float) (H - 1);
            // Darker toward the top and bottom edges, where the clock and the dock sit.
            float shade = 0.70f + 0.30f * (float) Math.sin(Math.PI * (0.15f + 0.8f * fy));
            for (int x = 0; x < W; x++) {
                float fx = x / (float) (W - 1);
                float r = pal[0], g = pal[1], b = pal[2];
                for (int i = 0; i < 4; i++) {
                    float dx = (fx - bx[i]) * 1.78f, dy = fy - by[i];
                    float d = 1f - (dx * dx + dy * dy) / (reach[i] * reach[i]);
                    if (d <= 0f) continue;
                    float w = d * d * str[i];
                    float cr, cg, cb;
                    if (i < 3) { cr = pal[3 + i * 3]; cg = pal[4 + i * 3]; cb = pal[5 + i * 3]; }
                    else { cr = ar; cg = ag; cb = ab; }
                    r += (cr - r) * w;
                    g += (cg - g) * w;
                    b += (cb - b) * w;
                }
                px[y * W + x] = 0xFF000000 | ((int) (r * shade) << 16) | ((int) (g * shade) << 8) | (int) (b * shade);
            }
        }
        sky.setPixels(px, 0, W, 0, 0, W, H);
    }

    /** Palette for the current time of day, blended between neighbouring keys. */
    private void palette() {
        Calendar now = Calendar.getInstance();
        float h = now.get(Calendar.HOUR_OF_DAY) + now.get(Calendar.MINUTE) / 60f;
        int k = 0;
        while (k < HOURS.length - 2 && h >= HOURS[k + 1]) k++;
        float t = (h - HOURS[k]) / (HOURS[k + 1] - HOURS[k]);
        t = t * t * (3 - 2 * t);
        for (int i = 0; i < 4; i++) {
            int c = mix(KEYS[k][i], KEYS[k + 1][i], t);
            pal[i * 3] = Color.red(c);
            pal[i * 3 + 1] = Color.green(c);
            pal[i * 3 + 2] = Color.blue(c);
        }
    }

    private static int mix(int a, int b, float t) {
        return Color.rgb(
                (int) (Color.red(a) + (Color.red(b) - Color.red(a)) * t),
                (int) (Color.green(a) + (Color.green(b) - Color.green(a)) * t),
                (int) (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t));
    }

    /** Fixed grain: faint light and dark specks. Also hides banding in the dark gradients. */
    private static Bitmap noise() {
        int n = 128;
        int[] p = new int[n * n];
        Random rnd = new Random(7);
        for (int i = 0; i < p.length; i++) {
            int a = rnd.nextInt(14);
            p[i] = (a << 24) | (rnd.nextBoolean() ? 0xFFFFFF : 0x000000);
        }
        return Bitmap.createBitmap(p, n, n, Bitmap.Config.ARGB_8888);
    }
}
