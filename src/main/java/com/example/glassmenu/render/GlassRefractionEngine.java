/*
 * GlassRefractionEngine - Architecture & Primary Responsibility:
 * Safe Glass Refraction Rendering Engine.
 * Copy regions of the main framebuffer color texture into a temporary texture
 * on the GPU before binding it to prevent OpenGL Texture Feedback Loops.
 * Renders the copied buffer with a refraction distortion shader.
 */
package com.example.glassmenu.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.example.glassmenu.shader.ModShaders;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.util.Util;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;

public class GlassRefractionEngine {
    private static int temporaryTextureId = -1;
    private static int lastWidth = -1;
    private static int lastHeight = -1;

    public static void drawRefractedPanel(DrawContext context, int x, int y, int width, int height, float strength, int color, float radius) {
        drawRefractedPanel(context, x, y, width, height, (float) x, (float) y, (float) width, (float) height, strength, color, radius);
    }

    public static void drawRefractedPanel(DrawContext context, 
                                          int copyX, int copyY, int copyWidth, int copyHeight,
                                          float drawX, float drawY, float drawWidth, float drawHeight,
                                          float strength, int color, float radius) {
        if (strength <= 0.0f) return;

        MinecraftClient client = MinecraftClient.getInstance();
        ShaderProgram shader = ModShaders.getGlassRefraction();
        if (shader == null) return;

        if (client.getWindow() == null) return;
        int fbWidth = client.getWindow().getFramebufferWidth();
        int fbHeight = client.getWindow().getFramebufferHeight();
        if (fbWidth <= 0 || fbHeight <= 0) return;

        float scaledW = client.getWindow().getScaledWidth();
        float scaledH = client.getWindow().getScaledHeight();
        if (scaledW <= 0 || scaledH <= 0) return;

        // 1. Lazy initialization / updating of texture matching the current window framebuffer size
        if (temporaryTextureId == -1 || lastWidth != fbWidth || lastHeight != fbHeight) {
            if (temporaryTextureId != -1) {
                GlStateManager._deleteTexture(temporaryTextureId);
            }
            int activeTexture = com.mojang.blaze3d.platform.GlStateManager._getActiveTexture();
            int previousTexture = com.example.glassmenu.render.RenderUtils.getTextureBinding2D(0);
            com.mojang.blaze3d.platform.GlStateManager._activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);

            temporaryTextureId = GlStateManager._genTexture();
            GlStateManager._bindTexture(temporaryTextureId);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, fbWidth, fbHeight, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);

            com.mojang.blaze3d.platform.GlStateManager._bindTexture(previousTexture);
            com.mojang.blaze3d.platform.GlStateManager._activeTexture(activeTexture);

            lastWidth = fbWidth;
            lastHeight = fbHeight;
        }

        // Calculate region boundaries from GUI coordinates to physical window space
        int pCopyX = Math.max(0, (int) ((copyX / scaledW) * fbWidth));
        int pCopyY = Math.max(0, (int) (((scaledH - (copyY + copyHeight)) / scaledH) * fbHeight));
        int pCopyW = Math.min(fbWidth - pCopyX, (int) ((copyWidth / scaledW) * fbWidth));
        int pCopyH = Math.min(fbHeight - pCopyY, (int) ((copyHeight / scaledH) * fbHeight));

        if (pCopyW <= 0 || pCopyH <= 0) return;

        context.draw(); // Flush buffered UI elements first

        int activeTexture = com.mojang.blaze3d.platform.GlStateManager._getActiveTexture();
        int previousTexture = com.example.glassmenu.render.RenderUtils.getTextureBinding2D(0);
        com.mojang.blaze3d.platform.GlStateManager._activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
        com.mojang.blaze3d.platform.GlStateManager._bindTexture(temporaryTextureId);
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, pCopyX, pCopyY, pCopyX, pCopyY, pCopyW, pCopyH);
        com.mojang.blaze3d.platform.GlStateManager._bindTexture(previousTexture);
        com.mojang.blaze3d.platform.GlStateManager._activeTexture(activeTexture);

        // 3. Configure rendering states and uniforms
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        ShaderProgram previousShader = RenderSystem.getShader();
        RenderSystem.setShader(() -> shader);

        if (shader.getUniform("Time") != null) {
            shader.getUniform("Time").set((Util.getMeasuringTimeMs() % 100000L) / 1000.0f);
        }
        if (shader.getUniform("DistortionStrength") != null) {
            shader.getUniform("DistortionStrength").set(strength);
        }
        if (shader.getUniform("ScreenSize") != null) {
            shader.getUniform("ScreenSize").set((float) fbWidth, (float) fbHeight);
        }
        if (shader.getUniform("Size") != null) {
            shader.getUniform("Size").set(drawWidth, drawHeight);
        }
        if (shader.getUniform("Radius") != null) {
            shader.getUniform("Radius").set(radius);
        }
        if (shader.getUniform("Color") != null) {
            float a = (float) (color >> 24 & 255) / 255.0F;
            float r = (float) (color >> 16 & 255) / 255.0F;
            float g = (float) (color >> 8 & 255) / 255.0F;
            float b = (float) (color & 255) / 255.0F;
            shader.getUniform("Color").set(r, g, b, a);
        }
        if (shader.getUniform("TexBounds") != null) {
            float u1 = (float) copyX / scaledW;
            float v1 = 1.0f - ((float) copyY / scaledH);
            float u2 = ((float) copyX + copyWidth) / scaledW;
            float v2 = 1.0f - (((float) copyY + copyHeight) / scaledH);
            shader.getUniform("TexBounds").set(u1, v1, u2, v2);
        }

        RenderSystem.setShaderTexture(0, temporaryTextureId);

        // 4. Draw refracted panel quad
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        float dx2 = drawX + drawWidth;
        float dy2 = drawY + drawHeight;

        buffer.vertex(matrix, drawX, dy2, 0).texture(0.0f, 1.0f);
        buffer.vertex(matrix, dx2, dy2, 0).texture(1.0f, 1.0f);
        buffer.vertex(matrix, dx2, drawY, 0).texture(1.0f, 0.0f);
        buffer.vertex(matrix, drawX, drawY, 0).texture(0.0f, 0.0f);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.setShader(() -> previousShader);
        RenderSystem.setShaderTexture(0, previousTexture);
    }

    public static void cleanup() {
        if (temporaryTextureId != -1) {
            GlStateManager._deleteTexture(temporaryTextureId);
            temporaryTextureId = -1;
            RenderSystem.setShaderTexture(0, 0);
        }
    }
}
