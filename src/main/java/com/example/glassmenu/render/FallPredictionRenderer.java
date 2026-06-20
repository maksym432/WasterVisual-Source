package com.example.glassmenu.render;

import com.example.glassmenu.GlassMenuClient;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class FallPredictionRenderer {

    private static double lastMinX = Double.NaN;
    private static double lastMinY = Double.NaN;
    private static double lastMinZ = Double.NaN;
    private static double lastMaxX = Double.NaN;
    private static double lastMaxY = Double.NaN;
    private static double lastMaxZ = Double.NaN;
    
    private static float globalAlpha = 0.0f;
    private static long lastTime = 0;

    public static void render(WorldRenderContext context) {
        if (!GlassMenuClient.CONFIG.enableFallPrediction()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        BlockPos targetPos = null;
        Vec3d vel = client.player.getVelocity();
        
        // Trigger if falling (vel.y < -0.05) and not on ground/flying/swimming
        if (vel.y < -0.05 && !client.player.isOnGround() && !client.player.getAbilities().flying && !client.player.isTouchingWater()) {
            targetPos = findLandingBlock(client.player, client.world);
        }

        long now = System.currentTimeMillis();
        float delta = lastTime == 0 ? 0.016f : (now - lastTime) / 1000f;
        lastTime = now;
        if (delta < 0f) delta = 0f;
        delta = Math.min(delta, 0.1f);

        if (targetPos != null) {
            globalAlpha = Math.min(1.0f, globalAlpha + delta * 9.0f);
        } else {
            globalAlpha = Math.max(0.0f, globalAlpha - delta * 15.0f); // Fast fade out when not falling
        }

        if (globalAlpha <= 0.0f) {
            lastMinX = Double.NaN;
            lastMinY = Double.NaN;
            lastMinZ = Double.NaN;
            lastMaxX = Double.NaN;
            lastMaxY = Double.NaN;
            lastMaxZ = Double.NaN;
            return;
        }

        if (targetPos != null) {
            double targetMinX = targetPos.getX();
            double targetMinY = targetPos.getY();
            double targetMinZ = targetPos.getZ();
            double targetMaxX = targetMinX + 1.0;
            double targetMaxY = targetMinY + 1.0;
            double targetMaxZ = targetMinZ + 1.0;

            if (Double.isNaN(lastMinX)) {
                lastMinX = targetMinX;
                lastMinY = targetMinY;
                lastMinZ = targetMinZ;
                lastMaxX = targetMaxX;
                lastMaxY = targetMaxY;
                lastMaxZ = targetMaxZ;
            } else {
                double dx = targetMinX - lastMinX;
                double dy = targetMinY - lastMinY;
                double dz = targetMinZ - lastMinZ;
                double distSq = dx * dx + dy * dy + dz * dz;
                
                if (distSq > 100.0) { // Reset if distance is too large
                    lastMinX = targetMinX;
                    lastMinY = targetMinY;
                    lastMinZ = targetMinZ;
                    lastMaxX = targetMaxX;
                    lastMaxY = targetMaxY;
                    lastMaxZ = targetMaxZ;
                    dx = 0; dy = 0; dz = 0;
                }

                double leadRate = 0.68f;
                double trailRate = 0.24f;

                double blendMinX = (dx > 0) ? trailRate : leadRate;
                double blendMaxX = (dx > 0) ? leadRate : trailRate;
                double blendMinY = (dy > 0) ? trailRate : leadRate;
                double blendMaxY = (dy > 0) ? leadRate : trailRate;
                double blendMinZ = (dz > 0) ? trailRate : leadRate;
                double blendMaxZ = (dz > 0) ? leadRate : trailRate;

                double minBlendX = 1.0 - Math.pow(1.0 - blendMinX, delta * 60.0f);
                double maxBlendX = 1.0 - Math.pow(1.0 - blendMaxX, delta * 60.0f);
                double minBlendY = 1.0 - Math.pow(1.0 - blendMinY, delta * 60.0f);
                double maxBlendY = 1.0 - Math.pow(1.0 - blendMaxY, delta * 60.0f);
                double minBlendZ = 1.0 - Math.pow(1.0 - blendMinZ, delta * 60.0f);
                double maxBlendZ = 1.0 - Math.pow(1.0 - blendMaxZ, delta * 60.0f);

                lastMinX = lastMinX + dx * minBlendX;
                lastMinY = lastMinY + dy * minBlendY;
                lastMinZ = lastMinZ + dz * minBlendZ;

                lastMaxX = lastMaxX + (targetMaxX - lastMaxX) * maxBlendX;
                lastMaxY = lastMaxY + (targetMaxY - lastMaxY) * maxBlendY;
                lastMaxZ = lastMaxZ + (targetMaxZ - lastMaxZ) * maxBlendZ;
            }
        }

        if (Double.isNaN(lastMinX)) return;

        MatrixStack matrices = context.matrixStack();
        Vec3d camPos = context.camera().getPos();

        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        net.minecraft.client.gl.ShaderProgram previousShader = RenderSystem.getShader();
        boolean previousBlend = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        Tessellator tessellator = Tessellator.getInstance();
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        int configColor = GlassMenuClient.CONFIG.fallPredictionColor();
        float r = ((configColor >> 16) & 0xFF) / 255.0f;
        float g = ((configColor >> 8) & 0xFF) / 255.0f;
        float b = (configColor & 0xFF) / 255.0f;

        float offset = -0.002f;
        float minX = (float) lastMinX + offset;
        float minY = (float) lastMinY + offset;
        float minZ = (float) lastMinZ + offset;
        float maxX = (float) lastMaxX - offset;
        float maxY = (float) lastMaxY - offset;
        float maxZ = (float) lastMaxZ - offset;

        float pulseTime = (float) (now % 100000) / 1000f;
        float pulse = 0.85f + 0.15f * (float) Math.sin(pulseTime * 6.0f);

        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        drawAllEdges(buffer, matrix, 0.035f * pulse, r, g, b, 0.12f * pulse * globalAlpha, minX, maxX, minY, maxY, minZ, maxZ);
        drawAllEdges(buffer, matrix, 0.015f * pulse, r, g, b, 0.35f * pulse * globalAlpha, minX, maxX, minY, maxY, minZ, maxZ);

        float cr = Math.min(1.0f, (r + 1.5f) / 2.5f);
        float cg = Math.min(1.0f, (g + 1.5f) / 2.5f);
        float cb = Math.min(1.0f, (b + 1.5f) / 2.5f);
        drawAllEdges(buffer, matrix, 0.005f, cr, cg, cb, 0.90f * globalAlpha, minX, maxX, minY, maxY, minZ, maxZ);

        // Solid background fill (Volumetric Glass Effect)
        drawSolidBox(buffer, matrix, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, 0.20f * globalAlpha);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

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
        matrices.pop();
    }

    private static BlockPos findLandingBlock(net.minecraft.client.network.ClientPlayerEntity player, net.minecraft.client.world.ClientWorld world) {
        Vec3d pos = player.getPos();
        Vec3d vel = player.getVelocity();
        
        // Predict future position based on velocity. If mostly falling straight down, keep it tight.
        double speedSq = vel.x * vel.x + vel.z * vel.z;
        double lookaheadTicks = speedSq > 0.005 ? 3.0 : 0.0; 
        
        net.minecraft.util.math.Box bbox = player.getBoundingBox().offset(vel.x * lookaheadTicks, 0, vel.z * lookaheadTicks);
        
        // Start checking from just above the player's feet to catch blocks they might be grazing.
        int startY = (int) Math.floor(pos.y + 0.1); 
        
        for (int y = startY; y > startY - 40; y--) {
            int minX = (int) Math.floor(bbox.minX);
            int maxX = (int) Math.floor(bbox.maxX);
            int minZ = (int) Math.floor(bbox.minZ);
            int maxZ = (int) Math.floor(bbox.maxZ);
            
            BlockPos bestPos = null;
            double bestDist = Double.MAX_VALUE;
            double centerX = (bbox.minX + bbox.maxX) / 2.0;
            double centerZ = (bbox.minZ + bbox.maxZ) / 2.0;
            
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos bp = new BlockPos(x, y, z);
                    net.minecraft.block.BlockState state = world.getBlockState(bp);
                    
                    if (!state.isAir() && !state.getCollisionShape(world, bp).isEmpty()) {
                        net.minecraft.util.shape.VoxelShape shape = state.getCollisionShape(world, bp);
                        net.minecraft.util.math.Box blockBox = shape.getBoundingBox().offset(bp);
                        
                        // Check if the block's collision box overlaps with the player's XZ bounding box footprint.
                        if (bbox.intersects(blockBox.minX, bbox.minY - 100.0, blockBox.minZ, blockBox.maxX, bbox.maxY + 100.0, blockBox.maxZ)) {
                            // If multiple blocks support the player at this level, pick the most central one.
                            double dx = (bp.getX() + 0.5) - centerX;
                            double dz = (bp.getZ() + 0.5) - centerZ;
                            double dist = dx * dx + dz * dz;
                            if (dist < bestDist) {
                                bestDist = dist;
                                bestPos = bp;
                            }
                        }
                    }
                }
            }
            if (bestPos != null) return bestPos;
        }
        return null;
    }

    private static void drawAllEdges(BufferBuilder buffer, Matrix4f matrix, float w, float r, float g, float b, float a,
                                     float minX, float maxX, float minY, float maxY, float minZ, float maxZ) {
        drawBlockEdge(buffer, matrix, 0, minY, minZ, w, r, g, b, a, minX, maxX);
        drawBlockEdge(buffer, matrix, 0, maxY, minZ, w, r, g, b, a, minX, maxX);
        drawBlockEdge(buffer, matrix, 0, minY, maxZ, w, r, g, b, a, minX, maxX);
        drawBlockEdge(buffer, matrix, 0, maxY, maxZ, w, r, g, b, a, minX, maxX);

        drawBlockEdge(buffer, matrix, 1, minX, minZ, w, r, g, b, a, minY, maxY);
        drawBlockEdge(buffer, matrix, 1, maxX, minZ, w, r, g, b, a, minY, maxY);
        drawBlockEdge(buffer, matrix, 1, minX, maxZ, w, r, g, b, a, minY, maxY);
        drawBlockEdge(buffer, matrix, 1, maxX, maxZ, w, r, g, b, a, minY, maxY);

        drawBlockEdge(buffer, matrix, 2, minX, minY, w, r, g, b, a, minZ, maxZ);
        drawBlockEdge(buffer, matrix, 2, maxX, minY, w, r, g, b, a, minZ, maxZ);
        drawBlockEdge(buffer, matrix, 2, minX, maxY, w, r, g, b, a, minZ, maxZ);
        drawBlockEdge(buffer, matrix, 2, maxX, maxY, w, r, g, b, a, minZ, maxZ);
    }

    private static void drawBlockEdge(BufferBuilder buffer, Matrix4f matrix, int axis, float c1, float c2, float w, float r, float g, float b, float a, float minVal, float maxVal) {
        float xMin, xMax, yMin, yMax, zMin, zMax;
        if (axis == 0) {
            xMin = minVal; xMax = maxVal;
            yMin = c1 - w; yMax = c1 + w;
            zMin = c2 - w; zMax = c2 + w;
        } else if (axis == 1) {
            xMin = c1 - w; xMax = c1 + w;
            yMin = minVal; yMax = maxVal;
            zMin = c2 - w; zMax = c2 + w;
        } else {
            xMin = c1 - w; xMax = c1 + w;
            yMin = c2 - w; yMax = c2 + w;
            zMin = minVal; zMax = maxVal;
        }
        drawSolidBox(buffer, matrix, xMin, yMin, zMin, xMax, yMax, zMax, r, g, b, a);
    }

    private static void drawSolidBox(BufferBuilder buffer, Matrix4f matrix,
                                     float xMin, float yMin, float zMin,
                                     float xMax, float yMax, float zMax,
                                     float r, float g, float b, float a) {
        buffer.vertex(matrix, xMin, yMin, zMin).color(r, g, b, a);
        buffer.vertex(matrix, xMax, yMin, zMin).color(r, g, b, a);
        buffer.vertex(matrix, xMax, yMin, zMax).color(r, g, b, a);
        buffer.vertex(matrix, xMin, yMin, zMax).color(r, g, b, a);
        
        buffer.vertex(matrix, xMin, yMax, zMin).color(r, g, b, a);
        buffer.vertex(matrix, xMin, yMax, zMax).color(r, g, b, a);
        buffer.vertex(matrix, xMax, yMax, zMax).color(r, g, b, a);
        buffer.vertex(matrix, xMax, yMax, zMin).color(r, g, b, a);
        
        buffer.vertex(matrix, xMin, yMin, zMin).color(r, g, b, a);
        buffer.vertex(matrix, xMin, yMax, zMin).color(r, g, b, a);
        buffer.vertex(matrix, xMax, yMax, zMin).color(r, g, b, a);
        buffer.vertex(matrix, xMax, yMin, zMin).color(r, g, b, a);
        
        buffer.vertex(matrix, xMin, yMin, zMax).color(r, g, b, a);
        buffer.vertex(matrix, xMax, yMin, zMax).color(r, g, b, a);
        buffer.vertex(matrix, xMax, yMax, zMax).color(r, g, b, a);
        buffer.vertex(matrix, xMin, yMax, zMax).color(r, g, b, a);
        
        buffer.vertex(matrix, xMin, yMin, zMin).color(r, g, b, a);
        buffer.vertex(matrix, xMin, yMin, zMax).color(r, g, b, a);
        buffer.vertex(matrix, xMin, yMax, zMax).color(r, g, b, a);
        buffer.vertex(matrix, xMin, yMax, zMin).color(r, g, b, a);
        
        buffer.vertex(matrix, xMax, yMin, zMin).color(r, g, b, a);
        buffer.vertex(matrix, xMax, yMax, zMin).color(r, g, b, a);
        buffer.vertex(matrix, xMax, yMax, zMax).color(r, g, b, a);
        buffer.vertex(matrix, xMax, yMin, zMax).color(r, g, b, a);
    }
}
