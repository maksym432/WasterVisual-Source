/*
 * LiquidGlassButton - Architecture & Primary Responsibility:
 * A custom high-fidelity button widget featuring modern micro-animations (hover swelling),
 * anti-aliased SDF-based rounded rect rendering (black fill, sharp white border),
 * and standard action handler callbacks. Used extensively across the settings screens.
 */
package com.example.glassmenu.widget;

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

        hoverProgress = MathHelper.lerp(dt * 10f, hoverProgress, isHovered() ? 1.0f : 0.0f);
        float swell = 1.0f + hoverProgress * 0.05f;

        context.getMatrices().push();
        context.getMatrices().translate(getX() + width / 2f, getY() + height / 2f, 0);
        context.getMatrices().scale(swell, swell, 1.0f);
        context.getMatrices().translate(-(getX() + width / 2f), -(getY() + height / 2f), 0);

        // Draw design: black fill, white border
        RenderUtils.drawSdfRoundedRect(context.getMatrices(), getX() - 0.5f, getY() - 0.5f, width + 1f, height + 1f, 6.5f, 0xFFFFFFFF, 0);
        RenderUtils.drawSdfRoundedRect(context.getMatrices(), getX(), getY(), width, height, 6f, 0xFF000000, 0);

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        context.drawCenteredTextWithShadow(tr, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, 0xFFFFFFFF);

        context.getMatrices().pop();
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        onPress.accept(this);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {}
}
