/*
 * LiquidGlassSwitch - Architecture & Primary Responsibility:
 * Custom SDF-Based Toggle Switch.
 * An iOS-style interactive switch with animated state transitions, 
 * hover effects, and vibrant color interpolation.
 */
package com.example.glassmenu.widget;

import com.example.glassmenu.render.RenderUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class LiquidGlassSwitch extends ClickableWidget {
    private boolean enabled;
    private float animationProgress;
    private float hoverProgress;
    private long lastTime;

    private java.util.function.Consumer<Boolean> onToggle;

    public LiquidGlassSwitch(int x, int y, int width, int height, boolean initialValue) {
        super(x, y, width, height, Text.empty());
        this.enabled = initialValue;
        this.animationProgress = initialValue ? 1.0f : 0.0f;
        this.lastTime = System.currentTimeMillis();
    }

    public void setOnToggle(java.util.function.Consumer<Boolean> onToggle) {
        this.onToggle = onToggle;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        float dt = (now - lastTime) / 1000f;
        lastTime = now;

        float target = enabled ? 1.0f : 0.0f;
        animationProgress = MathHelper.lerp(dt * 12.0f, animationProgress, target);

        float hoverTarget = isHovered() ? 1.0f : 0.0f;
        hoverProgress = MathHelper.lerp(dt * 8.0f, hoverProgress, hoverTarget);

        MatrixStack matrices = context.getMatrices();
        float r = height / 2.0f;

        // Background (SDF) - Translucent dark glass for "off", vibrant for "on"
        int bgColor = interpolateColor(0x2C1C1C24, 0xFF34C759, animationProgress);
        
        // Glass border outline (drawn first to act as a proper border highlight under the fill)
        RenderUtils.drawSdfRoundedOutline(matrices, getX(), getY(), width, height, r, 0.5f, 0x2AFFFFFF);
        
        // Background Fill
        RenderUtils.drawSdfRoundedRect(matrices, getX(), getY(), width, height, r, bgColor, hoverProgress);

        // Thumb - Back to Circular as requested
        float thumbSize = height * 0.75f; // Properly sized circle
        
        float thumbX = getX() + r + (width - r * 2) * animationProgress - thumbSize / 2f;
        float thumbY = getY() + (height - thumbSize) / 2f;
        
        // Thumb Fill (Circular SDF)
        RenderUtils.drawSdfRoundedRect(matrices, thumbX, thumbY, thumbSize, thumbSize, thumbSize / 2f, 0xFFFFFFFF, hoverProgress);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        this.enabled = !this.enabled;
        if (onToggle != null) onToggle.accept(this.enabled);
    }

    private int interpolateColor(int c1, int c2, float p) {
        int a = (int) MathHelper.lerp(p, (c1 >> 24) & 0xFF, (c2 >> 24) & 0xFF);
        int r = (int) MathHelper.lerp(p, (c1 >> 16) & 0xFF, (c2 >> 16) & 0xFF);
        int g = (int) MathHelper.lerp(p, (c1 >> 8) & 0xFF, (c2 >> 8) & 0xFF);
        int b = (int) MathHelper.lerp(p, c1 & 0xFF, c2 & 0xFF);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {}
}
