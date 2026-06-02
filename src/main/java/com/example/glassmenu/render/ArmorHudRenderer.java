/*
 * ArmorHudRenderer - Architecture & Primary Responsibility:
 * In-game overlay renderer for the Armor HUD.
 * Renders the player's equipped armor slots (Helmet, Chestplate, Leggings, Boots)
 * inside a customizable glassmorphic container, supporting vertical/horizontal layout
 * and resizing.
 */
package com.example.glassmenu.render;

import com.example.glassmenu.GlassMenuClient;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

public class ArmorHudRenderer {
    public static void render(DrawContext context, int screenWidth, int screenHeight) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;
        if (!GlassMenuClient.CONFIG.enableArmorHud()) return;

        int targetW = GlassMenuClient.CONFIG.armorHudWidth();
        int targetH = GlassMenuClient.CONFIG.armorHudHeight();
        boolean isVertical = GlassMenuClient.CONFIG.armorHudVertical();

        float baseW = isVertical ? 32f : 120f;
        float baseH = isVertical ? 120f : 32f;
        float scaleX = (float) targetW / baseW;
        float scaleY = (float) targetH / baseH;

        int x = GlassMenuClient.CONFIG.armorHudX() == -1 ? (screenWidth - targetW) / 2 : GlassMenuClient.CONFIG.armorHudX();
        int y = GlassMenuClient.CONFIG.armorHudY() == -1 ? screenHeight - targetH - 45 : GlassMenuClient.CONFIG.armorHudY();

        // ── Phase 1: Background & Slot Squares ────────────────────────────────
        boolean transparent = GlassMenuClient.CONFIG.transparentArmorHud();
        if (transparent) {
            GlassRefractionEngine.drawRefractedPanel(context, x, y, targetW, targetH,
                    0.8f, 0x22FFFFFF, 8f * Math.min(scaleX, scaleY));
        }

        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(scaleX, scaleY, 1.0f);

        if (!transparent) {
            int panelColor = GlassMenuClient.CONFIG.armorHudColor();
            int alpha = (panelColor >> 24) & 0xFF;
            if (alpha > 0) {
                int borderColor = (alpha * 0x2A / 0xEE) << 24 | 0x00FFFFFF;
                RenderUtils.drawSdfRoundedOutline(context.getMatrices(), 0, 0, baseW, baseH, 8f, 0.8f, borderColor);
                RenderUtils.drawSdfRoundedRect(context.getMatrices(), 0, 0, baseW, baseH, 8f, panelColor, 0);
            }
        } else {
            RenderUtils.drawSdfRoundedOutline(context.getMatrices(), 0, 0, baseW, baseH, 8f, 0.8f, 0x33FFFFFF);
        }
        context.draw(); // Flush background

        int slotOutlineColor = GlassMenuClient.CONFIG.transparentArmorHud() ? 0x22FFFFFF : 0x1AFFFFFF;
        int slotFillColor = GlassMenuClient.CONFIG.transparentArmorHud() ? 0x0F000000 : 0x12FFFFFF;

        for (int i = 0; i < 4; i++) {
            int localX = isVertical ? 4 : 4 + i * 28;
            int localY = isVertical ? 4 + i * 28 : 4;

            float slotSquareX = localX + 1f;
            float slotSquareY = localY + 1f;
            float slotSquareSize = 22f;

            RenderUtils.drawSdfRoundedOutline(context.getMatrices(), slotSquareX, slotSquareY, slotSquareSize, slotSquareSize, 5f, 0.6f, slotOutlineColor);
            RenderUtils.drawSdfRoundedRect(context.getMatrices(), slotSquareX, slotSquareY, slotSquareSize, slotSquareSize, 5f, slotFillColor, 0);
        }
        context.draw(); // Flush slot squares
        context.getMatrices().pop(); // Close scale matrix

        // ── Phase 2: Armor item textures ──────────────────────────────────────
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
        DiffuseLighting.enableGuiDepthLighting();

        for (int i = 0; i < 4; i++) {
            // Helmet (index 3) is at index 0 of loop, Boots (index 0) at index 3 of loop
            int armorSlot = 3 - i;
            ItemStack stack = client.player.getInventory().getArmorStack(armorSlot);

            int localX = isVertical ? 4 : 4 + i * 28;
            int localY = isVertical ? 4 + i * 28 : 4;

            // Center of the 24x24 slot cell in virtual space
            float slotCenterX = localX + 12f;
            float slotCenterY = localY + 12f;

            // Center of the slot in absolute screen coordinates
            float absCenterX = x + slotCenterX * scaleX;
            float absCenterY = y + slotCenterY * scaleY;

            if (!stack.isEmpty()) {
                context.draw(); // Flush previous state
                context.getMatrices().push();
                context.getMatrices().translate(absCenterX, absCenterY, 150.0f);
                
                float itemScale = Math.min(scaleX, scaleY) * 1.05f;
                context.getMatrices().scale(itemScale, itemScale, 1.0f);
                context.getMatrices().translate(-8f, -8f, 0f);

                context.drawItem(stack, 0, 0);
                context.drawItemInSlot(client.textRenderer, stack, 0, 0);

                context.getMatrices().pop();
            }
        }

        context.draw();
        DiffuseLighting.disableGuiDepthLighting();
        RenderSystem.disableDepthTest();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }
}
