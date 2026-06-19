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
        
        boolean isRainbow = GlassMenuClient.CONFIG.crosshairRainbow();
        float rainbowTime = (System.currentTimeMillis() % 10000) / 10000.0f * GlassMenuClient.CONFIG.crosshairRainbowSpeed() * -5.0f;

        RenderUtils.drawCustomCrosshair(context.getMatrices(), x, y, length, thickness, gap, mode, color, isRainbow, rainbowTime);
    }
}
