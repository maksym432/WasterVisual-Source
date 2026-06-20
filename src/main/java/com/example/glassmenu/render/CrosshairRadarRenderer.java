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
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        java.util.List<PlayerTarget> targets = new java.util.ArrayList<>();

        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            if (player == client.player || player.isSpectator() || player.isInvisible()) continue;

            double dx = player.getX() - cameraPos.x;
            double dz = player.getZ() - cameraPos.z;
            double distance = Math.sqrt(dx * dx + dz * dz);
            
            if (distance < 1.0) continue; // Too close

            // Calculate angle to target
            float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
            float angleDiff = MathHelper.wrapDegrees(targetYaw - yaw);
            
            // Convert angle to screen space (0 is top, 90 is right, etc)
            float angleRad = (float) Math.toRadians(angleDiff);
            
            float targetX = centerX + (float) Math.sin(angleRad) * radius;
            float targetY = centerY - (float) Math.cos(angleRad) * radius;

            targets.add(new PlayerTarget(player.getName().getString(), (int)distance, targetX, targetY, angleRad));

            // Draw Arrow (Triangle pointing in angleRad)
            float arrowSize = 6.0f;
            float p1x = targetX + (float) Math.sin(angleRad) * arrowSize;
            float p1y = targetY - (float) Math.cos(angleRad) * arrowSize;
            
            float p2x = targetX + (float) Math.sin(angleRad + Math.PI * 0.75) * arrowSize;
            float p2y = targetY - (float) Math.cos(angleRad + Math.PI * 0.75) * arrowSize;
            
            float p3x = targetX + (float) Math.sin(angleRad - Math.PI * 0.75) * arrowSize;
            float p3y = targetY - (float) Math.cos(angleRad - Math.PI * 0.75) * arrowSize;

            bufferBuilder.vertex(matrix, p1x, p1y, 0).color(r, g, b, 1.0f);
            bufferBuilder.vertex(matrix, p2x, p2y, 0).color(r, g, b, 1.0f);
            bufferBuilder.vertex(matrix, p3x, p3y, 0).color(r, g, b, 1.0f);
        }

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());

        // Draw Text
        for (PlayerTarget target : targets) {
            String text = target.name + " " + target.distance + "m";
            int textWidth = client.textRenderer.getWidth(text);
            
            float textOffsetX = (float) Math.sin(target.angleRad) * 12.0f;
            float textOffsetY = -(float) Math.cos(target.angleRad) * 12.0f;
            
            float drawX = target.x + textOffsetX - textWidth / 2f;
            float drawY = target.y + textOffsetY - 4f;

            context.drawTextWithShadow(client.textRenderer, text, (int)drawX, (int)drawY, 0xFFFFFFFF);
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
