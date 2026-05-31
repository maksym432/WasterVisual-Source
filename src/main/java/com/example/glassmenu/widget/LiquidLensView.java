/*
 * LiquidLensView - Architecture & Primary Responsibility:
 * Central Rounded Panel Widget.
 * Renders the main container for the settings menu with SDF rounded corners
 * and decorative glass-style separator lines.
 */
package com.example.glassmenu.widget;

import com.example.glassmenu.render.RenderUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public class LiquidLensView {
    private float x, y, width, height;
    private float radius = 20.0f;

    public LiquidLensView(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setX(float x) { this.x = x; }
    public void setY(float y) { this.y = y; }
    public void setWidth(float width) { this.width = width; }
    public void setHeight(float height) { this.height = height; }

    public void render(DrawContext context) {
        MatrixStack matrices = context.getMatrices();
        
        // Glass border outline (drawn first to act as a proper border highlight under the fill)
        RenderUtils.drawSdfRoundedOutline(matrices, x, y, width, height, radius, 1.0f, 0x2AFFFFFF);
        
        // Draw the refracted panel background matching the rounded menu container
        com.example.glassmenu.render.GlassRefractionEngine.drawRefractedPanel(
            context, (int)x, (int)y, (int)width, (int)height, 0.8f, 0x90FFFFFF, radius
        );
        
        // 1. Main Glass Panel (SDF) - iOS dark glassmorphism (drawn on top)
        RenderUtils.drawSdfRoundedRect(matrices, x, y, width, height, radius, 0x2C1C1C24, 0);
        
        // 2. Decorative Separator Lines
        RenderUtils.drawLine(matrices, x + radius, y + 25, x + width - radius, y + 25, 0.8f, 0x33FFFFFF);
        RenderUtils.drawLine(matrices, x + radius, y + height - 25, x + width - radius, y + height - 25, 0.8f, 0x33FFFFFF);
    }
}
