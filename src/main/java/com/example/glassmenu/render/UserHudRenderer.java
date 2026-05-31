/*
 * UserHudRenderer - Architecture & Primary Responsibility:
 * Renders a premium glassmorphic custom HUD overlay showing the player's
 * Health (HP), Food (FD), and Experience (XP) bars in place of the
 * vanilla status bars (cancelled via InGameHudMixin).
 *
 * Design:
 *  - Three horizontal bar rows inside a rounded-rect panel.
 *  - Transparent mode: uses GlassRefractionEngine for frosted-glass background.
 *  - Solid mode: dark panel (matching other HUD widgets).
 *  - Each row: colored icon label, gradient progress bar, numeric value.
 *  - Position and size configurable via the Position tab.
 *
 * OpenGL safety:
 *  - Captures GL_TEXTURE_BINDING_2D and current shader before any draw.
 *  - Restores them unconditionally after. Disables blend when done.
 *  - Does NOT bind custom textures to slot 0 (avoids atlas corruption).
 */
package com.example.glassmenu.render;

import com.example.glassmenu.GlassMenuClient;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

/**
 * Draws a three-row custom HUD (HP / Food / XP) using SDF rounded rectangles.
 * Called from {@link com.example.glassmenu.GlassMenuClient} via HudRenderCallback.
 */
public class UserHudRenderer {

    // ── Visual constants ──────────────────────────────────────────────────────
    private static final int CORNER_RADIUS = 6;
    private static final int PADDING       = 10;   // inner padding (px)
    private static final int ROW_H         = 18;   // height of each row block
    private static final int ROW_GAP       = 4;    // gap between rows
    private static final int BAR_H         = 8;    // height of the fill bar
    private static final int LABEL_W       = 22;   // width of icon/label column
    private static final int VALUE_W       = 36;   // width of value text on the right

    // ── Bar tint colours (ARGB) ───────────────────────────────────────────────
    private static final int COL_HP_BG   = 0x55330000;
    private static final int COL_HP_FILL = 0xFFCC3333;
    private static final int COL_HP_HIGH = 0xFFFF6666;   // top highlight tint

    private static final int COL_FD_BG   = 0x55332000;
    private static final int COL_FD_FILL = 0xFFCC7722;
    private static final int COL_FD_HIGH = 0xFFFFAA44;

    private static final int COL_XP_BG   = 0x55003300;
    private static final int COL_XP_FILL = 0xFF44CC44;
    private static final int COL_XP_HIGH = 0xFF88FF88;

    private static final int COL_LABEL_HP = 0xFFFF5555;
    private static final int COL_LABEL_FD = 0xFFFFAA33;
    private static final int COL_LABEL_XP = 0xFF55FF55;
    private static final int COL_VALUE    = 0xFFDDDDDD;

    // ── State ─────────────────────────────────────────────────────────────────
    /** Smooth XP animation (avoids jarring snap on level-up). */
    private static float smoothXp = 0f;

