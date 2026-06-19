/*
 * InGameHudMixin - Architecture & Primary Responsibility:
 * Mixin for InGameHud.
 * (1) Intercepts hotbar rendering to hide the vanilla hotbar when the
 *     custom Fast Item circular hotbar is active.
 * (2) Intercepts health/food status bars rendering when User HUD is active.
 * (3) Intercepts XP bar/level-text rendering when User HUD is active.
 */
package com.example.glassmenu.mixin;

import com.example.glassmenu.GlassMenuClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    /** Cancel vanilla hotbar when Fast Item circular hotbar is active. */
    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void glassmenu$onRenderHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (GlassMenuClient.CONFIG.enableFastItem() || GlassMenuClient.CONFIG.glassHotbar()) {
            ci.cancel();
        }
    }

    @Redirect(method = "renderHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lnet/minecraft/util/Identifier;IIII)V"))
    private void glassmenu$redirectDrawGuiTexture(DrawContext context, net.minecraft.util.Identifier texture, int x, int y, int width, int height) {
        if (GlassMenuClient.CONFIG.glassHotbar() && texture.getPath().contains("hotbar")) {
            if (texture.getPath().endsWith("hotbar_selection")) {
                context.drawGuiTexture(texture, x, y, width, height);
            } else {
                com.example.glassmenu.render.GlassRefractionEngine.drawRefractedPanel(context, x, y, width, height, 0.8f, 0x22FFFFFF, 4f);
                com.example.glassmenu.render.RenderUtils.drawSdfRoundedOutline(context.getMatrices(), x, y, width, height, 4f, 0.8f, 0x33FFFFFF);
            }
        } else {
            context.drawGuiTexture(texture, x, y, width, height);
        }
    }

    /**
     * Cancel vanilla health + food status bars when custom User HUD is active.
     * Method: renderStatusBars(DrawContext) — renders both HP and food rows.
     */
    @Inject(method = "renderStatusBars", at = @At("HEAD"), cancellable = true)
    private void glassmenu$onRenderStatusBars(DrawContext context, CallbackInfo ci) {
        if (GlassMenuClient.CONFIG.enableUserHud()) {
            ci.cancel();
        }
    }

    /**
     * Cancel vanilla XP bar when custom User HUD is active.
     * Method: renderExperienceBar(DrawContext, int) — renders the green XP bar.
     */
    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    private void glassmenu$onRenderExperienceBar(DrawContext context, int x, CallbackInfo ci) {
        if (GlassMenuClient.CONFIG.enableUserHud()) {
            ci.cancel();
        }
    }

    /**
     * Cancel vanilla XP level number text when custom User HUD is active.
     * Method: renderExperienceLevel(DrawContext, RenderTickCounter) — the green level number above XP bar.
     */
    @Inject(method = "renderExperienceLevel", at = @At("HEAD"), cancellable = true)
    private void glassmenu$onRenderExperienceLevel(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (GlassMenuClient.CONFIG.enableUserHud()) {
            ci.cancel();
        }
    }

    /**
     * Cancel vanilla status effect overlays when custom Effects HUD is active.
     * Method: renderStatusEffectOverlay(DrawContext, RenderTickCounter)
     */
    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void glassmenu$onRenderStatusEffectOverlay(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (GlassMenuClient.CONFIG.enableEffectsHud()) {
            ci.cancel();
        }
    }

    /**
     * Cancel vanilla crosshair when custom Crosshair is active.
     */
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void glassmenu$onRenderCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (GlassMenuClient.CONFIG.enableCustomCrosshair()) {
            ci.cancel();
            com.example.glassmenu.render.CustomCrosshairEngine.render(context, tickCounter.getTickDelta(true));
        }
    }
}
