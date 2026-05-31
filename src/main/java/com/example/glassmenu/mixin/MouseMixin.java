/*
 * MouseMixin - Architecture & Primary Responsibility:
 * Mixin into net.minecraft.client.Mouse.
 * Intercepts mouse click events to update clicks per second (CPS) counter for the User Indicator HUD.
 */
package com.example.glassmenu.mixin;

import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        if (action == 1) { // GLFW.GLFW_PRESS
            com.example.glassmenu.render.UserIndicatorRenderer.registerClick(button);
        }
    }
}
