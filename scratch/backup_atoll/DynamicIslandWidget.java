/*
 * DynamicIslandWidget - Architecture & Primary Responsibility:
 * Dynamic Island UI Component.
 * Implements the morphing capsule widget with spring-based animations, 
 * media metadata display (using aspect-ratio corrected dynamic covers),
 * and interactive playback controls.
 */
package com.example.glassmenu.widget;

import com.example.glassmenu.GlassMenuClient;
import com.example.glassmenu.media.LinuxMediaController;
import com.example.glassmenu.shader.ModShaders;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

public class DynamicIslandWidget {
    private static final Identifier ICON_PLAY = Identifier.of("glassmenu", "textures/gui/ios_icons/play.png");
    private static final Identifier ICON_PAUSE = Identifier.of("glassmenu", "textures/gui/ios_icons/pause.png");
    private static final Identifier ICON_NEXT = Identifier.of("glassmenu", "textures/gui/ios_icons/next.png");
    private static final Identifier ICON_PREV = Identifier.of("glassmenu", "textures/gui/ios_icons/prev.png");
    private static final Identifier WHITE_TEX = Identifier.of("minecraft", "textures/misc/white.png");

    private static final float TENSION = 240f;
    private static final float FRICTION = 21f;

    private static float currentWidth = 100, targetWidth = 100, velocityWidth = 0;
    private static float currentHeight = 24, targetHeight = 24, velocityHeight = 0;
    private static float contentAlpha = 0;
    private static float widgetAlpha = 0;
    private static float smoothProgress = 0;
    private static float lastDt = 0.016f;
    private static long lastTime = System.currentTimeMillis();
    private static float animTime = 0f;

    private static boolean expanded = false;
    private static boolean hovered = false;

    // Interaction scales
    private static float prevScale = 1.0f, playScale = 1.0f, nextScale = 1.0f;
    private static float prevHover = 1.0f, playHover = 1.0f, nextHover = 1.0f;

    public static void render(DrawContext context, int width, int height, int mouseX, int mouseY, float delta) {
        boolean inPauseMenu = MinecraftClient.getInstance().currentScreen instanceof net.minecraft.client.gui.screen.GameMenuScreen;
        if (IpadLockScreenManager.dismissed && !inPauseMenu) return;

        LinuxMediaController.MediaState state = LinuxMediaController.getCurrentState();
        boolean hasMedia = !state.title.isEmpty() && !"Unknown".equals(state.title);
        
        long now = System.currentTimeMillis();
        float dt = (now - lastTime) / 1000f;
        lastTime = now;
        if (dt < 0f) dt = 0f;
        dt = Math.min(dt, 0.1f);
        lastDt = dt;

        if (state.isPlaying) {
            animTime += dt;
        } else {
            animTime += dt * 0.2f;
        }

        float targetWidgetAlpha = hasMedia ? 1.0f : 0.0f;
        widgetAlpha = MathHelper.lerp(8.0f * dt, widgetAlpha, targetWidgetAlpha);
        
        if (widgetAlpha <= 0.01f) return;

        float x = (width - currentWidth) / 2f;
        float y = 2; 
        
        hovered = mouseX >= x && mouseX <= x + currentWidth && mouseY >= y && mouseY <= y + currentHeight;

        if (hovered) {
            expanded = true;
            targetWidth = GlassMenuClient.CONFIG.expandedWidth() - 20; 
            targetHeight = 80; 
        } else {
            expanded = false;
            targetWidth = GlassMenuClient.CONFIG.capsuleWidth(); 
            targetHeight = GlassMenuClient.CONFIG.capsuleHeight();
        }

        float remainingDt = dt;
        float step = 0.002f;
        while (remainingDt > 0.0001f) {
            float substep = Math.min(remainingDt, step);
            
            // Width spring
            float forceWidth = (targetWidth - currentWidth) * TENSION;
            velocityWidth += forceWidth * substep;
            velocityWidth *= (float) Math.exp(-FRICTION * substep);
            currentWidth += velocityWidth * substep;

            // Height spring
            float forceHeight = (targetHeight - currentHeight) * TENSION;
            velocityHeight += forceHeight * substep;
            velocityHeight *= (float) Math.exp(-FRICTION * substep);
            currentHeight += velocityHeight * substep;

            remainingDt -= substep;
        }

        float morphProgress = (currentHeight - GlassMenuClient.CONFIG.capsuleHeight()) / (80f - GlassMenuClient.CONFIG.capsuleHeight());
        morphProgress = MathHelper.clamp(morphProgress, 0, 1);
        smoothProgress = morphProgress * morphProgress * (3 - 2 * morphProgress);

        contentAlpha = MathHelper.clamp((smoothProgress - 0.2f) / 0.8f, 0, 1) * widgetAlpha;

        prevScale = MathHelper.lerp(10.0f * dt, prevScale, 1.0f);
        playScale = MathHelper.lerp(10.0f * dt, playScale, 1.0f);
        nextScale = MathHelper.lerp(10.0f * dt, nextScale, 1.0f);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);

