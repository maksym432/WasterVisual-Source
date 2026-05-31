/*
 * UserHudRenderer - Architecture & Primary Responsibility:
 * Renders a premium glassmorphic custom HUD overlay showing the player's
 * Health (HP), Food (FD) and Experience (XP) status.
 *
 * Design updates:
 *  - First row contains HP (Green) and Food (Yellow) values and status segments
 *    arranged side-by-side.
 *  - Second row contains the XP level and smooth experience bar (Green)
 *    shifted below HP and Food.
 *  - Supports both transparent frosted-glass and solid background modes.
 *  - Position and size configurable via the Position tab.
 *
 * OpenGL safety:
 *  - Restores active textures, shaders and blend states unconditionally.
 */
package com.example.glassmenu.render;

import com.example.glassmenu.GlassMenuClient;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

/**
 * Draws custom HUD status bar overlay (HP/FD combined on row 1, XP on row 2).
 */
public class UserHudRenderer {

    private static final int CORNER_RADIUS = 6;
    private static final int PADDING       = 10;
    private static final int BAR_H         = 6;

    // HP (Green colors)
    private static final int COL_HP_BG   = 0x55003300;
    private static final int COL_HP_FILL = 0xFF33CC33;
    private static final int COL_HP_HIGH = 0xFF88FF88;

    // FD (Yellow colors)
    private static final int COL_FD_BG   = 0x55333300;
    private static final int COL_FD_FILL = 0xFFFFBB00;
    private static final int COL_FD_HIGH = 0xFFFFFF88;

    // XP (Green colors)
    private static final int COL_XP_BG   = 0x55003300;
    private static final int COL_XP_FILL = 0xFF44CC44;
    private static final int COL_XP_HIGH = 0xFF88FF88;

    private static final int COL_LABEL_HP = 0xFF44FF44;
    private static final int COL_LABEL_FD = 0xFFFFCC00;
    private static final int COL_LABEL_XP = 0xFF55FF55;

    private static float smoothXp = 0f;

    public static void render(DrawContext context, int screenW, int screenH) {
        if (!GlassMenuClient.CONFIG.enableUserHud()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        // --- Gather values ---
        float hp    = client.player.getHealth();
        float maxHp = client.player.getMaxHealth();
        float hpPct = maxHp > 0 ? MathHelper.clamp(hp / maxHp, 0f, 1f) : 0f;

        int   food  = client.player.getHungerManager().getFoodLevel();
        float fdPct = food / 20f;

        float xpTarget = client.player.experienceProgress;
        smoothXp = MathHelper.lerp(0.15f, smoothXp, xpTarget);
        int xpLevel = client.player.experienceLevel;

        // --- Layout dimensions ---
        int W = GlassMenuClient.CONFIG.userHudWidth();
        int H = GlassMenuClient.CONFIG.userHudHeight();
        W = MathHelper.clamp(W, 150, 350);
        H = MathHelper.clamp(H, 56, 120);

        int cfgX = GlassMenuClient.CONFIG.userHudX();
        int cfgY = GlassMenuClient.CONFIG.userHudY();
        int px = cfgX == -1 ? (screenW - W) / 2 : cfgX;
        int py = cfgY == -1 ? screenH - H - 8  : cfgY;

        // --- Save state ---
        int prevTex    = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        var prevShader = RenderSystem.getShader();

        // --- Draw Background panel ---
        boolean transparent = GlassMenuClient.CONFIG.transparentUserHud();
        if (transparent) {
            GlassRefractionEngine.drawRefractedPanel(context, px, py, W, H,
                    0.8f, 0x22FFFFFF, (float)CORNER_RADIUS);
        } else {
            int bgColor = GlassMenuClient.CONFIG.userHudColor();
            RenderUtils.drawSdfRoundedRect(context.getMatrices(), (float)px, (float)py, (float)W, (float)H,
                    (float)CORNER_RADIUS, bgColor, 0f);
            RenderUtils.drawSdfRoundedOutline(context.getMatrices(), (float)px, (float)py, (float)W, (float)H,
                    (float)CORNER_RADIUS, 1.2f, 0x18FFFFFF);
        }

        // --- Position Rows ---
        int rowH = (H - PADDING * 2) / 2;
        int y1 = py + PADDING;
        int y2 = y1 + rowH;

        var tr = client.textRenderer;

        // 1. HP Left side & FD Right side texts
        String hpStr = "HP: " + (int) hp + "/" + (int) maxHp;
        context.drawText(tr, hpStr, px + PADDING, y1, COL_LABEL_HP, transparent);

        String fdStr = "FD: " + food + "/20";
        int fdTextW = tr.getWidth(fdStr);
        context.drawText(tr, fdStr, px + W - PADDING - fdTextW, y1, COL_LABEL_FD, transparent);

        // Progress bar logic: split the width in two segments
        int barW = W - PADDING * 2;
        int gap = 6;
        int halfBarW = (barW - gap) / 2;
        halfBarW = Math.max(halfBarW, 20);

        int barX1 = px + PADDING;
        int barX2 = barX1 + halfBarW + gap;

        // Draw HP and FD segments
        int barY1 = y1 + 11;
        drawBarSegment(context, barX1, barY1, halfBarW, BAR_H, hpPct, COL_HP_BG, COL_HP_FILL, COL_HP_HIGH);
        drawBarSegment(context, barX2, barY1, halfBarW, BAR_H, fdPct, COL_FD_BG, COL_FD_FILL, COL_FD_HIGH);

        // 2. XP Row below
        String xpLabel = "XP";
        context.drawText(tr, xpLabel, px + PADDING, y2, COL_LABEL_XP, transparent);

        String xpLvlStr = "Lv." + xpLevel;
        int xpLvlW = tr.getWidth(xpLvlStr);
        context.drawText(tr, xpLvlStr, px + W - PADDING - xpLvlW, y2, 0xFFDDDDDD, transparent);

        // Draw XP bar spanning full width
        int barY2 = y2 + 11;
        drawBarSegment(context, barX1, barY2, barW, BAR_H, smoothXp, COL_XP_BG, COL_XP_FILL, COL_XP_HIGH);

        // --- Restore state ---
        RenderSystem.setShaderTexture(0, prevTex);
        if (prevShader != null) {
            RenderSystem.setShader(() -> prevShader);
        }
        RenderSystem.disableBlend();
    }

    private static void drawBarSegment(DrawContext context,
                                        int x, int y, int barW, int barH,
                                        float pct,
                                        int bgColor, int fillColor, int highColor) {
        int fillW = Math.round(pct * barW);

        // Background
        RenderUtils.drawSdfRoundedRect(context.getMatrices(), (float)x, (float)y, (float)barW, (float)barH,
                barH / 2f, bgColor, 0f);

        // Fill
        if (fillW > 0) {
            float r = Math.min(barH / 2f, fillW / 2f);
            RenderUtils.drawSdfRoundedRect(context.getMatrices(), (float)x, (float)y, (float)fillW, (float)barH,
                    r, fillColor, 0f);
            // Subtle highlight
            if (fillW > 4) {
                RenderUtils.drawSdfRoundedRect(context.getMatrices(), (float)x, (float)y, (float)fillW, 1.5f,
                        0.8f, highColor & 0x44FFFFFF | (highColor & 0xFF000000) >> 3, 0f);
            }
        }
    }
}
