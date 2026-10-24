package com.human.visual;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;

public class Module {
    public final String name, category;
    public boolean enabled;
    public final List<Setting> settings = new ArrayList<>();

    public Module(String name, String category, boolean enabled) {
        this.name = name;
        this.category = category;
        this.enabled = enabled;
    }

    public <T extends Setting> T add(T s) { settings.add(s); return s; }
    public boolean hasSettings() { return !settings.isEmpty(); }
    public void toggle() { enabled = !enabled; }

    // ---------- settings ----------
    public static abstract class Setting {
        public final String name;
        protected Setting(String name) { this.name = name; }
        public abstract JsonElement save();
        public abstract void load(JsonElement e);
    }

    public static class Bool extends Setting {
        public boolean value;
        public Bool(String n, boolean v) { super(n); value = v; }
        public JsonElement save() { return new JsonPrimitive(value); }
        public void load(JsonElement e) { value = e.getAsBoolean(); }
    }

    public static class Mode extends Setting {
        public final String[] options;
        public int index;
        public Mode(String n, String... options) { super(n); this.options = options; }
        public String get() { return options[index]; }
        public void next() { index = (index + 1) % options.length; }
        public JsonElement save() { return new JsonPrimitive(index); }
        public void load(JsonElement e) { int i = e.getAsInt(); if (i >= 0 && i < options.length) index = i; }
    }

    public static class Slider extends Setting {
        public final double min, max, step;
        public double value;
        public Slider(String n, double min, double max, double step, double value) {
            super(n); this.min = min; this.max = max; this.step = step; this.value = value;
        }
        public double fraction() { return (value - min) / (max - min); }
        public void setFromFraction(double f) {
            f = Math.max(0, Math.min(1, f));
            double v = min + (max - min) * f;
            v = Math.round(v / step) * step;
            value = Math.max(min, Math.min(max, v));
        }
        public JsonElement save() { return new JsonPrimitive(value); }
        public void load(JsonElement e) { value = Math.max(min, Math.min(max, e.getAsDouble())); }
    }

    public static class Color extends Setting {
        public float h, s, v, a;
        public int rgb; // cached
        public final String alphaLabel;
        public boolean hasAlpha = true;

        public Color(String n, float h, float s, float v, float a, String alphaLabel) {
            super(n);
            this.h = h; this.s = s; this.v = v; this.a = a;
            this.alphaLabel = alphaLabel;
            update();
        }
        public Color noAlpha() { hasAlpha = false; return this; }
        public void update() { rgb = hsv(h, s, v); }
        public int argb() { return ((int) (a * 255f + 0.5f) << 24) | rgb; }

        public JsonElement save() {
            JsonArray arr = new JsonArray();
            arr.add(h); arr.add(s); arr.add(v); arr.add(a);
            return arr;
        }
        public void load(JsonElement e) {
            JsonArray arr = e.getAsJsonArray();
            h = arr.get(0).getAsFloat(); s = arr.get(1).getAsFloat();
            v = arr.get(2).getAsFloat(); a = arr.get(3).getAsFloat();
            update();
        }
    }

    /** HSV (0..1) to 0xRRGGBB. */
    public static int hsv(float h, float s, float v) {
        float x = h * 6f;
        int i = ((int) Math.floor(x)) % 6;
        float f = x - (float) Math.floor(x);
        float p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
        float r, g, b;
        switch (i) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            default: r = v; g = p; b = q;
        }
        return ((int) (r * 255f + 0.5f) << 16) | ((int) (g * 255f + 0.5f) << 8) | (int) (b * 255f + 0.5f);
    }
}
