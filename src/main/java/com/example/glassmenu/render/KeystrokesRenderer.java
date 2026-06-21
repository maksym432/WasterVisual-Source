package com.example.glassmenu.render;

import com.example.glassmenu.GlassMenuClient;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

import java.util.HashMap;
import java.util.Map;

public class KeystrokesRenderer {

    private static final Map<String, Float> pressProgress = new HashMap<>();

    public static void render(DrawContext context, float tickDelta) {
        if (!GlassMenuClient.CONFIG.enableKeystrokes()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.hudHidden) return;

        int configX = GlassMenuClient.CONFIG.keystrokesX();
        int configY = GlassMenuClient.CONFIG.keystrokesY();
        int configW = GlassMenuClient.CONFIG.keystrokesWidth();
        int configH = GlassMenuClient.CONFIG.keystrokesHeight();

        int startX = configX == -1 ? 10 : configX;
        int startY = configY == -1 ? 10 : configY;

        boolean transparent = GlassMenuClient.CONFIG.transparentKeystrokes();
        int color = GlassMenuClient.CONFIG.keystrokesColor();

        // Standard size ratio based on default 76x68 size.
        // We will scale everything based on the configured width/height.
        float scaleX = configW / 76.0f;
        float scaleY = configH / 68.0f;

        boolean wPressed = client.options.forwardKey.isPressed();
        boolean aPressed = client.options.leftKey.isPressed();
        boolean sPressed = client.options.backKey.isPressed();
        boolean dPressed = client.options.rightKey.isPressed();
        boolean spacePressed = client.options.jumpKey.isPressed();

        updateProgress("W", wPressed, tickDelta);
        updateProgress("A", aPressed, tickDelta);
        updateProgress("S", sPressed, tickDelta);
        updateProgress("D", dPressed, tickDelta);
        updateProgress("SPACE", spacePressed, tickDelta);

        int gap = 2;
        int size = 24;

        // Draw W
        drawKey(context, "W", startX + (size + gap) * scaleX, startY, size * scaleX, size * scaleY, pressProgress.getOrDefault("W", 0f), transparent);
        // Draw A, S, D
        drawKey(context, "A", startX, startY + (size + gap) * scaleY, size * scaleX, size * scaleY, pressProgress.getOrDefault("A", 0f), transparent);
        drawKey(context, "S", startX + (size + gap) * scaleX, startY + (size + gap) * scaleY, size * scaleX, size * scaleY, pressProgress.getOrDefault("S", 0f), transparent);
        drawKey(context, "D", startX + (size + gap) * 2 * scaleX, startY + (size + gap) * scaleY, size * scaleX, size * scaleY, pressProgress.getOrDefault("D", 0f), transparent);
        // Draw SPACE
        drawKey(context, "---", startX, startY + (size + gap) * 2 * scaleY, (size * 3 + gap * 2) * scaleX, 16 * scaleY, pressProgress.getOrDefault("SPACE", 0f), transparent);
    }

    private static void updateProgress(String key, boolean pressed, float tickDelta) {
        float current = pressProgress.getOrDefault(key, 0f);
        float target = pressed ? 1.0f : 0.0f;
        // Simple lerp. Actually since we are called every frame, we can use a constant speed.
        // Let's approximate speed
        float speed = 0.2f; 
        current = MathHelper.lerp(speed, current, target);
        pressProgress.put(key, current);
    }

    private static void drawKey(DrawContext context, String label, float x, float y, float w, float h, float progress, boolean transparent) {
        MatrixStack matrices = context.getMatrices();

        int outlineColor = transparent ? 0x33FFFFFF : 0x66FFFFFF; // Subtle outline
        // Glow outline when pressed
        int outlineColorActive = 0xFFFFFFFF;
        
        int fillColor = transparent ? 0x22000000 : 0x88000000;
        int fillColorActive = 0xAAFFFFFF;

        int currentOutline = lerpColor(outlineColor, outlineColorActive, progress);
        int currentFill = lerpColor(fillColor, fillColorActive, progress);

        RenderSystem.enableBlend();
        if (transparent) {
            GlassRefractionEngine.drawRefractedPanel(context, (int)x, (int)y, (int)w, (int)h, 0.8f, currentFill, 4f);
            RenderUtils.drawSdfRoundedOutline(matrices, x, y, w, h, 4f, 1.0f, currentOutline);
        } else {
            RenderUtils.drawSdfRoundedRect(matrices, x, y, w, h, 4f, currentFill, 0);
            RenderUtils.drawSdfRoundedOutline(matrices, x, y, w, h, 4f, 1.0f, currentOutline);
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int textColor = lerpColor(0xFFAAAAAA, 0xFF000000, progress);

        int textW = client.textRenderer.getWidth(label);
        float textX = x + (w - textW) / 2f;
        float textY = y + (h - client.textRenderer.fontHeight) / 2f + 1f;

        matrices.push();
        matrices.translate(0, 0, 100); // bring text forward
        context.drawText(client.textRenderer, label, (int)textX, (int)textY, textColor, false); // No shadow for modern look
        matrices.pop();
    }

    private static int lerpColor(int c1, int c2, float p) {
        int a = (int) MathHelper.lerp(p, (c1 >> 24) & 0xFF, (c2 >> 24) & 0xFF);
        int r = (int) MathHelper.lerp(p, (c1 >> 16) & 0xFF, (c2 >> 16) & 0xFF);
        int g = (int) MathHelper.lerp(p, (c1 >> 8) & 0xFF, (c2 >> 8) & 0xFF);
        int b = (int) MathHelper.lerp(p, c1 & 0xFF, c2 & 0xFF);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
