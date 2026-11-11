package kr.scanworks.workorbit;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Draws the Work Orbit star system. Layout is in a 720-unit design space scaled to the screen. */
final class OrbitRenderer {

    private static final float TILT = 0.30f;
    private static final float[] RADII = {95f, 165f, 235f, 300f};
    private static final float[] PLANET_R = {17f, 14f, 13f, 12f};
    private static final float[] PERIOD_S = {70f, 100f, 140f, 190f};
    private static final float SPIN_S = 8f;
    private static final float PULSE_S = 5f;

    private static final int ORANGE = Color.rgb(255, 163, 107);
    private static final int HOT = Color.rgb(255, 122, 61);
    private static final int PALE = Color.rgb(255, 217, 192);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mono = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sans = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();

    private static final class Body {
        float depth, x, y, r, alpha;
        String label;
        int index;
    }

    private final List<Body> bodies = new ArrayList<>();
    private float ut = 1f;

    OrbitRenderer() {
        stroke.setStyle(Paint.Style.STROKE);
        fill.setStyle(Paint.Style.FILL);
        glow.setStyle(Paint.Style.FILL);
        mono.setTypeface(Typeface.MONOSPACE);
        sans.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        sans.setTextAlign(Paint.Align.CENTER);
    }

    void draw(Canvas c, int w, int h, long nowMs, OrbitData data) {
        float u = Math.min(w, h) / 720f;
        // tall cover screen: same orbit size, larger text
        ut = (h > w * 1.6f) ? u * 1.4f : u;
        float cx = w / 2f;
        float cy = h / 2f - 90f * u;
        double t = nowMs / 1000.0;

        c.drawColor(Color.BLACK);

        stroke.setStrokeWidth(Math.max(1f, 1f * u));

        // back halves of orbit rings
        stroke.setColor(withAlpha(ORANGE, 55));
        for (float r : RADII) {
            oval.set(cx - r * u, cy - r * TILT * u, cx + r * u, cy + r * TILT * u);
            c.drawArc(oval, 180f, 180f, false, stroke);
        }

        // planet positions
        bodies.clear();
        for (int i = 0; i < data.planets.size(); i++) {
            OrbitData.Planet p = data.planets.get(i);
            double th = p.a0 + 2.0 * Math.PI * (t / PERIOD_S[p.orbit]);
            float r = RADII[p.orbit];
            float depth = (float) Math.sin(th);
            Body b = new Body();
            b.depth = depth;
            b.x = cx + (float) (r * Math.cos(th)) * u;
            b.y = cy + r * TILT * depth * u;
            float k = (depth + 1f) / 2f;
            b.r = PLANET_R[p.orbit] * (0.82f + 0.22f * k) * u;
            b.alpha = 0.55f + 0.45f * k;
            b.label = p.open > 0 ? p.name + " · " + p.open : p.name;
            b.index = i;
            bodies.add(b);
        }
        Collections.sort(bodies, (a, b) -> Float.compare(a.depth, b.depth));

        for (Body b : bodies) {
            if (b.depth < 0) drawPlanet(c, b, t, u);
        }

        drawStar(c, cx, cy, t, u);

        // front halves of rings
        stroke.setColor(withAlpha(ORANGE, 95));
        for (float r : RADII) {
            oval.set(cx - r * u, cy - r * TILT * u, cx + r * u, cy + r * TILT * u);
            c.drawArc(oval, 0f, 180f, false, stroke);
        }

        for (Body b : bodies) {
            if (b.depth >= 0) drawPlanet(c, b, t, u);
        }

        drawText(c, cx, cy, u, data);
    }

