/*
 * MusicModule - Architecture & Primary Responsibility:
 * Dynamic Island Music Player Module.
 * Implements the minimal, compact, and expanded rendering modes for the media player.
 * Features procedurally animated vine-like particle waves, compact play/next buttons,
 * and slide-up controls animation.
 */
package com.example.glassmenu.widget.impl;

import com.example.glassmenu.GlassMenuClient;
import com.example.glassmenu.media.LinuxMediaController;
import com.example.glassmenu.shader.ModShaders;
import com.example.glassmenu.widget.IslandModule;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

public class MusicModule implements IslandModule {
    private static final Identifier WHITE_TEX = Identifier.of("minecraft", "textures/misc/white.png");

    public enum Mode {
        MINIMAL,
        COMPACT,
        EXPANDED
    }

    private Mode currentMode = Mode.COMPACT;

    // Interaction scales
    private float shuffleScale = 1.0f, prevScale = 1.0f, playScale = 1.0f, nextScale = 1.0f, repeatScale = 1.0f;
    private float shuffleHover = 1.0f, prevHover = 1.0f, playHover = 1.0f, nextHover = 1.0f, repeatHover = 1.0f;

    // Animation timers
    private float animTime = 0.0f;

    // Album Art micro-animation state
    private float artScale = 0.85f;

    public void resetMode() {
        this.currentMode = Mode.COMPACT;
    }

    @Override
    public boolean isActive() {
        LinuxMediaController.MediaState state = LinuxMediaController.getCurrentState();
        return state != null && !state.title.isEmpty() && !"Unknown".equals(state.title);
    }

    @Override
    public int getPriority() {
        return 10; // High priority for music module
    }

    @Override
    public float getTargetWidth() {
        if (currentMode == Mode.MINIMAL) {
            return 100; // Idle width
        } else if (currentMode == Mode.COMPACT) {
            return 200; // Hovered width
        } else {
            return 200; // Expanded width
        }
    }

    @Override
    public float getTargetHeight() {
        if (currentMode == Mode.MINIMAL) {
            return 24; // Idle height
        } else if (currentMode == Mode.COMPACT) {
            return 32; // Hovered height
        } else {
            return 112; // Expanded height
        }
    }

    @Override
    public void tick(float dt) {
        LinuxMediaController.MediaState state = LinuxMediaController.getCurrentState();
        boolean isPlaying = state != null && state.isPlaying;

        if (isPlaying) {
            animTime += dt;
        } else {
            animTime += dt * 0.2f;
        }

        // Album Art micro-animation (grow when playing, shrink when paused)
        float targetArtScale = isPlaying ? 1.0f : 0.85f;
        artScale = MathHelper.lerp(8f * dt, artScale, targetArtScale);

        // Lerp button interaction scales back to 1.0f
        shuffleScale = MathHelper.lerp(10.0f * dt, shuffleScale, 1.0f);
        prevScale = MathHelper.lerp(10.0f * dt, prevScale, 1.0f);
        playScale = MathHelper.lerp(10.0f * dt, playScale, 1.0f);
        nextScale = MathHelper.lerp(10.0f * dt, nextScale, 1.0f);
        repeatScale = MathHelper.lerp(10.0f * dt, repeatScale, 1.0f);
    }

