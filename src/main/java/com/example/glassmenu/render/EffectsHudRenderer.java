/*
 * EffectsHudRenderer - Architecture & Primary Responsibility:
 * Renders potion/status effects horizontally in a custom HUD panel.
 *
 * Design:
 *  - Premium glassmorphic background panel or solid dark panel.
 *  - Width of the panel dynamically scales with the number of active effects.
 *  - Hidden completely when there are no active potion effects.
 *  - Individual effect boxes fade-in and slide-up ("всплывают") when added.
 *  - Uses Minecraft's StatusEffectSpriteManager for rendering icons.
 */
package com.example.glassmenu.render;

import com.example.glassmenu.GlassMenuClient;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.StatusEffectSpriteManager;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws the custom potion effects status overlay.
 */
public class EffectsHudRenderer {

    private static float smoothActiveCount = 0f;

    public static void render(DrawContext context, int screenW, int screenH) {
        renderEffects(context, screenW, screenH, false);
    }

    public static void renderEffects(DrawContext context, int screenW, int screenH, boolean isPreview) {
        if (!GlassMenuClient.CONFIG.enableEffectsHud()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        // --- Gather active effects ---
        List<StatusEffectInstance> effects = new ArrayList<>();
        if (isPreview) {
            // Fake list for preview mode
        } else {
            effects.addAll(client.player.getStatusEffects());
        }

        int actualCount = isPreview ? 3 : effects.size();

        // --- Smooth width transition logic ---
        float lerpFactor = 0.12f;
        if (isPreview) {
            smoothActiveCount = 3.0f;
        } else {
            smoothActiveCount = MathHelper.lerp(lerpFactor, smoothActiveCount, (float)actualCount);
        }

        // Hide completely if no effects
        if (smoothActiveCount < 0.05f) {
            return;
        }

        // --- Dimension calculations ---
        int H = GlassMenuClient.CONFIG.effectsHudHeight();
        H = MathHelper.clamp(H, 18, 50);

        int padding = 3;
        int gap = 4;
        int boxSize = H - padding * 2;

        float smoothW = padding * 2 + smoothActiveCount * (boxSize + gap) - gap;
        if (smoothW < padding * 2) smoothW = padding * 2;
        int W = Math.round(smoothW);

        // Position coordinates
        int cfgX = GlassMenuClient.CONFIG.effectsHudX();
        int cfgY = GlassMenuClient.CONFIG.effectsHudY();
        int px = cfgX == -1 ? screenW - W - 10 : cfgX;
        int py = cfgY == -1 ? 10 : cfgY;

        // Save original OpenGL states
        int prevTex = RenderUtils.getTextureBinding2D(0);
        var prevShader = RenderSystem.getShader();
        boolean wasBlend = GL11.glIsEnabled(GL11.GL_BLEND);

        // --- Draw Background Panel ---
        boolean transparent = GlassMenuClient.CONFIG.transparentEffectsHud();
        if (transparent) {
            GlassRefractionEngine.drawRefractedPanel(context, px, py, W, H,
                    0.8f, 0x22FFFFFF, 5f);
        } else {
            int bgColor = GlassMenuClient.CONFIG.effectsHudColor();
            RenderUtils.drawSdfRoundedRect(context.getMatrices(), (float)px, (float)py, (float)W, (float)H,
                    5f, bgColor, 0f);
            RenderUtils.drawSdfRoundedOutline(context.getMatrices(), (float)px, (float)py, (float)W, (float)H,
                    5f, 1.0f, 0x18FFFFFF);
        }
        context.draw(); // Flush background

        // --- Draw Squares/Boxes and Icons ---
        int slotOutlineColor = transparent ? 0x22FFFFFF : 0x1AFFFFFF;
        int slotFillColor = transparent ? 0x0F000000 : 0x12FFFFFF;

        StatusEffectSpriteManager spriteManager = client.getStatusEffectSpriteManager();

        for (int i = 0; i < actualCount; i++) {
            float v_i = isPreview ? 1.0f : MathHelper.clamp(smoothActiveCount - i, 0f, 1f);
            if (v_i <= 0f) continue;

            float slideY = (1.0f - v_i) * 8.0f;
            float bx = px + padding + i * (boxSize + gap);
            float by = py + padding + slideY;

            // Apply item alpha dynamically
            int alphaInt = Math.round(v_i * 255);
            int itemOutline = (alphaInt * (slotOutlineColor & 0xFF) / 255) | (slotOutlineColor & 0xFFFFFF00);
            int itemFill = (alphaInt * ((slotFillColor >> 24) & 0xFF) / 255) << 24 | (slotFillColor & 0xFFFFFF);

            // Draw Rounded box frame around the effect
            RenderUtils.drawSdfRoundedOutline(context.getMatrices(), bx, by, (float)boxSize, (float)boxSize, 4f, 0.6f, itemOutline);
            RenderUtils.drawSdfRoundedRect(context.getMatrices(), bx, by, (float)boxSize, (float)boxSize, 4f, itemFill, 0f);
            context.draw(); // Flush block

            // Draw the effect sprite icon
            if (!isPreview && i < effects.size()) {
                StatusEffectInstance effectInstance = effects.get(i);
                Sprite sprite = spriteManager.getSprite(effectInstance.getEffectType());
                
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                
                // Draw sprite with transparency fade
                RenderSystem.setShaderColor(1f, 1f, 1f, v_i);
                context.drawSprite(Math.round(bx + 1), Math.round(by + 1), 0, boxSize - 2, boxSize - 2, sprite);
                context.draw();
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f); // reset
            } else {
                // Simulating preview icon: just draw a small colored square representing the effect
                int previewColor = (i == 0) ? 0xFF44FF44 : ((i == 1) ? 0xFFFF4444 : 0xFF5555FF);
                int fadedColor = (alphaInt * 0x7F / 255) << 24 | (previewColor & 0xFFFFFF);
                RenderUtils.drawSdfRoundedRect(context.getMatrices(), bx + 3, by + 3, (float)boxSize - 6, (float)boxSize - 6, 2f, fadedColor, 0f);
                context.draw();
            }
        }

        // Restore original states
        RenderSystem.setShaderTexture(0, prevTex);
        if (prevShader != null) {
            RenderSystem.setShader(() -> prevShader);
        }
        if (!wasBlend) {
            RenderSystem.disableBlend();
        } else {
            RenderSystem.enableBlend();
        }
    }
}
