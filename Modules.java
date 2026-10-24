package com.human.visual;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.human.visual.mixin.OverlayTextureAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL13;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Modules {
    public static final List<Module> ALL = new ArrayList<>();

    // ---- Visuals ----
    public static final Module HIT_COLOR = add("Hit Color", "Visuals", true);
    public static final Module.Color HIT_C = HIT_COLOR.add(new Module.Color("Color", 0.75f, 0.75f, 1f, 0.6f, "Intensity"));

    public static final Module TARGET_ESP = add("Target ESP", "Visuals", true);
    public static final Module.Color ESP_C = TARGET_ESP.add(new Module.Color("Color", 0.72f, 0.65f, 1f, 0.9f, "Opacity"));
    public static final Module.Mode ESP_STYLE = TARGET_ESP.add(new Module.Mode("Style", "Ring", "Box"));
    public static final Module.Bool ESP_PLAYERS = TARGET_ESP.add(new Module.Bool("Players", true));
    public static final Module.Bool ESP_MOBS = TARGET_ESP.add(new Module.Bool("Mobs", true));
    public static final Module.Bool ESP_ANIMALS = TARGET_ESP.add(new Module.Bool("Animals", false));
    public static final Module.Bool ESP_RAINBOW = TARGET_ESP.add(new Module.Bool("Rainbow", false));

    public static final Module BLOCK_OVERLAY = add("Block Overlay", "Visuals", true);
    public static final Module.Color BO_C = BLOCK_OVERLAY.add(new Module.Color("Color", 0.72f, 0.65f, 1f, 0.95f, "Opacity"));

    public static final Module CHINA_HAT = add("China Hat", "Visuals", false);
    public static final Module.Color HAT_C = CHINA_HAT.add(new Module.Color("Color", 0.95f, 0.7f, 1f, 0.9f, "Opacity"));
    public static final Module.Bool HAT_RAINBOW = CHINA_HAT.add(new Module.Bool("Rainbow", true));

    public static final Module WORLD = add("World Customizer", "Visuals", false);
    public static final Module.Slider WORLD_TIME = WORLD.add(new Module.Slider("Time", 0, 24000, 500, 13000));
    public static final Module.Bool WORLD_CLEAR = WORLD.add(new Module.Bool("Clear Weather", true));

    public static final Module FULLBRIGHT = add("Full Bright", "Visuals", false);

    // ---- HUD ----
    public static final Module FPS = add("FPS", "HUD", true);
    public static final Module CPS = add("CPS", "HUD", true);
    public static final Module COORDS = add("Coordinates", "HUD", false);
    public static final Module KEYS = add("Keystrokes", "HUD", true);
    public static final Module TARGET_HUD = add("Target HUD", "HUD", true);
    public static final Module HUD_BG = add("HUD Background", "HUD", true);

    // ---- Utilities ----
    public static final Module THEME = add("Theme", "Utilities", true);
    public static final Module.Color THEME_C = THEME.add(new Module.Color("Accent", 0.70f, 0.69f, 0.96f, 1f, "Opacity").noAlpha());

    private static final int VANILLA_HIT = 0xB2FF0000;
    private static final Deque<Long> clicks = new ArrayDeque<>();
    private static final Map<String, List<Module>> BY_CAT = new HashMap<>();
    private static boolean wasDown, gammaApplied;
    private static double oldGamma = 1.0;
    private static int appliedHit = VANILLA_HIT;

    public static LivingEntity target;
    public static long targetTime;

    private Modules() {}

    private static Module add(String n, String c, boolean on) {
        Module m = new Module(n, c, on);
        ALL.add(m);
        return m;
    }

    public static List<Module> of(String category) {
        return BY_CAT.computeIfAbsent(category, k -> {
            List<Module> out = new ArrayList<>();
            for (Module m : ALL) if (m.category.equals(k)) out.add(m);
            return out;
        });
    }

    public static int accent() { return THEME.enabled ? (0xFF000000 | THEME_C.rgb) : 0xFF6C4CF5; }

    // ---- target tracking ----
    public static void setTarget(Entity e) {
        if (e instanceof LivingEntity le) { target = le; targetTime = System.currentTimeMillis(); }
    }

    public static LivingEntity activeTarget() {
        LivingEntity t = target;
        if (t == null) return null;
        if (t.isRemoved() || !t.isAlive() || System.currentTimeMillis() - targetTime > 4000L) {
            target = null;
            return null;
        }
        return allowed(t) ? t : null;
    }

    public static boolean allowed(LivingEntity e) {
        if (e instanceof PlayerEntity) return ESP_PLAYERS.value;
        if (e instanceof AnimalEntity) return ESP_ANIMALS.value;
        if (e instanceof HostileEntity) return ESP_MOBS.value;
        return ESP_MOBS.value;
    }

    // ---- cps ----
    public static int cps() {
        prune();
        return clicks.size();
    }

    private static void prune() {
        long now = System.currentTimeMillis();
        while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000L) clicks.pollFirst();
    }

    // ---- tick ----
    public static void tick(MinecraftClient mc) {
        if (mc.getWindow() != null) {
            boolean down = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            if (down && !wasDown && mc.currentScreen == null) clicks.addLast(System.currentTimeMillis());
            wasDown = down;
            prune();
        }

        if (FULLBRIGHT.enabled) {
            if (!gammaApplied) { oldGamma = mc.options.getGamma().getValue(); gammaApplied = true; }
            mc.options.getGamma().setValue(16.0);
        } else {
            restoreGamma(mc);
        }

        if (WORLD.enabled && WORLD_CLEAR.value && mc.world != null) {
            mc.world.setRainGradient(0f);
            mc.world.setThunderGradient(0f);
        }

        applyHitColor(mc);
    }

    /**
     * Hit Color: rewrites the "hurt" rows of the overlay texture, only when the colour actually changes.
     * In the shader the overlay alpha is the weight of the ORIGINAL colour, so intensity = 1 - alpha.
     */
    private static void applyHitColor(MinecraftClient mc) {
        if (mc.gameRenderer == null) return;
        int want = VANILLA_HIT;
        if (HIT_COLOR.enabled) want = ((int) ((1f - HIT_C.a) * 255f) << 24) | HIT_C.rgb;
        if (want == appliedHit) return;

        OverlayTexture ot = mc.gameRenderer.getOverlayTexture();
        NativeImageBackedTexture tex = ((OverlayTextureAccessor) ot).humanvisual$getTexture();
        NativeImage img = tex.getImage();
        if (img == null) return;
        for (int y = 0; y < 8; y++) for (int x = 0; x < 16; x++) img.setColorArgb(x, y, want);
        RenderSystem.activeTexture(GL13.GL_TEXTURE1);
        tex.upload();
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        appliedHit = want;
    }

    public static void restoreGamma(MinecraftClient mc) {
        if (gammaApplied) {
            mc.options.getGamma().setValue(oldGamma);
            gammaApplied = false;
        }
    }

    // ---- config ----
    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("humanvisual.json"); }

    public static void save() {
        try {
            JsonObject root = new JsonObject();
            for (Module m : ALL) {
                JsonObject o = new JsonObject();
                o.addProperty("enabled", m.enabled);
                for (Module.Setting s : m.settings) o.add(s.name, s.save());
                root.add(m.name, o);
            }
            Files.writeString(file(), new GsonBuilder().setPrettyPrinting().create().toJson(root));
        } catch (Exception ignored) {}
    }

    public static void load() {
        try {
            Path p = file();
            if (!Files.exists(p)) return;
            JsonObject root = JsonParser.parseString(Files.readString(p)).getAsJsonObject();
            for (Module m : ALL) {
                if (!root.has(m.name)) continue;
                JsonObject o = root.getAsJsonObject(m.name);
                if (o.has("enabled")) m.enabled = o.get("enabled").getAsBoolean();
                for (Module.Setting s : m.settings) {
                    try { if (o.has(s.name)) s.load(o.get(s.name)); } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}
    }
}
