/*
 * BedWarsESPManager - Architecture & Primary Responsibility:
 * Team-based ESP (Extra Sensory Perception) for BedWars.
 * Renders team-colored 3D bounding boxes (respecting world depth) or billboarded 
 * alert marks above teammates and enemies, and renders nametags through walls.
 * Detects team colors using a robust hierarchy of:
 *   1. Scoreboard Team formatting.
 *   2. Player display name formatting codes.
 *   3. Dyed leather armor color matching their team.
 */
package com.example.glassmenu.render;

import com.example.glassmenu.GlassMenuClient;
import com.example.glassmenu.GlassMenuConfig;
import com.example.glassmenu.GlassMenuConfigModel;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class BedWarsESPManager {

    public static void render(net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        // Ensure at least one setting is enabled
        if (!GlassMenuClient.CONFIG.enableBedWarsEsp() && !GlassMenuClient.CONFIG.enableBedWarsNames()) return;

        net.minecraft.client.gl.ShaderProgram previousShader = RenderSystem.getShader();
        boolean previousBlend = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND);

        int prevActiveTexture = com.mojang.blaze3d.platform.GlStateManager._getActiveTexture();
        com.mojang.blaze3d.platform.GlStateManager._activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
        int prevTexture0 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

        boolean anyRendered = false;

        MatrixStack matrices = context.matrixStack();
        float tickDelta = context.tickCounter().getTickDelta(false);
        Camera camera = context.camera();
        Vec3d camPos = camera.getPos();

        for (PlayerEntity player : client.world.getPlayers()) {
            // Do not render ESP on local player or dead/invisible players
            if (player == client.player || !player.isAlive() || player.isInvisible()) continue;

            int teamColor = getTeamColor(player);

            double pX = MathHelper.lerp(tickDelta, player.prevX, player.getX());
            double pY = MathHelper.lerp(tickDelta, player.prevY, player.getY());
            double pZ = MathHelper.lerp(tickDelta, player.prevZ, player.getZ());

            double rx = pX - camPos.x;
            double ry = pY - camPos.y;
            double rz = pZ - camPos.z;

            // 1. Render Nametags through walls
            if (GlassMenuClient.CONFIG.enableBedWarsNames()) {
                renderNameTag(matrices, camera, player, rx, ry, rz, teamColor);
                anyRendered = true;
            }

            // 2. Render Team ESP (Hitbox or Alert Mark / Heart) - Only if team color is not white
            if (GlassMenuClient.CONFIG.enableBedWarsEsp() && (teamColor & 0x00FFFFFF) != 0x00FFFFFF) {
                if (player.isTeammate(client.player) && GlassMenuClient.CONFIG.enableBedWarsHearts()) {
                    renderHeartIcon(matrices, camera, player, rx, ry, rz, teamColor);
                } else if (GlassMenuClient.CONFIG.bedWarsEspMode() == GlassMenuConfigModel.BedWarsEspMode.HITBOX) {
                    renderHitbox(matrices, player, rx, ry, rz, teamColor);
                } else {
                    renderAlertIcon(matrices, camera, player, rx, ry, rz, teamColor);
                }
                anyRendered = true;
            }
        }

        // Clean up our modifications to OpenGL states BEFORE calling draw() on provider!
        RenderSystem.lineWidth(1.0f);
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        if (!previousBlend) {
            RenderSystem.disableBlend();
        } else {
            RenderSystem.enableBlend();
        }
        if (previousShader != null) {
            RenderSystem.setShader(() -> previousShader);
        }

        // Restore texture slot 0 — both the raw GL binding and Minecraft's tracked state.
        com.mojang.blaze3d.platform.GlStateManager._activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
        org.lwjgl.opengl.GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTexture0);
        RenderSystem.setShaderTexture(0, prevTexture0);
        // Restore active texture unit.
        com.mojang.blaze3d.platform.GlStateManager._activeTexture(prevActiveTexture);

        if (anyRendered) {
            // Immediately flush name tags / elements to prevent shader/state issues in ImmediatelyFast or vanilla buffer builders
            client.getBufferBuilders().getEntityVertexConsumers().draw();
        }
    }

    private static int getTeamColor(PlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return 0xFFFFFFFF;

        // Heuristics 1: Scoreboard team formatting color
        var team = client.world.getScoreboard().getScoreHolderTeam(player.getGameProfile().getName());
        if (team != null && team.getColor() != null && team.getColor().isColor()) {
            return getColorFromFormatting(team.getColor());
        }

        // Heuristics 2: Display name formatting prefix style color
        var displayName = player.getDisplayName();
        if (displayName.getStyle().getColor() != null) {
            return displayName.getStyle().getColor().getRgb() | 0xFF000000;
        }
        for (var sibling : displayName.getSiblings()) {
            if (sibling.getStyle().getColor() != null) {
                return sibling.getStyle().getColor().getRgb() | 0xFF000000;
            }
        }

        // Heuristics 3: Leather armor color worn in BedWars
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                ItemStack stack = player.getEquippedStack(slot);
                if (!stack.isEmpty() && stack.contains(DataComponentTypes.DYED_COLOR)) {
                    var dyed = stack.get(DataComponentTypes.DYED_COLOR);
                    if (dyed != null) {
                        return dyed.rgb() | 0xFF000000;
                    }
                }
            }
        }

        // Heuristics 4: Teammate check fallback
        return player.isTeammate(client.player) ? 0xFF55FF55 : 0xFFFF5555;
    }

    private static int getColorFromFormatting(Formatting formatting) {
        Integer val = formatting.getColorValue();
        if (val != null) {
            return val | 0xFF000000;
        }
        switch (formatting) {
            case RED: return 0xFFFF5555;
            case DARK_RED: return 0xFFAA0000;
            case GOLD: return 0xFFFFAA00;
            case YELLOW: return 0xFFFFFF55;
            case GREEN: return 0xFF55FF55;
            case DARK_GREEN: return 0xFF00AA00;
            case BLUE: return 0xFF5555FF;
            case DARK_BLUE: return 0xFF0000AA;
            case AQUA: return 0xFF55FFFF;
            case DARK_AQUA: return 0xFF00AAAA;
            case LIGHT_PURPLE: return 0xFFFF55FF;
            case DARK_PURPLE: return 0xFFAA00AA;
            case WHITE: return 0xFFFFFFFF;
            case GRAY: return 0xFFAAAAAA;
            case DARK_GRAY: return 0xFF555555;
            case BLACK: return 0xFF000000;
            default: return 0xFFFFFFFF;
        }
    }

    private static void renderNameTag(MatrixStack matrices, Camera camera, PlayerEntity player, double rx, double ry, double rz, int color) {
        matrices.push();
        matrices.translate(rx, ry + player.getHeight() + 0.5f, rz);
        matrices.multiply(camera.getRotation());
        matrices.scale(-0.025f, -0.025f, 0.025f);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        MinecraftClient client = MinecraftClient.getInstance();
        var provider = client.getBufferBuilders().getEntityVertexConsumers();
        
        String name = player.getGameProfile().getName();
        float xOffset = -client.textRenderer.getWidth(name) / 2.0f;
        int bgColor = 0x40000000;

        // Draw SEE_THROUGH layer text so nicknames render behind blocks
        client.textRenderer.draw(
            name, 
            xOffset, 
            0, 
            color, 
            false, 
            matrix, 
            provider, 
            TextRenderer.TextLayerType.SEE_THROUGH, 
            bgColor, 
            0xF000F0
        );

        matrices.pop();
    }

    private static void renderHitbox(MatrixStack matrices, PlayerEntity player, double rx, double ry, double rz, int color) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = 0.7f;

        float w = player.getWidth() / 2.0f;
        float h = player.getHeight();

        matrices.push();
        matrices.translate(rx, ry, rz);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest(); // Hitbox stays hidden behind blocks
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.lineWidth(6.0f);

        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        // Bottom ring
        buffer.vertex(matrix, -w, 0, -w).color(r, g, b, a);
        buffer.vertex(matrix, w, 0, -w).color(r, g, b, a);

        buffer.vertex(matrix, w, 0, -w).color(r, g, b, a);
        buffer.vertex(matrix, w, 0, w).color(r, g, b, a);

        buffer.vertex(matrix, w, 0, w).color(r, g, b, a);
        buffer.vertex(matrix, -w, 0, w).color(r, g, b, a);

        buffer.vertex(matrix, -w, 0, w).color(r, g, b, a);
        buffer.vertex(matrix, -w, 0, -w).color(r, g, b, a);

        // Top ring
        buffer.vertex(matrix, -w, h, -w).color(r, g, b, a);
        buffer.vertex(matrix, w, h, -w).color(r, g, b, a);

        buffer.vertex(matrix, w, h, -w).color(r, g, b, a);
        buffer.vertex(matrix, w, h, w).color(r, g, b, a);

        buffer.vertex(matrix, w, h, w).color(r, g, b, a);
        buffer.vertex(matrix, -w, h, w).color(r, g, b, a);

        buffer.vertex(matrix, -w, h, w).color(r, g, b, a);
        buffer.vertex(matrix, -w, h, -w).color(r, g, b, a);

        // Vertical lines
        buffer.vertex(matrix, -w, 0, -w).color(r, g, b, a);
        buffer.vertex(matrix, -w, h, -w).color(r, g, b, a);

        buffer.vertex(matrix, w, 0, -w).color(r, g, b, a);
        buffer.vertex(matrix, w, h, -w).color(r, g, b, a);

        buffer.vertex(matrix, w, 0, w).color(r, g, b, a);
        buffer.vertex(matrix, w, h, w).color(r, g, b, a);

        buffer.vertex(matrix, -w, 0, w).color(r, g, b, a);
        buffer.vertex(matrix, -w, h, w).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.lineWidth(1.0f);

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        matrices.pop();
    }

    private static void renderAlertIcon(MatrixStack matrices, Camera camera, PlayerEntity player, double rx, double ry, double rz, int color) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = 0.85f;

        matrices.push();
        matrices.translate(rx, ry + player.getHeight() + 1.4f, rz);
        matrices.multiply(camera.getRotation());
        matrices.scale(0.9f, 0.9f, 0.9f); // Large exclamation billboard scale

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest(); // Alerts respect depth
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        // Exclamation point (!) shape
        // Tapered vertical bar
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        
        float w = 0.16f;
        buffer.vertex(matrix, -w, 0.4f, 0).color(r, g, b, a);
        buffer.vertex(matrix, w, 0.4f, 0).color(r, g, b, a);
        buffer.vertex(matrix, w * 0.45f, -0.2f, 0).color(r, g, b, a);
        buffer.vertex(matrix, -w * 0.45f, -0.2f, 0).color(r, g, b, a);
        
        // Dot at bottom
        float dot = 0.11f;
        buffer.vertex(matrix, -dot, -0.45f, 0).color(r, g, b, a);
        buffer.vertex(matrix, dot, -0.45f, 0).color(r, g, b, a);
        buffer.vertex(matrix, dot, -0.65f, 0).color(r, g, b, a);
        buffer.vertex(matrix, -dot, -0.65f, 0).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        matrices.pop();
    }

    private static void renderHeartIcon(MatrixStack matrices, Camera camera, PlayerEntity player, double rx, double ry, double rz, int color) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = 0.85f;

        matrices.push();
        matrices.translate(rx, ry + player.getHeight() + 1.4f, rz);
        matrices.multiply(camera.getRotation());
        matrices.scale(0.9f, 0.9f, 0.9f); // Large heart billboard scale

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest(); // Hearts respect depth
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        
        // 1. Left lobe
        buffer.vertex(matrix, 0, 0, 0).color(r, g, b, a);
        buffer.vertex(matrix, -0.2f, 0.35f, 0).color(r, g, b, a);
        buffer.vertex(matrix, -0.4f, 0.1f, 0).color(r, g, b, a);
        
        // 2. Left side rounding
        buffer.vertex(matrix, 0, 0, 0).color(r, g, b, a);
        buffer.vertex(matrix, -0.4f, 0.1f, 0).color(r, g, b, a);
        buffer.vertex(matrix, -0.45f, -0.1f, 0).color(r, g, b, a);
        
        // 3. Left bottom
        buffer.vertex(matrix, 0, 0, 0).color(r, g, b, a);
        buffer.vertex(matrix, -0.45f, -0.1f, 0).color(r, g, b, a);
        buffer.vertex(matrix, 0, -0.45f, 0).color(r, g, b, a);
        
        // 4. Right bottom
        buffer.vertex(matrix, 0, 0, 0).color(r, g, b, a);
        buffer.vertex(matrix, 0, -0.45f, 0).color(r, g, b, a);
        buffer.vertex(matrix, 0.45f, -0.1f, 0).color(r, g, b, a);
        
        // 5. Right side rounding
        buffer.vertex(matrix, 0, 0, 0).color(r, g, b, a);
        buffer.vertex(matrix, 0.45f, -0.1f, 0).color(r, g, b, a);
        buffer.vertex(matrix, 0.4f, 0.1f, 0).color(r, g, b, a);
        
        // 6. Right lobe
        buffer.vertex(matrix, 0, 0, 0).color(r, g, b, a);
        buffer.vertex(matrix, 0.4f, 0.1f, 0).color(r, g, b, a);
        buffer.vertex(matrix, 0.2f, 0.35f, 0).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        matrices.pop();
    }
}
