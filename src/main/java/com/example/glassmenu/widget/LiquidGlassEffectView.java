/*
 * LiquidGlassEffectView - Architecture & Primary Responsibility:
 * UI Background Effect Manager.
 * Responsible for rendering the full-screen glassmorphic background tint 
 * and managing the full-screen post-processing blur shader.
 */
package com.example.glassmenu.widget;

import com.example.glassmenu.mixin.GameRendererAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

public class LiquidGlassEffectView {

    public void enableBlur() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.gameRenderer instanceof GameRendererAccessor accessor) {
            accessor.glassmenu$loadPostProcessor(Identifier.of("minecraft", "shaders/post/glass_blur.json"));
        }
    }

    public void disableBlur() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.gameRenderer instanceof GameRendererAccessor accessor) {
            accessor.glassmenu$disablePostProcessor();
        }
        com.example.glassmenu.render.GlassRefractionEngine.cleanup();
    }

    public void render(DrawContext context, int width, int height) {
        // 1. COMPLETELY isolate OpenGL state
        boolean wasBlendEnabled = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND);
        boolean wasDepthEnabled = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        // 2. Use the most basic "Position-Color" shader (No textures allowed)
        ShaderProgram previousShader = RenderSystem.getShader();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        
        // 4. Use POSITION_COLOR format - this format does NOT support textures
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        // Premium iOS dark glassmorphism background overlay tint (15% opacity)
        float r = 0.02f, g = 0.02f, b = 0.04f, a = 0.15f;

        // Draw quad covering full screen dimensions
        buffer.vertex(matrix, 0, (float)height, 0).color(r, g, b, a);
        buffer.vertex(matrix, (float)width, (float)height, 0).color(r, g, b, a);
        buffer.vertex(matrix, (float)width, 0, 0).color(r, g, b, a);
        buffer.vertex(matrix, 0, 0, 0).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        
        if (previousShader != null) {
            RenderSystem.setShader(() -> previousShader);
        }
        
        // 5. Restore state
        if (wasDepthEnabled) {
            RenderSystem.enableDepthTest();
        } else {
            RenderSystem.disableDepthTest();
        }
        if (!wasBlendEnabled) {
            RenderSystem.disableBlend();
        }
    }
}
