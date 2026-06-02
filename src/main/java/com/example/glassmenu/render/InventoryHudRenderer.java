/*
 * InventoryHudRenderer - Architecture & Primary Responsibility:
 * In-game overlay renderer for the Inventory HUD.
 * Renders the player's 3x9 inventory grid (slots 9 to 35) on the HUD
 * inside a customizable glassmorphic container.
 *
 * Rendering order (back to front):
 *   1. Panel background + Rounded slot squares (scaled matrix)
 *   2. Item textures (scaled uniformly around slot centers, absolute coords)
 */
package com.example.glassmenu.render;

import com.example.glassmenu.GlassMenuClient;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

public class InventoryHudRenderer {
    public static void render(DrawContext context, int screenWidth, int screenHeight) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;
        if (!GlassMenuClient.CONFIG.enableInventoryHud()) return;

        int targetW = GlassMenuClient.CONFIG.inventoryHudWidth();
        int targetH = GlassMenuClient.CONFIG.inventoryHudHeight();
        float scaleX = (float) targetW / 260f;
        float scaleY = (float) targetH / 92f;

        int x = GlassMenuClient.CONFIG.inventoryHudX() == -1 ? 10 : GlassMenuClient.CONFIG.inventoryHudX();
        int y = GlassMenuClient.CONFIG.inventoryHudY() == -1 ? screenHeight - targetH - 10 : GlassMenuClient.CONFIG.inventoryHudY();

        final int SLOT_SIZE = 24;
        final int PADDING   = 4;

        // ── Phase 1: Panel background & Slot Squares ──────────────────────────
        // Drawn within the scaled matrix so all UI elements align and scale together.
        boolean transparent = GlassMenuClient.CONFIG.transparentBackground();
        if (transparent) {
            GlassRefractionEngine.drawRefractedPanel(context, x, y, targetW, targetH,
                    0.8f, 0x22FFFFFF, 8f * Math.min(scaleX, scaleY));
        }

        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(scaleX, scaleY, 1.0f);

        if (!transparent) {
            int panelColor = GlassMenuClient.CONFIG.inventoryHudColor();
            int alpha = (panelColor >> 24) & 0xFF;
            if (alpha > 0) {
                int borderColor = (alpha * 0x2A / 0xEE) << 24 | 0x00FFFFFF;
                RenderUtils.drawSdfRoundedOutline(context.getMatrices(), 0, 0, 260, 92, 8f, 0.8f, borderColor);
                RenderUtils.drawSdfRoundedRect(context.getMatrices(), 0, 0, 260, 92, 8f, panelColor, 0);
            }
        } else {
            RenderUtils.drawSdfRoundedOutline(context.getMatrices(), 0, 0, 260, 92, 8f, 0.8f, 0x33FFFFFF);
        }
        context.draw(); // Flush background

        // Draw individual slot squares (empty and filled alike)
        int slotOutlineColor = GlassMenuClient.CONFIG.transparentBackground() ? 0x22FFFFFF : 0x1AFFFFFF;
        int slotFillColor = GlassMenuClient.CONFIG.transparentBackground() ? 0x0F000000 : 0x12FFFFFF;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int localX = 6 + col * (SLOT_SIZE + PADDING);
                int localY = 6 + row * (SLOT_SIZE + PADDING);

                // Slot square dimensions (22x22 centered in the 24x24 cell)
                float slotSquareX = localX + 1f;
                float slotSquareY = localY + 1f;
                float slotSquareSize = 22f;

                RenderUtils.drawSdfRoundedOutline(context.getMatrices(), slotSquareX, slotSquareY, slotSquareSize, slotSquareSize, 5f, 0.6f, slotOutlineColor);
                RenderUtils.drawSdfRoundedRect(context.getMatrices(), slotSquareX, slotSquareY, slotSquareSize, slotSquareSize, 5f, slotFillColor, 0);
            }
        }
        context.draw(); // Flush slot squares
        context.getMatrices().pop(); // Close scale matrix

        // ── Phase 2: Item textures ────────────────────────────────────────────
        // Rendered at absolute screen coordinates with uniform scaling.
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
        DiffuseLighting.enableGuiDepthLighting();

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = 9 + row * 9 + col;
                ItemStack stack = client.player.getInventory().getStack(slotIndex);

                int localX = 6 + col * (SLOT_SIZE + PADDING);
                int localY = 6 + row * (SLOT_SIZE + PADDING);

                // Center of the 24x24 slot in virtual space
                float slotCenterX = localX + 12f;
                float slotCenterY = localY + 12f;

                // Center of the slot in absolute screen coordinates
                float absCenterX = x + slotCenterX * scaleX;
                float absCenterY = y + slotCenterY * scaleY;

                if (!stack.isEmpty()) {
                    context.draw(); // Flush previous state
                    context.getMatrices().push();
                    context.getMatrices().translate(absCenterX, absCenterY, 150.0f);
                    
                    // Scale uniformly based on the scale settings
                    float itemScale = Math.min(scaleX, scaleY) * 1.05f;
                    context.getMatrices().scale(itemScale, itemScale, 1.0f);
                    
                    // Center the 16x16 item model relative to the center
                    context.getMatrices().translate(-8f, -8f, 0f);

                    context.drawItem(stack, 0, 0);
                    context.drawItemInSlot(client.textRenderer, stack, 0, 0);

                    context.getMatrices().pop();
                }
            }
        }

        context.draw();
        DiffuseLighting.disableGuiDepthLighting();
        RenderSystem.disableDepthTest();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }
}
