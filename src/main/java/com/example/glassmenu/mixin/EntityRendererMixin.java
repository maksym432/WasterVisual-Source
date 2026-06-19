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
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {

    @Shadow public abstract TextRenderer getTextRenderer();

    @Redirect(
        method = "renderLabelIfPresent",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/font/TextRenderer;draw(Lnet/minecraft/text/Text;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)I"
        )
    )
    private int glassmenu$redirectNametagDraw(TextRenderer instance, Text text, float x, float y, int color, boolean shadow, Matrix4f matrix, VertexConsumerProvider vertexConsumers, TextRenderer.TextLayerType layerType, int backgroundColor, int light) {
        // Force the text to be fully opaque white for max visibility
        int newTextColor = 0xFFFFFFFF;
        
        // Use a softer, custom glass-like background
        int newBgColor = 0x55000000;
        
        // Force SEE_THROUGH so it renders through walls
        TextRenderer.TextLayerType newLayerType = TextRenderer.TextLayerType.SEE_THROUGH;

        return instance.draw(text, x, y, newTextColor, shadow, matrix, vertexConsumers, newLayerType, newBgColor, light);
    }
}
