package com.human.visual;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class HumanScreen extends Screen {
    private static final String[] TABS = {"Visuals", "HUD", "Utilities"};
    private static String tab = "Visuals";

    private static final int BG = 0xFF0F0D1A, BORDER = 0xFF2A2740, ROW = 0xFF1A1830, ROW_HOVER = 0xFF241F44;
    private static final int OFF = 0xFF2C2A3C, WHITE = 0xFFFFFFFF, DIM = 0xFF8A87A3;
    private static final int PW = 440, PH = 276, ROW_H = 30, GAP = 4;
    private static final int SV_W = 150, SV_H = 96;

    private int px, py, scroll, maxScroll;
    private Module open;

    private int dragKind; // 0 none, 1 SV, 2 hue, 3 slider, 4 alpha
    private Module.Color dragColor;
    private Module.Slider dragSlider;
    private int dx, dy, dw, dh;

    public HumanScreen() { super(Text.literal("Human Visual")); }

    @Override
    protected void init() {
        px = (width - PW) / 2;
        py = (height - PH) / 2;
    }

    // ---------- helpers ----------
    private static void rrect(DrawContext c, int x1, int y1, int x2, int y2, int col) {
        c.fill(x1 + 2, y1, x2 - 2, y2, col);
        c.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, col);
        c.fill(x1, y1 + 2, x2, y2 - 2, col);
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static int darker(int c) {
        int r = ((c >> 16) & 255) * 6 / 10, g = ((c >> 8) & 255) * 6 / 10, b = (c & 255) * 6 / 10;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static Module.Color firstColor(Module m) {
        for (Module.Setting s : m.settings) if (s instanceof Module.Color c) return c;
        return null;
    }

    private int tabX(int idx) {
        int x = px + 16;
        for (int i = 0; i < idx; i++) x += textRenderer.getWidth(TABS[i]) + 18;
        return x;
    }

    private int colW() { return (PW - 32 - 12) / 2; }
    private int rowX(int i) { return px + 16 + (i % 2) * (colW() + 12); }
    private int rowY(int i) { return py + 42 + (i / 2) * (ROW_H + GAP) - scroll; }
    private int viewTop() { return py + 42; }
    private int viewBottom() { return py + PH - 12; }

    private int pickX() { return px + 16; }
    private int pickY() { return py + 62; }
    private int rowAt(int idx) { return py + 46 + idx * 28; }

    private void drawSwitch(DrawContext ctx, int x, int y, boolean on, int acc) {
        rrect(ctx, x, y, x + 26, y + 12, on ? acc : OFF);
        int kx = on ? x + 16 : x + 2;
        ctx.fill(kx, y + 2, kx + 8, y + 10, WHITE);
    }

    // ---------- render ----------
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0x88000000);
        int acc = Modules.accent();

        Text title = Text.literal("Human Visual").formatted(Formatting.BOLD);
        int tw = textRenderer.getWidth(title);
        ctx.getMatrices().push();
        ctx.getMatrices().scale(1.5f, 1.5f, 1f);
        ctx.drawTextWithShadow(textRenderer, title, (int) ((width / 2f) / 1.5f - tw / 2f), (int) ((py - 26) / 1.5f), WHITE);
        ctx.getMatrices().pop();
        ctx.fill(width / 2 - 18, py - 8, width / 2 + 18, py - 6, acc);

        rrect(ctx, px - 1, py - 1, px + PW + 1, py + PH + 1, BORDER);
        rrect(ctx, px, py, px + PW, py + PH, BG);

        if (open != null) {
            renderSettings(ctx, mx, my, acc);
            return;
        }

        for (int i = 0; i < TABS.length; i++) {
            boolean sel = TABS[i].equals(tab);
            int x = tabX(i);
            ctx.drawTextWithShadow(textRenderer, TABS[i], x, py + 14, sel ? WHITE : DIM);
            if (sel) ctx.fill(x, py + 26, x + textRenderer.getWidth(TABS[i]), py + 28, acc);
        }
        renderList(ctx, mx, my, acc);
    }

    private void renderList(DrawContext ctx, int mx, int my, int acc) {
        List<Module> mods = Modules.of(tab);
        int rows = (mods.size() + 1) / 2;
        int viewH = viewBottom() - viewTop();
        maxScroll = Math.max(0, rows * (ROW_H + GAP) - viewH);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        ctx.enableScissor(px, viewTop(), px + PW, viewBottom());
        if (mods.isEmpty()) ctx.drawCenteredTextWithShadow(textRenderer, "Coming soon", width / 2, py + PH / 2, DIM);
        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            int x = rowX(i), y = rowY(i), w = colW();
            if (y + ROW_H < viewTop() || y > viewBottom()) continue;
            boolean hover = in(mx, my, x, y, w, ROW_H) && my >= viewTop() && my <= viewBottom();
            rrect(ctx, x, y, x + w, y + ROW_H, hover ? ROW_HOVER : ROW);
            ctx.drawTextWithShadow(textRenderer, m.name, x + 12, y + 11, m.enabled ? WHITE : DIM);
            drawSwitch(ctx, x + w - 38, y + 9, m.enabled, acc);
            if (m.hasSettings()) {
                Module.Color c = firstColor(m);
                int sx = x + w - 62;
                if (c != null) rrect(ctx, sx, y + 10, sx + 12, y + 20, 0xFF000000 | c.rgb);
                else for (int k = 0; k < 3; k++) ctx.fill(sx + k * 5, y + 14, sx + k * 5 + 2, y + 16, DIM);
            }
        }
        ctx.disableScissor();

        if (maxScroll > 0) {
            int barH = Math.max(20, viewH * viewH / (viewH + maxScroll));
            int barY = viewTop() + (int) ((viewH - barH) * (scroll / (float) maxScroll));
            ctx.fill(px + PW - 7, barY, px + PW - 5, barY + barH, 0xFF3A3658);
        }
        ctx.drawTextWithShadow(textRenderer, "Click: toggle   Right click / dots: settings", px + 16, py + PH - 10, 0xFF4E4B6B);
    }

    private void renderSettings(DrawContext ctx, int mx, int my, int acc) {
        boolean backHover = in(mx, my, px + 12, py + 10, 48, 16);
        ctx.drawTextWithShadow(textRenderer, "< Back", px + 16, py + 16, backHover ? WHITE : DIM);
        ctx.drawCenteredTextWithShadow(textRenderer, open.name, px + PW / 2, py + 16, WHITE);
        ctx.fill(px + 16, py + 32, px + PW - 16, py + 33, BORDER);

        Module.Color c = firstColor(open);
        int rx = c != null ? px + 256 : px + 16;
        int rw = c != null ? PW - 16 - 256 : PW - 32;
        if (c != null) drawPicker(ctx, c, acc);

        int idx = 0;
        drawToggleRow(ctx, rx, rowAt(idx++), rw, "Enabled", open.enabled, mx, my, acc);
        for (Module.Setting s : open.settings) {
            if (s == c) continue;
            int y = rowAt(idx++);
            if (s instanceof Module.Bool b) {
                drawToggleRow(ctx, rx, y, rw, b.name, b.value, mx, my, acc);
            } else if (s instanceof Module.Mode md) {
                boolean hv = in(mx, my, rx, y, rw, 24);
                rrect(ctx, rx, y, rx + rw, y + 24, hv ? ROW_HOVER : ROW);
                ctx.drawTextWithShadow(textRenderer, md.name, rx + 10, y + 8, WHITE);
                String v = "< " + md.get() + " >";
                ctx.drawTextWithShadow(textRenderer, v, rx + rw - 10 - textRenderer.getWidth(v), y + 8, acc);
            } else if (s instanceof Module.Slider sl) {
                drawSliderRow(ctx, rx, y, rw, sl.name + ": " + (int) sl.value, (float) sl.fraction(), acc);
            }
        }
    }

    private void drawToggleRow(DrawContext ctx, int x, int y, int w, String label, boolean on, int mx, int my, int acc) {
        rrect(ctx, x, y, x + w, y + 24, in(mx, my, x, y, w, 24) ? ROW_HOVER : ROW);
        ctx.drawTextWithShadow(textRenderer, label, x + 10, y + 8, on ? WHITE : DIM);
        drawSwitch(ctx, x + w - 36, y + 6, on, acc);
    }

    private void drawSliderRow(DrawContext ctx, int x, int y, int w, String text, float frac, int acc) {
        rrect(ctx, x, y, x + w, y + 24, ROW);
        int fw = Math.max(4, Math.round(w * frac));
        rrect(ctx, x, y, x + fw, y + 24, darker(acc));
        ctx.drawTextWithShadow(textRenderer, text, x + 10, y + 8, WHITE);
    }

    private void drawPicker(DrawContext ctx, Module.Color c, int acc) {
        int x = pickX(), y = pickY();
        ctx.drawTextWithShadow(textRenderer, "Color", x, py + 46, DIM);

        // saturation / value square: 30 vertical gradients
        for (int i = 0; i < 30; i++) {
            int x1 = x + i * 5;
            ctx.fillGradient(x1, y, x1 + 5, y + SV_H, 0xFF000000 | Module.hsv(c.h, (i + 0.5f) / 30f, 1f), 0xFF000000);
        }
        int mxp = x + Math.round(c.s * SV_W), myp = y + Math.round((1f - c.v) * SV_H);
        ctx.fill(mxp - 3, myp - 3, mxp + 3, myp + 3, WHITE);
        ctx.fill(mxp - 2, myp - 2, mxp + 2, myp + 2, 0xFF000000 | c.rgb);

        // hue bar: 24 gradients
        int hx = x + 158;
        for (int i = 0; i < 24; i++) {
            ctx.fillGradient(hx, y + i * 4, hx + 14, y + i * 4 + 4,
                    0xFF000000 | Module.hsv(i / 24f, 1f, 1f), 0xFF000000 | Module.hsv((i + 1) / 24f, 1f, 1f));
        }
        int hy = y + Math.round(c.h * SV_H);
        ctx.fill(hx - 2, hy - 1, hx + 16, hy + 1, WHITE);

        // preview + hex
        rrect(ctx, x + 180, y, x + 224, y + 30, 0xFF000000 | c.rgb);
        ctx.drawTextWithShadow(textRenderer, String.format("#%06X", c.rgb), x + 180, y + 36, DIM);

        if (c.hasAlpha) {
            drawSliderRow(ctx, x, y + 104, SV_W, c.alphaLabel + ": " + Math.round(c.a * 100) + "%", c.a, acc);
        }
    }

    // ---------- input ----------
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (open != null) {
            clickSettings(mx, my, button);
            return true;
        }
        if (button == 0) {
            for (int i = 0; i < TABS.length; i++) {
                int x = tabX(i);
                if (in(mx, my, x, py + 10, textRenderer.getWidth(TABS[i]), 18)) {
                    tab = TABS[i];
                    scroll = 0;
                    return true;
                }
            }
        }
        if (my >= viewTop() && my <= viewBottom()) {
            List<Module> mods = Modules.of(tab);
            for (int i = 0; i < mods.size(); i++) {
                Module m = mods.get(i);
                int x = rowX(i), y = rowY(i), w = colW();
                if (!in(mx, my, x, y, w, ROW_H)) continue;
                boolean gear = m.hasSettings() && mx >= x + w - 70 && mx <= x + w - 44;
                if (button == 1 || (button == 0 && gear)) {
                    if (m.hasSettings()) open = m;
                } else if (button == 0) {
                    m.toggle();
                }
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private void clickSettings(double mx, double my, int button) {
        if (button != 0) return;
        if (in(mx, my, px + 12, py + 10, 48, 16)) { open = null; return; }

        Module.Color c = firstColor(open);
        int rx = c != null ? px + 256 : px + 16;
        int rw = c != null ? PW - 16 - 256 : PW - 32;

        if (c != null) {
            int x = pickX(), y = pickY();
            if (in(mx, my, x, y, SV_W, SV_H)) { startDrag(1, c, null, x, y, SV_W, SV_H, mx, my); return; }
            if (in(mx, my, x + 158, y, 14, SV_H)) { startDrag(2, c, null, x + 158, y, 14, SV_H, mx, my); return; }
            if (c.hasAlpha && in(mx, my, x, y + 104, SV_W, 24)) { startDrag(4, c, null, x, y + 104, SV_W, 24, mx, my); return; }
        }

        int idx = 0;
        if (in(mx, my, rx, rowAt(idx++), rw, 24)) { open.toggle(); return; }
        for (Module.Setting s : open.settings) {
            if (s == c) continue;
            int y = rowAt(idx++);
            if (!in(mx, my, rx, y, rw, 24)) continue;
            if (s instanceof Module.Bool b) b.value = !b.value;
            else if (s instanceof Module.Mode md) md.next();
            else if (s instanceof Module.Slider sl) startDrag(3, null, sl, rx, y, rw, 24, mx, my);
            return;
        }
    }

    private void startDrag(int kind, Module.Color c, Module.Slider s, int x, int y, int w, int h, double mx, double my) {
        dragKind = kind; dragColor = c; dragSlider = s;
        dx = x; dy = y; dw = w; dh = h;
        updateDrag(mx, my);
    }

    private static float clamp01(double v) { return (float) Math.max(0, Math.min(1, v)); }

    private void updateDrag(double mx, double my) {
        switch (dragKind) {
            case 1 -> {
                dragColor.s = clamp01((mx - dx) / dw);
                dragColor.v = 1f - clamp01((my - dy) / dh);
                dragColor.update();
            }
            case 2 -> {
                dragColor.h = Math.min(0.999f, clamp01((my - dy) / dh));
                dragColor.update();
            }
            case 3 -> dragSlider.setFromFraction((mx - dx) / dw);
            case 4 -> dragColor.a = clamp01((mx - dx) / dw);
            default -> {}
        }
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double ddx, double ddy) {
        if (dragKind != 0) { updateDrag(mx, my); return true; }
        return super.mouseDragged(mx, my, button, ddx, ddy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragKind = 0;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        if (open == null) scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (vAmount * 20)));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) { close(); return true; }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && open != null) { open = null; return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() { Modules.save(); }

    @Override
    public boolean shouldPause() { return false; }
}