    @Override
    public void renderContent(DrawContext context, float x, float y, float width, float height, float widgetAlpha, float progress, double rx, double ry) {
        LinuxMediaController.MediaState state = LinuxMediaController.getCurrentState();
        if (state == null) return;

        // Calculate progress transitions
        float wProgress = MathHelper.clamp((width - 100f) / (200f - 100f), 0.0f, 1.0f);
        float heightProgress = MathHelper.clamp((height - 32f) / (112f - 32f), 0.0f, 1.0f);
        float smoothProgress = heightProgress * heightProgress * (3 - 2 * heightProgress);

        // 1. MORPHING ALBUM ART
        float s1 = MathHelper.lerp(wProgress, 16f, 24f);
        float artSize = MathHelper.lerp(smoothProgress, s1, 32f);

        float artX = MathHelper.lerp(smoothProgress, 4f, 10f);
        
        float y1 = MathHelper.lerp(wProgress, (24f - 16f) / 2f, 4f);
        float artY = MathHelper.lerp(smoothProgress, y1, 10f);

        float r1 = MathHelper.lerp(wProgress, 8f, 12f);
        float artRadius = MathHelper.lerp(smoothProgress, r1, 6f);

        context.getMatrices().push();
        float currentArtScale = MathHelper.lerp(smoothProgress, 1.0f, artScale);
        float centerX = artX + artSize / 2f;
        float centerY = artY + artSize / 2f;
        context.getMatrices().translate(x + centerX, y + centerY, 0);
        context.getMatrices().scale(currentArtScale, currentArtScale, 1.0f);
        context.getMatrices().translate(-(x + centerX), -(y + centerY), 0);

        if (state.artTexture != null && state.artWidth > 0 && MinecraftClient.getInstance().getTextureManager().getTexture(state.artTexture) != null) {
            drawRoundedTexture(context, state.artTexture, x + artX, y + artY, artSize, artRadius, widgetAlpha, state.artWidth, state.artHeight);
        } else {
            drawSdfBackground(context, x + artX, y + artY, artSize, artSize, artRadius, 0xFF222222, 1.5f, widgetAlpha);
        }
        
        // Render 1px border highlight (rgba(53, 53, 53, 0.6))
        int borderColor = ((int)(widgetAlpha * 153) << 24) | 0x353535;
        com.example.glassmenu.render.RenderUtils.drawSdfRoundedOutline(context.getMatrices(), x + artX, y + artY, artSize, artSize, artRadius, 1.0f, borderColor);
        context.getMatrices().pop();

        // 2. IDLE MODE DECORATION: Vine visualizer on the right when not fully hovered
        if (wProgress < 0.99f) {
            float idleVisualizerAlpha = (1.0f - wProgress) * widgetAlpha;
            if (state.isPlaying) {
                drawVinesParticles(context, x + 72f, y + 5f, 24f, 14f, idleVisualizerAlpha, 0.0f);
            }
        }

        // 3. COMPACT MODE CONTENT (Visible when hovered, but not fully expanded)
        float compactAlpha = wProgress * (1.0f - smoothProgress) * widgetAlpha;
        if (compactAlpha > 0.01f) {
            renderCompactModeContent(context, x, y, state, compactAlpha, rx, ry);
        }

        // 4. EXPANDED MODE CONTENT (Visible when expanded)
        float expandedAlpha = smoothProgress * widgetAlpha;
        if (expandedAlpha > 0.01f) {
            renderExpandedModeContent(context, x, y, state, expandedAlpha, rx, ry);
        }
    }

    private void renderCompactModeContent(DrawContext context, float x, float y, LinuxMediaController.MediaState state, float alpha, double rx, double ry) {
        MinecraftClient client = MinecraftClient.getInstance();
        int color = ((int) (alpha * 255) << 24) | 0xFFFFFF;
        int subColor = ((int) (alpha * 150) << 24) | 0xCCCCCC;

        // Song title and artist stacked vertically next to album art (X: 34)
        boolean hasArtist = state.artist != null && !state.artist.isEmpty() && !"Unknown".equalsIgnoreCase(state.artist);
        if (hasArtist) {
            context.getMatrices().push();
            context.getMatrices().translate(x + 34f, y + 10f, 0);
            context.getMatrices().scale(0.7f, 0.7f, 1.0f);
            context.drawText(client.textRenderer, truncate(state.title, 13), 0, 0, color, false);
            context.getMatrices().pop();

            context.getMatrices().push();
            context.getMatrices().translate(x + 34f, y + 20f, 0);
            context.getMatrices().scale(0.5f, 0.5f, 1.0f);
            context.drawText(client.textRenderer, truncate(state.artist, 16), 0, 0, subColor, false);
            context.getMatrices().pop();
        } else {
            context.getMatrices().push();
            context.getMatrices().translate(x + 34f, y + 12f, 0);
            context.getMatrices().scale(0.75f, 0.75f, 1.0f);
            context.drawText(client.textRenderer, truncate(state.title, 15), 0, 0, color, false);
            context.getMatrices().pop();
        }

        // Play/Pause and Next buttons on the right side (Centers: 155 and 180, Y: 16)
        float dt = 0.016f;

        boolean playHovered = rx >= 145 && rx <= 165 && ry >= 6 && ry <= 26;
        boolean nextHovered = rx >= 170 && rx <= 190 && ry >= 6 && ry <= 26;

        playHover = MathHelper.lerp(16.0f * dt, playHover, playHovered ? 1.15f : 1.0f);
        nextHover = MathHelper.lerp(16.0f * dt, nextHover, nextHovered ? 1.15f : 1.0f);

        float playHoverProgress = MathHelper.clamp((playHover - 1.0f) / 0.15f, 0.0f, 1.0f);
        float nextHoverProgress = MathHelper.clamp((nextHover - 1.0f) / 0.15f, 0.0f, 1.0f);

        String playPauseIcon = state.isPlaying ? "media-playback-pause-symbolic" : "media-playback-start-symbolic";
        drawSymbolicButton(context, x, y, playPauseIcon, 155f, 16f, 18f, 8f, alpha, playScale * playHover, playHoverProgress, alpha);
        drawSymbolicButton(context, x, y, "media-seek-forward", 180f, 16f, 18f, 8f, alpha, nextScale * nextHover, nextHoverProgress, alpha);
    }

