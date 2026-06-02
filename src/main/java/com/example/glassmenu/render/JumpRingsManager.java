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
import com.example.glassmenu.shader.ModShaders;
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

        net.minecraft.client.gl.ShaderProgram originalShader = RenderSystem.getShader();
        boolean originalBlend = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SrcFactor.SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.DstFactor.ONE);
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
                float rotation = (p.age + tickDelta) * 4.0f;
                drawRing(matrices, client.world, cameraPos, p, radius, r, g, b, alpha, rotation);
            } else {
                // BLOCK_OUTLINE: Faster and larger 8 block range
                float radius = 0.3f + (life * 7.5f) + pulse; 
                renderBlockOutlines(matrices, client, cameraPos, p, radius, r, g, b, alpha, tickDelta);
            }
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        if (!originalBlend) {
            RenderSystem.disableBlend();
        } else {
            RenderSystem.enableBlend();
        }
        if (originalShader != null) {
            RenderSystem.setShader(() -> originalShader);
        }
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
                if (shape.isEmpty()) {
                    String translationKey = state.getBlock().getTranslationKey();
                    boolean isWalkableLayer = translationKey.contains("snow") 
                                           || translationKey.contains("carpet") 
                                           || translationKey.contains("lily") 
                                           || translationKey.contains("pad");
                    if (isWalkableLayer) {
                        shape = state.getOutlineShape(client.world, pos);
                    }
                }
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
                    matrices.translate(pos.getX() - cameraPos.x, surfaceY + 0.08 - cameraPos.y, pos.getZ() - cameraPos.z);
                    
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

    private static double getTerrainHeight(net.minecraft.client.world.ClientWorld world, double x, double z, double defaultY) {
        int floorX = net.minecraft.util.math.MathHelper.floor(x);
        int floorZ = net.minecraft.util.math.MathHelper.floor(z);
        
        // Scan a 6-block vertical range from defaultY + 3.0 down to defaultY - 3.0
        int startY = net.minecraft.util.math.MathHelper.floor(defaultY) + 3;
        int endY = net.minecraft.util.math.MathHelper.floor(defaultY) - 3;
        
        double bestY = defaultY;
        boolean found = false;
        
        for (int y = startY; y >= endY; y--) {
            net.minecraft.util.math.BlockPos bp = new net.minecraft.util.math.BlockPos(floorX, y, floorZ);
            if (!world.isPosLoaded(bp.getX(), bp.getZ())) continue;
            
            net.minecraft.block.BlockState state = world.getBlockState(bp);
            if (state.isAir()) continue;
            
            // Check for fluid first (so rings float on water/lava)
            if (!state.getFluidState().isEmpty()) {
                float fluidHeight = state.getFluidState().getHeight(world, bp);
                bestY = bp.getY() + fluidHeight;
                found = true;
                break;
            }
            
            // Get shape - fallback to outline shape for walkable non-solid blocks
            net.minecraft.util.shape.VoxelShape shape = state.getCollisionShape(world, bp);
            boolean isFoliage = shape.isEmpty();
            
            if (isFoliage) {
                // Check if it's a known walkable/layered block like snow layers, carpets, lily pads
                String translationKey = state.getBlock().getTranslationKey();
                boolean isWalkableLayer = translationKey.contains("snow") 
                                       || translationKey.contains("carpet") 
                                       || translationKey.contains("lily") 
                                       || translationKey.contains("pad");
                if (isWalkableLayer) {
                    shape = state.getOutlineShape(world, bp);
                } else {
                    // Ignore other non-solid blocks (grass, flowers, ferns, etc.)
                    continue;
                }
            }
            
            if (!shape.isEmpty()) {
                double maxY = shape.getMax(net.minecraft.util.math.Direction.Axis.Y);
                bestY = bp.getY() + maxY;
                found = true;
                break;
            }
        }
        return found ? bestY : defaultY;
    }

    private static double getTerrainHeightAt(net.minecraft.client.world.ClientWorld world, int floorX, int floorZ, double referenceY) {
        int refY = net.minecraft.util.math.MathHelper.floor(referenceY);
        
        // Scan around referenceY: refY + 2 down to refY - 2 (5 blocks total)
        for (int y = refY + 2; y >= refY - 2; y--) {
            net.minecraft.util.math.BlockPos bp = new net.minecraft.util.math.BlockPos(floorX, y, floorZ);
            if (!world.isPosLoaded(bp.getX(), bp.getZ())) continue;
            
            net.minecraft.block.BlockState state = world.getBlockState(bp);
            if (state.isAir()) continue;
            
            // Check for fluid
            if (!state.getFluidState().isEmpty()) {
                return bp.getY() + state.getFluidState().getHeight(world, bp);
            }
            
            net.minecraft.util.shape.VoxelShape shape = state.getCollisionShape(world, bp);
            if (shape.isEmpty()) {
                String translationKey = state.getBlock().getTranslationKey();
                boolean isWalkableLayer = translationKey.contains("snow") 
                                       || translationKey.contains("carpet") 
                                       || translationKey.contains("lily") 
                                       || translationKey.contains("pad");
                if (isWalkableLayer) {
                    shape = state.getOutlineShape(world, bp);
                } else {
                    continue;
                }
            }
            
            if (!shape.isEmpty()) {
                return bp.getY() + shape.getMax(net.minecraft.util.math.Direction.Axis.Y);
            }
        }
        return referenceY;
    }

    private static Vec3d getSurfacePoint(net.minecraft.client.world.ClientWorld world, double centerX, double centerY, double centerZ, double dirX, double dirZ, double targetDistance) {
        double currentX = centerX;
        double currentZ = centerZ;
        double currentY = centerY;
        
        double traveled = 0.0;
        double step = 0.2; // 20cm steps for excellent balance of smoothness and performance
        
        double lastX = currentX;
        double lastZ = currentZ;
        double lastY = currentY;
        
        double prevX = currentX;
        double prevZ = currentZ;
        double prevY = currentY;
        double prevTraveled = 0.0;
        
        while (traveled < targetDistance) {
            double remaining = targetDistance - traveled;
            if (remaining < 0.001) {
                break;
            }
            
            prevX = currentX;
            prevY = currentY;
            prevZ = currentZ;
            prevTraveled = traveled;
            
            double currentStep = Math.min(step, remaining);
            
            currentX += dirX * currentStep;
            currentZ += dirZ * currentStep;
            
            int floorX = net.minecraft.util.math.MathHelper.floor(currentX);
            int floorZ = net.minecraft.util.math.MathHelper.floor(currentZ);
            
            // Query new height near the last known Y
            double newY = getTerrainHeightAt(world, floorX, floorZ, lastY);
            
            // Calculate 3D distance traveled
            double dx = currentX - lastX;
            double dz = currentZ - lastZ;
            double dy = newY - lastY;
            double stepDist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            
            if (stepDist < 0.001) {
                break; // Prevent infinite loop under floating point underflow
            }
            
            traveled += stepDist;
            
            lastX = currentX;
            lastZ = currentZ;
            lastY = newY;
            currentY = newY;
        }
        
        // Interpolate to get exact target distance
        if (traveled > targetDistance && (traveled - prevTraveled) > 0.001) {
            double t = (targetDistance - prevTraveled) / (traveled - prevTraveled);
            double interpX = prevX + (currentX - prevX) * t;
            double interpY = prevY + (lastY - prevY) * t;
            double interpZ = prevZ + (currentZ - prevZ) * t;
            return new Vec3d(interpX, interpY, interpZ);
        }
        
        return new Vec3d(lastX, lastY, lastZ);
    }

    private static void drawRing(MatrixStack matrices, net.minecraft.client.world.ClientWorld world, Vec3d cameraPos, Pulse p, float radius, float r, float g, float b, float a, float rotation) {
        Tessellator tessellator = Tessellator.getInstance();
        int segments = 120; // High precision for sharpness
        float radRot = rotation * (float)Math.PI / 180f;

        // Position matrices translated relative to camera, but absolute world Y is processed per vertex
        matrices.push();
        matrices.translate(p.x - cameraPos.x, -cameraPos.y, p.z - cameraPos.z);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // --- PHASE 1: Atmospheric filled shapes with jump_glow shader ---
        var glowShader = ModShaders.getJumpGlow();
        var previousShader = RenderSystem.getShader();
        if (glowShader != null) {
            RenderSystem.setShader(() -> glowShader);
            if (glowShader.getUniform("Time") != null) {
                glowShader.getUniform("Time").set((net.minecraft.util.Util.getMeasuringTimeMs() % 100000L) / 1000.0f);
            }
        }

        // Precalculate all outer and inner vertices to avoid duplicate raycasts
        float[] localXInners = new float[segments + 1];
        float[] localZInners = new float[segments + 1];
        double[] vertexYInners = new double[segments + 1];
        
        float[] localXOuters = new float[segments + 1];
        float[] localZOuters = new float[segments + 1];
        double[] vertexYOuters = new double[segments + 1];

        for (int i = 0; i <= segments; i++) {
            float angle = i * (float) Math.PI * 2 / segments;
            float bumpOuter = (float) Math.sin(angle * 5.0f + radRot * 2.0f) * 0.12f;
            float radiusOuter = radius + bumpOuter;
            
            float bumpInner = (float) Math.sin(angle * 5.0f + radRot * 2.0f) * 0.1f;
            float radiusInner = (radius * 0.85f) + bumpInner;
            
            float cos = MathHelper.cos(angle + radRot);
            float sin = MathHelper.sin(angle + radRot);
            
            Vec3d pointInner = getSurfacePoint(world, p.x, p.y, p.z, cos, sin, radiusInner);
            localXInners[i] = (float) (pointInner.x - p.x);
            localZInners[i] = (float) (pointInner.z - p.z);
            vertexYInners[i] = pointInner.y + 0.08;
            
            Vec3d pointOuter = getSurfacePoint(world, p.x, p.y, p.z, cos, sin, radiusOuter);
            localXOuters[i] = (float) (pointOuter.x - p.x);
            localZOuters[i] = (float) (pointOuter.z - p.z);
            vertexYOuters[i] = pointOuter.y + 0.08;
        }

        // 1. Soft inner star glow fading towards center
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_TEXTURE_COLOR);
        double centerY = getTerrainHeight(world, p.x, p.z, p.y) + 0.08;
        buffer.vertex(matrix, 0, (float) centerY, 0).texture(0, 0).color(r, g, b, 0.0f);
        for (int i = 0; i <= segments; i++) {
            buffer.vertex(matrix, localXInners[i], (float) vertexYInners[i], localZInners[i]).texture(localXInners[i], localZInners[i]).color(r, g, b, a * 0.2f);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        // 2. Glowing fill ribbon between inner and outer star
        buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_TEXTURE_COLOR);
        for (int i = 0; i <= segments; i++) {
            buffer.vertex(matrix, localXInners[i], (float) vertexYInners[i], localZInners[i]).texture(localXInners[i], localZInners[i]).color(r, g, b, a * 0.6f);
            buffer.vertex(matrix, localXOuters[i], (float) vertexYOuters[i], localZOuters[i]).texture(localXOuters[i], localZOuters[i]).color(r, g, b, a * 0.3f);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        // 3a. Volumetric outer border glow (3D depth wall - bottom half)
        buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_TEXTURE_COLOR);
        for (int i = 0; i <= segments; i++) {
            buffer.vertex(matrix, localXOuters[i], (float) (vertexYOuters[i] - 0.15), localZOuters[i]).texture(localXOuters[i], localZOuters[i]).color(r, g, b, 0.0f);
            buffer.vertex(matrix, localXOuters[i], (float) vertexYOuters[i], localZOuters[i]).texture(localXOuters[i], localZOuters[i]).color(r, g, b, a * 0.4f);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        // 3b. Volumetric outer border glow (3D depth wall - top half)
        buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_TEXTURE_COLOR);
        for (int i = 0; i <= segments; i++) {
            buffer.vertex(matrix, localXOuters[i], (float) vertexYOuters[i], localZOuters[i]).texture(localXOuters[i], localZOuters[i]).color(r, g, b, a * 0.4f);
            buffer.vertex(matrix, localXOuters[i], (float) (vertexYOuters[i] + 0.15), localZOuters[i]).texture(localXOuters[i], localZOuters[i]).color(r, g, b, 0.0f);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        // 4a. Volumetric inner border glow (3D depth wall - bottom half)
        buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_TEXTURE_COLOR);
        for (int i = 0; i <= segments; i++) {
            buffer.vertex(matrix, localXInners[i], (float) (vertexYInners[i] - 0.12), localZInners[i]).texture(localXInners[i], localZInners[i]).color(r, g, b, 0.0f);
            buffer.vertex(matrix, localXInners[i], (float) vertexYInners[i], localZInners[i]).texture(localXInners[i], localZInners[i]).color(r, g, b, a * 0.3f);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        // 4b. Volumetric inner border glow (3D depth wall - top half)
        buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_TEXTURE_COLOR);
        for (int i = 0; i <= segments; i++) {
            buffer.vertex(matrix, localXInners[i], (float) vertexYInners[i], localZInners[i]).texture(localXInners[i], localZInners[i]).color(r, g, b, a * 0.3f);
            buffer.vertex(matrix, localXInners[i], (float) (vertexYInners[i] + 0.12), localZInners[i]).texture(localXInners[i], localZInners[i]).color(r, g, b, 0.0f);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        if (glowShader != null) {
            RenderSystem.setShader(() -> previousShader);
        }

        // --- PHASE 2: Sharp Neon Borders ---
        // Main sharp outer ring
        RenderSystem.lineWidth(7.0f);
        buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        double lastX = 0, lastY = 0, lastZ = 0;
        boolean hasLast = false;
        for (int i = 0; i <= segments; i++) {
            float localX = localXOuters[i];
            float localZ = localZOuters[i];
            double vertexY = vertexYOuters[i];
            
            if (hasLast && Math.abs(vertexY - lastY) > 0.05) {
                if (vertexY > lastY) {
                    buffer.vertex(matrix, (float) lastX, (float) vertexY, (float) lastZ).color(r, g, b, a);
                } else {
                    buffer.vertex(matrix, localX, (float) lastY, localZ).color(r, g, b, a);
                }
            }
            
            buffer.vertex(matrix, localX, (float) vertexY, localZ).color(r, g, b, a);
            lastX = localX;
            lastY = vertexY;
            lastZ = localZ;
            hasLast = true;
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        // Nested sharp inner ring (0.85x scale) for high-fidelity look
        RenderSystem.lineWidth(4.0f);
        buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        hasLast = false;
        for (int i = 0; i <= segments; i++) {
            float localX = localXInners[i];
            float localZ = localZInners[i];
            double vertexY = vertexYInners[i];
            
            if (hasLast && Math.abs(vertexY - lastY) > 0.05) {
                if (vertexY > lastY) {
                    buffer.vertex(matrix, (float) lastX, (float) vertexY, (float) lastZ).color(r, g, b, a * 0.7f);
                } else {
                    buffer.vertex(matrix, localX, (float) lastY, localZ).color(r, g, b, a * 0.7f);
                }
            }
            
            buffer.vertex(matrix, localX, (float) vertexY, localZ).color(r, g, b, a * 0.7f);
            lastX = localX;
            lastY = vertexY;
            lastZ = localZ;
            hasLast = true;
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        
        // Reset line width
        RenderSystem.lineWidth(1.0f);
        
        matrices.pop();
    }

    private static class Pulse {
        final double x, y, z;
        int age = 0;

        Pulse(double x, double y, double z) {
            this.x = x; this.y = y; this.z = z;
        }
    }
}
