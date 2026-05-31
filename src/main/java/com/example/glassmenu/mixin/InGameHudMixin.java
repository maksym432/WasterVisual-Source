/*
 * InGameHudMixin - Architecture & Primary Responsibility:
 * Mixin for InGameHud.
 * Intercepts hotbar rendering to hide the vanilla hotbar when the
 * custom Fast Item circular hotbar is active.
 */
package com.example.glassmenu.mixin;

import com.example.glassmenu.GlassMenuClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void glassmenu$onRenderHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (GlassMenuClient.CONFIG.enableFastItem()) {
            ci.cancel();
        }
    }
}