    private void renderExpandedModeContent(DrawContext context, float x, float y, LinuxMediaController.MediaState state, float alpha, double rx, double ry) {
        MinecraftClient client = MinecraftClient.getInstance();
        int color = ((int) (alpha * 255) << 24) | 0xFFFFFF;
        int subColor = ((int) (alpha * 150) << 24) | 0xCCCCCC;

        // Title and Artist (X: 48, Y: 12 / 24)
        context.getMatrices().push();
        context.getMatrices().translate(x + 48, y + 12, 0);
        context.getMatrices().scale(0.75f, 0.75f, 1.0f);
        context.drawText(client.textRenderer, truncate(state.title, 14), 0, 0, color, false);
        context.getMatrices().pop();

        context.getMatrices().push();
        context.getMatrices().translate(x + 48, y + 24, 0);
        context.getMatrices().scale(0.55f, 0.55f, 1.0f);
        context.drawText(client.textRenderer, truncate(state.artist, 18), 0, 0, subColor, false);
        context.getMatrices().pop();

        // Vine visualizer on the top right (X: 150, Y: 16, Width: 40, Height: 18)
        if (state.isPlaying) {
            drawVinesParticles(context, x + 150f, y + 16f, 40f, 18f, alpha, 1.0f);
        }

        // Timeline Progress Section (Y: 55)
        float elapsedX = 8;
        float sliderX = 32;
        float sliderW = 136;
        float sliderY = 55;
        float remainingX = 172;

        double pct = (state.length > 0) ? (state.position / state.length) : 0.0;
        pct = MathHelper.clamp(pct, 0.0, 1.0);

        // Gray background line
        drawSdfBackground(context, x + sliderX, y + sliderY, sliderW, 2f, 1.0f, ((int) (alpha * 50) << 24) | 0xFFFFFF, 0.5f, alpha);

        // Progress filled line (Colored using avgColor)
        if (pct > 0.0) {
            drawSdfBackground(context, x + sliderX, y + sliderY, (float) (sliderW * pct), 2f, 1.0f, applyAlpha(state.avgColor, alpha), 0.5f, alpha);
        }

        // Progress handle (Thumb dot) - size 3x3
        float thumbX = sliderX + (float) (sliderW * pct);
        drawSdfBackground(context, x + thumbX - 1.5f, y + sliderY + 1f - 1.5f, 3f, 3f, 1.5f, ((int) (alpha * 255) << 24) | 0xFFFFFF, 0.5f, alpha);

        // Elapsed & Remaining time
        String elapsedStr = formatTime(state.position);
        String remainingStr = "-" + formatTime(Math.max(0.0, state.length - state.position));

        context.getMatrices().push();
        context.getMatrices().translate(x + elapsedX, y + 53, 0);
        context.getMatrices().scale(0.5f, 0.5f, 1.0f);
        context.drawText(client.textRenderer, elapsedStr, 0, 0, subColor, false);
        context.getMatrices().pop();

        float remainingWidth = client.textRenderer.getWidth(remainingStr) * 0.5f;
        context.getMatrices().push();
        context.getMatrices().translate(x + remainingX + 20 - remainingWidth, y + 53, 0);
        context.getMatrices().scale(0.5f, 0.5f, 1.0f);
        context.drawText(client.textRenderer, remainingStr, 0, 0, subColor, false);
        context.getMatrices().pop();

        // 5. Playback Controls Section (Center Y: buttonY)
        float dt = 0.016f;
        float buttonY = 88f + 20f * (1.0f - alpha); // Animation slide-up from bottom

        // Hover detection:
        boolean shuffleHovered = rx >= 15 && rx <= 45 && ry >= buttonY - 15 && ry <= buttonY + 15;
        boolean prevHovered = rx >= 50 && rx <= 80 && ry >= buttonY - 15 && ry <= buttonY + 15;
        boolean playHovered = rx >= 85 && rx <= 115 && ry >= buttonY - 15 && ry <= buttonY + 15;
        boolean nextHovered = rx >= 120 && rx <= 150 && ry >= buttonY - 15 && ry <= buttonY + 15;
        boolean repeatHovered = rx >= 155 && rx <= 185 && ry >= buttonY - 15 && ry <= buttonY + 15;

        shuffleHover = MathHelper.lerp(16.0f * dt, shuffleHover, shuffleHovered ? 1.15f : 1.0f);
        prevHover = MathHelper.lerp(16.0f * dt, prevHover, prevHovered ? 1.15f : 1.0f);
        playHover = MathHelper.lerp(16.0f * dt, playHover, playHovered ? 1.15f : 1.0f);
        nextHover = MathHelper.lerp(16.0f * dt, nextHover, nextHovered ? 1.15f : 1.0f);
        repeatHover = MathHelper.lerp(16.0f * dt, repeatHover, repeatHovered ? 1.15f : 1.0f);

        float shuffleHoverProgress = MathHelper.clamp((shuffleHover - 1.0f) / 0.15f, 0.0f, 1.0f);
        float prevHoverProgress = MathHelper.clamp((prevHover - 1.0f) / 0.15f, 0.0f, 1.0f);
        float playHoverProgress = MathHelper.clamp((playHover - 1.0f) / 0.15f, 0.0f, 1.0f);
        float nextHoverProgress = MathHelper.clamp((nextHover - 1.0f) / 0.15f, 0.0f, 1.0f);
        float repeatHoverProgress = MathHelper.clamp((repeatHover - 1.0f) / 0.15f, 0.0f, 1.0f);

        // Draw Shuffle button (Center X: 30, Size 16, Icon 8)
        String shuffleIcon = state.shuffle ? "media-playlist-shuffle-symbolic" : "media-playlist-consecutive-symbolic";
        drawSymbolicButton(context, x, y, shuffleIcon, 30f, buttonY, 16f, 8f, alpha, shuffleScale * shuffleHover, shuffleHoverProgress, alpha);

        // Draw Prev button (Center X: 65, Size 20, Icon 10)
        drawSymbolicButton(context, x, y, "media-seek-backward", 65f, buttonY, 20f, 10f, alpha, prevScale * prevHover, prevHoverProgress, alpha);

        // Draw Play/Pause button (Center X: 100, Size 24, Icon 12)
        String playPauseIcon = state.isPlaying ? "media-playback-pause-symbolic" : "media-playback-start-symbolic";
        drawSymbolicButton(context, x, y, playPauseIcon, 100f, buttonY, 24f, 12f, alpha, playScale * playHover, playHoverProgress, alpha);

        // Draw Next button (Center X: 135, Size 20, Icon 10)
        drawSymbolicButton(context, x, y, "media-seek-forward", 135f, buttonY, 20f, 10f, alpha, nextScale * nextHover, nextHoverProgress, alpha);

        // Draw Repeat button (Center X: 170, Size 16, Icon 8)
        String repeatIcon = "mail-forward";
        if ("Track".equalsIgnoreCase(state.loopStatus)) {
            repeatIcon = "media-playlist-repeat-song-symbolic";
        } else if ("Playlist".equalsIgnoreCase(state.loopStatus)) {
            repeatIcon = "media-playlist-repeat-symbolic";
        }
        drawSymbolicButton(context, x, y, repeatIcon, 170f, buttonY, 16f, 8f, alpha, repeatScale * repeatHover, repeatHoverProgress, alpha);
    }

