/*
 * PlayerCardRenderer - Architecture & Primary Responsibility:
 * Renders a glassmorphic target player info card (Name, Head skin, Health bar)
 * when hovering the cursor over another player.
 *
 * Animations:
 *   - Pop-in: Bubble-like swell and settle overshoot animation (from 0.0 to 1.0).
 *   - Pop-out: Fades out, swells up to 1.1x then shrinks rapidly to 0.0.
 *   - Timed out: Card stays visible for 3 seconds after looking away, then fades/shrinks out.
 *
 * Positioning:
 *   - Automatically renders just below the Dynamic Island.
 */
package com.example.glassmenu.render;

import com.example.glassmenu.GlassMenuClient;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

import java.util.UUID;

public class PlayerCardRenderer {
    private static PlayerEntity targetPlayer = null;
    private static UUID lastTargetUuid = null;
    private static String targetName = "";
    private static float targetHealth = 0f;
    private static float targetMaxHealth = 20f;
    private static Identifier skinTexture = null;
    
    private static long lastTimeLooked = 0;
    private static long stateStartTime = 0;
    
    private enum CardState {
        HIDDEN,
        APPEARING,
        VISIBLE,
        DISAPPEARING
    }
    
    private static CardState currentState = CardState.HIDDEN;
    
    public static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            currentState = CardState.HIDDEN;
            targetPlayer = null;
            return;
        }

        // Detect if hovering over a player
        PlayerEntity lookedPlayer = null;
        if (client.targetedEntity instanceof PlayerEntity) {
            lookedPlayer = (PlayerEntity) client.targetedEntity;
        }

        long now = System.currentTimeMillis();

        if (lookedPlayer != null) {
            // Update targeted player details
            targetPlayer = lookedPlayer;
            lastTargetUuid = lookedPlayer.getUuid();
            targetName = lookedPlayer.getGameProfile().getName();
            targetHealth = lookedPlayer.getHealth();
            targetMaxHealth = lookedPlayer.getMaxHealth();
            
            if (lookedPlayer instanceof AbstractClientPlayerEntity clientPlayer) {
                skinTexture = clientPlayer.getSkinTextures().texture();
            } else {
                skinTexture = null;
            }
            
            lastTimeLooked = now;
            
            if (currentState == CardState.HIDDEN || currentState == CardState.DISAPPEARING) {
                currentState = CardState.APPEARING;
                stateStartTime = now;
            }
        } else {
            // Count down visibility timer if looking away
            if (currentState == CardState.APPEARING || currentState == CardState.VISIBLE) {
                if (now - lastTimeLooked > 3000) { // 3 seconds timeout
                    currentState = CardState.DISAPPEARING;
                    stateStartTime = now;
                }
            }
        }

        // Handle animation state boundaries
        if (currentState == CardState.APPEARING) {
            if (now - stateStartTime >= 300) {
                currentState = CardState.VISIBLE;
            }
        } else if (currentState == CardState.DISAPPEARING) {
            if (now - stateStartTime >= 300) {
                currentState = CardState.HIDDEN;
                targetPlayer = null; // Clear to prevent reference leaks
            }
        }
    }

    public static void render(DrawContext context, int screenWidth, int screenHeight) {
        if (!GlassMenuClient.CONFIG.enablePlayerCard()) return;
        if (currentState == CardState.HIDDEN) return;

        MinecraftClient client = MinecraftClient.getInstance();
        long now = System.currentTimeMillis();
        long elapsed = now - stateStartTime;

        float scale = 1.0f;
        float alpha = 1.0f;

        if (currentState == CardState.APPEARING) {
            float t = MathHelper.clamp(elapsed / 300f, 0f, 1f);
            // Premium bubble pop-in (overshoots and settles)
            float f = 1.0f - t;
            scale = 1.0f - f * f * f + 0.12f * MathHelper.sin(t * (float)Math.PI);
            alpha = t;
        } else if (currentState == CardState.DISAPPEARING) {
            float t = MathHelper.clamp(elapsed / 300f, 0f, 1f);
            // Drop-like swell and collapse
            if (t < 0.25f) {
                float swellProgress = t / 0.25f;
                scale = 1.0f + 0.08f * MathHelper.sin(swellProgress * (float)Math.PI * 0.5f);
            } else {
                float shrinkProgress = (t - 0.25f) / 0.75f;
                scale = 1.08f * (1.0f - shrinkProgress) * (1.0f - shrinkProgress);
            }
            alpha = 1.0f - t;
        }

        alpha = MathHelper.clamp(alpha, 0f, 1f);
        if (scale <= 0.0f) return;

        // Actual layout dimensions from config
        int cardW = GlassMenuClient.CONFIG.playerCardWidth();
        int cardH = GlassMenuClient.CONFIG.playerCardHeight();
        int cardX = GlassMenuClient.CONFIG.playerCardX() == -1 ? (screenWidth - cardW) / 2 : GlassMenuClient.CONFIG.playerCardX();
        
        int cardY;
        if (GlassMenuClient.CONFIG.playerCardY() == -1) {
            // Stack card below the Dynamic Island
            int islandY = GlassMenuClient.CONFIG.islandY();
            int islandH = GlassMenuClient.CONFIG.capsuleHeight();
            cardY = islandY + islandH + 8;
        } else {
            cardY = GlassMenuClient.CONFIG.playerCardY();
        }

        float centerX = cardX + cardW / 2f;
        float centerY = cardY + cardH / 2f;

        float transformedX = cardX;
        float transformedY = cardY;
        float transformedW = cardW;
        float transformedH = cardH;
        if (scale != 1.0f) {
            transformedW = cardW * scale;
            transformedH = cardH * scale;
            transformedX = centerX - transformedW / 2f;
            transformedY = centerY - transformedH / 2f;
        }

        if (GlassMenuClient.CONFIG.transparentPlayerCard()) {
            GlassRefractionEngine.drawRefractedPanel(context, (int)transformedX, (int)transformedY, (int)transformedW, (int)transformedH,
                    0.8f, (Math.round(0x22 * alpha)) << 24 | 0x00FFFFFF, 8f * scale);
        }

        // Calculate scaling relative to standard virtual dimensions (140x54)
        float scaleX = (float) cardW / 140f;
        float scaleY = (float) cardH / 54f;

        // Virtual center and actual rendering uses virtual coordinates (140x54)
        float virtualCenterX = cardX + 70f;
        float virtualCenterY = cardY + 27f;

        context.getMatrices().push();
        // Pivot scale transformation around the center of the card
        context.getMatrices().translate(centerX, centerY, 300.0f);
        context.getMatrices().scale(scale * scaleX, scale * scaleY, 1.0f);
        context.getMatrices().translate(-virtualCenterX, -virtualCenterY, 0f);

        net.minecraft.client.gl.ShaderProgram originalShader = RenderSystem.getShader();
        int originalTex = com.example.glassmenu.render.RenderUtils.getTextureBinding2D(0);
        boolean originalBlend = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND);

        // Apply shader alpha
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // 1. Render Card Background (Rounded Glass Panel)
        int panelColor = GlassMenuClient.CONFIG.playerCardColor();
        int colorAlpha = Math.round(((panelColor >> 24) & 0xFF) * alpha);
        int finalPanelColor = (colorAlpha << 24) | (panelColor & 0x00FFFFFF);
        int borderColor = (Math.round(0x2A * alpha)) << 24 | 0x00FFFFFF;

        if (!GlassMenuClient.CONFIG.transparentPlayerCard()) {
            RenderUtils.drawSdfRoundedOutline(context.getMatrices(), cardX, cardY, 140, 54, 8f, 0.8f, borderColor);
            RenderUtils.drawSdfRoundedRect(context.getMatrices(), cardX, cardY, 140, 54, 8f, finalPanelColor, 0);
        } else {
            RenderUtils.drawSdfRoundedOutline(context.getMatrices(), cardX, cardY, 140, 54, 8f, 0.8f, (Math.round(0x33 * alpha)) << 24 | 0x00FFFFFF);
        }
        context.draw(); // Flush background

        // Define slot container styling for elements inside the card
        int slotOutlineColor = (Math.round((GlassMenuClient.CONFIG.transparentPlayerCard() ? 0x22 : 0x1A) * alpha)) << 24 | 0x00FFFFFF;
        int slotFillColor = (Math.round((GlassMenuClient.CONFIG.transparentPlayerCard() ? 0x0F : 0x12) * alpha)) << 24 | (GlassMenuClient.CONFIG.transparentPlayerCard() ? 0x00000000 : 0x00FFFFFF);

        // 2. Render Player Head Slot Container & Head Texture (Sized down to 20x20 for ultra-compact layout)
        float headFrameX = cardX + 5f;
        float headFrameY = cardY + 5f;
        float headFrameSize = 20f;
        
        RenderUtils.drawSdfRoundedOutline(context.getMatrices(), headFrameX, headFrameY, headFrameSize, headFrameSize, 4f, 0.6f, slotOutlineColor);
        RenderUtils.drawSdfRoundedRect(context.getMatrices(), headFrameX, headFrameY, headFrameSize, headFrameSize, 4f, slotFillColor, 0);
        context.draw();

        if (skinTexture != null) {
            // Draw head base and hat overlay with rounded corners (18x18 size inside 20x20 slot)
            RenderUtils.drawSdfRoundedTexture(context.getMatrices(), headFrameX + 1f, headFrameY + 1f, headFrameSize - 2f, headFrameSize - 2f, 3.5f, skinTexture, 0xFFFFFFFF, 0.125f, 0.125f, 0.25f, 0.25f);
            RenderUtils.drawSdfRoundedTexture(context.getMatrices(), headFrameX + 1f, headFrameY + 1f, headFrameSize - 2f, headFrameSize - 2f, 3.5f, skinTexture, 0xFFFFFFFF, 0.625f, 0.125f, 0.75f, 0.25f);
        }

        // Calculate HP details first to properly allocate text spacing
        String hpText = String.format("%.1f HP", targetHealth);
        int hpTextWidth = client.textRenderer.getWidth(hpText);
        int hpTextX = cardX + 140 - 5 - hpTextWidth;
        int hpTextColor = (Math.round(180 * alpha) << 24) | 0xFF8888;

        // 3. Render Truncated Username (Prevents overlapping with HP text)
        int textX = cardX + 29;
        int textY = cardY + 5;
        int textColor = (Math.round(255 * alpha) << 24) | 0xFFFFFF;
        
        String displayName = targetName;
        int maxNameWidth = hpTextX - textX - 6; // Dynamic gap calculation
        if (client.textRenderer.getWidth(displayName) > maxNameWidth) {
            displayName = client.textRenderer.trimToWidth(displayName, maxNameWidth - 8) + "...";
        }
        context.drawTextWithShadow(client.textRenderer, displayName, textX, textY, textColor);

        // 4. Render Health Status Text
        context.drawTextWithShadow(client.textRenderer, hpText, hpTextX, textY, hpTextColor);

        // 5. Render Health Bar Slot Container & Health Bar (Raised and thinner: 106x6)
        int healthFrameX = cardX + 29;
        int healthFrameY = cardY + 16;
        int healthFrameW = 106;
        int healthFrameH = 6;

        RenderUtils.drawSdfRoundedOutline(context.getMatrices(), healthFrameX, healthFrameY, healthFrameW, healthFrameH, 2.5f, 0.6f, slotOutlineColor);
        RenderUtils.drawSdfRoundedRect(context.getMatrices(), healthFrameX, healthFrameY, healthFrameW, healthFrameH, 2.5f, slotFillColor, 0);
        context.draw();

        // Inner health bar dimensions centered within its container
        int barX = healthFrameX + 1;
        int barY = healthFrameY + 1;
        int barW = healthFrameW - 2;
        int barH = 4;

        float hpPercent = MathHelper.clamp(targetHealth / targetMaxHealth, 0f, 1f);
        int barFillW = Math.round(barW * hpPercent);

        int unfilledColor = (Math.round(0x40 * alpha) << 24) | 0x505050;
        int filledColor = (Math.round(0xEE * alpha) << 24) | 0xE03030;

        RenderUtils.drawSdfRoundedRect(context.getMatrices(), barX, barY, barW, barH, 1.5f, unfilledColor, 0);
        context.draw();
        
        if (barFillW > 0) {
            RenderUtils.drawSdfRoundedRect(context.getMatrices(), barX, barY, barFillW, barH, 1.5f, filledColor, 0);
            context.draw();
        }

        // 6. Render Armor & Held Items Slots (Sized down to 18x18, gap 2px)
        float slotSize = 18f;
        float gap = 2f;
        float totalW = 6f * slotSize + 5f * gap; // 6 * 18 + 5 * 2 = 118
        float startX = cardX + (140 - totalW) / 2f; // 140 - 118 = 22 -> startX = cardX + 11
        float slotsY = cardY + 31f;

        // Draw slots background/outlines
        for (int i = 0; i < 6; i++) {
            float currentSlotX = startX + i * (slotSize + gap);
            RenderUtils.drawSdfRoundedOutline(context.getMatrices(), currentSlotX, slotsY, slotSize, slotSize, 3f, 0.6f, slotOutlineColor);
            RenderUtils.drawSdfRoundedRect(context.getMatrices(), currentSlotX, slotsY, slotSize, slotSize, 3f, slotFillColor, 0);
        }
        context.draw();

        if (targetPlayer != null) {
            net.minecraft.item.ItemStack[] items = new net.minecraft.item.ItemStack[6];
            items[0] = targetPlayer.getMainHandStack();
            items[1] = targetPlayer.getOffHandStack();
            items[2] = targetPlayer.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD);
            items[3] = targetPlayer.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST);
            items[4] = targetPlayer.getEquippedStack(net.minecraft.entity.EquipmentSlot.LEGS);
            items[5] = targetPlayer.getEquippedStack(net.minecraft.entity.EquipmentSlot.FEET);

            // Enable depth lighting and transparency for drawing item stacks
            RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
            RenderSystem.enableDepthTest();
            net.minecraft.client.render.DiffuseLighting.enableGuiDepthLighting();

            for (int i = 0; i < 6; i++) {
                net.minecraft.item.ItemStack stack = items[i];
                if (!stack.isEmpty()) {
                    float currentSlotX = startX + i * (slotSize + gap);
                    context.getMatrices().push();
                    // Place 16x16 item centered inside the 18x18 slot (+1px offset)
                    context.getMatrices().translate(currentSlotX + 1f, slotsY + 1f, 100.0f);
                    
                    context.drawItem(stack, 0, 0);
                    context.drawItemInSlot(client.textRenderer, stack, 0, 0);
                    
                    context.getMatrices().pop();
                }
            }

            net.minecraft.client.render.DiffuseLighting.disableGuiDepthLighting();
            RenderSystem.disableDepthTest();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }

        context.getMatrices().pop();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        RenderSystem.setShader(() -> originalShader);
        RenderSystem.setShaderTexture(0, originalTex);
        if (!originalBlend) {
            RenderSystem.disableBlend();
        } else {
            RenderSystem.enableBlend();
        }
    }
}
