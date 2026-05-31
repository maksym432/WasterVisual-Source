/*
 * ScreenMixin - Architecture & Primary Responsibility:
 * Base Screen Mixin.
 * Intercepts general screen rendering and input handling to overlay the
 * Dynamic Island on all open GUIs and handle click events.
 */
package com.example.glassmenu.mixin;

import com.example.glassmenu.widget.IslandManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public abstract class ScreenMixin {

    @Inject(at = @At("TAIL"), method = "render")
    private void glassmenu$renderIsland(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        // TitleScreen has its own lock screen and island rendering logic
        if (!(screen instanceof net.minecraft.client.gui.screen.TitleScreen)) {
            IslandManager.render(context, screen.width, screen.height, mouseX, mouseY, delta);
        }
    }
}
