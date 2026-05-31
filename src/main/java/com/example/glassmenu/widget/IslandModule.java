/*
 * IslandModule - Architecture & Primary Responsibility:
 * Dynamic Island Widget Module Interface.
 * Defines the contract for dynamic island modules including priority, active status,
 * rendering, layout targets, ticks, and click handling.
 */
package com.example.glassmenu.widget;

import net.minecraft.client.gui.DrawContext;

public interface IslandModule {
    boolean isActive();
    int getPriority();
    float getTargetWidth();
    float getTargetHeight();
    void renderContent(DrawContext context, float x, float y, float width, float height, float alpha, float progress, double rx, double ry);
    boolean mouseClicked(double rx, double ry, int button, float width, float height);
    void tick(float dt);
}
