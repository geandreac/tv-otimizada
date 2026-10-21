package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * Frosted glass for panels over the sky: a translucent white body with a top-lit sheen and a hairline
 * rim that is bright along the top edge and fades down the sides. No real blur; over the soft sky it
 * reads the same and costs nothing.
 * Ported from Fireglass (github.com/bhaweshverma50/fireglass, Look.Glass), MIT, (c) 2026 Mihir Gupta.
 */
final class Glass extends Drawable {
    private final Paint body = new Paint(Paint.ANTI_ALIAS_FLAG), rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final float radius, strength;

    Glass(float radius, float strength) {
        this.radius = radius;
        this.strength = strength;
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(1.5f);
    }

    @Override protected void onBoundsChange(Rect b) {
        r.set(b);
        r.inset(0.75f, 0.75f);
        body.setShader(new LinearGradient(0, b.top, 0, b.bottom,
                new int[]{white(0.17f * strength), white(0.07f * strength), white(0.10f * strength)},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        rim.setShader(new LinearGradient(0, b.top, 0, b.bottom,
                new int[]{white(0.50f), white(0.08f), white(0.16f)},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
    }

    @Override public void draw(Canvas c) {
        c.drawRoundRect(r, radius, radius, body);
        c.drawRoundRect(r, radius, radius, rim);
    }

    private static int white(float alpha) {
        return Math.round(Math.max(0f, Math.min(1f, alpha)) * 255) << 24 | 0xFFFFFF;
    }

    @Override public void setAlpha(int a) { body.setAlpha(a); rim.setAlpha(a); }
    @Override public void setColorFilter(ColorFilter f) { }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
