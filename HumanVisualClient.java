package com.human.visual;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.ActionResult;
import org.lwjgl.glfw.GLFW;

public class HumanVisualClient implements ClientModInitializer {
    public static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        Modules.load();

        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.humanvisual.open", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT, "category.humanvisual"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new HumanScreen());
            }
            Modules.tick(client);
        });

        HudRenderCallback.EVENT.register(Hud::render);
        Render3D.init();

        // remember whom you hit (client side only)
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient()) Modules.setTarget(entity);
            return ActionResult.PASS;
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            Modules.restoreGamma(client);
            Modules.save();
        });
    }
}
