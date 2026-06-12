/*
 * FastItemRenderer - Architecture & Primary Responsibility:
 * Renders a premium circular Hotbar replacement/overlay.
 * Arranges the 9 hotbar items along the circumference of a circle,
 * animating rotation so the selected item is always at the top.
 *
 * SAFETY: All OpenGL state (shader, textures, blend, depth test, diffuse
 * lighting) is fully saved before rendering and fully restored afterwards
 * to prevent texture/skin corruption in subsequent frames.
 */
package com.example.glassmenu.render;

import com.example.glassmenu.GlassMenuClient;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

public class FastItemRenderer {
    private static double currentRotation = -Math.PI / 2.0;
    private static long lastTime = System.nanoTime();

    public static void render(DrawContext context, int screenWidth, int screenHeight) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;
        if (!GlassMenuClient.CONFIG.enableFastItem()) return;

        long now = System.nanoTime();
        float dt = (now - lastTime) / 1_000_000_000f;
        lastTime = now;
        if (dt < 0) dt = 0;
        if (dt > 0.1f) dt = 0.1f;

        int selectedSlot = client.player.getInventory().selectedSlot;

        // Smoothly interpolate rotation so the selected slot is at the top (-pi/2)
        double targetRotation = -Math.PI / 2.0 - selectedSlot * (2.0 * Math.PI / 9.0);
        double diff = targetRotation - currentRotation;
        while (diff < -Math.PI) diff += 2.0 * Math.PI;
        while (diff > Math.PI) diff -= 2.0 * Math.PI;
        currentRotation += diff * Math.min(1.0f, dt * 10f);

        int targetW = GlassMenuClient.CONFIG.fastItemWidth();
        int targetH = GlassMenuClient.CONFIG.fastItemHeight();
        float baseW = 160f;
        float baseH = 160f;
        float scaleX = (float) targetW / baseW;
        float scaleY = (float) targetH / baseH;

        int x = GlassMenuClient.CONFIG.fastItemX() == -1 ? (screenWidth - targetW) / 2 : GlassMenuClient.CONFIG.fastItemX();
        int y = GlassMenuClient.CONFIG.fastItemY() == -1 ? screenHeight - targetH - 10 : GlassMenuClient.CONFIG.fastItemY();
        x = Math.max(0, Math.min(x, screenWidth - targetW));
        y = Math.max(0, Math.min(y, screenHeight - targetH));

        float radius = baseW / 2f;
        float distributionRadius = radius - 20f;

        // ── Save ALL state we might touch ──────────────────────────────────────
        boolean wasBlend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean wasDepth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        ShaderProgram savedShader = RenderSystem.getShader();
        int savedTexture = RenderUtils.getTextureBinding2D(0);

        boolean transparent = GlassMenuClient.CONFIG.transparentFastItem();
        if (transparent) {
            GlassRefractionEngine.drawRefractedPanel(context, x, y, targetW, targetH,
                    0.8f, 0x22FFFFFF, radius * Math.min(scaleX, scaleY));
        }

        // ── Phase 1: Background & Slots (SDF, no texture corruption) ──────────
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(scaleX, scaleY, 1.0f);

        if (!transparent) {
            int panelColor = GlassMenuClient.CONFIG.fastItemColor();
            int alpha = (panelColor >> 24) & 0xFF;
            if (alpha > 0) {
                int borderColor = (alpha * 0x2A / 0xEE) << 24 | 0x00FFFFFF;
                RenderUtils.drawSdfRoundedOutline(context.getMatrices(), 0, 0, baseW, baseH, radius, 0.8f, borderColor);
                RenderUtils.drawSdfRoundedRect(context.getMatrices(), 0, 0, baseW, baseH, radius, panelColor, 0);
            }
        } else {
            RenderUtils.drawSdfRoundedOutline(context.getMatrices(), 0, 0, baseW, baseH, radius, 0.8f, 0x33FFFFFF);
        }
        context.draw(); // Flush background

        if (GlassMenuClient.CONFIG.fastItemSlots()) {
            int slotOutlineColor = GlassMenuClient.CONFIG.transparentFastItem() ? 0x22FFFFFF : 0x1AFFFFFF;
            int slotFillColor = GlassMenuClient.CONFIG.transparentFastItem() ? 0x0F000000 : 0x12FFFFFF;

            for (int i = 0; i < 9; i++) {
                double angle = currentRotation + i * (2.0 * Math.PI / 9.0);
                float slotCenterX = baseW / 2f + (float) Math.cos(angle) * distributionRadius;
                float slotCenterY = baseH / 2f + (float) Math.sin(angle) * distributionRadius;

                float slotSize = i == selectedSlot ? 24f : 20f;
                float rx = slotCenterX - slotSize / 2f;
                float ry = slotCenterY - slotSize / 2f;

                RenderUtils.drawSdfRoundedOutline(context.getMatrices(), rx, ry, slotSize, slotSize, slotSize / 2f, 0.6f, slotOutlineColor);
                RenderUtils.drawSdfRoundedRect(context.getMatrices(), rx, ry, slotSize, slotSize, slotSize / 2f, slotFillColor, 0);

                if (i == selectedSlot) {
                    RenderUtils.drawSdfRoundedOutline(context.getMatrices(), rx - 1f, ry - 1f, slotSize + 2f, slotSize + 2f, (slotSize + 2f) / 2f, 1.0f, 0xFFFFFFFF);
                }
            }
            context.draw(); // Flush slots
        }
        context.getMatrices().pop(); // Close scale matrix

        // ── Phase 2: Items ────────────────────────────────────────────────────
        // Flush any pending batches before touching item rendering state
        context.draw();

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableBlend();
        DiffuseLighting.enableGuiDepthLighting();

        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            double angle = currentRotation + i * (2.0 * Math.PI / 9.0);
            float slotCenterX = baseW / 2f + (float) Math.cos(angle) * distributionRadius;
            float slotCenterY = baseH / 2f + (float) Math.sin(angle) * distributionRadius;

            float absCenterX = x + slotCenterX * scaleX;
            float absCenterY = y + slotCenterY * scaleY;

            context.getMatrices().push();
            context.getMatrices().translate(absCenterX, absCenterY, 150.0f);

            float itemScale = Math.min(scaleX, scaleY) * (i == selectedSlot ? 1.35f : 1.05f);
            context.getMatrices().scale(itemScale, itemScale, 1.0f);
            context.getMatrices().translate(-8f, -8f, 0f);

            context.drawItem(stack, 0, 0);
            context.drawItemInSlot(client.textRenderer, stack, 0, 0);

            context.getMatrices().pop();
        }

        // Flush all item batches while DiffuseLighting is still in GUI mode
        context.draw();

        // ── Restore ALL state unconditionally ─────────────────────────────────
        DiffuseLighting.disableGuiDepthLighting();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        if (!wasDepth) RenderSystem.disableDepthTest();
        if (!wasBlend) RenderSystem.disableBlend();

        // Restore shader
        if (savedShader != null) {
            RenderSystem.setShader(() -> savedShader);
        } else {
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        }

        // Restore texture binding to slot 0
        RenderSystem.setShaderTexture(0, savedTexture);
    }
}
