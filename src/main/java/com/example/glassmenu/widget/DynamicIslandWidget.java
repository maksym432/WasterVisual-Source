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

    private static final float TENSION = 190f;
    private static final float FRICTION = 18f;

    private static float currentWidth = 100, targetWidth = 100, velocityWidth = 0;
    private static float currentHeight = 24, targetHeight = 24, velocityHeight = 0;
    private static float contentAlpha = 0;
    private static float widgetAlpha = 0;
    private static float smoothProgress = 0;

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
        
        float targetWidgetAlpha = hasMedia ? 1.0f : 0.0f;
        widgetAlpha = MathHelper.lerp(0.15f * delta, widgetAlpha, targetWidgetAlpha);
        
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

        float dt = delta / 20f;
        velocityWidth += (targetWidth - currentWidth) * TENSION * dt;
        velocityWidth *= (1 - FRICTION * dt);
        currentWidth += velocityWidth * dt;

        velocityHeight += (targetHeight - currentHeight) * TENSION * dt;
        velocityHeight *= (1 - FRICTION * dt);
        currentHeight += velocityHeight * dt;

        float morphProgress = (currentHeight - GlassMenuClient.CONFIG.capsuleHeight()) / (80f - GlassMenuClient.CONFIG.capsuleHeight());
        morphProgress = MathHelper.clamp(morphProgress, 0, 1);
        smoothProgress = morphProgress * morphProgress * (3 - 2 * morphProgress);

        contentAlpha = MathHelper.clamp((smoothProgress - 0.2f) / 0.8f, 0, 1) * widgetAlpha;

        prevScale = MathHelper.lerp(0.25f * delta, prevScale, 1.0f);
        playScale = MathHelper.lerp(0.25f * delta, playScale, 1.0f);
        nextScale = MathHelper.lerp(0.25f * delta, nextScale, 1.0f);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);

        // 1. Background
        drawSdfBackground(context, 0, 0, currentWidth, currentHeight, 11f, GlassMenuClient.CONFIG.capsuleColor());

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
        renderSynchronizedContent(context, state, contentAlpha, smoothProgress, mouseX - x, mouseY - y);
        
        // 4. EQUALIZER
        // Stays fixed on the right edge in compact, transitions only when expanding
        float eqAlpha = widgetAlpha * (state.isPlaying ? 1.0f : contentAlpha);
        
        // Horizontal: Stays relative to current width right edge
        float compactEqX = currentWidth - 30; 
        float expandedEqX = currentWidth - 50;
        float eqX = MathHelper.lerp(smoothProgress, compactEqX, expandedEqX);
        
        // Vertical: Fixed in compact (centered), rises in expanded
        float compactEqY = (GlassMenuClient.CONFIG.capsuleHeight() - 14) / 2f;
        float expandedEqY = 14;
        float eqY = MathHelper.lerp(smoothProgress, compactEqY, expandedEqY);
        
        float eqW = MathHelper.lerp(smoothProgress, 24, 32); 
        float eqH = MathHelper.lerp(smoothProgress, 14, 12); 
        
        drawAnimatedEqualizer(context, (int)eqX, (int)eqY, (int)eqW, (int)eqH, eqAlpha, smoothProgress);

        context.getMatrices().pop();
        RenderSystem.enableDepthTest();
    }

    private static void renderSynchronizedContent(DrawContext context, LinuxMediaController.MediaState state, float alpha, float progress, double rx, double ry) {
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

        // Controls - Appear as capsule grows
        float iconY = MathHelper.lerp(progress, GlassMenuClient.CONFIG.capsuleHeight(), 56);
        int spacing = 60; 
        int iconSize = 16; 
        float startX = (currentWidth - (spacing * 2 + iconSize)) / 2f;
        
        float hoverScale = 1.35f;
        prevHover = MathHelper.lerp(0.3f, prevHover, (rx >= startX - 8 && rx <= startX + iconSize + 8 && ry >= iconY - 8 && ry <= iconY + iconSize + 8) ? hoverScale : 1.0f);
        playHover = MathHelper.lerp(0.3f, playHover, (rx >= startX + spacing - 8 && rx <= startX + spacing + iconSize + 8 && ry >= iconY - 8 && ry <= iconY + iconSize + 8) ? hoverScale : 1.0f);
        nextHover = MathHelper.lerp(0.3f, nextHover, (rx >= startX + spacing * 2 - 8 && rx <= startX + spacing * 2 + iconSize + 8 && ry >= iconY - 8 && ry <= iconY + iconSize + 8) ? hoverScale : 1.0f);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        drawIconWithScale(context, ICON_PREV, startX, iconY, iconSize, alpha, prevScale * prevHover);
        drawIconWithScale(context, state.isPlaying ? ICON_PAUSE : ICON_PLAY, startX + spacing, iconY, iconSize, alpha, playScale * playHover);
        drawIconWithScale(context, ICON_NEXT, startX + (spacing * 2), iconY, iconSize, alpha, nextScale * nextHover);
        
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

    private static void drawAnimatedEqualizer(DrawContext context, int x, int y, int w, int h, float alpha, float progress) {
        int barCount = (int)MathHelper.lerp(progress, 6, 10);
        int barWidth = Math.max(1, w / barCount - 1);
        long time = System.currentTimeMillis();
        
        for (int i = 0; i < barCount; i++) {
            float freq = 0.2f + (i * 0.08f);
            float offset = i * 0.8f;
            float heightScale = (float) (Math.sin((time / 120.0) * freq + offset) * 0.5 + 0.5);
            int barHeight = (int) (h * heightScale);
            if (barHeight < 2) barHeight = 2;

            int barY = y + (h - barHeight);
            context.fill(x + (i * (barWidth + 1)), barY, x + (i * (barWidth + 1)) + barWidth, y + h, 
                ((int)(alpha * 255) << 24) | 0x33FF33);
        }
    }

    private static void drawSdfBackground(DrawContext context, float x, float y, float w, float h, float r, int argb) {
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
        if (shader.getUniform("EdgeSoftness") != null) shader.getUniform("EdgeSoftness").set(1.0f);
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
        if (shader.getUniform("EdgeSoftness") != null) shader.getUniform("EdgeSoftness").set(1.0f);

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
        float iconY = MathHelper.lerp(smoothProgress, GlassMenuClient.CONFIG.capsuleHeight(), 56);
        int spacing = 60; 
        int iconSize = 16;
        float startX = (currentWidth - (spacing * 2 + iconSize)) / 2f;
        
        if (ry >= iconY - 8 && ry <= iconY + iconSize + 8) {
            if (rx >= startX - 8 && rx <= startX + iconSize + 8) { 
                prevScale = 0.5f; LinuxMediaController.previous(); return true; 
            }
            if (rx >= startX + spacing - 8 && rx <= startX + spacing + iconSize + 8) { 
                playScale = 0.5f; LinuxMediaController.playPause(); return true; 
            }
            if (rx >= startX + (spacing * 2) - 8 && rx <= startX + (spacing * 2) + iconSize + 8) { 
                nextScale = 0.5f; LinuxMediaController.next(); return true; 
            }
        }
        return hovered;
    }
}