    private void drawStar(Canvas c, float cx, float cy, double t, float u) {
        float pulse = (float) (0.5 + 0.5 * Math.sin(2.0 * Math.PI * t / PULSE_S));
        float gr = (100f + 8f * pulse) * u;
        glow.setShader(new RadialGradient(cx, cy, gr,
                new int[]{withAlpha(HOT, 170), withAlpha(HOT, 70), withAlpha(HOT, 0)},
                new float[]{0f, 0.4f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, gr, glow);
        glow.setShader(null);
        fill.setColor(Color.rgb(255, 196, 150));
        c.drawCircle(cx, cy, 20f * u, fill);
        fill.setColor(Color.rgb(255, 240, 225));
        c.drawCircle(cx, cy, 11f * u, fill);
    }

    private void drawPlanet(Canvas c, Body b, double t, float u) {
        int a = (int) (255 * b.alpha);
        float x = b.x, y = b.y, r = b.r;

        float gr = r * 1.8f;
        glow.setShader(new RadialGradient(x, y, gr,
                new int[]{withAlpha(HOT, (int) (90 * b.alpha)), withAlpha(HOT, 0)},
                null, Shader.TileMode.CLAMP));
        c.drawCircle(x, y, gr, glow);
        glow.setShader(null);

        fill.setColor(Color.rgb(10, 6, 4));
        c.drawCircle(x, y, r, fill);

        stroke.setColor(withAlpha(ORANGE, a));
        c.drawCircle(x, y, r, stroke);

        stroke.setColor(withAlpha(ORANGE, (int) (a * 0.45f)));
        float[] lats = {-0.45f, 0f, 0.45f};
        for (float k : lats) {
            float s = (float) Math.sqrt(1 - k * k);
            float rx = r * s, ry = r * 0.28f * s, yy = y + k * r;
            oval.set(x - rx, yy - ry, x + rx, yy + ry);
            c.drawOval(oval, stroke);
        }
        double spin = 2.0 * Math.PI * t / SPIN_S + b.index;
        for (int m = 0; m < 2; m++) {
            float mrx = (float) Math.abs(Math.cos(spin + m * Math.PI / 2)) * r;
            if (mrx > 1.2f) {
                oval.set(x - mrx, y - r, x + mrx, y + r);
                c.drawOval(oval, stroke);
            }
        }

        mono.setTextSize(12f * ut);
        mono.setColor(withAlpha(ORANGE, (int) (200 * b.alpha)));
        float lx = x + r + 6f * u;
        if (lx + mono.measureText(b.label) > c.getWidth() - 8f * u) {
            // near the right edge: put the label on the left of the planet
            mono.setTextAlign(Paint.Align.RIGHT);
            lx = x - r - 6f * u;
        } else {
            mono.setTextAlign(Paint.Align.LEFT);
        }
        c.drawText(b.label, lx, y - r - 2f * u, mono);
    }

    private void drawText(Canvas c, float cx, float cy, float u, OrbitData data) {
        mono.setTextAlign(Paint.Align.CENTER);
        mono.setTextSize(13f * ut);
        mono.setColor(withAlpha(ORANGE, 150));
        mono.setLetterSpacing(0.6f);
        c.drawText("WORK ORBIT", cx, cy - 175f * u, mono);
        mono.setLetterSpacing(0f);

        if (data.nextTitle == null || data.nextTitle.isEmpty()) {
            return;
        }
        float y0 = cy + 225f * u;
        float v = ut;
        stroke.setColor(withAlpha(ORANGE, 90));
        c.drawLine(cx - 150f * v, y0, cx - 70f * v, y0, stroke);
        c.drawLine(cx + 70f * v, y0, cx + 150f * v, y0, stroke);
        mono.setTextSize(12f * v);
        mono.setColor(withAlpha(ORANGE, 170));
        c.drawText("NEXT DEADLINE", cx, y0 + 4f * v, mono);

        sans.setTextSize(30f * v);
        sans.setColor(PALE);
        c.drawText(data.nextTitle, cx, y0 + 50f * v, sans);

        if (data.nextDate != null) {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), data.nextDate);
            String dday = days > 0 ? "D-" + days : (days == 0 ? "D-DAY" : "D+" + (-days));
            mono.setTextSize(24f * v);
            mono.setColor(ORANGE);
            c.drawText(data.nextDate.format(DATE_FMT) + "  " + dday, cx, y0 + 92f * v, mono);
        }
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(Math.max(0, Math.min(255, alpha)),
                Color.red(color), Color.green(color), Color.blue(color));
    }
}
