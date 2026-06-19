package com.example.glassmenu.render;

import com.example.glassmenu.GlassMenuClient;
import com.example.glassmenu.shader.ModShaders;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.*;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import com.mojang.blaze3d.systems.VertexSorter;

import java.nio.ByteBuffer;

public class ColorGradingEngine {
    private static int temporaryTextureId = -1;
    private static int lastWidth = -1;
    private static int lastHeight = -1;

    public static void renderColorGrading() {
        if (!GlassMenuClient.CONFIG.enableColorGrading()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        ShaderProgram shader = ModShaders.getColorGrading();
        if (shader == null) return;

        if (client.getWindow() == null) return;
        int fbWidth = client.getWindow().getFramebufferWidth();
        int fbHeight = client.getWindow().getFramebufferHeight();
        if (fbWidth <= 0 || fbHeight <= 0) return;

        float scaledW = client.getWindow().getScaledWidth();
        float scaledH = client.getWindow().getScaledHeight();
        if (scaledW <= 0 || scaledH <= 0) return;

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

        // Copy screen
        int activeTexture = com.mojang.blaze3d.platform.GlStateManager._getActiveTexture();
        int previousTexture = com.example.glassmenu.render.RenderUtils.getTextureBinding2D(0);
        com.mojang.blaze3d.platform.GlStateManager._activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
        com.mojang.blaze3d.platform.GlStateManager._bindTexture(temporaryTextureId);
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, fbWidth, fbHeight);
        com.mojang.blaze3d.platform.GlStateManager._bindTexture(previousTexture);
        com.mojang.blaze3d.platform.GlStateManager._activeTexture(activeTexture);

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();

        ShaderProgram previousShader = RenderSystem.getShader();
        RenderSystem.setShader(() -> shader);

        if (shader.getUniform("Saturation") != null) {
            shader.getUniform("Saturation").set(GlassMenuClient.CONFIG.cgSaturation());
        }
        if (shader.getUniform("Contrast") != null) {
            shader.getUniform("Contrast").set(GlassMenuClient.CONFIG.cgContrast());
        }
        if (shader.getUniform("ColorTint") != null) {
            int color = GlassMenuClient.CONFIG.cgTint();
            float r = (float) ((color >> 16) & 0xFF) / 255.0f;
            float g = (float) ((color >> 8) & 0xFF) / 255.0f;
            float b = (float) (color & 0xFF) / 255.0f;
            shader.getUniform("ColorTint").set(r, g, b);
        }

        RenderSystem.setShaderTexture(0, temporaryTextureId);

        // Draw quad
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        
        // Use an identity projection for a screen quad spanning -1 to 1 or 0 to scaledW/H
        Matrix4f prevProj = RenderSystem.getProjectionMatrix();
        Matrix4f proj = new Matrix4f().setOrtho(0.0f, scaledW, scaledH, 0.0f, 1000.0f, 3000.0f);
        RenderSystem.setProjectionMatrix(proj, VertexSorter.BY_Z);
        Matrix4f pos = new Matrix4f().translation(0.0f, 0.0f, -2000.0f);

        buffer.vertex(pos, 0, scaledH, 0).texture(0.0f, 0.0f);
        buffer.vertex(pos, scaledW, scaledH, 0).texture(1.0f, 0.0f);
        buffer.vertex(pos, scaledW, 0, 0).texture(1.0f, 1.0f);
        buffer.vertex(pos, 0, 0, 0).texture(0.0f, 1.0f);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.setProjectionMatrix(prevProj, VertexSorter.BY_Z);
        RenderSystem.setShader(() -> previousShader);
        RenderSystem.setShaderTexture(0, previousTexture);
        com.mojang.blaze3d.platform.GlStateManager._activeTexture(activeTexture);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
    }
}
