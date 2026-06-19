package com.example.glassmenu.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {

    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void glassmenu$onRenderLabelIfPresent(T entity, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float tickDelta, CallbackInfo ci) {
        double d = MinecraftClient.getInstance().getEntityRenderDispatcher().getSquaredDistanceToCamera(entity);
        if (d > 4096.0D) {
            return;
        }

        // Custom Nametag Rendering
        boolean bl = !entity.isSneaky();
        float f = entity.getHeight() + 0.5F;
        int yOffset = "deadmau5".equals(text.getString()) ? -10 : 0;

        matrices.push();
        matrices.translate(0.0D, (double)f, 0.0D);
        matrices.multiply(MinecraftClient.getInstance().getEntityRenderDispatcher().getRotation());
        matrices.scale(-0.025F, -0.025F, 0.025F);
        
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        TextRenderer textRenderer = this.getTextRenderer();
        float textWidth = (float)textRenderer.getWidth(text);
        
        // Ensure buffered geometry is flushed before rendering our custom SDF
        if (vertexConsumers instanceof VertexConsumerProvider.Immediate) {
            ((VertexConsumerProvider.Immediate) vertexConsumers).draw();
        }

        float halfW = textWidth / 2.0F;
        float paddingX = 4.0F;
        float paddingY = 2.0F;
        float height = textRenderer.fontHeight + paddingY * 2.0F;
        
        float drawX = -halfW - paddingX;
        float drawY = (float)yOffset - paddingY;

        int bgColor = 0x55000000; // Translucent dark background (glass-like)

        // Draw whiter text, ignore stealth fading (always full white) for better visibility
        int textColor = 0xFFFFFFFF; // Full white, fully opaque
        // To be visible through walls, we must use the vertexConsumers with seeThrough
        textRenderer.draw(text, -halfW, (float)yOffset, textColor, false, matrix4f, vertexConsumers, TextRenderer.TextLayerType.SEE_THROUGH, bgColor, light);

        matrices.pop();
        ci.cancel();
    }
}
