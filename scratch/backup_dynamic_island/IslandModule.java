package com.example.glassmenu.widget;

import net.minecraft.client.gui.DrawContext;

/**
 * Interface representing a widget module inside the Dynamic Island.
 * Inspired by Linux Dynisland-ABI.
 */
public interface IslandModule {
    /**
     * Determines whether the module is currently active.
     */
    boolean isActive();

    /**
     * Priority of this module. If multiple modules are active, the one with
     * the highest priority is rendered and receives input events.
     */
    int getPriority();

    /**
     * Target width of the capsule when this module is expanded (e.g. on hover/interaction).
     */
    float getTargetWidth();

    /**
     * Target height of the capsule when this module is expanded (e.g. on hover/interaction).
     */
    float getTargetHeight();

    /**
     * Renders the module content inside the capsule.
     * Renders when the capsule is drawn.
     *
     * @param context  Minecraft DrawContext
     * @param x        Top-left X coordinate of the capsule background
     * @param y        Top-left Y coordinate of the capsule background
     * @param width    Current animated width of the capsule
     * @param height   Current animated height of the capsule
     * @param alpha    Current opacity of the content (combining fade and widget visibility)
     * @param progress Smooth morph progress (0.0f = compact/closed, 1.0f = fully expanded)
     * @param rx       Relative mouse X coordinate inside the capsule (mouseX - x)
     * @param ry       Relative mouse Y coordinate inside the capsule (mouseY - y)
     */
    void renderContent(DrawContext context, float x, float y, float width, float height, float alpha, float progress, double rx, double ry);

    /**
     * Handles mouse click events inside the module.
     *
     * @param rx     Relative mouse X coordinate inside the capsule (mouseX - x)
     * @param ry     Relative mouse Y coordinate inside the capsule (mouseY - y)
     * @param button Mouse button code (0 = left click)
     * @param width  Current capsule width
     * @param height Current capsule height
     * @return true if the event was consumed, false otherwise
     */
    boolean mouseClicked(double rx, double ry, int button, float width, float height);

    /**
     * Frame update loop called for updating animations and ticking state.
     *
     * @param dt Frame delta time in seconds
     */
    void tick(float dt);
}
