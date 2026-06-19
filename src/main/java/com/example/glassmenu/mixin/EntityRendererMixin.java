package com.example.glassmenu.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.example.glassmenu.GlassMenuClient;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {

    @Shadow public abstract TextRenderer getTextRenderer();
    @Shadow @Final protected EntityRenderDispatcher dispatcher;

    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void glassmenu$overrideNametagRender(T entity, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float tickDelta, CallbackInfo ci) {
        if (!GlassMenuClient.CONFIG.enableCustomNametags()) {
            return; // Let vanilla / other mods handle it
        }

        double d = this.dispatcher.getSquaredDistanceToCamera(entity);
        if (d > 4096.0D) {
            return;
        }

        float f = entity.getHeight() + 0.5f;
        int i = "deadmau5".equals(text.getString()) ? -10 : 0;
        
        matrices.push();
        matrices.translate(0.0F, f, 0.0F);
        matrices.multiply(this.dispatcher.getRotation());
        matrices.scale(-0.025F, -0.025F, 0.025F);
        
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        
        TextRenderer textRenderer = this.getTextRenderer();
        float h = (float)(-textRenderer.getWidth(text) / 2);
        
        int newTextColor = 0xFF000000 | GlassMenuClient.CONFIG.customNametagColor();
        int newBgColor = 0x55000000; // Glass effect
        TextRenderer.TextLayerType newLayerType = TextRenderer.TextLayerType.SEE_THROUGH; // Through walls
        
        Text unformattedText = Text.literal(text.getString());

        // Draw background and foreground in one pass, forcing color and glass effect
        textRenderer.draw(unformattedText, h, (float)i, newTextColor, false, matrix4f, vertexConsumers, newLayerType, newBgColor, light);
        
        matrices.pop();
        ci.cancel(); // Cancel original rendering
    }
}
