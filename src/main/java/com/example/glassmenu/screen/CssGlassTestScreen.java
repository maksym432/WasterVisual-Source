/*
 * CssGlassTestScreen - Architecture & Primary Responsibility:
 * CSS Glassmorphism Test Sandbox Screen.
 * Demonstrates loading and parsing standard CSS styles from config/glassmenu_test.css,
 * applying them dynamically (background color, border color/width, corner radius, blur, scale)
 * to panels and buttons with smooth mouse hover animations.
 * Supports hot-reloading from disk every 500ms to allow real-time style tweaking.
 */
package com.example.glassmenu.screen;

import com.example.glassmenu.render.CssParser;
import com.example.glassmenu.render.CssParser.StyleRule;
import com.example.glassmenu.render.RenderUtils;
import com.example.glassmenu.widget.LiquidGlassEffectView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class CssGlassTestScreen extends Screen {
    private final CssParser cssParser = new CssParser();
    private final LiquidGlassEffectView effectView = new LiquidGlassEffectView();
    
    private long lastCheckedTime = 0;
    private long lastModifiedTime = 0;
    
    public CssGlassTestScreen() {
        super(Text.literal("CSS Glassmorphism Sandbox"));
    }

    @Override
    protected void init() {
        // Create default CSS file if it does not exist
        File file = new File("config/glassmenu_test.css");
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                Files.writeString(file.toPath(), 
                    "/* WasterVisual CSS Glassmorphism Test Sheet */\n\n" +
                    ".panel {\n" +
                    "    background-color: rgba(28, 28, 36, 0.25);\n" +
                    "    border-color: rgba(255, 255, 255, 0.25);\n" +
                    "    border-width: 1.0px;\n" +
                    "    border-radius: 20px;\n" +
                    "    blur-radius: 20.0px;\n" +
                    "}\n\n" +
                    ".button {\n" +
                    "    background-color: rgba(255, 255, 255, 0.08);\n" +
                    "    border-color: rgba(255, 255, 255, 0.15);\n" +
                    "    border-width: 1.0px;\n" +
                    "    border-radius: 8px;\n" +
                    "    color: #DDDDDD;\n" +
                    "    scale: 1.0;\n" +
                    "}\n\n" +
                    ".button:hover {\n" +
                    "    background-color: rgba(255, 255, 255, 0.18);\n" +
                    "    border-color: rgba(255, 255, 255, 0.4);\n" +
                    "    color: #FFFFFF;\n" +
                    "    scale: 1.05;\n" +
                    "}\n"
                );
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Load initially
        cssParser.load(file);
        lastModifiedTime = file.exists() ? file.lastModified() : 0;

        // Apply blur if set by CSS
        StyleRule panelRule = cssParser.getRule(".panel");
        if (panelRule.blurRadius > 0) {
            effectView.enableBlur();
        }

        refreshWidgets();
    }

    private void refreshWidgets() {
        this.clearChildren();

        int panelW = 300;
        int panelH = 200;
        int x = (this.width - panelW) / 2;
        int y = (this.height - panelH) / 2;

        // Add two mock CSS-styled buttons
        this.addDrawableChild(new CssStyledButton(x + 40, y + 130, 100, 22, Text.literal("Apply"), ".button", ".button:hover", () -> {
            // Apply mock action
        }));
        this.addDrawableChild(new CssStyledButton(x + 160, y + 130, 100, 22, Text.literal("Close"), ".button", ".button:hover", this::close));
    }

    private void checkCssReload() {
        long now = System.currentTimeMillis();
        if (now - lastCheckedTime > 500) {
            lastCheckedTime = now;
            File file = new File("config/glassmenu_test.css");
            if (file.exists()) {
                long modified = file.lastModified();
                if (modified > lastModifiedTime) {
                    lastModifiedTime = modified;
                    cssParser.load(file);
                    
                    // Manage blur state based on new CSS setting
                    StyleRule panelRule = cssParser.getRule(".panel");
                    if (panelRule.blurRadius > 0) {
                        effectView.enableBlur();
                    } else {
                        effectView.disableBlur();
                    }
                    
                    refreshWidgets();
                }
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        checkCssReload();

        // 1. Draw backdrop overlay (will show the post-processed blur beneath it)
        effectView.render(context, this.width, this.height);

        // 2. Load panel style rule
        StyleRule panelRule = cssParser.getRule(".panel");
        int panelW = 300;
        int panelH = 200;
        int x = (this.width - panelW) / 2;
        int y = (this.height - panelH) / 2;

        // 3. Render CSS-styled panel
        if (panelRule.borderWidth > 0) {
            RenderUtils.drawSdfRoundedOutline(context.getMatrices(), x, y, panelW, panelH, panelRule.borderRadius, panelRule.borderWidth, panelRule.borderColor);
        }
        RenderUtils.drawSdfRoundedRect(context.getMatrices(), x, y, panelW, panelH, panelRule.borderRadius, panelRule.backgroundColor, 0);

        // 4. Draw texts inside panel
        int titleColor = panelRule.textColor;
        context.drawCenteredTextWithShadow(textRenderer, "iOS Glassmorphism CSS", x + panelW / 2, y + 25, titleColor);
        context.drawCenteredTextWithShadow(textRenderer, "Edit config/glassmenu_test.css", x + panelW / 2, y + 60, 0x88FFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, "to watch styling reload in real time!", x + panelW / 2, y + 80, 0x88FFFFFF);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        this.effectView.disableBlur();
        super.close();
    }

    @Override
    public void removed() {
        this.effectView.disableBlur();
        super.removed();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // Dynamic styled button leveraging CSS rules
    private class CssStyledButton extends ClickableWidget {
        private final String baseSelector;
        private final String hoverSelector;
        private float hoverProgress = 0f;
        private long lastTime = System.currentTimeMillis();
        private final Runnable action;

        public CssStyledButton(int x, int y, int width, int height, Text message, String baseSelector, String hoverSelector, Runnable action) {
            super(x, y, width, height, message);
            this.baseSelector = baseSelector;
            this.hoverSelector = hoverSelector;
            this.action = action;
        }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            long now = System.currentTimeMillis();
            float dt = (now - lastTime) / 1000f;
            lastTime = now;
            if (dt < 0) dt = 0;
            
            hoverProgress = MathHelper.lerp(dt * 10f, hoverProgress, isHovered() ? 1.0f : 0.0f);
            
            StyleRule base = cssParser.getRule(baseSelector);
            StyleRule hover = cssParser.getRule(hoverSelector);
            
            // Interpolate colors and properties
            float currentScale = MathHelper.lerp(hoverProgress, base.scale, hover.scale);
            int fillColor = interpolateColor(base.backgroundColor, hover.backgroundColor, hoverProgress);
            int borderColor = interpolateColor(base.borderColor, hover.borderColor, hoverProgress);
            int textColor = interpolateColor(base.textColor, hover.textColor, hoverProgress);
            float borderRadius = MathHelper.lerp(hoverProgress, base.borderRadius, hover.borderRadius);
            float borderWidth = MathHelper.lerp(hoverProgress, base.borderWidth, hover.borderWidth);

            context.getMatrices().push();
            context.getMatrices().translate(getX() + width / 2f, getY() + height / 2f, 0);
            context.getMatrices().scale(currentScale, currentScale, 1.0f);
            context.getMatrices().translate(-(getX() + width / 2f), -(getY() + height / 2f), 0);

            if (borderWidth > 0) {
                RenderUtils.drawSdfRoundedOutline(context.getMatrices(), getX(), getY(), width, height, borderRadius, borderWidth, borderColor);
            }
            RenderUtils.drawSdfRoundedRect(context.getMatrices(), getX(), getY(), width, height, borderRadius, fillColor, 0);

            var client = MinecraftClient.getInstance();
            context.drawCenteredTextWithShadow(client.textRenderer, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, textColor);

            context.getMatrices().pop();
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            if (action != null) action.run();
        }

        private int interpolateColor(int c1, int c2, float p) {
            int a = (int) MathHelper.lerp(p, (c1 >> 24) & 0xFF, (c2 >> 24) & 0xFF);
            int r = (int) MathHelper.lerp(p, (c1 >> 16) & 0xFF, (c2 >> 16) & 0xFF);
            int g = (int) MathHelper.lerp(p, (c1 >> 8) & 0xFF, (c2 >> 8) & 0xFF);
            int b = (int) MathHelper.lerp(p, c1 & 0xFF, c2 & 0xFF);
            return (a << 24) | (r << 16) | (g << 8) | b;
        }

        @Override
        protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {}
    }
}
