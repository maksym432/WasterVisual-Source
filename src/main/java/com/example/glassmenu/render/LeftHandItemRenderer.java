/*
 * LeftHandItemRenderer - Architecture & Primary Responsibility:
 * In-game overlay renderer for the Left Hand Item (offhand) HUD component.
 * Displays the item currently held in the player's offhand (left hand) inside
 * a glassmorphic/refracted slot. Renders only when an item is held during
 * normal gameplay, but always renders in preview/position editor modes.
 */
package com.example.glassmenu.render;

import com.example.glassmenu.GlassMenuClient;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

public class LeftHandItemRenderer {
    public static void render(DrawContext context, int screenWidth, int screenHeight) {
        render(context, screenWidth, screenHeight, false);
    }

    public static void render(DrawContext context, int screenWidth, int screenHeight, boolean isPreview) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;
        if (!isPreview && !GlassMenuClient.CONFIG.enableLeftHandItem()) return;

        ItemStack stack = client.player.getOffHandStack();
        if (!isPreview && stack.isEmpty()) return;

        int targetW = GlassMenuClient.CONFIG.leftHandItemWidth();
        int targetH = GlassMenuClient.CONFIG.leftHandItemHeight();

        float baseW = 32f;
        float baseH = 32f;
        float scaleX = (float) targetW / baseW;
        float scaleY = (float) targetH / baseH;

        int x = GlassMenuClient.CONFIG.leftHandItemX() == -1 ? 10 : GlassMenuClient.CONFIG.leftHandItemX();
        int y = GlassMenuClient.CONFIG.leftHandItemY() == -1 ? (screenHeight - targetH) / 2 : GlassMenuClient.CONFIG.leftHandItemY();

        // ── Phase 1: Background & Slot Square ────────────────────────────────
        boolean transparent = GlassMenuClient.CONFIG.transparentLeftHandItem();
        if (transparent) {
            // Draw glass refraction panel on absolute screen coordinates
            GlassRefractionEngine.drawRefractedPanel(context, x, y, targetW, targetH, 0.8f, 0x22FFFFFF, 8f * Math.min(scaleX, scaleY));
        }

        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(scaleX, scaleY, 1.0f);

        if (!transparent) {
            int panelColor = GlassMenuClient.CONFIG.playerCardColor();
            int alpha = (panelColor >> 24) & 0xFF;
            if (alpha > 0) {
                RenderUtils.drawSdfRoundedRect(context.getMatrices(), 0, 0, baseW, baseH, 8f, panelColor, 0);
            }
        } else {
            RenderUtils.drawSdfRoundedOutline(context.getMatrices(), 0, 0, baseW, baseH, 8f, 0.8f, 0x33FFFFFF);
        }
        context.draw(); // Flush background

        // Inner item slot
        int slotOutlineColor = transparent ? 0x22FFFFFF : 0x00000000;
        int slotFillColor = transparent ? 0x0F000000 : 0x22000000;

        float slotX = 5f;
        float slotY = 5f;
        float slotSize = 22f;

        RenderUtils.drawSdfRoundedOutline(context.getMatrices(), slotX, slotY, slotSize, slotSize, 5f, 0.6f, slotOutlineColor);
        RenderUtils.drawSdfRoundedRect(context.getMatrices(), slotX, slotY, slotSize, slotSize, 5f, slotFillColor, 0);
        context.draw(); // Flush slot square
        context.getMatrices().pop(); // Close scale matrix

        // ── Phase 2: Render Item ──────────────────────────────────────────────
        ItemStack itemToRender = stack;
        if (isPreview && itemToRender.isEmpty()) {
            // Fake shield for preview mode
            itemToRender = new ItemStack(net.minecraft.item.Items.SHIELD);
        }

        if (!itemToRender.isEmpty()) {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
            DiffuseLighting.enableGuiDepthLighting();

            // Center of the 22x22 slot cell (which is at 5,5 to 27,27 in virtual space)
            float slotCenterX = 5f + 11f; // 16
            float slotCenterY = 5f + 11f; // 16

            float absCenterX = x + slotCenterX * scaleX;
            float absCenterY = y + slotCenterY * scaleY;

            context.draw(); // Flush previous state before matrix changes
            context.getMatrices().push();
            context.getMatrices().translate(absCenterX, absCenterY, 150.0f);

            float itemScale = Math.min(scaleX, scaleY) * 1.05f;
            context.getMatrices().scale(itemScale, itemScale, 1.0f);
            context.getMatrices().translate(-8f, -8f, 0f);

            context.drawItem(itemToRender, 0, 0);
            context.drawItemInSlot(client.textRenderer, itemToRender, 0, 0);

            context.getMatrices().pop();
            context.draw();
            DiffuseLighting.disableGuiDepthLighting();
            RenderSystem.disableDepthTest();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }
    }
}
