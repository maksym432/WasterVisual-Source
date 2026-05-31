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

public class AtollDynamicIslandWidget {
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
    private static long lastTime = System.nanoTime();
    private static float animTime = 0f;

    private static boolean expanded = false;
    private static boolean hovered = false;

    // Interaction scales
    private static float prevScale = 1.0f, playScale = 1.0f, nextScale = 1.0f;
    private static float prevHover = 1.0f, playHover = 1.0f, nextHover = 1.0f;

    // Procedural Audio Visualizer state
    private static final float[] barHeights = new float[]{2f, 2f, 2f, 2f};
    private static final float[] targetHeights = new float[]{2f, 2f, 2f, 2f};
    private static float visTimer = 0f;
    
    // Album Art micro-animation state
    private static float artScale = 0.85f;

    public static void render(DrawContext context, int width, int height, int mouseX, int mouseY, float delta) {
        boolean inPauseMenu = MinecraftClient.getInstance().currentScreen instanceof net.minecraft.client.gui.screen.GameMenuScreen;
        if (IpadLockScreenManager.dismissed && !inPauseMenu) return;

        LinuxMediaController.MediaState state = LinuxMediaController.getCurrentState();
        boolean hasMedia = !state.title.isEmpty() && !"Unknown".equals(state.title);
        
        long now = System.nanoTime();
        float dt = (now - lastTime) / 1_000_000_000f;
        lastTime = now;
        if (dt < 0f) dt = 0f;
        dt = Math.min(dt, 0.1f);
        lastDt = dt;

        if (state.isPlaying) {
            animTime += dt;
        } else {
            animTime += dt * 0.2f;
        }

        // Procedural visualizer calculations
        visTimer += dt;
        if (visTimer >= 0.15f) {
            visTimer = 0f;
            if (state.isPlaying) {
                java.util.Random rand = new java.util.Random();
                targetHeights[0] = 3f + rand.nextFloat() * 9f;
                targetHeights[1] = 3f + rand.nextFloat() * 9f;
                targetHeights[2] = 3f + rand.nextFloat() * 9f;
                targetHeights[3] = 3f + rand.nextFloat() * 9f;
            } else {
                for (int i = 0; i < 4; i++) targetHeights[i] = 2.0f;
            }
        }
        for (int i = 0; i < 4; i++) {
            barHeights[i] = MathHelper.lerp(12f * dt, barHeights[i], targetHeights[i]);
        }

        // Album Art micro-animation (grow when playing, shrink when paused)
        float targetArtScale = state.isPlaying ? 1.0f : 0.85f;
        artScale = MathHelper.lerp(8f * dt, artScale, targetArtScale);

        float targetWidgetAlpha = hasMedia ? 1.0f : 0.0f;
        widgetAlpha = MathHelper.lerp(8.0f * dt, widgetAlpha, targetWidgetAlpha);
        
        if (widgetAlpha <= 0.01f) return;

        float x = (width - currentWidth) / 2f;
        float y = 2; 
        
        hovered = mouseX >= x && mouseX <= x + currentWidth && mouseY >= y && mouseY <= y + currentHeight;

        if (hovered) {
            expanded = true;
            targetWidth = Math.max(320, GlassMenuClient.CONFIG.expandedWidth() + 100); 
            targetHeight = 105; 
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

        float compactCapsuleH = GlassMenuClient.CONFIG.capsuleHeight();
        float morphProgress = (currentHeight - compactCapsuleH) / (105f - compactCapsuleH);
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

        // 1. Background (Opaque Solid Black - Copied exactly from Atoll)
        drawSdfBackground(context, 0, 0, currentWidth, currentHeight, 11f, 0xFF000000, 1.5f);

        // 3. MORPHING AVATAR / ALBUM ART
        float compactSize = 16;
        float expandedSize = 64;
        float size = MathHelper.lerp(smoothProgress, compactSize, expandedSize);
        
        float compactX = 4;
        float expandedX = 12;
        float posX = MathHelper.lerp(smoothProgress, compactX, expandedX);
        
        float compactY = (compactCapsuleH - compactSize) / 2f;
        float expandedY = 18;
        float posY = MathHelper.lerp(smoothProgress, compactY, expandedY);
        
        float radius = MathHelper.lerp(smoothProgress, size / 2f, 8f);

        context.getMatrices().push();
        // Smoothly lerp art scale factor so it transitions without jumping at boundary
        float currentArtScale = MathHelper.lerp(smoothProgress, 1.0f, artScale);
        float centerX = posX + size / 2f;
        float centerY = posY + size / 2f;
        context.getMatrices().translate(centerX, centerY, 0);
        context.getMatrices().scale(currentArtScale, currentArtScale, 1.0f);
        context.getMatrices().translate(-centerX, -centerY, 0);

        if (state.artTexture != null && state.artWidth > 0 && MinecraftClient.getInstance().getTextureManager().getTexture(state.artTexture) != null) {
            drawRoundedTexture(context, state.artTexture, posX, posY, size, radius, widgetAlpha, state.artWidth, state.artHeight);
        } else {
            drawSdfBackground(context, posX, posY, size, size, radius, 0xFF222222);
        }
        context.getMatrices().pop();

        // 4. ATOLL SYNCHRONIZED CONTENT (Text, Progress Slider, Elapsed/Remaining time, Controls)
        renderAtollContent(context, state, contentAlpha, smoothProgress, mouseX - x, mouseY - y, currentWidth);

        // 5. PROCEDURAL 4-BAR MUSIC VISUALIZER (Only when compact/closed notch, fades out as it expands)
        float visAlpha = (1.0f - smoothProgress) * widgetAlpha;
        if (visAlpha > 0.01f) {
            float rightMargin = MathHelper.lerp(smoothProgress, 4f, 12f);
            float vX = currentWidth - 14f - rightMargin;
            float vY = (currentHeight - 12f) / 2f;
            
            int barColor = ((int)(visAlpha * 255) << 24) | 0xFFFFFF;
            for (int i = 0; i < 4; i++) {
                float bx = vX + i * 4;
                float bh = barHeights[i];
                float by = vY + (12f - bh) / 2f;
                drawSdfBackground(context, bx, by, 2f, bh, 1f, barColor, 0.5f);
            }
        }

        context.getMatrices().pop();
        RenderSystem.enableDepthTest();
    }

    private static void renderAtollContent(DrawContext context, LinuxMediaController.MediaState state, float alpha, float progress, double rx, double ry, float currentWidth) {
        if (alpha <= 0.01f) return;
        
        MinecraftClient client = MinecraftClient.getInstance();
        int color = ((int)(alpha * 255) << 24) | 0xFFFFFF;
        int subColor = ((int)(alpha * 150) << 24) | 0xCCCCCC;

        // 1. Text Info (Title & Artist)
        float textX = 90;
        float titleY = 18;
        float artistY = 30;
        
        context.getMatrices().push();
        context.getMatrices().translate(textX, titleY, 0);
        context.getMatrices().scale(0.85f, 0.85f, 1.0f);
        context.drawText(client.textRenderer, truncate(state.title, 24), 0, 0, color, false);
        context.getMatrices().pop();

        context.getMatrices().push();
        context.getMatrices().translate(textX, artistY, 0);
        context.getMatrices().scale(0.7f, 0.7f, 1.0f);
        context.drawText(client.textRenderer, truncate(state.artist, 28), 0, 0, subColor, false);
        context.getMatrices().pop();

        // 2. Timeline Progress Bar
        float sliderX = 90;
        float sliderY = 48;
        float sliderW = currentWidth - sliderX - 16;
        float sliderH = 3;
        
        double pct = (state.length > 0) ? (state.position / state.length) : 0.0;
        pct = MathHelper.clamp(pct, 0.0, 1.0);
        
        // Gray background line
        drawSdfBackground(context, sliderX, sliderY, sliderW, sliderH, 1.5f, ((int)(alpha * 50) << 24) | 0xFFFFFF, 0.5f);
        
        // Progress filled line (tinted with average album color for premium aesthetics!)
        if (pct > 0.0) {
            int fillRGB = state.avgColor & 0xFFFFFF;
            int fillCol = ((int)(alpha * 220) << 24) | fillRGB;
            drawSdfBackground(context, sliderX, sliderY, (float)(sliderW * pct), sliderH, 1.5f, fillCol, 0.5f);
        }

        // Elapsed / Remaining Time Text
        String elapsedStr = formatTime(state.position);
        String remainingStr = "-" + formatTime(Math.max(0.0, state.length - state.position));
        
        context.getMatrices().push();
        context.getMatrices().translate(sliderX, 55, 0);
        context.getMatrices().scale(0.6f, 0.6f, 1.0f);
        context.drawText(client.textRenderer, elapsedStr, 0, 0, subColor, false);
        context.getMatrices().pop();

        float remainingWidth = client.textRenderer.getWidth(remainingStr) * 0.6f;
        context.getMatrices().push();
        context.getMatrices().translate(currentWidth - 16 - remainingWidth, 55, 0);
        context.getMatrices().scale(0.6f, 0.6f, 1.0f);
        context.drawText(client.textRenderer, remainingStr, 0, 0, subColor, false);
        context.getMatrices().pop();

        // 3. Playback Controls
        int playSize = 24;
        int skipSize = 20;
        float controlY = 72;
        
        float centerX = sliderX + sliderW / 2f;
        float prevX = centerX - 40 - skipSize / 2f;
        float playX = centerX - playSize / 2f;
        float nextX = centerX + 40 - skipSize / 2f;

        float hoverScale = 1.15f;
        prevHover = MathHelper.lerp(16.0f * lastDt, prevHover, (rx >= prevX - 6 && rx <= prevX + skipSize + 6 && ry >= controlY - 2 && ry <= controlY + skipSize + 6) ? hoverScale : 1.0f);
        playHover = MathHelper.lerp(16.0f * lastDt, playHover, (rx >= playX - 6 && rx <= playX + playSize + 6 && ry >= controlY - 6 && ry <= controlY + playSize + 6) ? hoverScale : 1.0f);
        nextHover = MathHelper.lerp(16.0f * lastDt, nextHover, (rx >= nextX - 6 && rx <= nextX + skipSize + 6 && ry >= controlY - 2 && ry <= controlY + skipSize + 6) ? hoverScale : 1.0f);

        float prevHoverProgress = MathHelper.clamp((prevHover - 1.0f) / 0.15f, 0.0f, 1.0f);
        float playHoverProgress = MathHelper.clamp((playHover - 1.0f) / 0.15f, 0.0f, 1.0f);
        float nextHoverProgress = MathHelper.clamp((nextHover - 1.0f) / 0.15f, 0.0f, 1.0f);

        drawCircularButton(context, ICON_PREV, prevX, controlY + (playSize - skipSize) / 2f, skipSize, 8, alpha, prevScale * prevHover, prevHoverProgress);
        drawCircularButton(context, state.isPlaying ? ICON_PAUSE : ICON_PLAY, playX, controlY, playSize, 12, alpha, playScale * playHover, playHoverProgress);
        drawCircularButton(context, ICON_NEXT, nextX, controlY + (playSize - skipSize) / 2f, skipSize, 8, alpha, nextScale * nextHover, nextHoverProgress);
        
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static String formatTime(double seconds) {
        int minutes = (int) (seconds / 60);
        int secs = (int) (seconds % 60);
        return String.format("%02d:%02d", minutes, secs);
    }

    private static void drawCircularButton(DrawContext context, Identifier texture, float x, float y, int btnSize, int iconSize, float alpha, float scale, float hoverProgress) {
        context.getMatrices().push();
        context.getMatrices().translate(x + btnSize / 2f, y + btnSize / 2f, 0);
        context.getMatrices().scale(scale, scale, 1.0f);
        
        // Hover background
        if (hoverProgress > 0.01f) {
            int bgCol = ((int)(alpha * 0.18f * hoverProgress * 255) << 24) | 0xFFFFFF;
            drawSdfBackground(context, -btnSize / 2f, -btnSize / 2f, btnSize, btnSize, btnSize * 0.32f, bgCol, 1.0f);
        }

        // Draw the premium texture icon instead of vector shapes
        float iconX = -iconSize / 2f;
        float iconY = -iconSize / 2f;
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        float brightness = MathHelper.lerp(hoverProgress, 0.85f, 1.0f);
        RenderSystem.setShaderColor(brightness, brightness, brightness, alpha);
        context.drawTexture(texture, (int)iconX, (int)iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        
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
        RenderSystem.lineWidth(1.2f);
        
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;
        
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        
        int segments = 48;
        for (int i = 0; i <= segments; i++) {
            float angle = i * (float) Math.PI * 2 / segments;
            float cos = MathHelper.cos(angle);
            float sin = MathHelper.sin(angle);
            bufferBuilder.vertex(matrix, cos * radius, sin * radius, 0).color(r, g, b, a);
        }
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.lineWidth(1.0f);
        RenderSystem.enableCull();
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

        int playSize = 24;
        int skipSize = 20;
        float controlY = 72;
        
        float sliderX = 90;
        float sliderW = currentWidth - sliderX - 16;
        
        float centerX = sliderX + sliderW / 2f;
        float prevX = centerX - 40 - skipSize / 2f;
        float playX = centerX - playSize / 2f;
        float nextX = centerX + 40 - skipSize / 2f;
        
        // Playback button clicks
        if (ry >= controlY - 6 && ry <= controlY + playSize + 6) {
            if (rx >= prevX - 6 && rx <= prevX + skipSize + 6) { 
                prevScale = 0.5f; LinuxMediaController.previous(); return true; 
            }
            if (rx >= playX - 6 && rx <= playX + playSize + 6) { 
                playScale = 0.5f; LinuxMediaController.playPause(); return true; 
            }
            if (rx >= nextX - 6 && rx <= nextX + skipSize + 6) { 
                nextScale = 0.5f; LinuxMediaController.next(); return true; 
            }
        }
        
        // Progress bar click to seek
        if (ry >= 44 && ry <= 53 && rx >= sliderX && rx <= sliderX + sliderW) {
            double frac = (rx - sliderX) / sliderW;
            LinuxMediaController.MediaState state = LinuxMediaController.getCurrentState();
            if (state.length > 0) {
                LinuxMediaController.seek(frac * state.length);
                return true;
            }
        }

        return hovered;
    }
}
