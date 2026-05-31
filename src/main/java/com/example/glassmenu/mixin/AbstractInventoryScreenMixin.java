/*
 * AbstractInventoryScreenMixin - Architecture & Primary Responsibility:
 * Mixin for AbstractInventoryScreen.
 * Prevents the visual coordinate shift and hides the vanilla status effects
 * display on the inventory screens when the custom Effects HUD is active.
 */
package com.example.glassmenu.mixin;

import com.example.glassmenu.GlassMenuClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractInventoryScreen.class)
public abstract class AbstractInventoryScreenMixin<T extends ScreenHandler> extends HandledScreen<T> {

    public AbstractInventoryScreenMixin(T handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "hideStatusEffectHud", at = @At("HEAD"), cancellable = true)
    private void glassmenu$onHideStatusEffectHud(CallbackInfoReturnable<Boolean> cir) {
        if (GlassMenuClient.CONFIG.enableEffectsHud()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "drawStatusEffects", at = @At("HEAD"), cancellable = true)
    private void glassmenu$onDrawStatusEffects(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        if (GlassMenuClient.CONFIG.enableEffectsHud()) {
            ci.cancel();
        }
    }
}
