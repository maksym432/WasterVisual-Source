/*
 * UserHudRenderer - Architecture & Primary Responsibility:
 * Renders a premium custom HUD widget containing player health (HP), food (FD), and experience level (XP)
 * inside separate rounded squares (slots) aligned horizontally in a single row.
 *
 * Design:
 *  - Premium glassmorphic background panel or solid dark panel.
 *  - Three separate slot blocks inside the panel using the inventory HUD style.
 *  - Slots contain numbers: HP (Green), FD (Yellow), XP (Green).
 *  - Safe OpenGL state restore.
 */
package com.example.glassmenu.render;

import com.example.glassmenu.GlassMenuClient;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

/**
 * Draws custom HUD display (inline HP, Food, and XP level in separate slot boxes).
 */
public class UserHudRenderer {

    // Colors
    private static final int COL_HP = 0xFF44FF44;
    private static final int COL_FD = 0xFFFFCC00;
    private static final int COL_XP = 0xFF55FF55;

    public static void render(DrawContext context, int screenW, int screenH) {
        if (!GlassMenuClient.CONFIG.enableUserHud()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        // --- Gather values ---
        int hp = (int) Math.ceil(client.player.getHealth());
        int food = client.player.getHungerManager().getFoodLevel();
        int xpLevel = client.player.experienceLevel;

        // --- Layout dimensions ---
        int W = GlassMenuClient.CONFIG.userHudWidth();
        int H = GlassMenuClient.CONFIG.userHudHeight();
        
        W = MathHelper.clamp(W, 100, 350);
        H = MathHelper.clamp(H, 15, 60);

        int cfgX = GlassMenuClient.CONFIG.userHudX();
        int cfgY = GlassMenuClient.CONFIG.userHudY();
        int px = cfgX == -1 ? (screenW - W) / 2 : cfgX;
        int py = cfgY == -1 ? screenH - H - 8  : cfgY;

        // Save original states
        int prevTex    = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        var prevShader = RenderSystem.getShader();
        boolean wasBlend = GL11.glIsEnabled(GL11.GL_BLEND);

        float scaleX = (float) W / 180f;
        float scaleY = (float) H / 26f;

        // --- Draw Background panel ---
        boolean transparent = GlassMenuClient.CONFIG.transparentUserHud();
        if (transparent) {
            GlassRefractionEngine.drawRefractedPanel(context, px, py, W, H,
                    0.8f, 0x22FFFFFF, 6f * Math.min(scaleX, scaleY));
        }

        context.getMatrices().push();
        context.getMatrices().translate(px, py, 0);
        context.getMatrices().scale(scaleX, scaleY, 1.0f);

        if (!transparent) {
            RenderUtils.drawSdfRoundedOutline(context.getMatrices(), 0f, 0f, 180f, 26f, 6f, 0.8f, 0x33FFFFFF);
            RenderUtils.drawSdfRoundedRect(context.getMatrices(), 0f, 0f, 180f, 26f, 6f, 0xFF000000, 0f);
        } else {
            RenderUtils.drawSdfRoundedOutline(context.getMatrices(), 0f, 0f, 180f, 26f, 6f, 0.8f, 0x33FFFFFF);
        }
        context.draw(); // Flush background

        // --- Draw separate slots (squares) around metrics ---
        int slotOutlineColor = transparent ? 0x22FFFFFF : 0x1AFFFFFF;
        int slotFillColor = transparent ? 0x0F000000 : 0x12FFFFFF;

        // 3 slots: HP, FD, XP (56x20 at x=3, 62, 121)
        for (int i = 0; i < 3; i++) {
            float bx = 3 + i * 59;
            float by = 3;
            RenderUtils.drawSdfRoundedOutline(context.getMatrices(), bx, by, 56f, 20f, 4f, 0.6f, slotOutlineColor);
            RenderUtils.drawSdfRoundedRect(context.getMatrices(), bx, by, 56f, 20f, 4f, slotFillColor, 0f);
        }
        context.draw(); // Flush slots

        // --- Render Numbers inside slots ---
        var tr = client.textRenderer;
        int textY = 9;

        // Slot 1: HP
        String hpText = String.valueOf(hp);
        int hpW = tr.getWidth(hpText);
        context.drawText(tr, hpText, 31 - hpW / 2, textY, COL_HP, transparent);

        // Slot 2: FD
        String fdText = String.valueOf(food);
        int fdW = tr.getWidth(fdText);
        context.drawText(tr, fdText, 90 - fdW / 2, textY, COL_FD, transparent);

        // Slot 3: XP
        String xpText = String.valueOf(xpLevel);
        int xpW = tr.getWidth(xpText);
        context.drawText(tr, xpText, 149 - xpW / 2, textY, COL_XP, transparent);

        context.getMatrices().pop();
        context.draw();

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
