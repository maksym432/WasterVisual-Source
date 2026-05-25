/*
 * LiquidGlassEffectView - Architecture & Primary Responsibility:
 * UI Background Effect Manager.
 * Responsible for rendering the full-screen dark overlay with 
 * OpenGL state resets to ensure no texture atlas leakage into UI backgrounds.
 */
package com.example.glassmenu.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import org.joml.Matrix4f;

public class LiquidGlassEffectView {

    public void enableBlur() {
        // Disabled
    }

    public void disableBlur() {
        // Disabled
    }

    public void render(DrawContext context, int width, int height) {
        // 1. COMPLETELY isolate OpenGL state
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        // 2. Use the most basic "Position-Color" shader (No textures allowed)
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        
        // 3. Explicitly UNBIND any textures from all slots to kill the atlas leak
        for (int i = 0; i < 12; i++) {
            RenderSystem.setShaderTexture(i, 0);
        }

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        
        // 4. Use POSITION_COLOR format - this format does NOT support textures
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        float r = 0.0f, g = 0.0f, b = 0.0f, a = 0.85f; // Deep dark 85%

        // Draw quad covering full screen dimensions
        buffer.vertex(matrix, 0, (float)height, 0).color(r, g, b, a);
        buffer.vertex(matrix, (float)width, (float)height, 0).color(r, g, b, a);
        buffer.vertex(matrix, (float)width, 0, 0).color(r, g, b, a);
        buffer.vertex(matrix, 0, 0, 0).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        
        // 5. Restore state
        RenderSystem.enableDepthTest();
    }
}