    private void drawSymbolicButton(DrawContext context, float x, float y, String iconName, float cx, float cy, float btnSize, float iconSize, float alpha, float scale, float hoverProgress, float widgetAlpha) {
        context.getMatrices().push();
        context.getMatrices().translate(x + cx, y + cy, 0);
        context.getMatrices().scale(scale, scale, 1.0f);

        // Hover/normal highlight: circular glass background highlight
        float bgAlpha = MathHelper.lerp(hoverProgress, 0.08f, 0.25f);
        int bgCol = ((int) (alpha * bgAlpha * 255) << 24) | 0xFFFFFF;
        drawSdfBackground(context, -btnSize / 2f, -btnSize / 2f, btnSize, btnSize, btnSize * 0.5f, bgCol, 1.0f, widgetAlpha);

        // Draw custom vector shapes based on GTK symbolic iconName
        int rVal = (int) MathHelper.lerp(hoverProgress, 200, 255);
        int gVal = (int) MathHelper.lerp(hoverProgress, 200, 255);
        int bVal = (int) MathHelper.lerp(hoverProgress, 200, 255);
        int iconAlphaVal = (int) (alpha * 255);
        int iconColor = (iconAlphaVal << 24) | (rVal << 16) | (gVal << 8) | bVal;

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        float halfIcon = iconSize / 2f;

        if ("media-seek-backward".equals(iconName)) {
            // Draw double arrow pointing left (using clockwise winding)
            drawTriangle(matrix, halfIcon * 0.5f, halfIcon * 0.7f, halfIcon * 0.5f, -halfIcon * 0.7f, -halfIcon * 0.1f, 0f, iconColor);
            drawTriangle(matrix, -halfIcon * 0.1f, halfIcon * 0.7f, -halfIcon * 0.1f, -halfIcon * 0.7f, -halfIcon * 0.7f, 0f, iconColor);
        } else if ("media-seek-forward".equals(iconName)) {
            // Draw double arrow pointing right
            drawTriangle(matrix, -halfIcon * 0.5f, -halfIcon * 0.7f, -halfIcon * 0.5f, halfIcon * 0.7f, halfIcon * 0.1f, 0f, iconColor);
            drawTriangle(matrix, halfIcon * 0.1f, -halfIcon * 0.7f, halfIcon * 0.1f, halfIcon * 0.7f, halfIcon * 0.7f, 0f, iconColor);
        } else if ("media-playback-start-symbolic".equals(iconName)) {
            // Draw single triangle pointing right
            drawTriangle(matrix, -halfIcon * 0.4f, -halfIcon * 0.7f, -halfIcon * 0.4f, halfIcon * 0.7f, halfIcon * 0.8f, 0f, iconColor);
        } else if ("media-playback-pause-symbolic".equals(iconName)) {
            // Draw two rounded vertical bars (capsules)
            drawSdfBackground(context, -halfIcon * 0.5f, -halfIcon * 0.7f, halfIcon * 0.35f, halfIcon * 1.4f, halfIcon * 0.17f, iconColor, 1.5f, widgetAlpha);
            drawSdfBackground(context, halfIcon * 0.15f, -halfIcon * 0.7f, halfIcon * 0.35f, halfIcon * 1.4f, halfIcon * 0.17f, iconColor, 1.5f, widgetAlpha);
        } else if ("media-playlist-shuffle-symbolic".equals(iconName) || "media-playlist-consecutive-symbolic".equals(iconName)) {
            // Crossing arrows for Shuffle
            boolean enabled = "media-playlist-shuffle-symbolic".equals(iconName);
            int shufCol = enabled ? (((int)(alpha * 255) << 24) | 0x34C759) : iconColor;
            
            // Arrow 1: top-left to bottom-right
            com.example.glassmenu.render.RenderUtils.drawLine(context.getMatrices(), -halfIcon, -halfIcon * 0.5f, -halfIcon * 0.3f, -halfIcon * 0.5f, 1.0f, shufCol);
            com.example.glassmenu.render.RenderUtils.drawLine(context.getMatrices(), -halfIcon * 0.3f, -halfIcon * 0.5f, halfIcon * 0.3f, halfIcon * 0.5f, 1.0f, shufCol);
            com.example.glassmenu.render.RenderUtils.drawLine(context.getMatrices(), halfIcon * 0.3f, halfIcon * 0.5f, halfIcon, halfIcon * 0.5f, 1.0f, shufCol);
            // arrowhead bottom-right
            com.example.glassmenu.render.RenderUtils.drawLine(context.getMatrices(), halfIcon * 0.6f, halfIcon * 0.2f, halfIcon, halfIcon * 0.5f, 1.0f, shufCol);
            com.example.glassmenu.render.RenderUtils.drawLine(context.getMatrices(), halfIcon * 0.6f, halfIcon * 0.8f, halfIcon, halfIcon * 0.5f, 1.0f, shufCol);

            // Arrow 2: bottom-left to top-right
            com.example.glassmenu.render.RenderUtils.drawLine(context.getMatrices(), -halfIcon, halfIcon * 0.5f, -halfIcon * 0.3f, halfIcon * 0.5f, 1.0f, shufCol);
            com.example.glassmenu.render.RenderUtils.drawLine(context.getMatrices(), -halfIcon * 0.3f, halfIcon * 0.5f, halfIcon * 0.3f, -halfIcon * 0.5f, 1.0f, shufCol);
            com.example.glassmenu.render.RenderUtils.drawLine(context.getMatrices(), halfIcon * 0.3f, -halfIcon * 0.5f, halfIcon, -halfIcon * 0.5f, 1.0f, shufCol);
            // arrowhead top-right
            com.example.glassmenu.render.RenderUtils.drawLine(context.getMatrices(), halfIcon * 0.6f, -halfIcon * 0.8f, halfIcon, -halfIcon * 0.5f, 1.0f, shufCol);
            com.example.glassmenu.render.RenderUtils.drawLine(context.getMatrices(), halfIcon * 0.6f, -halfIcon * 0.2f, halfIcon, -halfIcon * 0.5f, 1.0f, shufCol);
        } else if ("media-playlist-repeat-symbolic".equals(iconName) || "media-playlist-repeat-song-symbolic".equals(iconName) || "mail-forward".equals(iconName)) {
            // Repeat loop
            boolean enabled = !"mail-forward".equals(iconName);
            boolean isTrack = "media-playlist-repeat-song-symbolic".equals(iconName);
            int repCol = enabled ? (((int)(alpha * 255) << 24) | 0x34C759) : iconColor;

            float iw = halfIcon;
            float ih = halfIcon * 0.75f;
            
            // Top line
            com.example.glassmenu.render.RenderUtils.drawLine(context.getMatrices(), -iw, -ih, iw, -ih, 1.0f, repCol);
            // Right line
            com.example.glassmenu.render.RenderUtils.drawLine(context.getMatrices(), iw, -ih, iw, ih, 1.0f, repCol);
            // Bottom line
            com.example.glassmenu.render.RenderUtils.drawLine(context.getMatrices(), iw, ih, -iw, ih, 1.0f, repCol);
            // Left line
            com.example.glassmenu.render.RenderUtils.drawLine(context.getMatrices(), -iw, ih, -iw, -ih * 0.3f, 1.0f, repCol);
            
            // Arrow head pointing up-right
            com.example.glassmenu.render.RenderUtils.drawLine(context.getMatrices(), -iw - iw*0.3f, -ih*0.1f, -iw, -ih * 0.3f, 1.0f, repCol);
            com.example.glassmenu.render.RenderUtils.drawLine(context.getMatrices(), -iw + iw*0.3f, -ih*0.1f, -iw, -ih * 0.3f, 1.0f, repCol);

            // If isTrack, draw a tiny "1" in the center
            if (isTrack) {
                com.example.glassmenu.render.RenderUtils.drawLine(context.getMatrices(), 0, -ih*0.4f, 0, ih*0.4f, 0.8f, repCol);
                com.example.glassmenu.render.RenderUtils.drawLine(context.getMatrices(), -iw*0.15f, -ih*0.2f, 0, -ih*0.4f, 0.8f, repCol);
                com.example.glassmenu.render.RenderUtils.drawLine(context.getMatrices(), -iw*0.15f, ih*0.4f, iw*0.15f, ih*0.4f, 0.8f, repCol);
            }
        }

        context.getMatrices().pop();
    }

