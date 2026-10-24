package com.human.visual.mixin;

import com.human.visual.Modules;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** World Customizer: overrides the time of day on the client only. */
@Mixin(ClientWorld.Properties.class)
public class ClientWorldPropertiesMixin {
    @Inject(method = "getTimeOfDay", at = @At("HEAD"), cancellable = true)
    private void humanvisual$time(CallbackInfoReturnable<Long> cir) {
        if (Modules.WORLD.enabled) cir.setReturnValue((long) Modules.WORLD_TIME.value);
    }
}
