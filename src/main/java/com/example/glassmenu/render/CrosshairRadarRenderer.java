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

    private static final java.util.Map<String, Float> smoothedRadiusMap = new java.util.HashMap<>();
    private static long lastTime = System.currentTimeMillis();

    public static void render(DrawContext context, float tickDelta) {
        long now = System.currentTimeMillis();
        float dt = (now - lastTime) / 1000f;
        lastTime = now;
        if (dt < 0f) dt = 0f;
        dt = Math.min(dt, 0.1f);

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

            targets.add(new PlayerTarget(player.getName().getString(), (int)distance, targetX, targetY, angleRad, player.getSkinTextures().texture()));
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
            String text = target.distance + "m";
            float scale = 0.65f;
            int textWidth = (int)(client.textRenderer.getWidth(text) * scale);
            int textHeight = (int)(client.textRenderer.fontHeight * scale);
            
            int headSize = 10;
            int padding = 3;
            
            int totalWidth = headSize + padding + textWidth;
            int totalHeight = Math.max(headSize, textHeight);
            
            float currentRad = 12.0f;
            float drawX = 0;
            float drawY = 0;
            boolean overlapping = true;
            
            // Push text further away if it overlaps with existing text
            while (overlapping && currentRad < 200.0f) {
                float textOffsetX = (float) Math.sin(target.angleRad) * currentRad;
                float textOffsetY = -(float) Math.cos(target.angleRad) * currentRad;
                
                drawX = target.x + textOffsetX - totalWidth / 2f;
                drawY = target.y + textOffsetY - totalHeight / 2f;
                
                overlapping = false;
                net.minecraft.client.util.math.Rect2i currentRect = new net.minecraft.client.util.math.Rect2i((int)drawX - 2, (int)drawY - 2, totalWidth + 4, totalHeight + 4);
                
                for (net.minecraft.client.util.math.Rect2i rect : drawnRects) {
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

            // Smooth the radius
            float prevSmoothRad = smoothedRadiusMap.getOrDefault(target.name, currentRad);
            float smoothRad = MathHelper.lerp(MathHelper.clamp(dt * 15f, 0f, 1f), prevSmoothRad, currentRad);
            smoothedRadiusMap.put(target.name, smoothRad);
            
            // Recalculate drawX and drawY with smoothRad
            float smoothOffsetX = (float) Math.sin(target.angleRad) * smoothRad;
            float smoothOffsetY = -(float) Math.cos(target.angleRad) * smoothRad;
            drawX = target.x + smoothOffsetX - totalWidth / 2f;
            drawY = target.y + smoothOffsetY - totalHeight / 2f;

            // Draw glass background instead of fill
            GlassRefractionEngine.drawRefractedPanel(context, (int)drawX - 3, (int)drawY - 3, totalWidth + 6, totalHeight + 6, 0.8f, 0x22FFFFFF, 4f);
            
            // Draw Player Head using SDF
            if (target.skin != null) {
                RenderSystem.enableBlend();
                RenderUtils.drawSdfRoundedTexture(context.getMatrices(), drawX, drawY, headSize, headSize, 2.0f, target.skin, 0xFFFFFFFF, 0.125f, 0.125f, 0.25f, 0.25f);
                RenderUtils.drawSdfRoundedTexture(context.getMatrices(), drawX, drawY, headSize, headSize, 2.0f, target.skin, 0xFFFFFFFF, 0.625f, 0.125f, 0.75f, 0.25f);
            }
            
            // Draw text
            context.getMatrices().push();
            context.getMatrices().scale(scale, scale, 1.0f);
            float textDrawX = drawX + headSize + padding;
            float textDrawY = drawY + (totalHeight - textHeight) / 2f + 1f; // Center vertically
            context.drawTextWithShadow(client.textRenderer, text, (int)(textDrawX / scale), (int)(textDrawY / scale), 0xFFFFFFFF);
            context.getMatrices().pop();
        }
    }

    private static class PlayerTarget {
        String name;
        int distance;
        float x, y, angleRad;
        net.minecraft.util.Identifier skin;
        PlayerTarget(String name, int distance, float x, float y, float angleRad, net.minecraft.util.Identifier skin) {
            this.name = name; this.distance = distance; this.x = x; this.y = y; this.angleRad = angleRad; this.skin = skin;
        }
    }
}