    private static void drawTriangle(Matrix4f matrix, float x1, float y1, float x2, float y2, float x3, float y3, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;
        
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(matrix, x1, y1, 0).color(r, g, b, a);
        bufferBuilder.vertex(matrix, x2, y2, 0).color(r, g, b, a);
        bufferBuilder.vertex(matrix, x3, y3, 0).color(r, g, b, a);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.enableCull();
    }

    private void drawVinesParticles(DrawContext context, float eqX, float eqY, float eqW, float eqH, float alpha, float progress) {
        if (alpha <= 0.01f) return;
        float t = animTime;
        int numVines = 3;
        for (int v = 0; v < numVines; v++) {
            float phase = v * 2.09f;
            float amplitude = (eqH / 2f) * (0.5f + v * 0.15f);
            float speed = 2.0f + v * 0.4f;
            float freq = 5.0f + v * 1.0f;
            
            int numParticles = 7;
            for (int p = 0; p < numParticles; p++) {
                float progressX = p / (float) (numParticles - 1); // 0.0 to 1.0
                
                float px = eqX + progressX * eqW;
                float convergenceFactor = MathHelper.lerp(progress, 1.0f, 1.0f - progressX);
                float py = eqY + eqH / 2f + amplitude * MathHelper.sin(progressX * freq - t * speed + phase) * convergenceFactor;
                
                float pSize = MathHelper.lerp(progressX, 2.2f, 1.2f);
                
                int baseColor = 0xAAFFFFFF; // Translucent white
                if (v == 0) {
                    baseColor = 0xAA34C759; // iOS Green
                } else if (v == 2) {
                    baseColor = 0xAABBF2C0; // Mint green
                }
                
                int colorAlpha = (int) (((baseColor >> 24) & 0xFF) * alpha);
                int rgb = baseColor & 0xFFFFFF;
                int finalColor = (colorAlpha << 24) | rgb;
                
                drawSdfBackground(context, px - pSize / 2f, py - pSize / 2f, pSize, pSize, pSize / 2f, finalColor, 0.5f, 1.0f);
            }
        }
    }

