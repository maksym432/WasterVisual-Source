/*
 * MusicModule - Architecture & Primary Responsibility:
 * Dynamic Island Music Player Module.
 * Implements compact (hovered) and expanded views with Apple SF Symbols,
 * bouncing vertical capsule waveform, timeline progress scrubbing, and slide-up buttons.
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

    // SF Symbols (Recolored to white)
    private static final Identifier ICON_PLAY = Identifier.of("glassmenu", "textures/gui/symbolic_icons/play.png");
    private static final Identifier ICON_PAUSE = Identifier.of("glassmenu", "textures/gui/symbolic_icons/pause.png");
    private static final Identifier ICON_PREV = Identifier.of("glassmenu", "textures/gui/symbolic_icons/prev.png");
    private static final Identifier ICON_NEXT = Identifier.of("glassmenu", "textures/gui/symbolic_icons/next.png");
    private static final Identifier ICON_AIRPLAY = Identifier.of("glassmenu", "textures/gui/symbolic_icons/mail_forward.png"); // Using mail forward as elegant replacement for AirPlay

    public enum Mode {
        MINIMAL,
        COMPACT,
        EXPANDED
    }

    private Mode currentMode = Mode.COMPACT;

    // Interaction scales
    private float prevScale = 1.0f, playScale = 1.0f, nextScale = 1.0f;
    private float prevHover = 1.0f, playHover = 1.0f, nextHover = 1.0f;
    private float animTime = 0.0f;
    private float artScale = 0.85f;

    // Swell and Scrubbing states
    private boolean isScrubbing = false;
    private double scrubProgress = 0.0;
    private float scrubSwell = 0.0f;
    private float scrubVelocity = 0.0f;
    private double lastMouseX = 0.0;
    private float wobblePhase = 0.0f;
    private long lastRenderTime = 0;
    private long lastRealTimeSeek = 0;

    // Routing menu states
    private boolean routingMenuOpen = false;
    private float routingMenuProgress = 0.0f;
    private double lastRx = -1.0;
    private double lastRy = -1.0;
    
    // Highlight spring physics states
    private float highlightY = -100f;
    private float highlightVelocity = 0.0f;

    public void resetMode() {
        this.currentMode = Mode.COMPACT;
        this.routingMenuOpen = false;
    }

    @Override
    public boolean isActive() {
        LinuxMediaController.MediaState state = LinuxMediaController.getCurrentState();
        return state != null && !state.title.isEmpty() && !"Unknown".equals(state.title);
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public float getTargetWidth() {
        if (currentMode == Mode.MINIMAL) {
            return 75f; // Even smaller when not on lockscreen
        } else if (currentMode == Mode.COMPACT) {
            return 190f;
        } else {
            return 220f;
        }
    }

    @Override
    public float getTargetHeight() {
        if (currentMode == Mode.MINIMAL) {
            return 20f;
        } else if (currentMode == Mode.COMPACT) {
            return 30f;
        } else {
            return 110f;
        }
    }

    @Override
    public void tick(float dt) {
        LinuxMediaController.MediaState state = LinuxMediaController.getCurrentState();
        boolean isPlaying = state != null && state.isPlaying;

        if (isPlaying) {
            animTime += dt;
        } else {
            animTime += dt * 0.15f;
        }

        // Album Art micro-animation (grow when playing, shrink when paused)
        float targetArtScale = isPlaying ? 1.0f : 0.85f;
        artScale = MathHelper.lerp(8f * dt, artScale, targetArtScale);

        // Lerp button interaction scales back to 1.0f
        prevScale = MathHelper.lerp(10.0f * dt, prevScale, 1.0f);
        playScale = MathHelper.lerp(10.0f * dt, playScale, 1.0f);
        nextScale = MathHelper.lerp(10.0f * dt, nextScale, 1.0f);

        // Animate routingMenuProgress
        float targetProgress = routingMenuOpen ? 1.0f : 0.0f;
        routingMenuProgress = MathHelper.lerp(12.0f * dt, routingMenuProgress, targetProgress);

        // Highlight spring physics
        if (routingMenuOpen && !LinuxMediaController.ALL_PLAYERS_INFO.isEmpty()) {
            float targetY = -100f;
            boolean anyHovered = false;
            
            // Check if mouse is hovering any item (Expanded width is 220f)
            int idx = 0;
            for (LinuxMediaController.PlayerInfo info : LinuxMediaController.ALL_PLAYERS_INFO) {
                if (idx >= 3) break;
                float itemY = 32f + idx * 24f;
                if (lastRx >= 6 && lastRx <= 214 && lastRy >= itemY && lastRy <= itemY + 22f) {
                    targetY = itemY;
                    anyHovered = true;
                    break;
                }
                idx++;
            }
            
            if (!anyHovered) {
                // Find selected player index
                int selIdx = -1;
                for (int i = 0; i < LinuxMediaController.ALL_PLAYERS_INFO.size(); i++) {
                    if (LinuxMediaController.ALL_PLAYERS_INFO.get(i).name.equals(LinuxMediaController.selectedPlayer)) {
                        selIdx = i;
                        break;
                    }
                }
                if (selIdx == -1 && !LinuxMediaController.ALL_PLAYERS_INFO.isEmpty()) {
                    selIdx = 0;
                }
                if (selIdx >= 0 && selIdx < 3) {
                    targetY = 32f + selIdx * 24f;
                }
            }
            
            if (targetY != -100f) {
                if (highlightY == -100f) {
                    highlightY = targetY;
                }
                float forceY = (targetY - highlightY) * 350f;
                highlightVelocity += forceY * dt;
                highlightVelocity *= (float) Math.exp(-18f * dt);
                highlightY += highlightVelocity * dt;
            } else {
                highlightY = -100f;
                highlightVelocity = 0f;
            }
        } else {
            highlightY = -100f;
            highlightVelocity = 0f;
        }
    }

    @Override
    public void renderContent(DrawContext context, float x, float y, float width, float height, float widgetAlpha, float progress, double rx, double ry) {
        LinuxMediaController.MediaState state = LinuxMediaController.getCurrentState();
        if (state == null) return;

        // Frame-rate independent delta time calculation (using high-precision nanoTime to prevent menu stutter)
        long timeNow = System.nanoTime();
        float renderDt = lastRenderTime == 0 ? 0.016f : (timeNow - lastRenderTime) / 1_000_000_000f;
        lastRenderTime = timeNow;
        if (renderDt < 0f || renderDt > 0.1f) renderDt = 0.016f;

        this.lastRx = rx;
        this.lastRy = ry;

        float activeWidgetAlpha = widgetAlpha * (1.0f - routingMenuProgress);

        float minW = 75f;
        float minH = 20f;

        // Calculate progress transitions
        float wProgress = MathHelper.clamp((width - minW) / (190f - minW), 0.0f, 1.0f);
        float heightProgress = MathHelper.clamp((height - 30f) / (110f - 30f), 0.0f, 1.0f);
        float smoothProgress = heightProgress * heightProgress * (3 - 2 * heightProgress);

        if (activeWidgetAlpha > 0.01f) {
            // 1. MORPHING ALBUM ART
            float s1 = MathHelper.lerp(wProgress, 16f, 20f);
            float artSize = MathHelper.lerp(smoothProgress, s1, 36f);

            float artX = MathHelper.lerp(smoothProgress, 4f, 12f);
            
            float y1 = MathHelper.lerp(wProgress, (minH - 16f) / 2f, 5f);
            float artY = MathHelper.lerp(smoothProgress, y1, 12f);

            float r1 = MathHelper.lerp(wProgress, 8f, 10f);
            float artRadius = MathHelper.lerp(smoothProgress, r1, 6f);

            context.getMatrices().push();
            float currentArtScale = MathHelper.lerp(smoothProgress, 1.0f, artScale);
            float centerX = artX + artSize / 2f;
            float centerY = artY + artSize / 2f;
            context.getMatrices().translate(x + centerX, y + centerY, 0);
            context.getMatrices().scale(currentArtScale, currentArtScale, 1.0f);
            context.getMatrices().translate(-(x + centerX), -(y + centerY), 0);

            if (state.artTexture != null && state.artWidth > 0 && MinecraftClient.getInstance().getTextureManager().getTexture(state.artTexture) != null) {
                drawRoundedTexture(context, state.artTexture, x + artX, y + artY, artSize, artRadius, activeWidgetAlpha, state.artWidth, state.artHeight);
            } else {
                drawSdfBackground(context, x + artX, y + artY, artSize, artSize, artRadius, 0xFF1E1E1E, 1.5f, activeWidgetAlpha);
            }
            
            // Render 1px border highlight (rgba(255, 255, 255, 0.15))
            int borderColor = ((int)(activeWidgetAlpha * 38) << 24) | 0xFFFFFF;
            com.example.glassmenu.render.RenderUtils.drawSdfRoundedOutline(context.getMatrices(), x + artX, y + artY, artSize, artSize, artRadius, 0.5f, borderColor);
            context.getMatrices().pop();

            // 2. IDLE MODE DECORATION: iOS Bouncing Waveform Equalizer on the right when not hovered
            if (wProgress < 0.99f) {
                float idleVisualizerAlpha = (1.0f - wProgress) * activeWidgetAlpha;
                drawVerticalWaveform(context, x + width - 22f, y + (height - 10f) / 2f, 12f, 10f, idleVisualizerAlpha, state);
            }

            // 3. COMPACT CONTROLS MODE (Visible when hovered, but not expanded)
            float compactAlpha = wProgress * (1.0f - smoothProgress) * activeWidgetAlpha;
            if (compactAlpha > 0.01f) {
                renderCompactModeContent(context, x, y, width, height, state, compactAlpha);
            }

            // 4. EXPANDED PLAYER CARD MODE (Visible when expanded)
            float expandedAlpha = smoothProgress * activeWidgetAlpha;
            if (expandedAlpha > 0.01f) {
                renderExpandedModeContent(context, x, y, width, height, state, expandedAlpha, rx, ry, renderDt);
            }

            // --- DRAW UNIFIED ANIMATED BUTTONS ---
            if (wProgress > 0.01f) {
                float playX = MathHelper.lerp(smoothProgress, width - 36f, width / 2f);
                float playY = MathHelper.lerp(smoothProgress, height / 2f, 90f);
                float playSize = MathHelper.lerp(smoothProgress, 14f, 24f);
                float playIconSize = MathHelper.lerp(smoothProgress, 7f, 12f);

                float nextX = MathHelper.lerp(smoothProgress, width - 16f, width / 2f + 40f);
                float nextY = MathHelper.lerp(smoothProgress, height / 2f, 90f);
                float nextSize = MathHelper.lerp(smoothProgress, 14f, 20f);
                float nextIconSize = MathHelper.lerp(smoothProgress, 7f, 10f);

                float prevX = MathHelper.lerp(smoothProgress, width - 36f, width / 2f - 40f);
                float prevY = MathHelper.lerp(smoothProgress, height / 2f, 90f);
                float prevSize = MathHelper.lerp(smoothProgress, 14f, 20f);
                float prevIconSize = MathHelper.lerp(smoothProgress, 7f, 10f);

                // 1. Draw Prev button (fades in as it expands)
                float prevAlpha = smoothProgress * wProgress * activeWidgetAlpha;
                if (prevAlpha > 0.01f) {
                    boolean prevHovered = rx >= prevX - prevSize/2f - 2f && rx <= prevX + prevSize/2f + 2f && ry >= prevY - prevSize/2f - 2f && ry <= prevY + prevSize/2f + 2f;
                    prevHover = MathHelper.lerp(12f * renderDt, prevHover, prevHovered ? 1.15f : 1.0f);
                    float prevHoverProgress = MathHelper.clamp((prevHover - 1.0f) / 0.15f, 0.0f, 1.0f);
                    drawSymbolicButton(context, x, y, "media-seek-backward", prevX, prevY, prevSize, prevIconSize, prevAlpha, prevScale * prevHover, prevHoverProgress, prevAlpha);
                }

                // 2. Draw Play/Pause button
                float playAlpha = wProgress * activeWidgetAlpha;
                if (playAlpha > 0.01f) {
                    boolean playHovered = rx >= playX - playSize/2f - 2f && rx <= playX + playSize/2f + 2f && ry >= playY - playSize/2f - 2f && ry <= playY + playSize/2f + 2f;
                    playHover = MathHelper.lerp(12f * renderDt, playHover, playHovered ? 1.15f : 1.0f);
                    float playHoverProgress = MathHelper.clamp((playHover - 1.0f) / 0.15f, 0.0f, 1.0f);
                    String playPauseIcon = state.isPlaying ? "media-playback-pause-symbolic" : "media-playback-start-symbolic";
                    drawSymbolicButton(context, x, y, playPauseIcon, playX, playY, playSize, playIconSize, playAlpha, playScale * playHover, playHoverProgress, playAlpha);
                }

                // 3. Draw Next button
                float nextAlpha = wProgress * activeWidgetAlpha;
                if (nextAlpha > 0.01f) {
                    boolean nextHovered = rx >= nextX - nextSize/2f - 2f && rx <= nextX + nextSize/2f + 2f && ry >= nextY - nextSize/2f - 2f && ry <= nextY + nextSize/2f + 2f;
                    nextHover = MathHelper.lerp(12f * renderDt, nextHover, nextHovered ? 1.15f : 1.0f);
                    float nextHoverProgress = MathHelper.clamp((nextHover - 1.0f) / 0.15f, 0.0f, 1.0f);
                    drawSymbolicButton(context, x, y, "media-seek-forward", nextX, nextY, nextSize, nextIconSize, nextAlpha, nextScale * nextHover, nextHoverProgress, nextAlpha);
                }
            }
        }

        // Render source routing menu if it's opening/open
        if (routingMenuProgress > 0.01f) {
            renderRoutingMenu(context, x, y, width, height, widgetAlpha, renderDt);
        }
    }

    private void drawVerticalWaveform(DrawContext context, float eqX, float eqY, float eqW, float eqH, float alpha, LinuxMediaController.MediaState state) {
        if (alpha <= 0.01f) return;
        long time = System.currentTimeMillis();
        int numBars = 5;
        float barWidth = 2f;
        float barSpacing = 1f;

        // Waveform is colored dynamically based on the album art average color
        int baseColor = state.isPlaying ? state.avgColor : 0x44FFFFFF;
        if (baseColor == 0 || (baseColor & 0xFFFFFF) == 0) {
            baseColor = 0xFF34C759; // Default iOS Green
        }
        int colorAlpha = (int) (((baseColor >> 24) & 0xFF) * alpha);
        if (colorAlpha == 0) colorAlpha = (int) (255 * alpha);
        int finalColor = (colorAlpha << 24) | (baseColor & 0xFFFFFF);

        for (int i = 0; i < numBars; i++) {
            float freq = 0.2f + (i * 0.08f);
            float offset = i * 0.8f;
            
            float heightScale = state.isPlaying ? (float) (Math.sin((time / 100.0) * freq + offset) * 0.4f + 0.6f) : 0.2f;
            float barHeight = Math.max(2f, eqH * heightScale);
            float px = eqX + i * (barWidth + barSpacing);
            float py = eqY + (eqH - barHeight) / 2f; // Center vertically

            // SDF rounded capsule bar
            drawSdfBackground(context, px, py, barWidth, barHeight, barWidth / 2f, finalColor, 0.5f, alpha);
        }
    }

    private void renderCompactModeContent(DrawContext context, float x, float y, float w, float h, LinuxMediaController.MediaState state, float alpha) {
        MinecraftClient client = MinecraftClient.getInstance();
        int color = ((int) (alpha * 255) << 24) | 0xFFFFFF;

        // Song title next to album art (X: 30)
        context.getMatrices().push();
        context.getMatrices().translate(x + 30f, y + (h - 8f) / 2f, 0);
        context.getMatrices().scale(0.7f, 0.7f, 1.0f);
        context.drawText(client.textRenderer, truncate(state.title, 14), 0, 0, color, false);
        context.getMatrices().pop();
    }

    private void renderExpandedModeContent(DrawContext context, float x, float y, float w, float h, LinuxMediaController.MediaState state, float alpha, double rx, double ry, float renderDt) {
        MinecraftClient client = MinecraftClient.getInstance();
        int color = ((int) (alpha * 255) << 24) | 0xFFFFFF;
        int subColor = ((int) (alpha * 150) << 24) | 0xCCCCCC;

        // Title and Artist next to large album art (X: 56, Y: 14 / 26)
        context.getMatrices().push();
        context.getMatrices().translate(x + 56, y + 14, 0);
        context.getMatrices().scale(0.8f, 0.8f, 1.0f);
        context.drawText(client.textRenderer, truncate(state.title, 16), 0, 0, color, false);
        context.getMatrices().pop();

        context.getMatrices().push();
        context.getMatrices().translate(x + 56, y + 26, 0);
        context.getMatrices().scale(0.6f, 0.6f, 1.0f);
        context.drawText(client.textRenderer, truncate(state.artist, 20), 0, 0, subColor, false);
        context.getMatrices().pop();

        // AirPlay elegant routing button on the top right
        drawSymbolicButton(context, x, y, "airplay", w - 24f, 22f, 16f, 8f, alpha, 1.0f, 0f, alpha);

        // Timeline Progress Section (Y: 60)
        float sliderX = 12f;
        float sliderW = w - 24f;
        float sliderY = 60f;

        // Swell & Scrub logic
        boolean isMouseOverSlider = rx >= sliderX && rx <= (sliderX + sliderW) && ry >= (sliderY - 4f) && ry <= (sliderY + 6f);
        
        long window = MinecraftClient.getInstance().getWindow().getHandle();
        boolean leftMouseDown = org.lwjgl.glfw.GLFW.glfwGetMouseButton(window, org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;

        if (isScrubbing) {
            if (!leftMouseDown) {
                isScrubbing = false;
                if (state.length > 0) {
                    LinuxMediaController.seek(scrubProgress * state.length);
                }
            } else {
                double frac = (rx - sliderX) / sliderW;
                double newScrubProgress = MathHelper.clamp(frac, 0.0, 1.0);
                if (newScrubProgress != scrubProgress) {
                    scrubProgress = newScrubProgress;
                    long now = System.currentTimeMillis();
                    if (now - lastRealTimeSeek > 150) {
                        lastRealTimeSeek = now;
                        if (state.length > 0) {
                            LinuxMediaController.seek(scrubProgress * state.length);
                        }
                    }
                }
            }
        } else {
            if (leftMouseDown && isMouseOverSlider) {
                isScrubbing = true;
                double frac = (rx - sliderX) / sliderW;
                scrubProgress = MathHelper.clamp(frac, 0.0, 1.0);
                if (state.length > 0) {
                    LinuxMediaController.seek(scrubProgress * state.length);
                }
                lastRealTimeSeek = System.currentTimeMillis();
            }
        }

        // Swell interpolation (swell timelines and knobs when scrubbing or hovered)
        boolean targetSwell = isScrubbing || isMouseOverSlider;
        scrubSwell = MathHelper.lerp(10.0f * renderDt, scrubSwell, targetSwell ? 1.0f : 0.0f);

        float trackHeight = MathHelper.lerp(scrubSwell, 2f, 4f);
        float currentSliderY = sliderY + 1f - trackHeight / 2f;

        // Velocity-based stretch and wobble calculation
        float velocity = 0.0f;
        if (isScrubbing) {
            velocity = (float) (rx - lastMouseX);
            velocity = MathHelper.clamp(velocity, -40f, 40f);
            wobblePhase += Math.abs(velocity) * 0.4f;
        }
        lastMouseX = rx;

        scrubVelocity = MathHelper.lerp(10.0f * renderDt, scrubVelocity, velocity);

        float knobH = MathHelper.lerp(scrubSwell, 3.0f, 6.0f);
        float knobW = knobH;
        float wobbleY = 0.0f;
        if (isScrubbing) {
            // Stretch horizontally based on scrubSwell and current velocity
            knobW = MathHelper.lerp(scrubSwell, 3.0f, 10.0f) + Math.abs(scrubVelocity) * 0.7f;
            // Compress vertically based on velocity
            knobH = MathHelper.lerp(scrubSwell, 3.0f, 5.0f) - Math.min(2.0f, Math.abs(scrubVelocity) * 0.2f);
            // Wobble up and down slightly using sine wave modulated by velocity
            wobbleY = (float) Math.sin(wobblePhase) * Math.min(2.5f, Math.abs(scrubVelocity) * 0.25f);
        }

        double pct = isScrubbing ? scrubProgress : ((state.length > 0) ? (state.position / state.length) : 0.0);
        pct = MathHelper.clamp(pct, 0.0, 1.0);

        // 1. Track Background Line (Rounded SDF rendering)
        int trackBgColor = ((int) (alpha * 45) << 24) | 0xFFFFFF;
        drawSdfBackground(context, x + sliderX, y + currentSliderY, sliderW, trackHeight, trackHeight / 2f, trackBgColor, 0.5f, alpha);

        // 2. Progress Fill Line (Rounded SDF rendering)
        if (pct > 0.0) {
            int fillCol = state.avgColor != 0 ? state.avgColor : 0xFF34C759;
            int progressColor = applyAlpha(fillCol, alpha);
            float fillW = (float) (sliderW * pct);
            drawSdfBackground(context, x + sliderX, y + currentSliderY, fillW, trackHeight, trackHeight / 2f, progressColor, 0.5f, alpha);
        }

        // 3. Progress Knob/Thumb dot (Rounded SDF pill rendering, stretching oval)
        float thumbX = sliderX + (float) (sliderW * pct);
        int knobColor = ((int) (alpha * 255) << 24) | 0xFFFFFF;
        drawSdfBackground(context, x + thumbX - knobW / 2f, y + sliderY + 1f - knobH / 2f + wobbleY, knobW, knobH, knobH / 2f, knobColor, 0.5f, alpha);

        // Timeline Stamps
        double currentPos = isScrubbing ? (scrubProgress * state.length) : state.position;
        String elapsedStr = formatTime(currentPos);
        String remainingStr = "-" + formatTime(Math.max(0.0, state.length - currentPos));

        context.getMatrices().push();
        context.getMatrices().translate(x + sliderX, y + 66f, 0);
        context.getMatrices().scale(0.5f, 0.5f, 1.0f);
        context.drawText(client.textRenderer, elapsedStr, 0, 0, subColor, false);
        context.getMatrices().pop();

        float remainingWidth = client.textRenderer.getWidth(remainingStr) * 0.5f;
        context.getMatrices().push();
        context.getMatrices().translate(x + w - sliderX - remainingWidth, y + 66f, 0);
        context.getMatrices().scale(0.5f, 0.5f, 1.0f);
        context.drawText(client.textRenderer, remainingStr, 0, 0, subColor, false);
        context.getMatrices().pop();
    }

    @Override
    public boolean mouseClicked(double rx, double ry, int button, float width, float height) {
        LinuxMediaController.MediaState state = LinuxMediaController.getCurrentState();
        if (state == null) return false;

        if (routingMenuProgress > 0.8f) {
            // 1. Check if clicked the Close button (top right)
            float airplayX = width - 24f;
            float airplayY = 18f;
            if (rx >= airplayX - 12f && rx <= airplayX + 12f && ry >= airplayY - 8f && ry <= airplayY + 8f) {
                System.out.println("GlassMenu Debug: clicked Close button to close routing menu");
                routingMenuOpen = false;
                return true;
            }
            
            // 2. Check if clicked a player item
            int idx = 0;
            for (LinuxMediaController.PlayerInfo info : LinuxMediaController.ALL_PLAYERS_INFO) {
                if (idx >= 3) break;
                float itemY = 32f + idx * 24f;
                if (rx >= 6 && rx <= width - 6 && ry >= itemY && ry <= itemY + 22f) {
                    System.out.println("GlassMenu Debug: selected player source: " + info.name);
                    LinuxMediaController.selectedPlayer = info.name;
                    new Thread(() -> {
                        try { Thread.sleep(50); } catch (InterruptedException ignored) {}
                        LinuxMediaController.poll();
                    }).start();
                    routingMenuOpen = false;
                    return true;
                }
                idx++;
            }
            
            if (rx >= 0 && rx <= width && ry >= 0 && ry <= height) {
                return true;
            }
            return false;
        }

        float minW = 75f;
        float minH = 20f;
        float wProgress = MathHelper.clamp((width - minW) / (190f - minW), 0.0f, 1.0f);
        float heightProgress = MathHelper.clamp((height - 30f) / (110f - 30f), 0.0f, 1.0f);
        float smoothProgress = heightProgress * heightProgress * (3 - 2 * heightProgress);

        // Check AirPlay button click to open routing menu
        if (currentMode == Mode.EXPANDED && !routingMenuOpen) {
            float airplayX = width - 24f;
            float airplayY = 22f;
            if (rx >= airplayX - 12f && rx <= airplayX + 12f && ry >= airplayY - 8f && ry <= airplayY + 8f) {
                System.out.println("GlassMenu Debug: clicked AirPlay button to open routing menu");
                routingMenuOpen = true;
                return true;
            }
        }

        float playX = MathHelper.lerp(smoothProgress, width - 36f, width / 2f);
        float playY = MathHelper.lerp(smoothProgress, height / 2f, 90f);
        float playSize = MathHelper.lerp(smoothProgress, 14f, 24f);

        float nextX = MathHelper.lerp(smoothProgress, width - 16f, width / 2f + 40f);
        float nextY = MathHelper.lerp(smoothProgress, height / 2f, 90f);
        float nextSize = MathHelper.lerp(smoothProgress, 14f, 20f);

        float prevX = MathHelper.lerp(smoothProgress, width - 36f, width / 2f - 40f);
        float prevY = MathHelper.lerp(smoothProgress, height / 2f, 90f);
        float prevSize = MathHelper.lerp(smoothProgress, 14f, 20f);

        // 1. Check Prev button click
        if (smoothProgress > 0.1f && rx >= prevX - prevSize/2f - 2f && rx <= prevX + prevSize/2f + 2f && ry >= prevY - prevSize/2f - 2f && ry <= prevY + prevSize/2f + 2f) {
            System.out.println("GlassMenu Debug: clicked Prev");
            prevScale = 0.8f;
            LinuxMediaController.previous();
            return true;
        }

        // 2. Check Play/Pause button click
        if (wProgress > 0.01f && rx >= playX - playSize/2f - 2f && rx <= playX + playSize/2f + 2f && ry >= playY - playSize/2f - 2f && ry <= playY + playSize/2f + 2f) {
            System.out.println("GlassMenu Debug: clicked Play/Pause");
            playScale = 0.8f;
            LinuxMediaController.playPause();
            return true;
        }

        // 3. Check Next button click
        if (wProgress > 0.01f && rx >= nextX - nextSize/2f - 2f && rx <= nextX + nextSize/2f + 2f && ry >= nextY - nextSize/2f - 2f && ry <= nextY + nextSize/2f + 2f) {
            System.out.println("GlassMenu Debug: clicked Next");
            nextScale = 0.8f;
            LinuxMediaController.next();
            return true;
        }

        // 4. Check Timeline Seek
        if (smoothProgress > 0.8f) {
            if (ry >= 56 && ry <= 64 && rx >= 12 && rx <= (width - 12)) {
                double frac = (rx - 12) / (width - 24);
                System.out.println("GlassMenu Debug: clicked timeline seek to frac=" + frac);
                if (state.length > 0) {
                    this.isScrubbing = true;
                    this.scrubProgress = frac;
                    LinuxMediaController.seek(frac * state.length);
                    this.lastRealTimeSeek = System.currentTimeMillis();
                    return true;
                }
            }
        }

        // 5. Check Capsule expanding / collapsing clicks
        if (rx >= 0 && rx <= width && ry >= 0 && ry <= height) {
            if (currentMode == Mode.COMPACT && button == 0) {
                System.out.println("GlassMenu Debug: clicked capsule to expand");
                currentMode = Mode.EXPANDED;
                return true;
            } else if (currentMode == Mode.EXPANDED && button == 1) {
                System.out.println("GlassMenu Debug: right-clicked capsule to collapse");
                currentMode = Mode.COMPACT;
                return true;
            }
        }

        return false;
    }

    private void drawSymbolicButton(DrawContext context, float x, float y, String iconName, float cx, float cy, float btnSize, float iconSize, float alpha, float scale, float hoverProgress, float widgetAlpha) {
        context.getMatrices().push();
        context.getMatrices().translate(x + cx, y + cy, 0);
        context.getMatrices().scale(scale, scale, 1.0f);

        // Hover/normal highlight: circular glass background highlight (only for play/pause or AirPlay buttons in expanded mode)
        boolean hasCircleBg = btnSize > 18f && ("media-playback-start-symbolic".equals(iconName) || "media-playback-pause-symbolic".equals(iconName));
        if (hasCircleBg) {
            float bgAlpha = MathHelper.lerp(hoverProgress, 0.12f, 0.28f);
            int bgCol = ((int) (alpha * bgAlpha * 255) << 24) | 0xFFFFFF;
            drawSdfBackground(context, -btnSize / 2f, -btnSize / 2f, btnSize, btnSize, btnSize * 0.5f, bgCol, 1.0f, widgetAlpha);
        }

        // Resolve texture
        Identifier texture = null;
        if ("media-seek-backward".equals(iconName)) {
            texture = ICON_PREV;
        } else if ("media-seek-forward".equals(iconName)) {
            texture = ICON_NEXT;
        } else if ("media-playback-start-symbolic".equals(iconName)) {
            texture = ICON_PLAY;
        } else if ("media-playback-pause-symbolic".equals(iconName)) {
            texture = ICON_PAUSE;
        } else if ("airplay".equals(iconName)) {
            texture = ICON_AIRPLAY;
        }

        if (texture != null) {
            int val = (int) MathHelper.lerp(hoverProgress, 200, 255);
            int iconColor = 0xFF000000 | (val << 16) | (val << 8) | val;
            drawIconTexture(context, texture, -iconSize / 2f, -iconSize / 2f, iconSize, alpha, iconColor);
        }

        context.getMatrices().pop();
    }

    private void drawIconTexture(DrawContext context, Identifier texture, float x, float y, float size, float alpha, int color) {
        if (texture == null) return;
        
        float a = (float) (color >> 24 & 255) / 255.0F * alpha;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        net.minecraft.client.gl.ShaderProgram originalShader = RenderSystem.getShader();
        int originalTex = com.example.glassmenu.render.RenderUtils.getTextureBinding2D(0);
        boolean originalBlend = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0, texture);
        
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        
        bufferBuilder.vertex(matrix, x, y, 0).texture(0, 0).color(r, g, b, a);
        bufferBuilder.vertex(matrix, x, y + size, 0).texture(0, 1).color(r, g, b, a);
        bufferBuilder.vertex(matrix, x + size, y + size, 0).texture(1, 1).color(r, g, b, a);
        bufferBuilder.vertex(matrix, x + size, y, 0).texture(1, 0).color(r, g, b, a);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());

        RenderSystem.setShader(() -> originalShader);
        RenderSystem.setShaderTexture(0, originalTex);
        if (!originalBlend) {
            RenderSystem.disableBlend();
        } else {
            RenderSystem.enableBlend();
        }
    }

    private int applyAlpha(int argb, float alpha) {
        int a = (int) (((argb >> 24) & 0xFF) * alpha);
        if (a == 0) a = (int) (255 * alpha);
        return (a << 24) | (argb & 0xFFFFFF);
    }



    private void drawSdfBackground(DrawContext context, float x, float y, float w, float h, float r, int argb, float edgeSoftness, float widgetAlpha) {
        var shader = !GlassMenuClient.CONFIG.enableShaders() ? null : ModShaders.getSdfRoundedRect();
        if (shader == null) {
            drawRoundedRectGeometry(context, x, y, w, h, r, argb, widgetAlpha);
            return;
        }

        net.minecraft.client.gl.ShaderProgram originalShader = RenderSystem.getShader();
        int originalTex = com.example.glassmenu.render.RenderUtils.getTextureBinding2D(0);
        boolean originalBlend = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND);

        float a = (float) (argb >> 24 & 255) / 255.0F * widgetAlpha;
        float red = (float) (argb >> 16 & 255) / 255.0F;
        float green = (float) (argb >> 8 & 255) / 255.0F;
        float blue = (float) (argb & 255) / 255.0F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(() -> shader);
        RenderSystem.setShaderTexture(0, com.example.glassmenu.render.RenderUtils.getWhiteTexture());

        if (shader.getUniform("Color") != null) shader.getUniform("Color").set(red, green, blue, a);
        if (shader.getUniform("Size") != null) shader.getUniform("Size").set(w, h);
        if (shader.getUniform("Radius") != null) shader.getUniform("Radius").set(r);
        if (shader.getUniform("EdgeSoftness") != null) shader.getUniform("EdgeSoftness").set(edgeSoftness);
        if (shader.getUniform("TexBounds") != null) shader.getUniform("TexBounds").set(0.0f, 0.0f, 1.0f, 1.0f);
        if (shader.getUniform("UseTexture") != null) shader.getUniform("UseTexture").set(0.0f);

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        bufferBuilder.vertex(matrix, x, y, 0).texture(0, 0);
        bufferBuilder.vertex(matrix, x, y + h, 0).texture(0, 1);
        bufferBuilder.vertex(matrix, x + w, y + h, 0).texture(1, 1);
        bufferBuilder.vertex(matrix, x + w, y, 0).texture(1, 0);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());

        RenderSystem.setShader(() -> originalShader);
        RenderSystem.setShaderTexture(0, originalTex);
        if (!originalBlend) {
            RenderSystem.disableBlend();
        } else {
            RenderSystem.enableBlend();
        }
    }

    private void drawRoundedTexture(DrawContext context, Identifier texture, float x, float y, float size, float r, float alpha, int tw, int th) {
        if (texture == null) return;
        var textureManager = MinecraftClient.getInstance().getTextureManager();
        if (textureManager.getTexture(texture) == null) return;

        net.minecraft.client.gl.ShaderProgram originalShader = RenderSystem.getShader();
        int originalTex = com.example.glassmenu.render.RenderUtils.getTextureBinding2D(0);
        boolean originalBlend = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND);

        var shader = !GlassMenuClient.CONFIG.enableShaders() ? null : ModShaders.getSdfRoundedRect();
        if (shader == null) {
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
            context.drawTexture(texture, (int) x, (int) y, 0, 0, (int) size, (int) size, (int) size, (int) size);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.setShader(() -> originalShader);
            RenderSystem.setShaderTexture(0, originalTex);
            if (!originalBlend) {
                RenderSystem.disableBlend();
            } else {
                RenderSystem.enableBlend();
            }
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(() -> shader);
        RenderSystem.setShaderTexture(0, texture);

        if (shader.getUniform("Color") != null) shader.getUniform("Color").set(1.0f, 1.0f, 1.0f, alpha);
        if (shader.getUniform("Size") != null) shader.getUniform("Size").set(size, size);
        if (shader.getUniform("Radius") != null) shader.getUniform("Radius").set(r);
        if (shader.getUniform("EdgeSoftness") != null) shader.getUniform("EdgeSoftness").set(1.5f);
        if (shader.getUniform("UseTexture") != null) shader.getUniform("UseTexture").set(1.0f);

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
        if (shader.getUniform("UseTexture") != null) shader.getUniform("UseTexture").set(0.0f);

        RenderSystem.setShader(() -> originalShader);
        RenderSystem.setShaderTexture(0, originalTex);
        if (!originalBlend) {
            RenderSystem.disableBlend();
        } else {
            RenderSystem.enableBlend();
        }
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

    private void renderRoutingMenu(DrawContext context, float x, float y, float w, float h, float widgetAlpha, float renderDt) {
        MinecraftClient client = MinecraftClient.getInstance();
        float p = routingMenuProgress;
        float alpha = p * widgetAlpha;
        
        // 1. Draw the expanding card "droplet"
        float cx = MathHelper.lerp(p, w - 24f, 6f);
        float cy = MathHelper.lerp(p, 22f, 6f);
        float cw = MathHelper.lerp(p, 16f, w - 12f);
        float ch = MathHelper.lerp(p, 8f, h - 12f);
        float cr = MathHelper.lerp(p, 4f, 10f);
        
        int cardFillColor = GlassMenuClient.CONFIG.transparentIsland() ? 0x2A000000 : 0xFF1C1C1E;
        int cardBorderColor = ((int)(alpha * 45) << 24) | 0xFFFFFF; // subtle white border outline
        
        drawSdfBackground(context, x + cx, y + cy, cw, ch, cr, cardFillColor, 1.0f, alpha);
        com.example.glassmenu.render.RenderUtils.drawSdfRoundedOutline(context.getMatrices(), x + cx, y + cy, cw, ch, cr, 0.5f, cardBorderColor);
        
        if (p < 0.8f) return; // Wait until mostly expanded to draw inner text and list items
        
        // 2. Draw Header
        context.getMatrices().push();
        context.getMatrices().translate(x + 16f, y + 15f, 0);
        context.getMatrices().scale(0.7f, 0.7f, 1.0f);
        context.drawText(client.textRenderer, "Select Source", 0, 0, ((int)(alpha * 200) << 24) | 0xFFFFFF, false);
        context.getMatrices().pop();
        
        // Draw small Close button on the top right
        float airplayX = w - 24f;
        float airplayY = 18f;
        context.getMatrices().push();
        context.getMatrices().translate(x + airplayX, y + airplayY, 0);
        context.getMatrices().scale(0.6f, 0.6f, 1.0f);
        context.drawText(client.textRenderer, "✖", -4, -4, ((int)(alpha * 180) << 24) | 0xFFFFFF, false);
        context.getMatrices().pop();
        
        // 3. Draw Rubber Highlight
        if (highlightY != -100f && !LinuxMediaController.ALL_PLAYERS_INFO.isEmpty()) {
            float stretch = Math.abs(highlightVelocity) * 0.04f;
            stretch = MathHelper.clamp(stretch, 0f, 10f);
            float drawY = highlightVelocity > 0 ? (highlightY - stretch) : highlightY;
            float drawH = 22f + stretch;
            
            int highlightColor = 0x22FFFFFF;
            if (!GlassMenuClient.CONFIG.transparentIsland()) {
                highlightColor = 0x33FFFFFF;
            }
            drawSdfBackground(context, x + 10f, y + drawY, w - 20f, drawH, 6f, highlightColor, 0.5f, alpha);
        }
        
        // 4. Draw Player Items
        int idx = 0;
        java.util.List<LinuxMediaController.PlayerInfo> players = LinuxMediaController.ALL_PLAYERS_INFO;
        if (players.isEmpty()) {
            context.getMatrices().push();
            context.getMatrices().translate(x + w / 2f, y + h / 2f - 4f, 0);
            context.getMatrices().scale(0.65f, 0.65f, 1.0f);
            String text = "No active sources";
            float tw = client.textRenderer.getWidth(text) * 0.65f;
            context.drawText(client.textRenderer, text, (int)(-tw / 2f / 0.65f), 0, ((int)(alpha * 120) << 24) | 0xCCCCCC, false);
            context.getMatrices().pop();
        } else {
            for (LinuxMediaController.PlayerInfo info : players) {
                if (idx >= 3) break;
                float itemY = 32f + idx * 24f;
                
                // Icon / Avatar of the song or fallback emoji
                if (info.artTexture != null) {
                    drawRoundedTexture(context, info.artTexture, x + 14f, y + itemY + 4f, 14f, 4f, alpha, info.artWidth, info.artHeight);
                } else {
                    String iconChar = info.name.toLowerCase().contains("spotify") ? "🎵" : "🌐";
                    context.getMatrices().push();
                    context.getMatrices().translate(x + 16f, y + itemY + 6f, 0);
                    context.getMatrices().scale(0.7f, 0.7f, 1.0f);
                    context.drawText(client.textRenderer, iconChar, 0, 0, ((int)(alpha * 255) << 24) | 0xFFFFFF, false);
                    context.getMatrices().pop();
                }
                
                // Player display name
                String displayName = info.name;
                if (displayName.contains(".")) {
                    displayName = displayName.substring(0, displayName.indexOf("."));
                }
                if (!displayName.isEmpty()) {
                    displayName = displayName.substring(0, 1).toUpperCase() + displayName.substring(1);
                }
                
                int nameColor = info.name.equals(LinuxMediaController.selectedPlayer) ? 0xFF34C759 : 0xFFFFFF;
                context.getMatrices().push();
                context.getMatrices().translate(x + 32f, y + itemY + 4f, 0);
                context.getMatrices().scale(0.7f, 0.7f, 1.0f);
                context.drawText(client.textRenderer, displayName, 0, 0, ((int)(alpha * 255) << 24) | (nameColor & 0xFFFFFF), false);
                context.getMatrices().pop();
                
                // Mini slide / progress bar
                float barX = 32f;
                float barW = w - 48f;
                float barY = itemY + 16f;
                
                double pct = info.length > 0 ? (info.position / info.length) : 0.0;
                pct = MathHelper.clamp(pct, 0.0, 1.0);
                
                // Draw progress background line
                int barBgCol = ((int)(alpha * 30) << 24) | 0xFFFFFF;
                drawSdfBackground(context, x + barX, y + barY, barW, 2f, 1f, barBgCol, 0.5f, alpha);
                
                // Draw progress fill line
                if (pct > 0.0) {
                    int fillColVal = info.isPlaying ? 0xFF34C759 : 0xFF8E8E93;
                    int barFillCol = ((int)(alpha * 255) << 24) | (fillColVal & 0xFFFFFF);
                    drawSdfBackground(context, x + barX, y + barY, (float)(barW * pct), 2f, 1f, barFillCol, 0.5f, alpha);
                }
                
                // Song track info
                String trackInfo = info.title;
                if (!info.artist.isEmpty() && !info.artist.equals("Unknown")) {
                    trackInfo += " - " + info.artist;
                }
                if (!trackInfo.isEmpty() && !trackInfo.equals("Unknown")) {
                    context.getMatrices().push();
                    context.getMatrices().translate(x + 95f, y + itemY + 5f, 0);
                    context.getMatrices().scale(0.55f, 0.55f, 1.0f);
                    context.drawText(client.textRenderer, truncate(trackInfo, 22), 0, 0, ((int)(alpha * 150) << 24) | 0xCCCCCC, false);
                    context.getMatrices().pop();
                }
                
                idx++;
            }
        }
    }

    private void drawRoundedRectGeometry(DrawContext context, float x, float y, float w, float h, float r, int argb, float widgetAlpha) {
        net.minecraft.client.gl.ShaderProgram originalShader = RenderSystem.getShader();
        int originalTex = com.example.glassmenu.render.RenderUtils.getTextureBinding2D(0);
        boolean originalBlend = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND);

        float a = (float) (argb >> 24 & 255) / 255.0F * widgetAlpha;
        float red = (float) (argb >> 16 & 255) / 255.0F;
        float green = (float) (argb >> 8 & 255) / 255.0F;
        float blue = (float) (argb & 255) / 255.0F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        r = Math.min(r, Math.min(w / 2f, h / 2f));
        if (r < 0.5f) {
            Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
            BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            bufferBuilder.vertex(matrix, x, y, 0).color(red, green, blue, a);
            bufferBuilder.vertex(matrix, x, y + h, 0).color(red, green, blue, a);
            bufferBuilder.vertex(matrix, x + w, y + h, 0).color(red, green, blue, a);
            bufferBuilder.vertex(matrix, x + w, y, 0).color(red, green, blue, a);
            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());

            RenderSystem.setShader(() -> originalShader);
            RenderSystem.setShaderTexture(0, originalTex);
            if (!originalBlend) {
                RenderSystem.disableBlend();
            } else {
                RenderSystem.enableBlend();
            }
            return;
        }

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);

        float centerX = x + w / 2f;
        float centerY = y + h / 2f;
        bufferBuilder.vertex(matrix, centerX, centerY, 0).color(red, green, blue, a);

        float cx1 = x + r, cy1 = y + r;
        float cx2 = x + w - r, cy2 = y + r;
        float cx3 = x + w - r, cy3 = y + h - r;
        float cx4 = x + r, cy4 = y + h - r;

        int segments = 8;

        for (int i = 0; i <= segments; i++) {
            double theta = Math.PI + (Math.PI / 2.0) * i / segments;
            float px = cx1 + (float) Math.cos(theta) * r;
            float py = cy1 + (float) Math.sin(theta) * r;
            bufferBuilder.vertex(matrix, px, py, 0).color(red, green, blue, a);
        }

        for (int i = 0; i <= segments; i++) {
            double theta = 1.5 * Math.PI + (Math.PI / 2.0) * i / segments;
            float px = cx2 + (float) Math.cos(theta) * r;
            float py = cy2 + (float) Math.sin(theta) * r;
            bufferBuilder.vertex(matrix, px, py, 0).color(red, green, blue, a);
        }

        for (int i = 0; i <= segments; i++) {
            double theta = (Math.PI / 2.0) * i / segments;
            float px = cx3 + (float) Math.cos(theta) * r;
            float py = cy3 + (float) Math.sin(theta) * r;
            bufferBuilder.vertex(matrix, px, py, 0).color(red, green, blue, a);
        }

        for (int i = 0; i <= segments; i++) {
            double theta = 0.5 * Math.PI + (Math.PI / 2.0) * i / segments;
            float px = cx4 + (float) Math.cos(theta) * r;
            float py = cy4 + (float) Math.sin(theta) * r;
            bufferBuilder.vertex(matrix, px, py, 0).color(red, green, blue, a);
        }

        bufferBuilder.vertex(matrix, cx1 - r, cy1, 0).color(red, green, blue, a);

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());

        RenderSystem.setShader(() -> originalShader);
        RenderSystem.setShaderTexture(0, originalTex);
        if (!originalBlend) {
            RenderSystem.disableBlend();
        } else {
            RenderSystem.enableBlend();
        }
    }
}
