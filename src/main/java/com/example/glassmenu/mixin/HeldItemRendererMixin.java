/*
 * HeldItemRendererMixin - Architecture & Primary Responsibility:
 * This mixin intercepts and replaces first-person item rendering in Minecraft's HeldItemRenderer.
 * It bypasses default swing and equip animations when custom hand settings are enabled,
 * applying static transformations (position, rotation, scale), RGB rainbow overlays,
 * and custom hand space particle trails.
 */
package com.example.glassmenu.mixin;

import com.example.glassmenu.GlassMenuClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to HeldItemRenderer to implement fully static hand transformations.
 * This skips all vanilla swing and equip animations when custom swing is enabled.
 */
@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {

    @Shadow
    protected abstract void renderItem(net.minecraft.entity.LivingEntity entity, ItemStack item, net.minecraft.client.render.model.json.ModelTransformationMode modelTransformationMode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light);

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void removeVanillaAnimations(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (!GlassMenuClient.CONFIG.enableCustomSwing()) return;

        matrices.push();
        
        // Apply Hand Side Offset
        boolean isRightHand = hand == Hand.MAIN_HAND ? player.getMainArm() == net.minecraft.util.Arm.RIGHT : player.getMainArm() == net.minecraft.util.Arm.LEFT;
        float sideSign = isRightHand ? 1.0f : -1.0f;

        // 1. Apply ONLY Custom Static Transformations
        // Position
        matrices.translate(GlassMenuClient.CONFIG.handPosX() * sideSign, GlassMenuClient.CONFIG.handPosY(), GlassMenuClient.CONFIG.handPosZ());
        
        // Scale
        matrices.scale(GlassMenuClient.CONFIG.handScaleX(), GlassMenuClient.CONFIG.handScaleY(), GlassMenuClient.CONFIG.handScaleZ());
        
        // Base Rotation
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(GlassMenuClient.CONFIG.handRotX()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(GlassMenuClient.CONFIG.handRotY() * sideSign));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(GlassMenuClient.CONFIG.handRotZ() * sideSign));

        // 2. Add Micro-Nod Swing (Rotation-X ONLY)
        if (swingProgress > 0) {
            float f1 = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float)Math.PI);
            float speed = GlassMenuClient.CONFIG.swingSpeed();
            float swingRotation = f1 * GlassMenuClient.CONFIG.swingRotX() * speed * 0.08f;
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(swingRotation));
        }

        // 3. Hand Item Effects (Tinting and Particles)
        com.example.glassmenu.GlassMenuConfigModel.ItemEffect effect = GlassMenuClient.CONFIG.itemEffect();
        VertexConsumerProvider finalProvider = vertexConsumers;

        if (GlassMenuClient.CONFIG.enableItemEffects()) {
            if (effect == com.example.glassmenu.GlassMenuConfigModel.ItemEffect.RAINBOW) {
                float h = (System.currentTimeMillis() % 4000 / 4000f);
                int rgb = java.awt.Color.HSBtoRGB(h, 0.8f, 1.0f);
                float r = ((rgb >> 16) & 0xFF) / 255f;
                float g = ((rgb >> 8) & 0xFF) / 255f;
                float b = (rgb & 0xFF) / 255f;
                finalProvider = new com.example.glassmenu.render.RenderUtils.TintedVertexConsumerProvider(vertexConsumers, r, g, b, 1.0f);
            }
        }

        // Render the item
        renderItem(player, item, isRightHand ? net.minecraft.client.render.model.json.ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : net.minecraft.client.render.model.json.ModelTransformationMode.FIRST_PERSON_LEFT_HAND, 
            !isRightHand, matrices, finalProvider, light);

        // --- FLUSH AND RENDER PARTICLES ---
        if (finalProvider instanceof net.minecraft.client.render.VertexConsumerProvider.Immediate immediate) {
            immediate.draw();
        }

        if (GlassMenuClient.CONFIG.enableItemEffects()) {
            if (effect == com.example.glassmenu.GlassMenuConfigModel.ItemEffect.PARTICLES || effect == com.example.glassmenu.GlassMenuConfigModel.ItemEffect.RGB_PARTICLES) {
                com.example.glassmenu.render.HandParticleRenderer.render(matrices, effect == com.example.glassmenu.GlassMenuConfigModel.ItemEffect.RGB_PARTICLES, item, !isRightHand);
            }
        }

        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        matrices.pop();
        ci.cancel();
    }
}
