package com.human.visual;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/** Draws text with the bundled modern font (assets/humanvisual/font/modern.json). */
public final class Fonts {
    public static final Identifier ID = Identifier.of("humanvisual", "modern");
    public static final Style STYLE = Style.EMPTY.withFont(ID);
    public static final Style STYLE_BOLD = STYLE.withBold(true);

    private static final Map<String, Text> CACHE = new HashMap<>();

    private Fonts() {}

    public static Text of(String s) {
        Text t = CACHE.get(s);
        if (t == null) {
            if (CACHE.size() > 512) CACHE.clear();
            t = Text.literal(s).setStyle(STYLE);
            CACHE.put(s, t);
        }
        return t;
    }

    public static int width(TextRenderer tr, String s) { return tr.getWidth(of(s)); }

    public static void draw(DrawContext c, TextRenderer tr, String s, int x, int y, int color, boolean shadow) {
        c.drawText(tr, of(s), x, y, color, shadow);
    }

    public static void center(DrawContext c, TextRenderer tr, String s, int cx, int y, int color, boolean shadow) {
        c.drawText(tr, of(s), cx - tr.getWidth(of(s)) / 2, y, color, shadow);
    }

    public static void right(DrawContext c, TextRenderer tr, String s, int rx, int y, int color, boolean shadow) {
        c.drawText(tr, of(s), rx - tr.getWidth(of(s)), y, color, shadow);
    }
}
