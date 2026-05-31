/*
 * LiquidGlassSlider - Architecture & Primary Responsibility:
 * Custom SDF-Based Slider Widget.
 * A smooth, interactive slider component with hover animations 
 * and interpolated value transitions for configuration screens.
 */
package com.example.glassmenu.widget;

import com.example.glassmenu.render.RenderUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class LiquidGlassSlider extends ClickableWidget {
    private double targetValue;
    private double actualValue;
    private float hoverProgress;
    private float dragScale;
    private boolean dragging;
    private long lastTime;
    private boolean vertical = false;

    private java.util.function.Consumer<Double> onValueChange;

    public LiquidGlassSlider(int x, int y, int width, int height, double initialValue) {
        super(x, y, width, height, Text.empty());
        this.targetValue = initialValue;
        this.actualValue = initialValue;
        this.lastTime = System.currentTimeMillis();
    }

    public void setVertical(boolean vertical) {
        this.vertical = vertical;
    }

    public boolean isVertical() {
        return vertical;
    }

    public void setOnValueChange(java.util.function.Consumer<Double> onValueChange) {
        this.onValueChange = onValueChange;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        float dt = (now - lastTime) / 1000f;
        lastTime = now;

        actualValue = MathHelper.lerp(dt * 6.0f, actualValue, targetValue);

        float hoverTarget = isHovered() ? 1.0f : 0.0f;
        hoverProgress = MathHelper.lerp(dt * 8.0f, hoverProgress, hoverTarget);
        
        float dragTarget = dragging ? 1.0f : 0.0f;
        dragScale = MathHelper.lerp(dt * 10.0f, dragScale, dragTarget);

        MatrixStack matrices = context.getMatrices();
        
        if (vertical) {
            float r = width / 2.0f;
            // 1. Track Background
            RenderUtils.drawSdfRoundedRect(matrices, getX() + r - 2, getY(), 4, height, 2, 0x33FFFFFF, 0);
            RenderUtils.drawSdfRoundedOutline(matrices, getX() + r - 2, getY(), 4, height, 2, 0.5f, 0x1AFFFFFF);
            // 2. Progress
            float progressH = (float)(height * actualValue);
            if (progressH > 0) {
                RenderUtils.drawSdfRoundedRect(matrices, getX() + r - 2, getY() + height - progressH, 4, progressH, 2, 0xFF34C759, 0);
            }
            // 3. Knob
            float knobW = width * 0.8f;
            float knobH = knobW * 0.85f;
            float knobX = getX() + (width - knobW) / 2f;
            float knobY = getY() + height - (float)(height * actualValue) - knobH / 2f;
            float totalSwell = hoverProgress + dragScale * 0.5f;
            RenderUtils.drawSdfRoundedRect(matrices, knobX, knobY, knobW, knobH, knobH / 2f, 0xFFFFFFFF, totalSwell);
        } else {
            float r = height / 2.0f;
            // 1. Track Background
            RenderUtils.drawSdfRoundedRect(matrices, getX(), getY() + r - 2, width, 4, 2, 0x33FFFFFF, 0);
            RenderUtils.drawSdfRoundedOutline(matrices, getX(), getY() + r - 2, width, 4, 2, 0.5f, 0x1AFFFFFF);
            // 2. Progress
            float progressW = (float)(width * actualValue);
            if (progressW > 0) {
                RenderUtils.drawSdfRoundedRect(matrices, getX(), getY() + r - 2, progressW, 4, 2, 0xFF34C759, 0);
            }
            // 3. Knob
            float knobH = height * 0.8f;
            float knobW = knobH * 0.85f;
            float knobX = getX() + (float)(width * actualValue) - knobW / 2f;
            float knobY = getY() + (height - knobH) / 2f;
            float totalSwell = hoverProgress + dragScale * 0.5f;
            RenderUtils.drawSdfRoundedRect(matrices, knobX, knobY, knobW, knobH, knobW / 2f, 0xFFFFFFFF, totalSwell);
        }
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (vertical) {
            this.targetValue = MathHelper.clamp(1.0 - (mouseY - (double)getY()) / (double)height, 0.0, 1.0);
        } else {
            this.targetValue = MathHelper.clamp((mouseX - (double)getX()) / (double)width, 0.0, 1.0);
        }
        this.dragging = true;
        if (onValueChange != null) onValueChange.accept(targetValue);
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        this.dragging = false;
        if (onValueChange != null) onValueChange.accept(targetValue);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (vertical) {
            this.targetValue = MathHelper.clamp(1.0 - (mouseY - (double)getY()) / (double)height, 0.0, 1.0);
        } else {
            this.targetValue = MathHelper.clamp((mouseX - (double)getX()) / (double)width, 0.0, 1.0);
        }
        if (onValueChange != null) onValueChange.accept(targetValue);
    }

    public double getValue() {
        return targetValue;
    }

    public void setValue(double value) {
        this.targetValue = MathHelper.clamp(value, 0.0, 1.0);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {}
}
