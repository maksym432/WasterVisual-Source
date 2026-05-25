/**
 * TargetESPManager.java
 *
 * Primary Responsibility:
 * Renders an elegant 3D wrapping coil spiral (Target ESP) around the player's current combat target in the world,
 * as well as inside the LiquidGlass configuration menu preview.
 *
 * Architectural & Rendering Pipeline Role:
 * - Listens to net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.LAST to draw graphics in world space
 *   without interference from shader/culling optimizations.
 * - Utilizes Painter's Algorithm (sorting particles back-to-front by squared distance from camera) to handle
 *   transparency alpha blending perfectly without OpenGL sorting glitches.
 * - Applies true Quaternion Billboarding to keep flat particle droplets facing the player camera at all times.
 *
 * Key Mathematical & Technical Aspects:
 * - Dynamic spiral kinematics: 3 interwoven helical trails rotating in horizontal and vertical sine-cosine waves.
 * - Mock UI preview scaling: In-game entity bounds (1.8m height, 0.6m radius) are scaled in the GUI preview
 *   using height=60.0f and radius=22.0f. This perfectly matches the proportions of the GUI player preview
 *   and stops the target spiral from appearing flat or squashed.
 * - Volumetric depth shading: Droplets are drawn in 3 layered, Z-offset circles (glow backdrop, body, bright core)
 *   producing a brilliant physical glow effect.
 */
package com.example.glassmenu.render;

