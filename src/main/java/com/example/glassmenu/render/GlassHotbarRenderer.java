/*
 * GlassHotbarRenderer - Architecture & Primary Responsibility:
 * Renders the premium customizable Glass Hotbar that replaces the vanilla hotbar at the bottom.
 * Supports horizontal or vertical layouts, custom slot backgrounds, and hotkey numbering hints.
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

public class GlassHotbarRenderer {

    public static void render(DrawContext context, int screenWidth, int screenHeight) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;
        if (!GlassMenuClient.CONFIG.glassHotbar()) return;

        boolean vertical = GlassMenuClient.CONFIG.glassHotbarVertical();
        float baseW = vertical ? 34f : 220f;
        float baseH = vertical ? 220f : 34f;

        int targetW = GlassMenuClient.CONFIG.glassHotbarWidth();
        int targetH = GlassMenuClient.CONFIG.glassHotbarHeight();

        float scaleX = (float) targetW / baseW;
        float scaleY = (float) targetH / baseH;

        int x = GlassMenuClient.CONFIG.glassHotbarX() == -1 ? (screenWidth - targetW) / 2 : GlassMenuClient.CONFIG.glassHotbarX();
        int y = GlassMenuClient.CONFIG.glassHotbarY() == -1 ? screenHeight - targetH - 10 : GlassMenuClient.CONFIG.glassHotbarY();
        x = Math.max(0, Math.min(x, screenWidth - targetW));
        y = Math.max(0, Math.min(y, screenHeight - targetH));

        float radius = 4f;

        boolean wasBlend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean wasDepth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        ShaderProgram savedShader = RenderSystem.getShader();
        int savedTexture = RenderUtils.getTextureBinding2D(0);
        int savedActiveTexture = com.mojang.blaze3d.platform.GlStateManager._getActiveTexture();

        boolean transparent = GlassMenuClient.CONFIG.glassHotbarTransparent();
        if (transparent) {
            GlassRefractionEngine.drawRefractedPanel(context, x, y, targetW, targetH,
                    0.8f, 0x22FFFFFF, radius * Math.min(scaleX, scaleY));
        }

        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(scaleX, scaleY, 1.0f);

        if (!transparent) {
            int panelColor = GlassMenuClient.CONFIG.glassHotbarColor();
            int alpha = (panelColor >> 24) & 0xFF;
            if (alpha > 0) {
                int borderColor = (alpha * 0x2A / 0xEE) << 24 | 0x00FFFFFF;
                RenderUtils.drawSdfRoundedOutline(context.getMatrices(), 0, 0, baseW, baseH, radius, 0.8f, borderColor);
                RenderUtils.drawSdfRoundedRect(context.getMatrices(), 0, 0, baseW, baseH, radius, panelColor, 0);
            }
        } else {
            RenderUtils.drawSdfRoundedOutline(context.getMatrices(), 0, 0, baseW, baseH, radius, 0.8f, 0x33FFFFFF);
        }
        context.draw();

        int selectedSlot = client.player.getInventory().selectedSlot;

        if (GlassMenuClient.CONFIG.glassHotbarSlots()) {
            int slotOutlineColor = GlassMenuClient.CONFIG.glassHotbarTransparent() ? 0x44FFFFFF : 0x22FFFFFF;
            int slotFillColor = GlassMenuClient.CONFIG.glassHotbarTransparent() ? 0x15FFFFFF : 0x33000000;

            for (int i = 0; i < 9; i++) {
                float rx = vertical ? 7f : 4f + i * 24f;
                float ry = vertical ? 4f + i * 24f : 7f;
                float slotSize = 20f;

                RenderUtils.drawSdfRoundedOutline(context.getMatrices(), rx, ry, slotSize, slotSize, 4f, 0.6f, slotOutlineColor);
                RenderUtils.drawSdfRoundedRect(context.getMatrices(), rx, ry, slotSize, slotSize, 4f, slotFillColor, 0);

                if (i == selectedSlot) {
                    RenderUtils.drawSdfRoundedOutline(context.getMatrices(), rx - 1f, ry - 1f, slotSize + 2f, slotSize + 2f, 5f, 1.0f, 0xFFFFFFFF);
                }
            }
            context.draw();
        }

        context.getMatrices().pop();
        context.draw();

        // ── Phase 2: Items ────────────────────────────────────────────────────
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableBlend();
        DiffuseLighting.enableGuiDepthLighting();

        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            float rx = vertical ? 7f : 4f + i * 24f;
            float ry = vertical ? 4f + i * 24f : 7f;

            float absCenterX = x + (rx + 10f) * scaleX;
            float absCenterY = y + (ry + 10f) * scaleY;

            context.getMatrices().push();
            context.getMatrices().translate(absCenterX, absCenterY, 150.0f);

            float itemScale = Math.min(scaleX, scaleY) * (i == selectedSlot ? 1.25f : 1.0f);
            context.getMatrices().scale(itemScale, itemScale, 1.0f);
            context.getMatrices().translate(-8f, -8f, 0f);

            context.drawItem(stack, 0, 0);
            context.drawItemInSlot(client.textRenderer, stack, 0, 0);

            context.getMatrices().pop();
        }

        context.draw();
        DiffuseLighting.disableGuiDepthLighting();

        // ── Phase 3: Hotkey Numbers (Rendered on top of items) ──────────────────
        if (GlassMenuClient.CONFIG.glassHotbarNumbers()) {
            context.getMatrices().push();
            context.getMatrices().translate(x, y, 400.0f);
            context.getMatrices().scale(scaleX, scaleY, 1.0f);

            for (int i = 0; i < 9; i++) {
                float rx = vertical ? 7f : 4f + i * 24f;
                float ry = vertical ? 4f + i * 24f : 7f;
                float nx = rx - (vertical ? 6f : 4f);
                float ny = ry - (vertical ? 4f : 6f);
                String num = String.valueOf(i + 1);
                context.drawText(client.textRenderer, num, (int)nx, (int)ny, 0xFFFFFFFF, true);
            }
            RenderSystem.disableDepthTest();
            context.draw();
            RenderSystem.enableDepthTest();
            context.getMatrices().pop();
        }

        if (!wasDepth) RenderSystem.disableDepthTest();
        if (!wasBlend) RenderSystem.disableBlend();

        if (savedShader != null) {
            RenderSystem.setShader(() -> savedShader);
        } else {
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        }
        RenderSystem.setShaderTexture(0, savedTexture);
        com.mojang.blaze3d.platform.GlStateManager._activeTexture(savedActiveTexture);
    }
}
