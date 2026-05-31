/*
 * IslandManager - Architecture & Primary Responsibility:
 * Dynamic Island Layout Manager.
 * Handles registering, updating, and rendering active modules using spring-based
 * animations. Manages transitions between compact and expanded views.
 */
package com.example.glassmenu.widget;

import com.example.glassmenu.GlassMenuClient;
import com.example.glassmenu.shader.ModShaders;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class IslandManager {

    private static final List<IslandModule> MODULES = new ArrayList<>();
    private static IslandModule currentActiveModule = null;

    // Spring constants
    private static final float TENSION = 240f;
    private static final float FRICTION = 21f;

    // Capsule dimensions
    private static float currentWidth = 110;
    private static float targetWidth = 110;
    private static float velocityWidth = 0;

    private static float currentHeight = 24;
    private static float targetHeight = 24;
    private static float velocityHeight = 0;

    private static float widgetAlpha = 0;
    private static float smoothProgress = 0;

    private static long lastTime = System.nanoTime();
    private static boolean expanded = false;
    private static boolean hovered = false;

    // Configuration
    private static final File CONFIG_FILE = new File("config/wastervisual_island.json");
    private static int islandColor = 0xFF000000;
    private static float blurFactor = 4.0f;
    private static float animationSpeed = 0.2f;
    private static float defaultCornerRadius = 14.0f;

    private static long lastConfigCheck = 0;
    private static long lastConfigModified = 0;

    static {
        // Register modules
        register(new com.example.glassmenu.widget.impl.MusicModule());
        loadConfig();
    }

    public static void init() {
        // Triggers class loading and static block
    }

    public static void register(IslandModule module) {
        MODULES.add(module);
    }

    public static void loadConfig() {
        try {
            if (!CONFIG_FILE.exists()) {
                CONFIG_FILE.getParentFile().mkdirs();
                String defaultJson = "{\n" +
                        "  \"island_color\": \"#FF000000\",\n" +
                        "  \"blur_factor\": 4.0,\n" +
                        "  \"animation_speed\": 0.2,\n" +
                        "  \"default_corner_radius\": 14.0\n" +
                        "}";
                java.nio.file.Files.writeString(CONFIG_FILE.toPath(), defaultJson);
            }
            String content = java.nio.file.Files.readString(CONFIG_FILE.toPath());
            com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(content).getAsJsonObject();
            if (json.has("island_color")) {
                islandColor = parseHexColor(json.get("island_color").getAsString());
            }
            if (json.has("blur_factor")) {
                blurFactor = json.get("blur_factor").getAsFloat();
            }
            if (json.has("animation_speed")) {
                animationSpeed = json.get("animation_speed").getAsFloat();
            }
            if (json.has("default_corner_radius")) {
                defaultCornerRadius = json.get("default_corner_radius").getAsFloat();
            }
        } catch (Exception e) {
            System.err.println("GlassMenu: Failed to load wastervisual_island.json: " + e.getMessage());
        }
    }

    public static void checkConfigReload() {
        long now = System.currentTimeMillis();
        if (now - lastConfigCheck > 1000) {
            lastConfigCheck = now;
            if (CONFIG_FILE.exists()) {
                long modified = CONFIG_FILE.lastModified();
                if (modified != lastConfigModified) {
                    lastConfigModified = modified;
                    loadConfig();
                    System.out.println("GlassMenu: Reloaded wastervisual_island.json");
                }
            } else {
                loadConfig();
            }
        }
    }

    private static int parseHexColor(String hex) {
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        try {
            if (hex.length() == 8) {
                return (int) Long.parseLong(hex, 16);
            } else if (hex.length() == 6) {
                return 0xFF000000 | Integer.parseInt(hex, 16);
            }
        } catch (NumberFormatException ignored) {}
        return 0xFF000000;
    }

    public static void render(DrawContext context, int screenWidth, int screenHeight, int mouseX, int mouseY, float delta) {
        checkConfigReload();

        // Check active module
        IslandModule active = null;
        for (IslandModule m : MODULES) {
            if (m.isActive()) {
                if (active == null || m.getPriority() > active.getPriority()) {
                    active = m;
                }
            }
        }
        currentActiveModule = active;

        // Calculate dt
        long now = System.nanoTime();
        float dt = (now - lastTime) / 1_000_000_000f;
        lastTime = now;
        if (dt < 0f) dt = 0f;
        dt = Math.min(dt, 0.1f);

        // Tick all active modules
        for (IslandModule m : MODULES) {
            if (m.isActive()) {
                m.tick(dt);
            }
        }

        // Widget alpha transition
        float targetWidgetAlpha = (active != null) ? 1.0f : 0.0f;
        widgetAlpha = MathHelper.lerp(8.0f * dt, widgetAlpha, targetWidgetAlpha);

        if (widgetAlpha <= 0.01f) return;

        float x = GlassMenuClient.CONFIG.islandX() == -1 ? (screenWidth - currentWidth) / 2f : GlassMenuClient.CONFIG.islandX();
        float y = GlassMenuClient.CONFIG.islandY();

        // Hover check: only interactive when mouse is visible (like when a Screen is open)
        MinecraftClient client = MinecraftClient.getInstance();
        boolean hasMouse = client.currentScreen != null;
        hovered = hasMouse && mouseX >= x && mouseX <= x + currentWidth && mouseY >= y && mouseY <= y + currentHeight;

        if (active != null) {
            if (hovered) {
                targetWidth = active.getTargetWidth();
                targetHeight = active.getTargetHeight();
                expanded = true;
            } else {
                targetWidth = 75f;
                targetHeight = 20f;
                expanded = false;
                if (active instanceof com.example.glassmenu.widget.impl.MusicModule music) {
                    music.resetMode();
                }
            }
        } else {
            targetWidth = 75f;
            targetHeight = 20f;
            expanded = false;
        }

        // Spring physics animation loop scaled by animationSpeed
        float remainingDt = dt * (animationSpeed / 0.2f);
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

        // Morph progress
        float minCapsuleH = 20f;
        float expandedH = (active != null) ? active.getTargetHeight() : 110f;
        float morphProgress = (currentHeight - minCapsuleH) / (expandedH - minCapsuleH);
        morphProgress = MathHelper.clamp(morphProgress, 0, 1);
        smoothProgress = morphProgress * morphProgress * (3 - 2 * morphProgress);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 800); // Elevate to render on top of tooltips

        // 1. Draw capsule background with dynamic corner radius (pill in compact, rounded rect in expanded)
        float compactR = currentHeight / 2f;
        float expandedR = defaultCornerRadius;
        float finalRadius = MathHelper.lerp(smoothProgress, compactR, expandedR);

        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        drawSdfBackground(context, 0, 0, currentWidth, currentHeight, finalRadius, islandColor, 1.5f);
        context.getMatrices().pop();

        // 2. Draw active module content
        if (active != null) {
            double rx = mouseX - x;
            double ry = mouseY - y;
            active.renderContent(context, x, y, currentWidth, currentHeight, widgetAlpha, smoothProgress, rx, ry);
        }

        context.getMatrices().pop(); // Pop Z elevation layer

        RenderSystem.enableDepthTest();
    }

    public static boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (currentActiveModule == null || widgetAlpha <= 0.01f) {
            System.out.println("GlassMenu Debug: mouseClicked ignored (no active module or widgetAlpha <= 0.01f)");
            return false;
        }

        float x = GlassMenuClient.CONFIG.islandX() == -1 ? (MinecraftClient.getInstance().getWindow().getScaledWidth() - currentWidth) / 2f : GlassMenuClient.CONFIG.islandX();
        float y = GlassMenuClient.CONFIG.islandY();

        double rx = mouseX - x;
        double ry = mouseY - y;

        System.out.println("GlassMenu Debug: click on screen (" + mouseX + ", " + mouseY + "), relative to capsule (" + rx + ", " + ry + "), button=" + button + ", size=" + currentWidth + "x" + currentHeight + ", hovered=" + hovered);

        boolean consumed = currentActiveModule.mouseClicked(rx, ry, button, currentWidth, currentHeight);
        System.out.println("GlassMenu Debug: module consumed click = " + consumed);
        if (consumed) {
            return true;
        }

        return hovered;
    }

    private static void drawSdfBackground(DrawContext context, float x, float y, float w, float h, float r, int argb, float edgeSoftness) {
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

    private static void drawRoundedRectGeometry(DrawContext context, float x, float y, float w, float h, float r, int argb, float widgetAlpha) {
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
