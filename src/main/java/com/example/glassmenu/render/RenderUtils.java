/*
 * RenderUtils - Architecture & Primary Responsibility:
 * Core Rendering Utilities.
 * Provides helper methods for drawing SDF-based rounded rectangles, 
 * lines, and managing shader uniforms for high-fidelity UI elements.
 */
package com.example.glassmenu.render;

import com.example.glassmenu.shader.ModShaders;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

public class RenderUtils {

    public static void drawSdfRoundedRect(MatrixStack matrices, float x, float y, float w, float h, float radius, int color, float swell) {
        float centerX = x + w / 2f;
        float centerY = y + h / 2f;
        
        matrices.push();
        matrices.translate(centerX, centerY, 0);
        matrices.scale(1.0f + swell * 0.08f, 1.0f + swell * 0.08f, 1.0f);
        matrices.translate(-centerX, -centerY, 0);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        boolean wasBlendEnabled = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND);
        ShaderProgram originalShader = RenderSystem.getShader();

        ShaderProgram shader = !com.example.glassmenu.GlassMenuClient.CONFIG.enableShaders() ? null : ModShaders.getSdfRoundedRect();
        if (shader == null) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            buffer.vertex(matrix, x, y + h, 0).color(r, g, b, a);
            buffer.vertex(matrix, x + w, y + h, 0).color(r, g, b, a);
            buffer.vertex(matrix, x + w, y, 0).color(r, g, b, a);
            buffer.vertex(matrix, x, y, 0).color(r, g, b, a);
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            if (originalShader != null) {
                RenderSystem.setShader(() -> originalShader);
            }
            if (!wasBlendEnabled) {
                RenderSystem.disableBlend();
            }
            matrices.pop();
            return;
        }

        int originalActiveTexture = com.mojang.blaze3d.platform.GlStateManager._getActiveTexture();
        int previousTexture = getTextureBinding2D(0);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(() -> shader);
        if (shader.getUniform("Color") != null) shader.getUniform("Color").set(r, g, b, a);
        if (shader.getUniform("Size") != null) shader.getUniform("Size").set(w, h);
        if (shader.getUniform("Radius") != null) shader.getUniform("Radius").set(radius);
        if (shader.getUniform("EdgeSoftness") != null) shader.getUniform("EdgeSoftness").set(1.0f);
        if (shader.getUniform("TexBounds") != null) shader.getUniform("TexBounds").set(0f, 0f, 1f, 1f);
        if (shader.getUniform("OutlineThickness") != null) shader.getUniform("OutlineThickness").set(0.0f);
        if (shader.getUniform("UseTexture") != null) shader.getUniform("UseTexture").set(0.0f);

        RenderSystem.setShaderTexture(0, getWhiteTexture());

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

        buffer.vertex(matrix, x, y + h, 0).texture(0, 1);
        buffer.vertex(matrix, x + w, y + h, 0).texture(1, 1);
        buffer.vertex(matrix, x + w, y, 0).texture(1, 0);
        buffer.vertex(matrix, x, y, 0).texture(0, 0);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        if (originalShader != null) {
            RenderSystem.setShader(() -> originalShader);
        }
        RenderSystem.setShaderTexture(0, previousTexture);
        com.mojang.blaze3d.platform.GlStateManager._activeTexture(originalActiveTexture);
        if (!wasBlendEnabled) {
            RenderSystem.disableBlend();
        }
        matrices.pop();
    }

    public static void drawSdfRoundedOutline(MatrixStack matrices, float x, float y, float w, float h, float radius, float thickness, int color) {
        float centerX = x + w / 2f;
        float centerY = y + h / 2f;
        
        matrices.push();
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        boolean wasBlendEnabled = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND);
        ShaderProgram originalShader = RenderSystem.getShader();

        ShaderProgram shader = !com.example.glassmenu.GlassMenuClient.CONFIG.enableShaders() ? null : ModShaders.getSdfRoundedRect();
        if (shader == null) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
            buffer.vertex(matrix, x, y, 0).color(r, g, b, a);
            buffer.vertex(matrix, x + w, y, 0).color(r, g, b, a);
            buffer.vertex(matrix, x + w, y + h, 0).color(r, g, b, a);
            buffer.vertex(matrix, x, y + h, 0).color(r, g, b, a);
            buffer.vertex(matrix, x, y, 0).color(r, g, b, a);
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            if (originalShader != null) {
                RenderSystem.setShader(() -> originalShader);
            }
            if (!wasBlendEnabled) {
                RenderSystem.disableBlend();
            }
            matrices.pop();
            return;
        }

        int originalActiveTexture = com.mojang.blaze3d.platform.GlStateManager._getActiveTexture();
        int previousTexture = getTextureBinding2D(0);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(() -> shader);
        if (shader.getUniform("Color") != null) shader.getUniform("Color").set(r, g, b, a);
        if (shader.getUniform("Size") != null) shader.getUniform("Size").set(w, h);
        if (shader.getUniform("Radius") != null) shader.getUniform("Radius").set(radius);
        if (shader.getUniform("EdgeSoftness") != null) shader.getUniform("EdgeSoftness").set(1.0f);
        if (shader.getUniform("TexBounds") != null) shader.getUniform("TexBounds").set(0f, 0f, 1f, 1f);
        if (shader.getUniform("OutlineThickness") != null) shader.getUniform("OutlineThickness").set(thickness);
        if (shader.getUniform("UseTexture") != null) shader.getUniform("UseTexture").set(0.0f);

        RenderSystem.setShaderTexture(0, getWhiteTexture());

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

        buffer.vertex(matrix, x, y + h, 0).texture(0, 1);
        buffer.vertex(matrix, x + w, y + h, 0).texture(1, 1);
        buffer.vertex(matrix, x + w, y, 0).texture(1, 0);
        buffer.vertex(matrix, x, y, 0).texture(0, 0);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        if (shader.getUniform("OutlineThickness") != null) shader.getUniform("OutlineThickness").set(0.0f);
        if (originalShader != null) {
            RenderSystem.setShader(() -> originalShader);
        }
        RenderSystem.setShaderTexture(0, previousTexture);
        com.mojang.blaze3d.platform.GlStateManager._activeTexture(originalActiveTexture);
        if (!wasBlendEnabled) {
            RenderSystem.disableBlend();
        }
        matrices.pop();
    }

    public static void drawCustomCrosshair(MatrixStack matrices, float x, float y, float size, float thickness, float gap, int mode, int color, boolean rainbow, float time) {
        ShaderProgram shader = getCustomCrosshairProgram();
        if (shader == null) return;

        float quadSize = gap * 2.0f + size * 4.0f + thickness * 2.0f;
        if (mode == 3) quadSize = gap * 2.0f + size * 3.0f + thickness * 4.0f;
        quadSize = Math.max(quadSize, 50.0f);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(() -> shader);

        if (shader.getUniform("Size") != null) shader.getUniform("Size").set(quadSize, quadSize);
        if (shader.getUniform("Thickness") != null) shader.getUniform("Thickness").set(thickness);
        if (shader.getUniform("Gap") != null) shader.getUniform("Gap").set(gap);
        if (shader.getUniform("Mode") != null) shader.getUniform("Mode").set((float)mode);
        if (shader.getUniform("Rainbow") != null) shader.getUniform("Rainbow").set(rainbow ? 1.0f : 0.0f);
        if (shader.getUniform("Time") != null) shader.getUniform("Time").set(time);

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;
        if (shader.getUniform("Color") != null) shader.getUniform("Color").set(r, g, b, a);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        
        float drawX = x - quadSize / 2.0f;
        float drawY = y - quadSize / 2.0f;

        bufferBuilder.vertex(matrix, drawX, drawY, 0).texture(0.0f, 0.0f);
        bufferBuilder.vertex(matrix, drawX, drawY + quadSize, 0).texture(0.0f, 1.0f);
        bufferBuilder.vertex(matrix, drawX + quadSize, drawY + quadSize, 0).texture(1.0f, 1.0f);
        bufferBuilder.vertex(matrix, drawX + quadSize, drawY, 0).texture(1.0f, 0.0f);

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.disableBlend();
    }

    public static void drawLine(MatrixStack matrices, float x, float y, float x2, float y2, float thickness, int color) {
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        net.minecraft.client.gl.ShaderProgram originalShader = RenderSystem.getShader();
        boolean originalBlend = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        float half = thickness / 2f;
        
        buffer.vertex(matrix, x, y + half, 0).color(r, g, b, a);
        buffer.vertex(matrix, x2, y + half, 0).color(r, g, b, a);
        buffer.vertex(matrix, x2, y - half, 0).color(r, g, b, a);
        
        buffer.vertex(matrix, x, y + half, 0).color(r, g, b, a);
        buffer.vertex(matrix, x2, y - half, 0).color(r, g, b, a);
        buffer.vertex(matrix, x, y - half, 0).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        if (originalShader != null) {
            RenderSystem.setShader(() -> originalShader);
        }
        if (!originalBlend) {
            RenderSystem.disableBlend();
        } else {
            RenderSystem.enableBlend();
        }
    }

    private static boolean registeredWhite = false;
    private static final net.minecraft.util.Identifier WHITE_TEX = net.minecraft.util.Identifier.of("glassmenu", "white_texture");
    private static ShaderProgram customCrosshairProgram;

    public static net.minecraft.util.Identifier getWhiteTexture() {
        if (!registeredWhite) {
            net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
            if (client != null) {
                net.minecraft.client.texture.TextureManager manager = client.getTextureManager();
                if (manager != null) {
                    try {
                        net.minecraft.client.texture.NativeImage image = new net.minecraft.client.texture.NativeImage(1, 1, false);
                        image.setColor(0, 0, 0xFFFFFFFF);
                        net.minecraft.client.texture.NativeImageBackedTexture texture = new net.minecraft.client.texture.NativeImageBackedTexture(image);
                        manager.registerTexture(WHITE_TEX, texture);
                        registeredWhite = true;
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }
        }
        return registeredWhite ? WHITE_TEX : net.minecraft.util.Identifier.of("minecraft", "missingno");
    }

    public static int getTextureBinding2D(int textureUnit) {
        int originalActive = com.mojang.blaze3d.platform.GlStateManager._getActiveTexture();
        com.mojang.blaze3d.platform.GlStateManager._activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0 + textureUnit);
        int textureId = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL11.GL_TEXTURE_BINDING_2D);
        com.mojang.blaze3d.platform.GlStateManager._activeTexture(originalActive);
        return textureId;
    }

    public static ShaderProgram getCustomCrosshairProgram() {
        return customCrosshairProgram;
    }

    public static class TintedVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float tr, tg, tb, ta;

        public TintedVertexConsumer(VertexConsumer delegate, float r, float g, float b, float a) {
            this.delegate = delegate;
            this.tr = r; this.tg = g; this.tb = b; this.ta = a;
        }

        @Override public VertexConsumer vertex(float x, float y, float z) { return delegate.vertex(x, y, z); }
        
        @Override public VertexConsumer color(int r, int g, int b, int a) {
            return delegate.color((int)(r * tr), (int)(g * tg), (int)(b * tb), (int)(a * ta));
        }
        
        @Override public VertexConsumer texture(float u, float v) { return delegate.texture(u, v); }
        @Override public VertexConsumer overlay(int u, int v) { return delegate.overlay(u, v); }
        @Override public VertexConsumer light(int u, int v) { return delegate.light(u, v); }
        @Override public VertexConsumer normal(float x, float y, float z) { return delegate.normal(x, y, z); }
        
        @Override public void vertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
            int nr = (int)(((color >> 16) & 0xFF) * tr);
            int ng = (int)(((color >> 8) & 0xFF) * tg);
            int nb = (int)((color & 0xFF) * tb);
            int na = (int)(((color >> 24) & 0xFF) * ta);
            int nColor = (na << 24) | (nr << 16) | (ng << 8) | nb;
            delegate.vertex(x, y, z, nColor, u, v, overlay, light, normalX, normalY, normalZ);
        }

        @Override
        public VertexConsumer vertex(org.joml.Matrix4f matrix, float x, float y, float z) {
            return delegate.vertex(matrix, x, y, z);
        }

        @Override
        public VertexConsumer color(float r, float g, float b, float a) {
            return delegate.color(r * tr, g * tg, b * tb, a * ta);
        }

        @Override
        public VertexConsumer light(int light) {
            return delegate.light(light);
        }
        
        @Override
        public VertexConsumer overlay(int overlay) {
            return delegate.overlay(overlay);
        }
    }

    public static class TintedVertexConsumerProvider implements VertexConsumerProvider {
        private final VertexConsumerProvider delegate;
        private final float r, g, b, a;

        public TintedVertexConsumerProvider(VertexConsumerProvider delegate, float r, float g, float b, float a) {
            this.delegate = delegate;
            this.r = r; this.g = g; this.b = b; this.a = a;
        }

        @Override
        public VertexConsumer getBuffer(RenderLayer layer) {
            return new TintedVertexConsumer(delegate.getBuffer(layer), r, g, b, a);
        }
    }

    public static void drawSdfRoundedTexture(MatrixStack matrices, float x, float y, float w, float h, float radius, net.minecraft.util.Identifier texture, int color, float u1, float v1, float u2, float v2) {
        float centerX = x + w / 2f;
        float centerY = y + h / 2f;
        
        matrices.push();
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        boolean wasBlendEnabled = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND);
        ShaderProgram originalShader = RenderSystem.getShader();

        ShaderProgram shader = !com.example.glassmenu.GlassMenuClient.CONFIG.enableShaders() ? null : ModShaders.getSdfRoundedRect();
        if (shader == null) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
            RenderSystem.setShaderTexture(0, texture);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            buffer.vertex(matrix, x, y + h, 0).texture(u1, v2).color(r, g, b, a);
            buffer.vertex(matrix, x + w, y + h, 0).texture(u2, v2).color(r, g, b, a);
            buffer.vertex(matrix, x + w, y, 0).texture(u2, v1).color(r, g, b, a);
            buffer.vertex(matrix, x, y, 0).texture(u1, v1).color(r, g, b, a);
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            if (originalShader != null) {
                RenderSystem.setShader(() -> originalShader);
            }
            if (!wasBlendEnabled) {
                RenderSystem.disableBlend();
            }
            matrices.pop();
            return;
        }

        int originalActiveTexture = com.mojang.blaze3d.platform.GlStateManager._getActiveTexture();
        int previousTexture = getTextureBinding2D(0);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(() -> shader);
        if (shader.getUniform("Color") != null) shader.getUniform("Color").set(r, g, b, a);
        if (shader.getUniform("Size") != null) shader.getUniform("Size").set(w, h);
        if (shader.getUniform("Radius") != null) shader.getUniform("Radius").set(radius);
        if (shader.getUniform("EdgeSoftness") != null) shader.getUniform("EdgeSoftness").set(1.0f);
        if (shader.getUniform("TexBounds") != null) shader.getUniform("TexBounds").set(u1, v1, u2, v2);
        if (shader.getUniform("OutlineThickness") != null) shader.getUniform("OutlineThickness").set(0.0f);
        if (shader.getUniform("UseTexture") != null) shader.getUniform("UseTexture").set(1.0f);

        RenderSystem.setShaderTexture(0, texture);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

        buffer.vertex(matrix, x, y + h, 0).texture(0, 1);
        buffer.vertex(matrix, x + w, y + h, 0).texture(1, 1);
        buffer.vertex(matrix, x + w, y, 0).texture(1, 0);
        buffer.vertex(matrix, x, y, 0).texture(0, 0);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        if (originalShader != null) {
            RenderSystem.setShader(() -> originalShader);
        }
        RenderSystem.setShaderTexture(0, previousTexture);
        com.mojang.blaze3d.platform.GlStateManager._activeTexture(originalActiveTexture);
        if (!wasBlendEnabled) {
            RenderSystem.disableBlend();
        }
        matrices.pop();
    }
}
