/*
 * LiquidGlassExperimentalScreen - Architecture & Primary Responsibility:
 * A sandbox/testing screen that implements scroll panels, tab transitions,
 * and custom slider/switch widgets in the Liquid Glass style. Used for
 * development validation of UI layouts.
 */
package com.example.glassmenu.screen;

import com.example.glassmenu.widget.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

/**
 * A sandbox screen for testing Tabs and Scrolling in the Liquid Glass style.
 */
public class LiquidGlassExperimentalScreen extends Screen {
    private LiquidGlassEffectView effectView;
    private LiquidLensView lensView;
    
    // Tab System
    private int currentTab = 0; // 0: Settings, 1: Mods
    
    // Scroll System
    private float scrollOffset = 0;
    private float targetScrollOffset = 0;
    private long lastTime;

    public LiquidGlassExperimentalScreen() {
        super(Text.literal("Liquid Experimental"));
        this.effectView = new LiquidGlassEffectView();
        this.lastTime = System.currentTimeMillis();
    }

    @Override
    protected void init() {
        this.effectView.enableBlur();
        
        float panelW = 280;
        float panelH = 300;
        float x = (this.width - panelW) / 2f;
        float y = (this.height - panelH) / 2f;
        
        this.lensView = new LiquidLensView(x, y, panelW, panelH);

        refreshWidgets();
    }

    private void refreshWidgets() {
        this.clearChildren();
        
        float panelX = (this.width - 280) / 2f;
        float panelY = (this.height - 300) / 2f;

        // Tab Buttons
        this.addDrawableChild(new LiquidGlassSwitch((int)panelX + 30, (int)panelY + 35, 40, 16, currentTab == 0) {
            @Override
            public void onClick(double mouseX, double mouseY) { currentTab = 0; refreshWidgets(); }
        });
        this.addDrawableChild(new LiquidGlassSwitch((int)panelX + 80, (int)panelY + 35, 40, 16, currentTab == 1) {
            @Override
            public void onClick(double mouseX, double mouseY) { currentTab = 1; refreshWidgets(); }
        });

        // Content based on tab
        if (currentTab == 0) {
            // Settings Tab with Scrolling
            for (int i = 0; i < 10; i++) {
                final int index = i;
                this.addDrawableChild(new LiquidGlassSwitch((int)panelX + 200, (int)panelY + 70 + (i * 30), 40, 18, i % 2 == 0) {
                    @Override
                    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
                        // Apply scroll offset manually for list items
                        setY((int)(panelY + 70 + (index * 30) + scrollOffset));
                        // Clipping check (simple)
                        if (getY() > (int)panelY + 50 && getY() < (int)panelY + 250) {
                            super.renderWidget(context, mouseX, mouseY, delta);
                        }
                    }
                });
            }
        } else {
            // Mods Tab
            this.addDrawableChild(new LiquidGlassSlider((int)panelX + 40, (int)panelY + 100, 200, 20, 0.8));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        float dt = (now - lastTime) / 1000f;
        lastTime = now;
        if (dt < 0f) dt = 0f;
        dt = Math.min(dt, 0.1f);

        // Smooth scrolling lerp
        scrollOffset = MathHelper.lerp(dt * 10.0f, scrollOffset, targetScrollOffset);

        this.effectView.render(context, this.width, this.height);
        this.lensView.render(context);
        
        // Render Title and Tab Labels
        float panelX = (this.width - 280) / 2f;
        float panelY = (this.height - 300) / 2f;
        context.drawText(this.textRenderer, "SETTINGS", (int)panelX + 32, (int)panelY + 25, 0xFFFFFFFF, false);
        context.drawText(this.textRenderer, "MODS", (int)panelX + 85, (int)panelY + 25, 0xFFFFFFFF, false);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        targetScrollOffset += verticalAmount * 20;
        // Clamp scroll (simple example)
        targetScrollOffset = MathHelper.clamp(targetScrollOffset, -150, 0);
        return true;
    }

    @Override
    public void close() {
        this.effectView.disableBlur();
        super.close();
    }
}
