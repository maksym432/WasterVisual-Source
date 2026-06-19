package com.example.glassmenu.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import com.example.glassmenu.GlassMenuClient;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {

    @ModifyArgs(
        method = "renderLabelIfPresent",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/font/TextRenderer;draw(Lnet/minecraft/text/Text;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)I"
        )
    )
    private void glassmenu$modifyNametagDrawArgs(Args args) {
        if (!GlassMenuClient.CONFIG.enableCustomNametags()) {
            return;
        }

        // Args map:
        // 0: Text text
        // 1: float x
        // 2: float y
        // 3: int color
        // 4: boolean shadow
        // 5: Matrix4f matrix
        // 6: VertexConsumerProvider vertexConsumers
        // 7: TextRenderer.TextLayerType layerType
        // 8: int backgroundColor
        // 9: int light

        Text originalText = args.get(0);
        
        // 1. Strip formatting to force our color
        args.set(0, Text.literal(originalText.getString()));
        
        // 2. Set the custom RGB color
        args.set(3, 0xFF000000 | GlassMenuClient.CONFIG.customNametagColor());
        
        // 3. Force SEE_THROUGH so it renders through walls
        args.set(7, TextRenderer.TextLayerType.SEE_THROUGH);
        
        // 4. Custom glass effect background
        args.set(8, 0x55000000);
    }
}
