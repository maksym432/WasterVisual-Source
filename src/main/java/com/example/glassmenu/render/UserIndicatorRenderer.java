/*
 * UserIndicatorRenderer - Architecture & Primary Responsibility:
 * Overlay renderer for the User Indicator widget.
 * Displays FPS, Ping, and Left/Right mouse CPS horizontally in a customizable panel on the HUD.
 */
package com.example.glassmenu.render;

import com.example.glassmenu.GlassMenuClient;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import org.joml.Matrix4f;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class UserIndicatorRenderer {
    private static final List<Long> leftClicks = new ArrayList<>();
    private static final List<Long> rightClicks = new ArrayList<>();

    // Background socket ping metrics
    private static String lastAddress = null;
    private static volatile int measuredPing = -1;
    private static long lastPingTime = 0;
    private static volatile boolean pinging = false;

    public static synchronized void registerClick(int button) {
        long now = System.currentTimeMillis();
        if (button == 0) {
            leftClicks.add(now);
        } else if (button == 1) {
            rightClicks.add(now);
        }
    }

    private static synchronized int getCps(List<Long> clicks) {
        long now = System.currentTimeMillis();
        Iterator<Long> it = clicks.iterator();
        while (it.hasNext()) {
            if (now - it.next() > 1000) {
                it.remove();
            }
        }
        return clicks.size();
    }

    private static final net.minecraft.util.Identifier USER_PHOTO = net.minecraft.util.Identifier.of("glassmenu", "textures/gui/waster_visual_user.png");

    public static void render(DrawContext context, int screenWidth, int screenHeight) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;
        if (!GlassMenuClient.CONFIG.enableUserIndicator()) return;

        int w = GlassMenuClient.CONFIG.userIndicatorWidth();
        int h = GlassMenuClient.CONFIG.userIndicatorHeight();
        int x = GlassMenuClient.CONFIG.userIndicatorX() == -1 ? (screenWidth - w) / 2 : GlassMenuClient.CONFIG.userIndicatorX();
        int y = GlassMenuClient.CONFIG.userIndicatorY() == -1 ? 35 : GlassMenuClient.CONFIG.userIndicatorY();

        int fps = client.getCurrentFps();
        int ping = 0;

        if (client.getNetworkHandler() != null && client.getNetworkHandler().getConnection() != null) {
            var connection = client.getNetworkHandler().getConnection();
            if (connection.isLocal()) {
                ping = 0; // Singleplayer
            } else {
                java.net.SocketAddress socketAddress = connection.getAddress();
                if (socketAddress instanceof java.net.InetSocketAddress inetSocketAddress) {
                    String addressStr = inetSocketAddress.toString();
                    if (!addressStr.equals(lastAddress)) {
                        lastAddress = addressStr;
                        measuredPing = -1;
                        lastPingTime = 0;
                    }

                    long now = System.currentTimeMillis();
                    if (now - lastPingTime > 3000 && !pinging) {
                        lastPingTime = now;
                        pinging = true;
                        new Thread(() -> {
                            try {
                                long start = System.nanoTime();
                                java.net.Socket socket = new java.net.Socket();
                                socket.connect(inetSocketAddress, 1500);
                                socket.close();
                                long end = System.nanoTime();
                                measuredPing = (int) ((end - start) / 1000000L);
                            } catch (Exception e) {
                                measuredPing = -1;
                            } finally {
                                pinging = false;
                            }
                        }).start();
                    }
                }

                if (measuredPing >= 0) {
                    ping = measuredPing;
                } else {
                    // Fallback to PlayerListEntry ping
                    var entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
                    if (entry == null && client.getNetworkHandler().getProfile() != null) {
                        entry = client.getNetworkHandler().getPlayerListEntry(client.getNetworkHandler().getProfile().getId());
                    }
                    if (entry == null && client.player.getGameProfile() != null) {
                        entry = client.getNetworkHandler().getPlayerListEntry(client.player.getGameProfile().getId());
                    }
                    if (entry == null) {
                        String realName = client.player.getGameProfile() != null ? client.player.getGameProfile().getName() : "";
                        String netName = client.getNetworkHandler().getProfile() != null ? client.getNetworkHandler().getProfile().getName() : "";
                        String entityName = client.player.getName() != null ? client.player.getName().getString() : "";
                        String displayName = client.player.getDisplayName() != null ? client.player.getDisplayName().getString() : "";

                        for (var e : client.getNetworkHandler().getPlayerList()) {
                            String entryName = e.getProfile().getName();
                            if (entryName != null) {
                                if ((!realName.isEmpty() && entryName.equalsIgnoreCase(realName))
                                    || (!netName.isEmpty() && entryName.equalsIgnoreCase(netName))
                                    || (!entityName.isEmpty() && entryName.equalsIgnoreCase(entityName))
                                    || (!displayName.isEmpty() && entryName.equalsIgnoreCase(displayName))) {
                                    entry = e;
                                    break;
                                }
                            }
                        }
                    }
                    if (entry != null) {
                        ping = entry.getLatency();
                    }
                }
            }
        }

        int leftCps = getCps(leftClicks);
        int rightCps = getCps(rightClicks);

        renderIndicator(context, x, y, w, h, fps, ping, leftCps, rightCps, GlassMenuClient.CONFIG.transparentUserIndicator(), GlassMenuClient.CONFIG.userIndicatorColor());
    }

    public static void renderIndicator(DrawContext context, int x, int y, int w, int h, int fps, int ping, int leftCps, int rightCps, boolean transparent, int panelColor) {
        MinecraftClient client = MinecraftClient.getInstance();
        float scaleX = (float) w / 203f;
        float scaleY = (float) h / 26f;

        // Save original states at the very beginning of the method
        ShaderProgram originalShader = RenderSystem.getShader();
        int originalTex = RenderUtils.getTextureBinding2D(0);
        boolean originalBlend = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND);

        // 1. Draw Panel Background (Refraction shader if transparent)
        if (transparent) {
            GlassRefractionEngine.drawRefractedPanel(context, x, y, w, h, 0.8f, 0x22FFFFFF, 6f * Math.min(scaleX, scaleY));
        }

        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(scaleX, scaleY, 1.0f);

        if (!transparent) {
            RenderUtils.drawSdfRoundedOutline(context.getMatrices(), 0, 0, 203, 26, 6f, 0.8f, 0x33FFFFFF);
            RenderUtils.drawSdfRoundedRect(context.getMatrices(), 0, 0, 203, 26, 6f, 0xFF000000, 0);
        } else {
            RenderUtils.drawSdfRoundedOutline(context.getMatrices(), 0, 0, 203, 26, 6f, 0.8f, 0x33FFFFFF);
        }
        context.draw(); // Flush background

        // 2. Draw Squares/Boxes for each indicator
        int blockOutlineColor = transparent ? 0x22FFFFFF : 0x00000000;
        int blockFillColor = transparent ? 0x0F000000 : 0xEEFFFFFF;
        int photoBgColor = transparent ? 0x88000000 : 0xEEFFFFFF;

        // Draw Square 0: Photo (20x20 at x=3, y=3)
        RenderUtils.drawSdfRoundedOutline(context.getMatrices(), 3f, 3f, 20f, 20f, 4f, 0.6f, blockOutlineColor);
        RenderUtils.drawSdfRoundedRect(context.getMatrices(), 3f, 3f, 20f, 20f, 4f, photoBgColor, 0);

        // Draw Squares 1, 2, 3: Indicators (56x20 at x=26, 85, 144)
        for (int i = 0; i < 3; i++) {
            float bx = 26 + i * 59;
            float by = 3;
            RenderUtils.drawSdfRoundedOutline(context.getMatrices(), bx, by, 56f, 20f, 4f, 0.6f, blockOutlineColor);
            RenderUtils.drawSdfRoundedRect(context.getMatrices(), bx, by, 56f, 20f, 4f, blockFillColor, 0);
        }
        context.draw(); // Flush blocks

        // 3. Render the User Photo
        ShaderProgram previousShader = RenderSystem.getShader();
        int prevTex = RenderUtils.getTextureBinding2D(0);
        boolean wasBlend = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND);

        ShaderProgram sdfShader = !GlassMenuClient.CONFIG.enableShaders() ? null : com.example.glassmenu.shader.ModShaders.getSdfRoundedRect();
        if (sdfShader == null) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionTexProgram);
            RenderSystem.setShaderTexture(0, USER_PHOTO);
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
            Matrix4f mat = context.getMatrices().peek().getPositionMatrix();
            buf.vertex(mat, 3.5f, 22.5f, 0).texture(0f, 1f);
            buf.vertex(mat, 22.5f, 22.5f, 0).texture(1f, 1f);
            buf.vertex(mat, 22.5f, 3.5f, 0).texture(1f, 0f);
            buf.vertex(mat, 3.5f, 3.5f, 0).texture(0f, 0f);
            BufferRenderer.drawWithGlobalProgram(buf.end());
        } else {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(() -> sdfShader);
            RenderSystem.setShaderTexture(0, USER_PHOTO);

            if (sdfShader.getUniform("Color") != null) sdfShader.getUniform("Color").set(1.0f, 1.0f, 1.0f, 1.0f);
            if (sdfShader.getUniform("Size") != null) sdfShader.getUniform("Size").set(19f, 19f);
            if (sdfShader.getUniform("Radius") != null) sdfShader.getUniform("Radius").set(3.5f);
            if (sdfShader.getUniform("EdgeSoftness") != null) sdfShader.getUniform("EdgeSoftness").set(1.0f);
            if (sdfShader.getUniform("TexBounds") != null) sdfShader.getUniform("TexBounds").set(0f, 0f, 1f, 1f);
            if (sdfShader.getUniform("OutlineThickness") != null) sdfShader.getUniform("OutlineThickness").set(0.0f);
            if (sdfShader.getUniform("UseTexture") != null) sdfShader.getUniform("UseTexture").set(1.0f);

            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
            Matrix4f mat = context.getMatrices().peek().getPositionMatrix();
            buf.vertex(mat, 3.5f, 22.5f, 0).texture(0f, 1f);
            buf.vertex(mat, 22.5f, 22.5f, 0).texture(1f, 1f);
            buf.vertex(mat, 22.5f, 3.5f, 0).texture(1f, 0f);
            buf.vertex(mat, 3.5f, 3.5f, 0).texture(0f, 0f);
            BufferRenderer.drawWithGlobalProgram(buf.end());

            if (sdfShader.getUniform("UseTexture") != null) sdfShader.getUniform("UseTexture").set(0.0f);
        }

        // Restore states
        RenderSystem.setShader(() -> previousShader);
        RenderSystem.setShaderTexture(0, prevTex);
        if (!wasBlend) {
            RenderSystem.disableBlend();
        }
        context.draw(); // Flush photo

        // 4. Render Texts centered inside each block
        String fpsText = fps + " FPS";
        String pingText = ping + " ms";
        String cpsText = leftCps + " / " + rightCps;

        int textY = 9;

        int textColor = transparent ? 0xFFFFFFFF : 0xFF000000;
        boolean drawShadow = transparent;

        int fpsW = client.textRenderer.getWidth(fpsText);
        context.drawText(client.textRenderer, fpsText, 54 - fpsW / 2, textY, textColor, drawShadow);

        int pingW = client.textRenderer.getWidth(pingText);
        context.drawText(client.textRenderer, pingText, 113 - pingW / 2, textY, textColor, drawShadow);

        int cpsW = client.textRenderer.getWidth(cpsText);
        context.drawText(client.textRenderer, cpsText, 172 - cpsW / 2, textY, textColor, drawShadow);

        context.getMatrices().pop();
        context.draw(); // Flush text and everything else in the DrawContext

        // Restore original states at the very end of the method
        RenderSystem.setShader(() -> originalShader);
        RenderSystem.setShaderTexture(0, originalTex);
        if (!originalBlend) {
            RenderSystem.disableBlend();
        } else {
            RenderSystem.enableBlend();
        }
    }
}
