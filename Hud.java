package com.human.visual;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.LivingEntity;

public class Hud {
    private static final String[] LINES = new String[3];
    private static LivingEntity lastTarget;
    private static float shownHp;

    public static void render(DrawContext ctx, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden || mc.currentScreen instanceof HumanScreen) return;
        int acc = Modules.accent();
        info(ctx, mc, acc);
        if (Modules.KEYS.enabled) keys(ctx, mc, acc);
        if (Modules.TARGET_HUD.enabled) targetHud(ctx, mc);
    }

    private static void info(DrawContext ctx, MinecraftClient mc, int acc) {
        int n = 0;
        if (Modules.FPS.enabled) LINES[n++] = "FPS: " + mc.getCurrentFps();
        if (Modules.CPS.enabled) LINES[n++] = "CPS: " + Modules.cps();
        if (Modules.COORDS.enabled)
            LINES[n++] = "XYZ: " + (int) Math.floor(mc.player.getX()) + " " + (int) Math.floor(mc.player.getY()) + " " + (int) Math.floor(mc.player.getZ());
        if (n == 0) return;

        int w = 0;
        for (int i = 0; i < n; i++) w = Math.max(w, mc.textRenderer.getWidth(LINES[i]));
        int x = 6, y = 6, h = n * 11 + 4;
        if (Modules.HUD_BG.enabled) {
            ctx.fill(x, y, x + w + 12, y + h, 0xAA0F0D1A);
            ctx.fill(x, y, x + 2, y + h, acc);
        }
        for (int i = 0; i < n; i++) ctx.drawTextWithShadow(mc.textRenderer, LINES[i], x + 7, y + 3 + i * 11, 0xFFFFFFFF);
    }

    private static void keys(DrawContext ctx, MinecraftClient mc, int acc) {
        int x = 8, y = ctx.getScaledWindowHeight() - 78;
        key(ctx, mc, "W", mc.options.forwardKey.isPressed(), x + 24, y, 22, 22, acc);
        key(ctx, mc, "A", mc.options.leftKey.isPressed(), x, y + 24, 22, 22, acc);
        key(ctx, mc, "S", mc.options.backKey.isPressed(), x + 24, y + 24, 22, 22, acc);
        key(ctx, mc, "D", mc.options.rightKey.isPressed(), x + 48, y + 24, 22, 22, acc);
        key(ctx, mc, "LMB", mc.options.attackKey.isPressed(), x, y + 48, 35, 20, acc);
        key(ctx, mc, "RMB", mc.options.useKey.isPressed(), x + 37, y + 48, 33, 20, acc);
    }

    private static void key(DrawContext ctx, MinecraftClient mc, String label, boolean down, int x, int y, int w, int h, int acc) {
        ctx.fill(x, y, x + w, y + h, down ? ((acc & 0x00FFFFFF) | 0xCC000000) : 0x990F0D1A);
        ctx.drawCenteredTextWithShadow(mc.textRenderer, label, x + w / 2, y + (h - 8) / 2, 0xFFFFFFFF);
    }

    private static void targetHud(DrawContext ctx, MinecraftClient mc) {
        LivingEntity t = Modules.activeTarget();
        if (t == null) return;
        float hp = t.getHealth(), max = Math.max(1f, t.getMaxHealth());
        if (t != lastTarget) { lastTarget = t; shownHp = hp; }
        shownHp += (hp - shownHp) * 0.12f;

        String name = t.getName().getString();
        int w = Math.max(120, mc.textRenderer.getWidth(name) + 20);
        int x = ctx.getScaledWindowWidth() / 2 + 26, y = ctx.getScaledWindowHeight() / 2 + 14, h = 36;
        int acc = Modules.accent();

        ctx.fill(x, y, x + w, y + h, 0xCC0F0D1A);
        ctx.fill(x, y, x + 2, y + h, acc);
        ctx.drawTextWithShadow(mc.textRenderer, name, x + 8, y + 6, 0xFFFFFFFF);
        ctx.drawTextWithShadow(mc.textRenderer, Math.round(hp) + " HP", x + 8, y + 17, 0xFF8A87A3);

        float ratio = Math.max(0f, Math.min(1f, shownHp / max));
        int bx = x + 8, by = y + 28, bw = w - 16;
        ctx.fill(bx, by, bx + bw, by + 4, 0xFF2C2A3C);
        ctx.fill(bx, by, bx + Math.round(bw * ratio), by + 4, 0xFF000000 | Module.hsv(ratio * 0.33f, 0.75f, 1f));
    }
}
