/*
 * InvConfigMixin - Architecture & Primary Responsibility:
 * Intercepts InvConfig in Inventory HUD+ mod.
 * Programmatically disables the mod's built-in Potion HUD when our custom
 * Effects HUD is enabled in configurations.
 */
package com.example.glassmenu.mixin;

import com.example.glassmenu.GlassMenuClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dlovin.inventoryhud.config.InvConfig")
public class InvConfigMixin {

    @Inject(method = "getPot", at = @At("HEAD"), cancellable = true, remap = false)
    private void glassmenu$onGetPot(CallbackInfoReturnable<Boolean> cir) {
        if (GlassMenuClient.CONFIG.enableEffectsHud()) {
            cir.setReturnValue(false);
        }
    }
}