import com.example.glassmenu.GlassMenuClient;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TargetESPManager {
    private static Entity targetEntity = null;
    private static long lastTargetTime = 0;
    private static final long PERSISTENCE_MS = 5000;
    private static final long START_TIME = System.currentTimeMillis();

    /**
     * Data structure for the Painter's Algorithm sorting.
     */
    private record DropData(float ox, float oy, float oz, float size, float alpha, double distSq) {}

    public static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        Entity crosshairTarget = client.targetedEntity;
        if (crosshairTarget instanceof LivingEntity && crosshairTarget != client.player) {
            targetEntity = crosshairTarget;
            lastTargetTime = System.currentTimeMillis();
        }

        if (targetEntity != null && (System.currentTimeMillis() - lastTargetTime > PERSISTENCE_MS || !targetEntity.isAlive())) {
            targetEntity = null;
        }
    }

    /**
     * Entry point from WorldRenderEvents.LAST.
     */
    public static void render(net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext context) {
        if (!GlassMenuClient.CONFIG.enableTargetEsp() || targetEntity == null) return;
        
        MatrixStack matrices = context.matrixStack();
        float tickDelta = context.tickCounter().getTickDelta(false);
        Camera camera = context.camera();
        Vec3d camPos = camera.getPos();
        
        // 1. Calculate Target Interpolated World Position
        double targetX = MathHelper.lerp(tickDelta, targetEntity.prevX, targetEntity.getX());
        double targetY = MathHelper.lerp(tickDelta, targetEntity.prevY, targetEntity.getY());
        double targetZ = MathHelper.lerp(tickDelta, targetEntity.prevZ, targetEntity.getZ());

        // 2. Center position relative to camera
        double renderX = targetX - camPos.x;
        double renderY = targetY - camPos.y;
        double renderZ = targetZ - camPos.z;

        matrices.push();
        matrices.translate(renderX, renderY, renderZ);
        
        float baseWidth = targetEntity.getWidth();
        // COMPACT TIGHT RADIUS
        float dynamicRadius = (baseWidth / 2.0f) + 0.38f; 
        float renderHeight = targetEntity.getHeight();
        // Call the underlying spiral render engine. In world space (camera != null), the 10th parameter (rotationDegrees)
        // is ignored because the orientation is driven by camera rotations. We pass 0f as a default placeholder value.
        renderCoilSpiral(matrices, camera, renderHeight, dynamicRadius, tickDelta, camPos, targetX, targetY, targetZ, 0f);
        
        matrices.pop();
    }

    /**
     * Renders the Target ESP spiral inside the GUI screen preview (Combat Tab).
     * Scaled appropriately to match the InventoryScreen's player entity preview perfectly.
     *
     * @param matrices        The current GUI render matrix stack.
     * @param x               The X coordinate center.
     * @param y               The Y coordinate center (aligned with the player's feet).
     * @param scale           Scale factor.
     * @param rotationDegrees Rotation angle driven by user drag.
     */
    public static void renderGuiTargetSight(MatrixStack matrices, int x, int y, float scale, float rotationDegrees) {
        matrices.push();
        // 1. Shift the origin to the feet of the player entity preview.
        matrices.translate(x, y, 40); 
        
        // 2. CRITICAL FIX: Invert the Y-axis (-scale) in the GUI preview.
        // In Minecraft's GUI space, the positive Y direction points DOWNWARDS, whereas in the world/entity space
        // positive Y points UPWARDS. If we do not invert the Y scale here, the vertical translation (offsetY > 0)
        // causes the spiral to render completely upside-down, stretching far below the player's feet.
        // Inverting Y aligns the coordinate system so that positive offsetY translates particles upwards!
        matrices.scale(scale, -scale, 1);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationDegrees));
        
        // 3. Align the height and radius with the exact proportions of the player model in the GUI (Height 60.0f, Radius 22.0f).
        // This ensures the 3D helical particles hug the player's body and wrap around them exactly like in-game combat.
        renderCoilSpiral(matrices, null, 60.0f, 22.0f, 0f, Vec3d.ZERO, 0, 0, 0, rotationDegrees); 
        matrices.pop();
    }

    private static void renderCoilSpiral(MatrixStack matrices, Camera camera, float entityHeight, float baseRadius, float tickDelta, Vec3d camPos, double targetX, double targetY, double targetZ, float rotationDegrees) {
        int color = GlassMenuClient.CONFIG.targetEspColor();
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        VertexConsumerProvider.Immediate provider = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false); 

        double relativeTime = (double)(System.currentTimeMillis() - START_TIME) / 1000.0;
        // SLIGHTLY FASTER KINETICS
        double horizontalSpeed = 5.2; 
        double verticalSpeed = 0.8;
        float verticalAmplitude = entityHeight * 0.45f;

        List<DropData> drops = new ArrayList<>();
        int trailCount = 3;
        int segments = 45;

        for (int i = 0; i < trailCount; i++) {
            float phaseOffset = i * (float) (Math.PI * 2.0 / 3.0);
            
            for (int s = 0; s < segments; s++) {
                double timeOffset = s * 0.016;
                double t = relativeTime - timeOffset;
                
                float offsetX = (float) (baseRadius * Math.cos(t * horizontalSpeed + phaseOffset));
                float offsetZ = (float) (baseRadius * Math.sin(t * horizontalSpeed + phaseOffset));
                float offsetY = (entityHeight / 2.0f) + (float) (verticalAmplitude * Math.sin(t * verticalSpeed + phaseOffset));

                float taper = 1.0f - (s / (float)segments);
                // SMALLER DROPLETS (More elegant)
                // When rendering in GUI (camera == null), we adjust the size to 4.5f instead of 7.8f. This maintains the exact,
                // elegant 0.075 height-to-size ratio of the in-game effect, matching the preview perfectly with the in-game ESP proportions.
                float size = (camera != null ? 0.14f : 4.5f) * (0.45f + 0.55f * taper);
                float alpha = taper * 0.95f;

                double dropWorldX = targetX + offsetX;
                double dropWorldY = targetY + offsetY;
                double dropWorldZ = targetZ + offsetZ;

                double distSq = camera != null ? camPos.squaredDistanceTo(dropWorldX, dropWorldY, dropWorldZ) : -offsetZ;

                drops.add(new DropData(offsetX, offsetY, offsetZ, size, alpha, distSq));
            }
        }

        drops.sort(Comparator.comparingDouble(d -> -d.distSq));

        // 4. GUI BILLBOARDING: If rendering in GUI (camera == null), we cancel out the preview rotationDegrees
        // around the Y-axis. This keeps the flat circle droplets always facing the screen 2D plane (billboarding)
        // just like how camera.getRotation() billboards particles to face the first-person/third-person player camera in-game!
        Quaternionf rotation = camera != null ? camera.getRotation() : new Quaternionf().rotateY(-rotationDegrees * (float)Math.PI / 180f);

        for (DropData drop : drops) {
            matrices.push();
            matrices.translate(drop.ox, drop.oy, drop.oz);
            matrices.multiply(rotation);
            
            if (camera != null) matrices.translate(0, 0, 0.02f);

            drawVolumetricDroplet(matrices, drop.size, r, g, b, drop.alpha, camera != null);
            matrices.pop();
        }

        provider.draw();

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
    }

    private static void drawVolumetricDroplet(MatrixStack matrices, float size, float r, float g, float b, float alpha, boolean isWorld) {
        float zStep = isWorld ? 0.012f : 0.35f;
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        // 1. Back Layer (Glow)
        matrices.push(); matrices.translate(0, 0, -zStep);
        drawCircle(matrices.peek().getPositionMatrix(), size * 2.6f, r, g, b, alpha * 0.18f);
        matrices.pop();

        // 2. Middle Layer (Body)
        drawCircle(matrix, size * 1.5f, r, g, b, alpha * 0.42f);

        // 3. Front Layer (Core)
        matrices.push(); matrices.translate(0, 0, zStep);
        drawCircle(matrices.peek().getPositionMatrix(), size, r, g, b, alpha * 0.95f);
        matrices.pop();
    }

    private static void drawCircle(Matrix4f matrix, float size, float r, float g, float b, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix, 0, 0, 0).color(r, g, b, alpha);
        for (int i = 0; i <= 32; i++) {
            float a = i * (float)Math.PI * 2 / 32f;
            buffer.vertex(matrix, MathHelper.cos(a) * size, MathHelper.sin(a) * size, 0).color(r, g, b, 0f);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }
}
