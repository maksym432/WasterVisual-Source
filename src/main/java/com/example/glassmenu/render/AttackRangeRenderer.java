package com.example.glassmenu.render;

import com.example.glassmenu.GlassMenuClient;
import com.example.glassmenu.GlassMenuConfigModel;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class AttackRangeRenderer {

    private static final int CIRCLE_SEGMENTS = 360;
    private static final float ATTACK_RANGE_RADIUS = 3.0f; // Vanilla survival hit reach

    
    private static float getRadius(float angle, float time, boolean isGear) {
        float currentRadius = ATTACK_RANGE_RADIUS;
        if (isGear) {
            // 12 bumps, rotating. Changing form via secondary sine wave.
            float wave1 = (float) Math.sin(angle * 12 - time * 2.5f);
            float wave2 = (float) Math.sin(angle * 6 + time * 1.5f);
            currentRadius += (wave1 * 0.15f) + (wave2 * 0.05f);
        }
        return currentRadius;
    }

    public static void render(net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext context) {
        if (!GlassMenuClient.CONFIG.enableAttackRange()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        MatrixStack matrices = context.matrixStack();
        Camera camera = context.camera();
        Vec3d camPos = camera.getPos();
        float tickDelta = context.tickCounter().getTickDelta(false);

        int color = GlassMenuClient.CONFIG.attackRangeColor();
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
                GlassMenuConfigModel.AttackRangeMode mode = GlassMenuClient.CONFIG.attackRangeMode();
        boolean isGear = GlassMenuClient.CONFIG.attackRangeGear();
        float time = (System.currentTimeMillis() % 100000L) / 1000f;

        VertexConsumerProvider.Immediate provider = client.getBufferBuilders().getEntityVertexConsumers();

        net.minecraft.client.gl.ShaderProgram previousShader = RenderSystem.getShader();
        boolean previousBlend = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND);
        int prevActiveTexture = com.mojang.blaze3d.platform.GlStateManager._getActiveTexture();
        com.mojang.blaze3d.platform.GlStateManager._activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
        int prevTexture0 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        Tessellator tessellator = Tessellator.getInstance();

        for (Entity entity : client.world.getEntities()) {
            if (entity == client.player) continue;
            if (entity instanceof PlayerEntity || entity instanceof HostileEntity) {
                if (entity instanceof LivingEntity && !((LivingEntity) entity).isAlive()) continue;
                if (entity.squaredDistanceTo(client.player) > 225.0) continue; // 15 blocks
                if (entity instanceof PlayerEntity && entity.isTeammate(client.player)) continue;

                double targetX = MathHelper.lerp(tickDelta, entity.prevX, entity.getX());
                double targetY = MathHelper.lerp(tickDelta, entity.prevY, entity.getY());
                double targetZ = MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ());

                double renderX = targetX - camPos.x;
                double renderY = targetY - camPos.y + 0.05; // Slightly above ground
                double renderZ = targetZ - camPos.z;

                matrices.push();
                matrices.translate(renderX, renderY, renderZ);
                Matrix4f posMatrix = matrices.peek().getPositionMatrix();

                if (mode == GlassMenuConfigModel.AttackRangeMode.FILLED) {
                    RenderSystem.setShader(GameRenderer::getPositionColorProgram);
                    BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
                    buffer.vertex(posMatrix, 0, 0, 0).color(r, g, b, 0.3f); // Uniform transparency
                    for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
                        float angle = (float) (i * Math.PI * 2.0 / CIRCLE_SEGMENTS);
                        float cx = MathHelper.cos(angle) * getRadius(angle, time, isGear);
                        float cz = MathHelper.sin(angle) * getRadius(angle, time, isGear);
                        buffer.vertex(posMatrix, cx, 0, cz).color(r, g, b, 0.3f); // Solid transparency up to the contour
                    }
                    BufferRenderer.drawWithGlobalProgram(buffer.end());

                    // Add outline for FILLED mode as requested
                    RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);
                    RenderSystem.lineWidth(2.0f);
                    BufferBuilder outlineBuf = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
                    for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
                        float angle = (float) (i * Math.PI * 2.0 / CIRCLE_SEGMENTS);
                        float cx = MathHelper.cos(angle) * getRadius(angle, time, isGear);
                        float cz = MathHelper.sin(angle) * getRadius(angle, time, isGear);
                        outlineBuf.vertex(posMatrix, cx, 0, cz).color(r, g, b, 0.35f);
                    }
                    BufferRenderer.drawWithGlobalProgram(outlineBuf.end());
                } else if (mode == GlassMenuConfigModel.AttackRangeMode.SOLID_OUTLINE) {
                    RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);
                    RenderSystem.lineWidth(5.0f); // Thicker outline as requested
                    BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
                    for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
                        float angle = (float) (i * Math.PI * 2.0 / CIRCLE_SEGMENTS);
                        float cx = MathHelper.cos(angle) * getRadius(angle, time, isGear);
                        float cz = MathHelper.sin(angle) * getRadius(angle, time, isGear);
                        buffer.vertex(posMatrix, cx, 0, cz).color(r, g, b, 0.9f);
                    }
                    BufferRenderer.drawWithGlobalProgram(buffer.end());
                } else if (mode == GlassMenuConfigModel.AttackRangeMode.GLOW_OUTLINE) {
                    RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);
                    // Pass 1: Thick glow
                    RenderSystem.lineWidth(6.0f);
                    BufferBuilder buffer1 = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
                    for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
                        float angle = (float) (i * Math.PI * 2.0 / CIRCLE_SEGMENTS);
                        float cx = MathHelper.cos(angle) * getRadius(angle, time, isGear);
                        float cz = MathHelper.sin(angle) * getRadius(angle, time, isGear);
                        buffer1.vertex(posMatrix, cx, 0, cz).color(r, g, b, 0.25f);
                    }
                    BufferRenderer.drawWithGlobalProgram(buffer1.end());
                    
                    // Pass 2: Bright core line
                    RenderSystem.lineWidth(1.5f);
                    BufferBuilder buffer2 = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
                    for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
                        float angle = (float) (i * Math.PI * 2.0 / CIRCLE_SEGMENTS);
                        float cx = MathHelper.cos(angle) * getRadius(angle, time, isGear);
                        float cz = MathHelper.sin(angle) * getRadius(angle, time, isGear);
                        buffer2.vertex(posMatrix, cx, 0, cz).color(r, g, b, 0.95f);
                    }
                    BufferRenderer.drawWithGlobalProgram(buffer2.end());
                }

                matrices.pop();
            }
        }

        RenderSystem.lineWidth(1.0f);
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        if (!previousBlend) {
            RenderSystem.disableBlend();
        }

        if (previousShader != null) {
            RenderSystem.setShader(() -> previousShader);
        }

        // Restore textures strictly as per GEMINI.md
        com.mojang.blaze3d.platform.GlStateManager._activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
        org.lwjgl.opengl.GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTexture0);
        RenderSystem.setShaderTexture(0, prevTexture0);
        com.mojang.blaze3d.platform.GlStateManager._activeTexture(prevActiveTexture);

        provider.draw();
    }
}
