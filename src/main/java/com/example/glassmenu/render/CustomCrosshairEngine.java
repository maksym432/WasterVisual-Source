package com.example.glassmenu.render;

import com.example.glassmenu.GlassMenuClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class CustomCrosshairEngine {
    public static void render(DrawContext context, float tickDelta) {
        if (!GlassMenuClient.CONFIG.enableCustomCrosshair()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        float x = width / 2.0f;
        float y = height / 2.0f;

        int mode = GlassMenuClient.CONFIG.crosshairMode();
        int color = GlassMenuClient.CONFIG.crosshairColor();
        float size = GlassMenuClient.CONFIG.crosshairSize();
        float thickness = GlassMenuClient.CONFIG.crosshairThickness();
        float gap = GlassMenuClient.CONFIG.crosshairGap() * size;
        
        float length = 6.0f * size;
        float dotSize = 2.0f * size * thickness;

        switch (mode) {
            case 0: // Rounded Cross
                RenderUtils.drawSdfRoundedRect(context.getMatrices(), x - thickness / 2.0f, y - gap - length, thickness, length, thickness / 2.0f, color, 0f);
                RenderUtils.drawSdfRoundedRect(context.getMatrices(), x - thickness / 2.0f, y + gap, thickness, length, thickness / 2.0f, color, 0f);
                RenderUtils.drawSdfRoundedRect(context.getMatrices(), x - gap - length, y - thickness / 2.0f, length, thickness, thickness / 2.0f, color, 0f);
                RenderUtils.drawSdfRoundedRect(context.getMatrices(), x + gap, y - thickness / 2.0f, length, thickness, thickness / 2.0f, color, 0f);
                break;
            case 1: // Dot
                RenderUtils.drawSdfRoundedRect(context.getMatrices(), x - dotSize / 2.0f, y - dotSize / 2.0f, dotSize, dotSize, dotSize / 2.0f, color, 0f);
                break;
            case 2: // Circle
                float radius = gap + length / 2.0f;
                RenderUtils.drawSdfRoundedOutline(context.getMatrices(), x - radius, y - radius, radius * 2.0f, radius * 2.0f, radius, thickness, color);
                break;
            case 3: // 4-Corners
                float c = gap;
                float l = length * 0.8f;
                // Top-Left
                RenderUtils.drawSdfRoundedRect(context.getMatrices(), x - c - l, y - c - thickness, l + thickness, thickness, thickness / 2.0f, color, 0f);
                RenderUtils.drawSdfRoundedRect(context.getMatrices(), x - c - thickness, y - c - l, thickness, l, thickness / 2.0f, color, 0f);
                // Top-Right
                RenderUtils.drawSdfRoundedRect(context.getMatrices(), x + c, y - c - thickness, l + thickness, thickness, thickness / 2.0f, color, 0f);
                RenderUtils.drawSdfRoundedRect(context.getMatrices(), x + c, y - c - l, thickness, l, thickness / 2.0f, color, 0f);
                // Bottom-Left
                RenderUtils.drawSdfRoundedRect(context.getMatrices(), x - c - l, y + c, l + thickness, thickness, thickness / 2.0f, color, 0f);
                RenderUtils.drawSdfRoundedRect(context.getMatrices(), x - c - thickness, y + c, thickness, l + thickness, thickness / 2.0f, color, 0f);
                // Bottom-Right
                RenderUtils.drawSdfRoundedRect(context.getMatrices(), x + c, y + c, l + thickness, thickness, thickness / 2.0f, color, 0f);
                RenderUtils.drawSdfRoundedRect(context.getMatrices(), x + c, y + c, thickness, l + thickness, thickness / 2.0f, color, 0f);
                break;
        }
    }
}
