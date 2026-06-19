package com.example.glassmenu.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.example.glassmenu.GlassMenuClient;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {

    @ModifyVariable(method = "renderLabelIfPresent", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Text glassmenu$modifyNametagText(Text text, T entity) {
        if (!GlassMenuClient.CONFIG.enableCustomNametags()) {
            return text;
        }

        // Strip existing formatting and apply custom RGB color
        int customColor = GlassMenuClient.CONFIG.customNametagColor() & 0xFFFFFF;
        
        return Text.literal(text.getString()).setStyle(Style.EMPTY.withColor(customColor));
    }

    @Inject(
        method = "renderLabelIfPresent",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/font/TextRenderer;draw(Lnet/minecraft/text/Text;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)I",
            ordinal = 0
        )
    )
    private void glassmenu$drawRoundedBackground(T entity, Text text, net.minecraft.client.util.math.MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float tickDelta, CallbackInfo ci) {
        if (!GlassMenuClient.CONFIG.enableCustomNametags()) return;

        float width = MinecraftClient.getInstance().textRenderer.getWidth(text);
        float h = -width / 2.0f;
        int i = "deadmau5".equals(text.getString()) ? -10 : 0;
        
        float bgX = h - 3f;
        float bgY = i - 2f;
        float bgW = width + 6f;
        float bgH = 12f;
        float radius = 6f; // Very soft pill shape corners
        int glassColor = 0x55000000;
        
        boolean wasDepth = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
        
        com.example.glassmenu.render.RenderUtils.drawSdfRoundedRect(matrices, bgX, bgY, bgW, bgH, radius, glassColor, 0);
        
        if (wasDepth) com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
    }

    @ModifyArgs(
        method = "renderLabelIfPresent",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/font/TextRenderer;draw(Lnet/minecraft/text/Text;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)I"
        )
    )
    private void glassmenu$hideVanillaBackground(org.spongepowered.asm.mixin.injection.invoke.arg.Args args) {
        if (!GlassMenuClient.CONFIG.enableCustomNametags()) return;
        args.set(8, 0); // Hide vanilla sharp background for all text draw calls
    }
}