        // 1. Background (with 1px glass border highlight)
        drawSdfBackground(context, -0.7f, -0.7f, currentWidth + 1.4f, currentHeight + 1.4f, 11.7f, 0x2AFFFFFF, 1.5f);
        drawSdfBackground(context, 0, 0, currentWidth, currentHeight, 11f, GlassMenuClient.CONFIG.capsuleColor(), 1.5f);

        // 2. MORPHING AVATAR
        float compactSize = 16;
        float expandedSize = 34;
        float size = MathHelper.lerp(smoothProgress, compactSize, expandedSize);
        
        float compactX = 4;
        float expandedX = 12;
        float posX = MathHelper.lerp(smoothProgress, compactX, expandedX);
        
        float compactY = (GlassMenuClient.CONFIG.capsuleHeight() - compactSize) / 2f;
        float expandedY = 12;
        float posY = MathHelper.lerp(smoothProgress, compactY, expandedY);

        
        float radius = MathHelper.lerp(smoothProgress, size / 2f, 8f);

        if (state.artTexture != null && state.artWidth > 0 && MinecraftClient.getInstance().getTextureManager().getTexture(state.artTexture) != null) {
            drawRoundedTexture(context, state.artTexture, posX, posY, size, radius, widgetAlpha, state.artWidth, state.artHeight);
        } else {
            drawSdfBackground(context, posX, posY, size, size, radius, 0xFF222222);
        }

        // 3. SYNCHRONIZED CONTENT (Text & Buttons)
        renderSynchronizedContent(context, state, contentAlpha, smoothProgress, mouseX - x, mouseY - y, currentWidth);

        // 4. PARTICLES / VINES (Replacing Equalizer)
        float eqAlpha = state.isPlaying ? (expanded ? contentAlpha : widgetAlpha) : 0.0f;
        
        // Horizontal: Stays relative to current width right edge
        float compactEqX = currentWidth - 30; 
        float expandedEqX = currentWidth - 36;
        float eqX = MathHelper.lerp(smoothProgress, compactEqX, expandedEqX);
        
        // Vertical: Fixed in compact (centered), rises in expanded
        float compactEqY = (GlassMenuClient.CONFIG.capsuleHeight() - 14) / 2f;
        float expandedEqY = 18;
        float eqY = MathHelper.lerp(smoothProgress, compactEqY, expandedEqY);
        
        float eqW = MathHelper.lerp(smoothProgress, 24, 28); 
        float eqH = MathHelper.lerp(smoothProgress, 14, 16); 
        
        drawVinesParticles(context, eqX, eqY, eqW, eqH, eqAlpha, smoothProgress);

