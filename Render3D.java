package com.human.visual;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;

/**
 * World rendering. Everything is drawn with the depth-tested line layer,
 * so nothing is visible through walls. No allocations per frame.
 */
public final class Render3D {
    private static final int SEG = 40;
    private static final float TAU = (float) (Math.PI * 2);
    private static final float[] COS = new float[SEG + 1];
    private static final float[] SIN = new float[SEG + 1];

    static {
        for (int i = 0; i <= SEG; i++) {
            COS[i] = (float) Math.cos(TAU * i / SEG);
            SIN[i] = (float) Math.sin(TAU * i / SEG);
        }
    }

    private Render3D() {}

    public static void init() {
        WorldRenderEvents.AFTER_ENTITIES.register(Render3D::afterEntities);
        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register(Render3D::beforeOutline);
    }

    private static void afterEntities(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        boolean esp = Modules.TARGET_ESP.enabled, hat = Modules.CHINA_HAT.enabled;
        if (!esp && !hat) return;

        MatrixStack ms = ctx.matrixStack();
        VertexConsumerProvider vcp = ctx.consumers();
        if (ms == null || vcp == null) return;

        Vec3d cam = ctx.camera().getPos();
        float td = ctx.tickCounter().getTickDelta(false);
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getLines());
        Matrix4f m = ms.peek().getPositionMatrix();
        long now = System.currentTimeMillis();

        if (esp) {
            LivingEntity t = Modules.activeTarget();
            if (t != null && t != mc.player) {
                Vec3d p = t.getLerpedPos(td);
                float x = (float) (p.x - cam.x), y = (float) (p.y - cam.y), z = (float) (p.z - cam.z);
                int a = (int) (Modules.ESP_C.a * 255f + 0.5f);
                int col = Modules.ESP_RAINBOW.value ? rainbow(now, a, 0f) : Modules.ESP_C.argb();
                float w = t.getWidth(), h = t.getHeight();
                if (Modules.ESP_STYLE.index == 0) {
                    float k = 0.5f - 0.5f * MathHelper.cos((now % 1400L) / 1400f * TAU);
                    float r = w * 0.75f + 0.1f;
                    ring(vc, m, x, y + k * h, z, r, col);
                    ring(vc, m, x, y + 0.02f, z, r, (col & 0x00FFFFFF) | ((a / 3) << 24));
                } else {
                    float hw = w / 2f + 0.05f;
                    box(vc, m, x - hw, y, z - hw, x + hw, y + h + 0.05f, z + hw, col);
                }
            }
        }

        if (hat && !mc.options.getPerspective().isFirstPerson()) {
            Vec3d p = mc.player.getLerpedPos(td);
            float x = (float) (p.x - cam.x), y = (float) (p.y - cam.y), z = (float) (p.z - cam.z);
            int a = (int) (Modules.HAT_C.a * 255f + 0.5f);
            int col = Modules.HAT_RAINBOW.value ? rainbow(now, a, 0f) : Modules.HAT_C.argb();
            float base = y + mc.player.getHeight() - 0.02f;
            float apex = base + 0.32f;
            ring(vc, m, x, base, z, 0.52f, col);
            ring(vc, m, x, base + 0.16f, z, 0.26f, col);
            for (int i = 0; i < SEG; i += 4) {
                line(vc, m, x + COS[i] * 0.52f, base, z + SIN[i] * 0.52f, x, apex, z, col);
            }
        }

        if (vcp instanceof VertexConsumerProvider.Immediate imm) imm.draw(RenderLayer.getLines());
    }

    /** Replaces the vanilla block outline with a coloured one. Return false = cancel vanilla. */
    private static boolean beforeOutline(WorldRenderContext ctx, HitResult hit) {
        if (!Modules.BLOCK_OVERLAY.enabled || !(hit instanceof BlockHitResult bhr)) return true;
        MinecraftClient mc = MinecraftClient.getInstance();
        MatrixStack ms = ctx.matrixStack();
        VertexConsumerProvider vcp = ctx.consumers();
        if (mc.world == null || ms == null || vcp == null) return true;

        BlockPos pos = bhr.getBlockPos();
        BlockState st = mc.world.getBlockState(pos);
        if (st.isAir()) return true;

        VoxelShape shape = st.getOutlineShape(mc.world, pos, ShapeContext.of(ctx.camera().getFocusedEntity()));
        if (shape.isEmpty()) return true;

        Vec3d cam = ctx.camera().getPos();
        final double ox = pos.getX() - cam.x, oy = pos.getY() - cam.y, oz = pos.getZ() - cam.z;
        final int col = Modules.BO_C.argb();
        final VertexConsumer vc = vcp.getBuffer(RenderLayer.getLines());
        final Matrix4f m = ms.peek().getPositionMatrix();

        shape.forEachEdge((x1, y1, z1, x2, y2, z2) ->
                line(vc, m, (float) (x1 + ox), (float) (y1 + oy), (float) (z1 + oz),
                        (float) (x2 + ox), (float) (y2 + oy), (float) (z2 + oz), col));

        if (vcp instanceof VertexConsumerProvider.Immediate imm) imm.draw(RenderLayer.getLines());
        return false;
    }

    private static int rainbow(long now, int alpha, float offset) {
        float h = ((now % 4000L) / 4000f + offset) % 1f;
        return (alpha << 24) | Module.hsv(h, 0.7f, 1f);
    }

    private static void ring(VertexConsumer vc, Matrix4f m, float cx, float cy, float cz, float r, int argb) {
        for (int i = 0; i < SEG; i++) {
            line(vc, m, cx + COS[i] * r, cy, cz + SIN[i] * r, cx + COS[i + 1] * r, cy, cz + SIN[i + 1] * r, argb);
        }
    }

    private static void box(VertexConsumer vc, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, int c) {
        line(vc, m, x1, y1, z1, x2, y1, z1, c); line(vc, m, x2, y1, z1, x2, y1, z2, c);
        line(vc, m, x2, y1, z2, x1, y1, z2, c); line(vc, m, x1, y1, z2, x1, y1, z1, c);
        line(vc, m, x1, y2, z1, x2, y2, z1, c); line(vc, m, x2, y2, z1, x2, y2, z2, c);
        line(vc, m, x2, y2, z2, x1, y2, z2, c); line(vc, m, x1, y2, z2, x1, y2, z1, c);
        line(vc, m, x1, y1, z1, x1, y2, z1, c); line(vc, m, x2, y1, z1, x2, y2, z1, c);
        line(vc, m, x2, y1, z2, x2, y2, z2, c); line(vc, m, x1, y1, z2, x1, y2, z2, c);
    }

    private static void line(VertexConsumer vc, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, int argb) {
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-6f) return;
        dx /= len; dy /= len; dz /= len;
        int a = argb >>> 24, r = (argb >> 16) & 255, g = (argb >> 8) & 255, b = argb & 255;
        vc.vertex(m, x1, y1, z1).color(r, g, b, a).normal(dx, dy, dz);
        vc.vertex(m, x2, y2, z2).color(r, g, b, a).normal(dx, dy, dz);
    }
}