    private int applyAlpha(int argb, float alpha) {
        int a = (int) (((argb >> 24) & 0xFF) * alpha);
        return (a << 24) | (argb & 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double rx, double ry, int button, float width, float height) {
        LinuxMediaController.MediaState state = LinuxMediaController.getCurrentState();
        if (state == null) return false;

        // 1. Gesture Mode Transitions
        if (currentMode == Mode.COMPACT) {
            // Play/Pause button (Center X: 155, Y: 16)
            if (rx >= 145 && rx <= 165 && ry >= 6 && ry <= 26) {
                playScale = 0.8f;
                LinuxMediaController.playPause();
                return true;
            }
            // Next button (Center X: 180, Y: 16)
            if (rx >= 170 && rx <= 190 && ry >= 6 && ry <= 26) {
                nextScale = 0.8f;
                LinuxMediaController.next();
                return true;
            }

            // Click on the rest of the capsule -> expand to Expanded Mode
            if (button == 0) {
                currentMode = Mode.EXPANDED;
                return true;
            }
        }

        if (currentMode == Mode.EXPANDED) {
            if (button == 1) { // Right click -> Compact Mode
                currentMode = Mode.COMPACT;
                return true;
            }

            // Timeline Progress bar seek click (X: 32 to 168, Y: 50 to 62)
            if (ry >= 50 && ry <= 62 && rx >= 32 && rx <= 168) {
                double frac = (rx - 32) / 136.0;
                if (state.length > 0) {
                    LinuxMediaController.seek(frac * state.length);
                    return true;
                }
            }

            float buttonY = 88f; // Center Y of buttons in expanded mode

            // Playback controls clicks (Y: buttonY - 15 to buttonY + 15)
            if (ry >= buttonY - 15 && ry <= buttonY + 15) {
                // Shuffle (X: 15 to 45)
                if (rx >= 15 && rx <= 45) {
                    shuffleScale = 0.8f;
                    LinuxMediaController.toggleShuffle();
                    return true;
                }
                // Prev (X: 50 to 80)
                if (rx >= 50 && rx <= 80) {
                    prevScale = 0.8f;
                    LinuxMediaController.previous();
                    return true;
                }
                // Play (X: 85 to 115)
                if (rx >= 85 && rx <= 115) {
                    playScale = 0.8f;
                    LinuxMediaController.playPause();
                    return true;
                }
                // Next (X: 120 to 150)
                if (rx >= 120 && rx <= 150) {
                    nextScale = 0.8f;
                    LinuxMediaController.next();
                    return true;
                }
                // Repeat (X: 155 to 185)
                if (rx >= 155 && rx <= 185) {
                    repeatScale = 0.8f;
                    LinuxMediaController.toggleLoop();
                    return true;
                }
            }
        }

        return false;
    }

    private void drawSdfBackground(DrawContext context, float x, float y, float w, float h, float r, int argb, float edgeSoftness, float widgetAlpha) {
        var shader = !GlassMenuClient.CONFIG.enableShaders() ? null : ModShaders.getSdfRoundedRect();
        if (shader == null) {
            float a = (float) (argb >> 24 & 255) / 255.0F * widgetAlpha;
            int alphaVal = (int) (a * 255);
            int color = (alphaVal << 24) | (argb & 0xFFFFFF);
            context.fill((int) x, (int) y, (int) (x + w), (int) (y + h), color);
            return;
        }

        float a = (float) (argb >> 24 & 255) / 255.0F * widgetAlpha;
        float red = (float) (argb >> 16 & 255) / 255.0F;
        float green = (float) (argb >> 8 & 255) / 255.0F;
        float blue = (float) (argb & 255) / 255.0F;

        RenderSystem.setShader(() -> shader);
        RenderSystem.setShaderTexture(0, WHITE_TEX);

        if (shader.getUniform("Color") != null) shader.getUniform("Color").set(red, green, blue, a);
        if (shader.getUniform("Size") != null) shader.getUniform("Size").set(w, h);
        if (shader.getUniform("Radius") != null) shader.getUniform("Radius").set(r);
        if (shader.getUniform("EdgeSoftness") != null) shader.getUniform("EdgeSoftness").set(edgeSoftness);
        if (shader.getUniform("TexBounds") != null) shader.getUniform("TexBounds").set(0.0f, 0.0f, 1.0f, 1.0f);

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        bufferBuilder.vertex(matrix, x, y, 0).texture(0, 0);
        bufferBuilder.vertex(matrix, x, y + h, 0).texture(0, 1);
        bufferBuilder.vertex(matrix, x + w, y + h, 0).texture(1, 1);
        bufferBuilder.vertex(matrix, x + w, y, 0).texture(1, 0);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

    private void drawRoundedTexture(DrawContext context, Identifier texture, float x, float y, float size, float r, float alpha, int tw, int th) {
        if (texture == null) return;
        var textureManager = MinecraftClient.getInstance().getTextureManager();
        if (textureManager.getTexture(texture) == null) return;

        var shader = !GlassMenuClient.CONFIG.enableShaders() ? null : ModShaders.getSdfRoundedRect();
        if (shader == null) {
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
            context.drawTexture(texture, (int) x, (int) y, 0, 0, (int) size, (int) size, (int) size, (int) size);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            return;
        }

        RenderSystem.setShader(() -> shader);
        RenderSystem.setShaderTexture(0, texture);

        if (shader.getUniform("Color") != null) shader.getUniform("Color").set(1.0f, 1.0f, 1.0f, alpha);
        if (shader.getUniform("Size") != null) shader.getUniform("Size").set(size, size);
        if (shader.getUniform("Radius") != null) shader.getUniform("Radius").set(r);
        if (shader.getUniform("EdgeSoftness") != null) shader.getUniform("EdgeSoftness").set(1.5f);

        float u1 = 0, v1 = 0, u2 = 1, v2 = 1;
        if (tw > 0 && th > 0) {
            float texRatio = (float) tw / th;
            if (texRatio > 1.0f) {
                float widthFactor = 1.0f / texRatio;
                u1 = (1.0f - widthFactor) / 2.0f;
                u2 = 1.0f - u1;
            } else if (texRatio < 1.0f) {
                float heightFactor = texRatio;
                v1 = (1.0f - heightFactor) / 2.0f;
                v2 = 1.0f - v1;
            }
        }

        if (shader.getUniform("TexBounds") != null) shader.getUniform("TexBounds").set(u1, v1, u2, v2);

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        bufferBuilder.vertex(matrix, x, y, 0).texture(0, 0);
        bufferBuilder.vertex(matrix, x, y + size, 0).texture(0, 1);
        bufferBuilder.vertex(matrix, x + size, y + size, 0).texture(1, 1);
        bufferBuilder.vertex(matrix, x + size, y, 0).texture(1, 0);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

    private String formatTime(double seconds) {
        int minutes = (int) (seconds / 60);
        int secs = (int) (seconds % 60);
        return String.format("%02d:%02d", minutes, secs);
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 3) + "..." : s;
    }
}
