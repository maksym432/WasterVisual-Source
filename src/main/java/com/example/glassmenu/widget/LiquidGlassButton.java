/*
 * LiquidGlassButton - Architecture & Primary Responsibility:
 * A custom high-fidelity button widget featuring modern micro-animations (hover swelling),
 * anti-aliased SDF-based rounded rect rendering (black fill, sharp white border),
 * and standard action handler callbacks. Used extensively across the settings screens.
 */
package com.example.glassmenu.widget;

import com.example.glassmenu.GlassMenuClient;
import com.example.glassmenu.render.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.function.Consumer;

public class LiquidGlassButton extends ClickableWidget {
    private final Consumer<LiquidGlassButton> onPress;
    private float hoverProgress = 0f;
    private long lastTime;

    public LiquidGlassButton(int x, int y, int width, int height, Text message, Consumer<LiquidGlassButton> onPress) {
        super(x, y, width, height, message);
        this.onPress = onPress;
        this.lastTime = System.currentTimeMillis();
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        float dt = (now - lastTime) / 1000f;
        lastTime = now;
        if (dt < 0f) dt = 0f;
        dt = Math.min(dt, 1.0f);

        hoverProgress = MathHelper.lerp(MathHelper.clamp(dt * 10f, 0f, 1f), hoverProgress, isHovered() ? 1.0f : 0.0f);
        hoverProgress = MathHelper.clamp(hoverProgress, 0f, 1f);
        float swell = 1.0f + hoverProgress * 0.03f; // Premium subtle hover swell

        context.getMatrices().push();
        context.getMatrices().translate(getX() + width / 2f, getY() + height / 2f, 0);
        context.getMatrices().scale(swell, swell, 1.0f);
        context.getMatrices().translate(-(getX() + width / 2f), -(getY() + height / 2f), 0);

        // iOS glassmorphic button (white translucent glass) vs Solid Premium light-gray style
        int fillColor, borderColor;
        float radius = height / 2.2f;
        float outerRadius = radius + 0.5f;
        
        if (GlassMenuClient.CONFIG.glassEffect()) {
            fillColor = interpolateColor(0x15FFFFFF, 0x33FFFFFF, hoverProgress);
            borderColor = interpolateColor(0x26FFFFFF, 0x4DFFFFFF, hoverProgress);
        } else {
            // iOS premium solid off-white to pure-white theme
            fillColor = interpolateColor(0xFFF2F2F7, 0xFFFFFFFF, hoverProgress);
            borderColor = interpolateColor(0xFFD1D1D6, 0xFFC7C7CC, hoverProgress);
        }
        RenderUtils.drawSdfRoundedRect(context.getMatrices(), getX() - 0.5f, getY() - 0.5f, width + 1f, height + 1f, outerRadius, borderColor, 0);
        RenderUtils.drawSdfRoundedRect(context.getMatrices(), getX(), getY(), width, height, radius, fillColor, 0);

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        
        if (GlassMenuClient.CONFIG.glassEffect()) {
            // White text with shadow for glass transparency contrast
            context.drawCenteredTextWithShadow(tr, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, 0xFFFFFFFF);
        } else {
            // Dark text without shadow for flat iOS aesthetic
            context.drawText(tr, getMessage(), getX() + (width - tr.getWidth(getMessage())) / 2, getY() + (height - 8) / 2, 0xFF1C1C1E, false);
        }

        context.getMatrices().pop();
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        onPress.accept(this);
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