        context.getMatrices().pop();
        RenderSystem.enableDepthTest();
    }

    private static void renderSynchronizedContent(DrawContext context, LinuxMediaController.MediaState state, float alpha, float progress, double rx, double ry, float currentWidth) {
        if (alpha <= 0.01f) return;
        
        MinecraftClient client = MinecraftClient.getInstance();
        int color = ((int)(alpha * 255) << 24) | 0xFFFFFF;
        int subColor = ((int)(alpha * 160) << 24) | 0xBBBBBB;

        // Text reveal - Emerges FROM the compact capsule vertical center
        float compactCenterY = GlassMenuClient.CONFIG.capsuleHeight() / 2f - 8;
        float expandedY = 16;
        float textY = MathHelper.lerp(progress, compactCenterY, expandedY);
        
        float textX = MathHelper.lerp(progress, 30, 52);
        float textScale = MathHelper.lerp(progress, 0.4f, 0.85f); // Grows from smaller
        
        context.getMatrices().push();
        context.getMatrices().translate(textX, textY, 0);
        context.getMatrices().scale(textScale, textScale, 1.0f);
        context.drawText(client.textRenderer, truncate(state.title, 18), 0, 0, color, false);
        context.drawText(client.textRenderer, truncate(state.artist, 22), 0, 11, subColor, false);
        context.getMatrices().pop();

        int btnSize = 20;
        int iconSize = 10;
        float iconY = MathHelper.lerp(progress, GlassMenuClient.CONFIG.capsuleHeight() - 20, 52);
        
        // Responsive positioning relative to the capsule center width
        float spacing = 35f; 
        float centerX = currentWidth / 2f - 4f;
        float prevX = centerX - spacing - btnSize / 2f;
        float playX = centerX - btnSize / 2f;
        float nextX = centerX + spacing - btnSize / 2f;
        
        float hoverScale = 1.15f;
        prevHover = MathHelper.lerp(16.0f * lastDt, prevHover, (rx >= prevX - 8 && rx <= prevX + btnSize + 8 && ry >= iconY - 8 && ry <= iconY + btnSize + 8) ? hoverScale : 1.0f);
        playHover = MathHelper.lerp(16.0f * lastDt, playHover, (rx >= playX - 8 && rx <= playX + btnSize + 8 && ry >= iconY - 8 && ry <= iconY + btnSize + 8) ? hoverScale : 1.0f);
        nextHover = MathHelper.lerp(16.0f * lastDt, nextHover, (rx >= nextX - 8 && rx <= nextX + btnSize + 8 && ry >= iconY - 8 && ry <= iconY + btnSize + 8) ? hoverScale : 1.0f);

        float prevHoverProgress = MathHelper.clamp((prevHover - 1.0f) / 0.15f, 0.0f, 1.0f);
        float playHoverProgress = MathHelper.clamp((playHover - 1.0f) / 0.15f, 0.0f, 1.0f);
        float nextHoverProgress = MathHelper.clamp((nextHover - 1.0f) / 0.15f, 0.0f, 1.0f);

        drawCircularButton(context, ICON_PREV, prevX, iconY, btnSize, iconSize, alpha, prevScale * prevHover, prevHoverProgress);
        drawCircularButton(context, state.isPlaying ? ICON_PAUSE : ICON_PLAY, playX, iconY, btnSize, iconSize, alpha, playScale * playHover, playHoverProgress);
        drawCircularButton(context, ICON_NEXT, nextX, iconY, btnSize, iconSize, alpha, nextScale * nextHover, nextHoverProgress);
        
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static void drawIconWithScale(DrawContext context, Identifier texture, float x, float y, int size, float alpha, float scale) {
        context.getMatrices().push();
        context.getMatrices().translate(x + size / 2f, y + size / 2f, 0);
        context.getMatrices().scale(scale, scale, 1.0f);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
        context.drawTexture(texture, -size / 2, -size / 2, 0, 0, size, size, size, size);
        context.getMatrices().pop();
    }

    private static void drawCircularButton(DrawContext context, Identifier texture, float x, float y, int btnSize, int iconSize, float alpha, float scale, float hoverProgress) {
        context.getMatrices().push();
        context.getMatrices().translate(x + btnSize / 2f, y + btnSize / 2f, 0);
        context.getMatrices().scale(scale, scale, 1.0f);
        
        // 1. Draw circular glass background outline (white circle around, nothing inside)
        int circleColor = interpolateColor(0x28FFFFFF, 0xA0FFFFFF, hoverProgress);
        int aVal = (int) (((circleColor >> 24) & 0xFF) * alpha);
        int finalCircleColor = (aVal << 24) | (circleColor & 0xFFFFFF);
        
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        drawHollowCircle(matrix, btnSize / 2f, finalCircleColor);

        // 2. Render custom vector shapes for the icons (gray/white transitions)
        int rVal = (int) MathHelper.lerp(hoverProgress, 200, 255);
        int gVal = (int) MathHelper.lerp(hoverProgress, 200, 255);
        int bVal = (int) MathHelper.lerp(hoverProgress, 200, 255);
        int iconAlphaVal = (int) (alpha * 255);
        int iconColor = (iconAlphaVal << 24) | (rVal << 16) | (gVal << 8) | bVal;

        String path = texture.getPath();
        if (path.contains("prev")) {
            // Draw double arrow pointing left (using clockwise winding to prevent backface culling)
            drawTriangle(matrix, 2.5f, 3.5f, 2.5f, -3.5f, -0.5f, 0f, iconColor);
            drawTriangle(matrix, -0.5f, 3.5f, -0.5f, -3.5f, -3.5f, 0f, iconColor);
        } else if (path.contains("next")) {
            // Draw double arrow pointing right
            drawTriangle(matrix, -2.5f, -3.5f, -2.5f, 3.5f, 0.5f, 0f, iconColor);
            drawTriangle(matrix, 0.5f, -3.5f, 0.5f, 3.5f, 3.5f, 0f, iconColor);
        } else if (path.contains("play")) {
            // Draw single triangle pointing right (visually centered)
            drawTriangle(matrix, -2.0f, -3.5f, -2.0f, 3.5f, 4.0f, 0f, iconColor);
        } else if (path.contains("pause")) {
            // Draw two rounded vertical bars (capsules) using our SDF rounded rect shader for smooth edges
            drawSdfBackground(context, -2.5f, -3.5f, 1.5f, 7.0f, 0.75f, iconColor, 1.5f);
            drawSdfBackground(context, 1.0f, -3.5f, 1.5f, 7.0f, 0.75f, iconColor, 1.5f);
        }
        
        context.getMatrices().pop();
    }

    private static int interpolateColor(int c1, int c2, float p) {
        int a = (int) MathHelper.lerp(p, (c1 >> 24) & 0xFF, (c2 >> 24) & 0xFF);
        int r = (int) MathHelper.lerp(p, (c1 >> 16) & 0xFF, (c2 >> 16) & 0xFF);
        int g = (int) MathHelper.lerp(p, (c1 >> 8) & 0xFF, (c2 >> 8) & 0xFF);
        int b = (int) MathHelper.lerp(p, c1 & 0xFF, c2 & 0xFF);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static void drawHollowCircle(Matrix4f matrix, float radius, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.lineWidth(1.2f); // Ultra clean thin line outline
        
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;
        
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        
        int segments = 48; // Smooth circle
        for (int i = 0; i <= segments; i++) {
            float angle = i * (float) Math.PI * 2 / segments;
            float cos = MathHelper.cos(angle);
            float sin = MathHelper.sin(angle);
            bufferBuilder.vertex(matrix, cos * radius, sin * radius, 0).color(r, g, b, a);
        }
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.lineWidth(1.0f); // Reset
        RenderSystem.enableCull();
    }

    private static void drawTriangle(Matrix4f matrix, float x1, float y1, float x2, float y2, float x3, float y3, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull(); // Disable culling to ensure vector shapes render regardless of matrix projections
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
        RenderSystem.enableCull(); // Restore culling state
    }

    private static void drawVinesParticles(DrawContext context, float eqX, float eqY, float eqW, float eqH, float alpha, float progress) {
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
                
                drawSdfBackground(context, px - pSize / 2f, py - pSize / 2f, pSize, pSize, pSize / 2f, finalColor, 0.5f);
            }
        }
    }

    private static void drawSdfBackground(DrawContext context, float x, float y, float w, float h, float r, int argb) {
        drawSdfBackground(context, x, y, w, h, r, argb, 1.0f);
    }

    private static void drawSdfBackground(DrawContext context, float x, float y, float w, float h, float r, int argb, float edgeSoftness) {
        var shader = !GlassMenuClient.CONFIG.enableShaders() ? null : ModShaders.getSdfRoundedRect();
        if (shader == null) {
            float a = (float) (argb >> 24 & 255) / 255.0F * widgetAlpha;
            int alphaVal = (int)(a * 255);
            int color = (alphaVal << 24) | (argb & 0xFFFFFF);
            context.fill((int)x, (int)y, (int)(x + w), (int)(y + h), color);
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

    private static void drawRoundedTexture(DrawContext context, Identifier texture, float x, float y, float size, float r, float alpha, int tw, int th) {
        if (texture == null) return;
        var textureManager = MinecraftClient.getInstance().getTextureManager();
        if (textureManager.getTexture(texture) == null) return;

        var shader = !GlassMenuClient.CONFIG.enableShaders() ? null : ModShaders.getSdfRoundedRect();
        if (shader == null) {
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
            context.drawTexture(texture, (int)x, (int)y, 0, 0, (int)size, (int)size, (int)size, (int)size);
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

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 3) + "..." : s;
    }

    public static boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean inPauseMenu = MinecraftClient.getInstance().currentScreen instanceof net.minecraft.client.gui.screen.GameMenuScreen;
        if (IpadLockScreenManager.dismissed && !inPauseMenu) return false;
        if (!expanded || contentAlpha < 0.8f) return false;
        
        float x = (MinecraftClient.getInstance().getWindow().getScaledWidth() - currentWidth) / 2f;
        float y = 2;
        double rx = mouseX - x;
        double ry = mouseY - y;

        // Dynamic coordinate calculation
        int btnSize = 20;
        float iconY = MathHelper.lerp(smoothProgress, GlassMenuClient.CONFIG.capsuleHeight() - 20, 52);
        
        float spacing = 35f;
        float centerX = currentWidth / 2f - 4f;
        float prevX = centerX - spacing - btnSize / 2f;
        float playX = centerX - btnSize / 2f;
        float nextX = centerX + spacing - btnSize / 2f;
        
        if (ry >= iconY - 8 && ry <= iconY + btnSize + 8) {
            if (rx >= prevX - 8 && rx <= prevX + btnSize + 8) { 
                prevScale = 0.5f; LinuxMediaController.previous(); return true; 
            }
            if (rx >= playX - 8 && rx <= playX + btnSize + 8) { 
                playScale = 0.5f; LinuxMediaController.playPause(); return true; 
            }
            if (rx >= nextX - 8 && rx <= nextX + btnSize + 8) { 
                nextScale = 0.5f; LinuxMediaController.next(); return true; 
            }
        }
        return hovered;
    }
}
