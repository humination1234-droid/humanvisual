package com.human.visual;

import net.minecraft.client.gui.DrawContext;

/** Small 2D drawing helpers: rounded rectangles, soft shadow, colour maths. */
public final class Draw {
    private static final int[][] INSET = new int[9][];

    static {
        for (int r = 1; r <= 8; r++) {
            INSET[r] = new int[r];
            for (int i = 0; i < r; i++) {
                double dy = r - i - 0.5;
                INSET[r][i] = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
            }
        }
    }

    private Draw() {}

    public static void rrect(DrawContext c, int x1, int y1, int x2, int y2, int r, int col) {
        int w = x2 - x1, h = y2 - y1;
        if (w <= 0 || h <= 0) return;
        r = Math.min(8, Math.min(r, Math.min(w, h) / 2));
        if (r <= 0) { c.fill(x1, y1, x2, y2, col); return; }
        int[] in = INSET[r];
        for (int i = 0; i < r; i++) {
            c.fill(x1 + in[i], y1 + i, x2 - in[i], y1 + i + 1, col);
            c.fill(x1 + in[i], y2 - i - 1, x2 - in[i], y2 - i, col);
        }
        c.fill(x1, y1 + r, x2, y2 - r, col);
    }

    public static void shadow(DrawContext c, int x1, int y1, int x2, int y2, int r) {
        for (int i = 1; i <= 5; i++) {
            rrect(c, x1 - i, y1 - i + 3, x2 + i, y2 + i + 3, r + i, 0x0C000000);
        }
    }

    public static int alpha(int c, int a) { return (c & 0xFFFFFF) | (a << 24); }

    public static int lerp(int a, int b, float t) {
        int aa = a >>> 24, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int ba = b >>> 24, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }
}
