/*
 * PotionLayerMixin - Architecture & Primary Responsibility:
 * Mixin targeting Inventory HUD+ mod's PotionLayer.
 * Disables the third-party Potion HUD rendering when our custom Potion
 * Effects HUD is enabled in configurations to prevent overlapping displays.
 */
package com.example.glassmenu.mixin;

import com.example.glassmenu.GlassMenuClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dlovin.inventoryhud.gui.layers.PotionLayer")
public class PotionLayerMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = false)
    private void glassmenu$onRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (GlassMenuClient.CONFIG.enableEffectsHud()) {
            ci.cancel();
        }
    }
}