    // ─────────────────────────────────────────────────────────────────────────
    //  Entry point
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Called every HUD frame. Renders nothing if the feature is disabled or the
     * player is not in a world.
     */
    public static void render(DrawContext context, int screenW, int screenH) {
        if (!GlassMenuClient.CONFIG.enableUserHud()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        // ── Gather live values ──────────────────────────────────────────────
        float hp    = client.player.getHealth();
        float maxHp = client.player.getMaxHealth();
        float hpPct = maxHp > 0 ? MathHelper.clamp(hp / maxHp, 0f, 1f) : 0f;

        int   food    = client.player.getHungerManager().getFoodLevel();
        float fdPct   = food / 20f;

        float xpTarget = client.player.experienceProgress;  // 0..1 within current level
        smoothXp = MathHelper.lerp(0.15f, smoothXp, xpTarget);
        int xpLevel = client.player.experienceLevel;

        // ── Layout ─────────────────────────────────────────────────────────
        int W = GlassMenuClient.CONFIG.userHudWidth();
        int H = GlassMenuClient.CONFIG.userHudHeight();
        W = MathHelper.clamp(W, 150, 350);
        H = MathHelper.clamp(H, 56, 120);

        int cfgX = GlassMenuClient.CONFIG.userHudX();
        int cfgY = GlassMenuClient.CONFIG.userHudY();
        int px = cfgX == -1 ? (screenW - W) / 2 : cfgX;
        int py = cfgY == -1 ? screenH - H - 8  : cfgY;

        // ── Save OpenGL state ───────────────────────────────────────────────
        int prevTex    = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        var  prevShader = RenderSystem.getShader();

        // ── Background ─────────────────────────────────────────────────────
        boolean transparent = GlassMenuClient.CONFIG.transparentUserHud();
        if (transparent) {
            GlassRefractionEngine.drawRefractedPanel(context, px, py, W, H,
                    0.8f, 0x22FFFFFF, (float)CORNER_RADIUS);
        } else {
            int bgColor = GlassMenuClient.CONFIG.userHudColor();
            RenderUtils.drawSdfRoundedRect(context.getMatrices(), (float)px, (float)py, (float)W, (float)H,
                    (float)CORNER_RADIUS, bgColor, 0f);
            // subtle white outline
            RenderUtils.drawSdfRoundedOutline(context.getMatrices(), (float)px, (float)py, (float)W, (float)H,
                    (float)CORNER_RADIUS, 1.2f, 0x18FFFFFF);
        }

        // ── Draw three rows ─────────────────────────────────────────────────
        //   Row layout inside panel:
        //     y0 = py + PADDING            → HP row
        //     y1 = y0 + ROW_H + ROW_GAP   → FD row
        //     y2 = y1 + ROW_H + ROW_GAP   → XP row
        int barW = W - PADDING * 2 - LABEL_W - VALUE_W - 6;
        barW = Math.max(barW, 40);

        int y0 = py + PADDING;
        int y1 = y0 + ROW_H + ROW_GAP;
        int y2 = y1 + ROW_H + ROW_GAP;

        int barX = px + PADDING + LABEL_W;

        boolean dark = !transparent;
        int textColor = dark ? 0xFF111111 : 0xFFFFFFFF;

        drawBar(context, barX, y0, barW, BAR_H,
                hpPct, COL_HP_BG, COL_HP_FILL, COL_HP_HIGH);
        drawBar(context, barX, y1, barW, BAR_H,
                fdPct, COL_FD_BG, COL_FD_FILL, COL_FD_HIGH);
        drawBar(context, barX, y2, barW, BAR_H,
                smoothXp, COL_XP_BG, COL_XP_FILL, COL_XP_HIGH);

        // ── Labels (icon column) ────────────────────────────────────────────
        var tr = client.textRenderer;
        context.drawText(tr, "HP",
                px + PADDING, y0 + (ROW_H - BAR_H) / 2 - 1, COL_LABEL_HP, transparent);
        context.drawText(tr, "FD",
                px + PADDING, y1 + (ROW_H - BAR_H) / 2 - 1, COL_LABEL_FD, transparent);
        context.drawText(tr, "XP",
                px + PADDING, y2 + (ROW_H - BAR_H) / 2 - 1, COL_LABEL_XP, transparent);

        // ── Value text (right side) ─────────────────────────────────────────
        int valueX = barX + barW + 4;
        String hpStr = (int) hp + "/" + (int) maxHp;
        String fdStr = food + "/20";
        String xpStr = "Lv." + xpLevel;

        context.drawText(tr, hpStr, valueX,
                y0 + (ROW_H - BAR_H) / 2 - 1, COL_VALUE, transparent);
        context.drawText(tr, fdStr, valueX,
                y1 + (ROW_H - BAR_H) / 2 - 1, COL_VALUE, transparent);
        context.drawText(tr, xpStr, valueX,
                y2 + (ROW_H - BAR_H) / 2 - 1, COL_VALUE, transparent);

        // ── Restore OpenGL state ────────────────────────────────────────────
        RenderSystem.setShaderTexture(0, prevTex);
        if (prevShader != null) {
            RenderSystem.setShader(() -> prevShader);
        }
        RenderSystem.disableBlend();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Draws one progress bar (background + filled portion) at the given coords.
     *
     * @param barW total bar width (background)
     * @param barH bar height
     * @param pct  fill fraction [0,1]
     */
    private static void drawBar(DrawContext context,
                                 int x, int rowY, int barW, int barH,
                                 float pct,
                                 int bgColor, int fillColor, int highColor) {
        int centerY = rowY + (ROW_H - barH) / 2;
        int fillW   = Math.round(pct * barW);

        // Background
        RenderUtils.drawSdfRoundedRect(context.getMatrices(), (float)x, (float)centerY, (float)barW, (float)barH,
                barH / 2f, bgColor, 0f);

        // Fill
        if (fillW > 0) {
            // clamp fill corner to fit cleanly
            float r = Math.min(barH / 2f, fillW / 2f);
            RenderUtils.drawSdfRoundedRect(context.getMatrices(), (float)x, (float)centerY, (float)fillW, (float)barH,
                    r, fillColor, 0f);
            // subtle top-edge highlight (1px tall overlay)
            if (fillW > 4) {
                RenderUtils.drawSdfRoundedRect(context.getMatrices(), (float)x, (float)centerY, (float)fillW, 2f,
                        1f, highColor & 0x44FFFFFF | (highColor & 0xFF000000) >> 3, 0f);
            }
        }
    }
}
