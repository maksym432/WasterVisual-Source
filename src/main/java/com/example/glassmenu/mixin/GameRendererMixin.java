package com.example.glassmenu.mixin;

import com.example.glassmenu.GlassMenuClient;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.example.glassmenu.render.ColorGradingEngine;
import net.minecraft.client.render.RenderTickCounter;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "getBasicProjectionMatrix", at = @At("RETURN"), cancellable = true)
    private void onGetBasicProjectionMatrix(double fov, CallbackInfoReturnable<Matrix4f> cir) {
        if (GlassMenuClient.CONFIG.enableStretch()) {
            Matrix4f matrix = cir.getReturnValue();
            float h = GlassMenuClient.CONFIG.stretchHorizontal();
            float v = GlassMenuClient.CONFIG.stretchVertical();
            // Scale the projection matrix:
            // Since scaling projection X down makes things look wider (stretched),
            // and scaling Y down makes things look taller,
            // we multiply by 1/h and 1/v so a slider value of >1 means stretching.
            matrix.scale(1.0f / h, 1.0f / v, 1.0f);
            cir.setReturnValue(matrix);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        ColorGradingEngine.renderColorGrading();
    }
}
