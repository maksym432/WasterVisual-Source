package com.example.glassmenu.render;

import com.example.glassmenu.GlassMenuClient;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;

public class CrosshairRadarRenderer {

    public static void render(DrawContext context, float tickDelta) {
        if (!GlassMenuClient.CONFIG.enableCrosshairRadar()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.options.hudHidden) return;

        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();
        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f;

        float radius = GlassMenuClient.CONFIG.crosshairRadarRadius();
        
        int baseColor = GlassMenuClient.CONFIG.crosshairRadarColor();
        if (GlassMenuClient.CONFIG.crosshairRadarRgb()) {
            float hue = (System.currentTimeMillis() % 2000L) / 2000.0f;
            baseColor = Color.HSBtoRGB(hue, 1.0f, 1.0f);
        }

        float r = ((baseColor >> 16) & 0xFF) / 255.0f;
        float g = ((baseColor >> 8) & 0xFF) / 255.0f;
        float b = (baseColor & 0xFF) / 255.0f;

        Vec3d cameraPos = client.gameRenderer.getCamera().getPos();
        float yaw = client.gameRenderer.getCamera().getYaw();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        java.util.List<PlayerTarget> targets = new java.util.ArrayList<>();
        float maxDistance = GlassMenuClient.CONFIG.crosshairRadarSearchDistance();

        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            if (player == client.player || player.isSpectator() || player.isInvisible()) continue;

            double lerpedX = MathHelper.lerp(tickDelta, player.prevX, player.getX());
            double lerpedZ = MathHelper.lerp(tickDelta, player.prevZ, player.getZ());
            
            double dx = lerpedX - cameraPos.x;
            double dz = lerpedZ - cameraPos.z;
            double distance = Math.sqrt(dx * dx + dz * dz);
            
            if (distance < 1.0 || distance > maxDistance) continue; // Too close or too far

            // Calculate angle to target
            float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
            float angleDiff = MathHelper.wrapDegrees(targetYaw - yaw);
            
            // Convert angle to screen space (0 is top, 90 is right, etc)
            float angleRad = (float) Math.toRadians(angleDiff);
            
            float targetX = centerX + (float) Math.sin(angleRad) * radius;
            float targetY = centerY - (float) Math.cos(angleRad) * radius;

            targets.add(new PlayerTarget(player.getName().getString(), (int)distance, targetX, targetY, angleRad));
        }

        if (targets.isEmpty()) {
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
            return;
        }

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        for (PlayerTarget target : targets) {
            float arrowSize = GlassMenuClient.CONFIG.crosshairRadarIconSize();
            float p1x = target.x + (float) Math.sin(target.angleRad) * arrowSize;
            float p1y = target.y - (float) Math.cos(target.angleRad) * arrowSize;
            
            float p2x = target.x + (float) Math.sin(target.angleRad + Math.PI * 0.75) * arrowSize;
            float p2y = target.y - (float) Math.cos(target.angleRad + Math.PI * 0.75) * arrowSize;
            
            float p3x = target.x + (float) Math.sin(target.angleRad - Math.PI * 0.75) * arrowSize;
            float p3y = target.y - (float) Math.cos(target.angleRad - Math.PI * 0.75) * arrowSize;

            bufferBuilder.vertex(matrix, p1x, p1y, 0).color(r, g, b, 1.0f);
            bufferBuilder.vertex(matrix, p2x, p2y, 0).color(r, g, b, 1.0f);
            bufferBuilder.vertex(matrix, p3x, p3y, 0).color(r, g, b, 1.0f);
        }

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();

        // Draw Text with background squares and anti-overlap
        java.util.List<net.minecraft.client.util.math.Rect2i> drawnRects = new java.util.ArrayList<>();
        
        targets.sort(java.util.Comparator.comparingInt(t -> t.distance));

        for (PlayerTarget target : targets) {
            String text = target.name + " " + target.distance + "m";
            float scale = 0.65f;
            int textWidth = (int)(client.textRenderer.getWidth(text) * scale);
            int textHeight = (int)(client.textRenderer.fontHeight * scale);
            
            float currentRad = 12.0f;
            float drawX = 0;
            float drawY = 0;
            boolean overlapping = true;
            
            // Push text further away if it overlaps with existing text
            while (overlapping && currentRad < 200.0f) {
                float textOffsetX = (float) Math.sin(target.angleRad) * currentRad;
                float textOffsetY = -(float) Math.cos(target.angleRad) * currentRad;
                
                drawX = target.x + textOffsetX - textWidth / 2f;
                drawY = target.y + textOffsetY - textHeight / 2f;
                
                overlapping = false;
                net.minecraft.client.util.math.Rect2i currentRect = new net.minecraft.client.util.math.Rect2i((int)drawX - 2, (int)drawY - 2, textWidth + 4, textHeight + 4);
                
                for (net.minecraft.client.util.math.Rect2i rect : drawnRects) {
                    // Check intersection
                    if (currentRect.getX() < rect.getX() + rect.getWidth() && 
                        currentRect.getX() + currentRect.getWidth() > rect.getX() &&
                        currentRect.getY() < rect.getY() + rect.getHeight() && 
                        currentRect.getY() + currentRect.getHeight() > rect.getY()) {
                        overlapping = true;
                        break;
                    }
                }
                
                if (overlapping) {
                    currentRad += 10.0f;
                } else {
                    drawnRects.add(currentRect);
                }
            }

            // Draw dark background square
            context.fill((int)drawX - 2, (int)drawY - 2, (int)drawX + textWidth + 2, (int)drawY + textHeight + 1, 0x80000000);
            
            // Draw text
            context.getMatrices().push();
            context.getMatrices().scale(scale, scale, 1.0f);
            context.drawTextWithShadow(client.textRenderer, text, (int)(drawX / scale), (int)(drawY / scale), 0xFFFFFFFF);
            context.getMatrices().pop();
        }
    }

    private static class PlayerTarget {
        String name;
        int distance;
        float x, y, angleRad;
        PlayerTarget(String name, int distance, float x, float y, float angleRad) {
            this.name = name; this.distance = distance; this.x = x; this.y = y; this.angleRad = angleRad;
        }
    }
}
