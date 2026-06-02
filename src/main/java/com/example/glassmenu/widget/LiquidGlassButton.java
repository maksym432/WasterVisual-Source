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
        float swell = 1.0f + hoverProgress * 0.05f;

        context.getMatrices().push();
        context.getMatrices().translate(getX() + width / 2f, getY() + height / 2f, 0);
        context.getMatrices().scale(swell, swell, 1.0f);
        context.getMatrices().translate(-(getX() + width / 2f), -(getY() + height / 2f), 0);

        // Draw design: iOS glassmorphic button vs Solid Black mode
        int fillColor, borderColor;
        if (GlassMenuClient.CONFIG.glassEffect()) {
            fillColor = interpolateColor(0x1F222226, 0x3833333D, hoverProgress);
            borderColor = interpolateColor(0x22FFFFFF, 0x3DFFFFFF, hoverProgress);
        } else {
            // Premium solid black/dark mode theme
            fillColor = interpolateColor(0xFF0C0C0F, 0xFF1E1E26, hoverProgress);
            borderColor = interpolateColor(0xFF22222B, 0xFF555566, hoverProgress);
        }
        RenderUtils.drawSdfRoundedRect(context.getMatrices(), getX() - 0.5f, getY() - 0.5f, width + 1f, height + 1f, 6.5f, borderColor, 0);
        RenderUtils.drawSdfRoundedRect(context.getMatrices(), getX(), getY(), width, height, 6f, fillColor, 0);

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        // Text is white
        context.drawCenteredTextWithShadow(tr, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, 0xFFFFFFFF);

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
