/*
 * JumpRingsManager - Architecture & Primary Responsibility:
 * Jump Pulse Rings Manager.
 * Renders expanding rings on the ground when the player jumps using
 * AFTER_ENTITIES render event from Fabric API, supporting both smooth
 * circles and liquid block boundary stretching modes.
 */
package com.example.glassmenu.render;

import com.example.glassmenu.GlassMenuClient;
import com.example.glassmenu.GlassMenuConfigModel;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Jump Pulse Rings Manager.
 * Renders expanding rings on the ground when the player jumps.
 */
public class JumpRingsManager {
    private static final List<Pulse> pulses = new ArrayList<>();
    private static boolean wasOnGround = true;

    public static void init() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            render(context.matrixStack(), context.tickCounter().getTickDelta(false));
        });
    }

    public static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        boolean onGround = client.player.isOnGround();
        // Detect jump: was on ground, now isn't, and moving upwards
        if (wasOnGround && !onGround && client.player.getVelocity().y > 0.1) {
            if (GlassMenuClient.CONFIG.enableJumpRings()) {
                // Use previous position for more accurate ground locking
                double x = client.player.prevX;
                double y = client.player.prevY;
                double z = client.player.prevZ;
                
                net.minecraft.util.math.BlockPos bp = net.minecraft.util.math.BlockPos.ofFloored(x, y, z);
                net.minecraft.block.BlockState bs = client.world.getBlockState(bp);
                
                double groundY = y;
                // Check block at feet first (slabs, carpets, snow layers)
                if (!bs.isAir() && !bs.getCollisionShape(client.world, bp).isEmpty()) {
                    groundY = bp.getY() + bs.getCollisionShape(client.world, bp).getMax(net.minecraft.util.math.Direction.Axis.Y);
                } else {
                    // Check block below feet
                    bp = bp.down();
                    bs = client.world.getBlockState(bp);
                    if (!bs.isAir() && !bs.getCollisionShape(client.world, bp).isEmpty()) {
                        groundY = bp.getY() + bs.getCollisionShape(client.world, bp).getMax(net.minecraft.util.math.Direction.Axis.Y);
                    }
                }
                
                pulses.add(new Pulse(x, groundY, z));
            }
        }
        wasOnGround = onGround;

        pulses.forEach(p -> p.age++);
        pulses.removeIf(p -> p.age > 45); // Slightly longer life for better pulsation
    }

    public static void render(MatrixStack matrices, float tickDelta) {
        if (pulses.isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;
        Vec3d cameraPos = client.gameRenderer.getCamera().getPos();

        int color = GlassMenuClient.CONFIG.jumpRingsColor();
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        GlassMenuConfigModel.JumpRingMode mode = GlassMenuClient.CONFIG.jumpRingMode();

        for (Pulse p : pulses) {
            float life = (p.age + tickDelta) / 30f; // Smoother lifecycle
            if (life > 1.0f) continue;
            
            float alpha = 1.0f - life;
            float pulse = (float) Math.sin((p.age + tickDelta) * 0.4f) * 0.05f;

            if (mode == GlassMenuConfigModel.JumpRingMode.CIRCLE) {
                // CIRCLE: Locked to 4 block range, stays more central
                float radius = 0.5f + (life * 1.5f) + pulse; 
                matrices.push();
                matrices.translate(p.x - cameraPos.x, p.y - cameraPos.y, p.z - cameraPos.z);
                float rotation = (p.age + tickDelta) * 4.0f;
                drawRing(matrices.peek().getPositionMatrix(), radius, r, g, b, alpha, rotation);
                matrices.pop();
            } else {
                // BLOCK_OUTLINE: Faster and larger 8 block range
                float radius = 0.3f + (life * 7.5f) + pulse; 
                renderBlockOutlines(matrices, client, cameraPos, p, radius, r, g, b, alpha, tickDelta);
            }
        }

        RenderSystem.depthMask(true);
    }

    private static void renderBlockOutlines(MatrixStack matrices, MinecraftClient client, Vec3d cameraPos, Pulse p, float radius, float r, float g, float b, float a, float tickDelta) {
        if (client.world == null) return;

        // EXPANDED RANGE TO 8 BLOCKS
        int range = Math.min(8, (int) Math.ceil(radius) + 1);
        net.minecraft.util.math.BlockPos center = net.minecraft.util.math.BlockPos.ofFloored(p.x, p.y - 0.1, p.z);

        RenderSystem.lineWidth(7.0f); 

        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                net.minecraft.util.math.BlockPos pos = center.add(x, 0, z);
                if (!client.world.isPosLoaded(pos.getX(), pos.getZ())) continue;

                net.minecraft.block.BlockState state = client.world.getBlockState(pos);

                if (state.isAir()) {
                    pos = pos.down();
                    if (!client.world.isPosLoaded(pos.getX(), pos.getZ())) continue;
                    state = client.world.getBlockState(pos);
                    if (state.isAir()) continue;
                }

                net.minecraft.util.shape.VoxelShape shape = state.getCollisionShape(client.world, pos);
                if (shape.isEmpty()) continue;
                
                double maxY = shape.getMax(net.minecraft.util.math.Direction.Axis.Y);
                double surfaceY = pos.getY() + maxY;
                
                if (Math.abs(surfaceY - p.y) > 0.5) continue;
                if (!client.world.getBlockState(pos.up()).isAir() && client.world.getBlockState(pos.up()).isFullCube(client.world, pos.up())) continue;

                double dx = (pos.getX() + 0.5) - p.x;
                double dz = (pos.getZ() + 0.5) - p.z;
                double dist = Math.sqrt(dx * dx + dz * dz);
                
                // LIQUID STRETCH LOGIC (Improved for larger range)
                float frontEdge = 0.2f; 
                float backEdge = 0.8f; // Longer tail for dramatic stretch
                
                float localAlpha = 0;
                if (dist >= radius - backEdge && dist <= radius + frontEdge) {
                    if (dist > radius) {
                        localAlpha = a * (1.0f - (float)(dist - radius) / frontEdge);
                    } else {
                        localAlpha = a * (1.0f - (float)(radius - dist) / backEdge);
                    }
                    
                    matrices.push();
                    matrices.translate(pos.getX() - cameraPos.x, surfaceY + 0.002 - cameraPos.y, pos.getZ() - cameraPos.z);
                    
                    drawSquareOutline(matrices.peek().getPositionMatrix(), r, g, b, localAlpha * 0.4f);
                    
                    matrices.push();
                    matrices.translate(0.1, 0, 0.1);
                    matrices.scale(0.8f, 1f, 0.8f);
                    drawFullSquareOutline(matrices.peek().getPositionMatrix(), r, g, b, localAlpha * 0.6f);
                    matrices.pop();

                    drawFullSquareOutline(matrices.peek().getPositionMatrix(), r, g, b, localAlpha);
                    
                    matrices.pop();
                }
            }
        }
        RenderSystem.lineWidth(1.0f);
    }

    private static void drawSquareOutline(Matrix4f matrix, float r, float g, float b, float a) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        
        buffer.vertex(matrix, 0, 0, 0).color(r, g, b, a * 0.3f);
        buffer.vertex(matrix, 0, 0, 1).color(r, g, b, a * 0.3f);
        buffer.vertex(matrix, 1, 0, 1).color(r, g, b, a * 0.3f);
        buffer.vertex(matrix, 1, 0, 0).color(r, g, b, a * 0.3f);
        
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private static void drawFullSquareOutline(Matrix4f matrix, float r, float g, float b, float a) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        
        buffer.vertex(matrix, 0, 0, 0).color(r, g, b, a);
        buffer.vertex(matrix, 1, 0, 0).color(r, g, b, a);
        buffer.vertex(matrix, 1, 0, 1).color(r, g, b, a);
        buffer.vertex(matrix, 0, 0, 1).color(r, g, b, a);
        buffer.vertex(matrix, 0, 0, 0).color(r, g, b, a);
        
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private static void drawRing(Matrix4f matrix, float radius, float r, float g, float b, float a, float rotation) {
        Tessellator tessellator = Tessellator.getInstance();
        
        // SHARP NEON LINE RENDERING
        RenderSystem.lineWidth(7.0f);
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        
        int segments = 120; // High precision for sharpness
        float radRot = rotation * (float)Math.PI / 180f;
        
        // Main sharp outer ring
        for (int i = 0; i <= segments; i++) {
            float angle = i * (float) Math.PI * 2 / segments;
            float bump = (float) Math.sin(angle * 5.0f + radRot * 2.0f) * 0.12f;
            float currentRadius = radius + bump;
            
            float cos = MathHelper.cos(angle + radRot);
            float sin = MathHelper.sin(angle + radRot);
            
            buffer.vertex(matrix, cos * currentRadius, 0, sin * currentRadius).color(r, g, b, a);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        // Nested sharp inner ring (0.85x scale) for high-fidelity look
        RenderSystem.lineWidth(4.0f);
        buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            float angle = i * (float) Math.PI * 2 / segments;
            float bump = (float) Math.sin(angle * 5.0f + radRot * 2.0f) * 0.1f;
            float currentRadius = (radius * 0.85f) + bump;
            
            float cos = MathHelper.cos(angle + radRot);
            float sin = MathHelper.sin(angle + radRot);
            
            buffer.vertex(matrix, cos * currentRadius, 0, sin * currentRadius).color(r, g, b, a * 0.7f);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        
        // Reset line width
        RenderSystem.lineWidth(1.0f);
    }

    private static class Pulse {
        final double x, y, z;
        int age = 0;

        Pulse(double x, double y, double z) {
            this.x = x; this.y = y; this.z = z;
        }
    }
}
