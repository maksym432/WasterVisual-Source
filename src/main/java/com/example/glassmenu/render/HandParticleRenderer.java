/**
 * HandParticleRenderer.java
 *
 * Primary Responsibility:
 * Handles high-fidelity 3D particle trail rendering around player hands and equipped items in first-person view.
 *
 * Architectural Role:
 * Implements Fabric Hud/Item render hook hooks to visualize gorgeous spatialized fluid animations:
 * - Double Helix Nebula: The default smooth spiral trail for hands.
 * - Cosmic Crystalline Ring Vortex: 3 tilted nested armillary rings orbiting the offhand.
 * - Double Helix Wrapping Vortex: Helix wrapping around weapons (swords).
 * - Radar Scanning Block Sphere: Holographic scanning grids around blocks.
 *
 * Performance Features:
 * - Optimized to compile, batch, and draw all 3D droplet geometry in a single WebGL draw call (1 Draw Call).
 * - Implements camera-relative, frame-rate independent position smoothing and NaN protection.
 */
package com.example.glassmenu.render;

import com.example.glassmenu.GlassMenuClient;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HandParticleRenderer {
    private static final long START_TIME = System.currentTimeMillis();

    // Camera Space Simulation State for main and off hands
    private static class HandState {
        Vector3f[] particlePositions = new Vector3f[27];
        Quaternionf[] particleRotations = new Quaternionf[27];
        Vector3f pos = new Vector3f(); // Stores last frame's hand world position for speed/stretch calculation
        Vector3f lastCamPos = new Vector3f(); // Stores last frame's camera world position to compute translation displacement
        Quaternionf lastCamRot = new Quaternionf(); // Stores last frame's camera rotation to compute relative rotation
        float animationTime = 0f;
        float summonProgress = 0f;
        float burstProgress = 0f; // Impact burst animation
        long lastFrameTime = System.currentTimeMillis();
        boolean initialized = false;

        // Smooth transition states for orbital scaling (moved here to prevent multi-context preview leaks)
        float currentOrbitRadius = 0.22f;
        float currentOrbitHeight = 0.25f;
        float currentBaseSize = 0.030f;

        public HandState() {
            for (int i = 0; i < 27; i++) {
                particlePositions[i] = new Vector3f();
                particleRotations[i] = new Quaternionf();
            }
        }
    }

    private static final HandState mainHandState = new HandState();
    private static final HandState offHandState = new HandState();
    private static final HandState guiMainHandState = new HandState();
    private static final HandState guiOffHandState = new HandState();

    private record Particle(float x, float y, float z, float size, float alpha, float depth, Quaternionf rotation, int color) {}

    private static float blend(float baseFactor, float delta) {
        float d = Math.min(delta, 0.1f);
        return 1.0f - (float)Math.pow(1.0f - baseFactor, d * 60.0f);
    }

    public static void render(MatrixStack matrices, boolean isRgb, ItemStack stack, boolean isLeftHand) {
        render(matrices, isRgb, stack, isLeftHand, false);
    }

    public static void render(MatrixStack matrices, boolean isRgb, ItemStack stack, boolean isLeftHand, boolean isGui) {
        long now = System.currentTimeMillis();
        HandState state = isGui ? (isLeftHand ? guiOffHandState : guiMainHandState) : (isLeftHand ? offHandState : mainHandState);
        
        float delta = (now - state.lastFrameTime) / 1000f;
        state.lastFrameTime = now;

        if (delta < 0f) delta = 0f;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.gameRenderer == null || client.gameRenderer.getCamera() == null) {
            return;
        }
        net.minecraft.client.render.Camera camera = client.gameRenderer.getCamera();
        net.minecraft.util.math.Vec3d cameraPos = camera.getPos();

        Matrix4f currentMatrix = new Matrix4f(matrices.peek().getPositionMatrix());
        
        // 1. SAFETY FIRST: Protect against singular / degenerate matrices
        float det = currentMatrix.determinant();
        if (!Float.isFinite(det) || Math.abs(det) < 1e-6f) {
            return;
        }

        // Extract camera-space hand position and rotation
        Vector3f handCamPos = currentMatrix.getTranslation(new Vector3f());
        Quaternionf handCamRot = new Quaternionf();
        currentMatrix.getNormalizedRotation(handCamRot);
        if (!Float.isFinite(handCamRot.x) || !Float.isFinite(handCamRot.y) || !Float.isFinite(handCamRot.z) || !Float.isFinite(handCamRot.w)) {
            handCamRot.identity();
        }
        handCamRot.normalize();

        // Frame-rate independent delta accumulation for smooth non-lagging rotation
        float clampedDelta = Math.min(delta, 0.1f);
        state.animationTime += clampedDelta * 1.8f;
        double animTime = state.animationTime;

        int total = SwordFormationEngine.TOTAL_PARTICLES; // 27 total particles

        // 3. CLASSIFY CURRENT EQUIPPED ITEM
        // Determine if player is holding a block (BlockItem) or weapon (SwordItem).
        // These states are used to determine which particle formation blueprint to morph into.
        boolean isBlock = stack.getItem() instanceof BlockItem;
        boolean isWeapon = stack.getItem() instanceof SwordItem;

        // 2. MORPH & BURST STATE MACHINE
        // A hand is considered "summoning" if it is either empty (stack.isEmpty()), holding a weapon (isWeapon),
        // or holding a block (isBlock). This drives the transition from the base nebula trail (summonProgress -> 0)
        // to the structural item blueprints (summonProgress -> 1).
        boolean isSummoning = stack.isEmpty() || isWeapon || isBlock;
        
        if (client.player != null && client.player.handSwingProgress > 0 && client.player.handSwingProgress < 0.15) {
            state.burstProgress = 1.0f;
        }
        state.burstProgress = MathHelper.stepTowards(state.burstProgress, 0f, clampedDelta * 4.0f);

        if (isSummoning) {
            state.summonProgress = Math.min(1.0f, state.summonProgress + clampedDelta * 5.0f); 
        } else {
            state.summonProgress = Math.max(0.0f, state.summonProgress - clampedDelta * 3.5f);
        }

        // Convert hand position and rotation to world-space for organic physical velocity calculation
        Vector3f currentCamPosVec = new Vector3f((float)cameraPos.x, (float)cameraPos.y, (float)cameraPos.z);
        Quaternionf camRotWorld = new Quaternionf(camera.getRotation()); // Camera-to-world rotation
        Vector3f handWorldPos = new Vector3f(handCamPos).rotate(camRotWorld).add(currentCamPosVec);
        Quaternionf handWorldRot = new Quaternionf(camRotWorld).mul(handCamRot).normalize();

        // Speed and drag calculations based on hand's world-space velocity.
        // Disabled in GUI mode to prevent camera-movement stretching while config menu is open.
        float speedFactor = 0f;
        Vector3f trailVec = new Vector3f();
        if (!isGui && state.initialized) {
            trailVec.set(state.pos).sub(handWorldPos);
            speedFactor = trailVec.length();
        }
        state.pos.set(handWorldPos);
        speedFactor = MathHelper.clamp(speedFactor * 4.0f, 0f, 1.5f);

        // 3. CLASSIFY & ANALYZE ITEM GEOMETRY ONCE PER FRAME
        float tx_m = 0.070625f;
        float ty_m = 0.2f;
        float tz_m = 0.070625f;
        float rx_m = 0f;
        float ry_m = -90f;
        float rz_m = 25f;
        float sx_m = 0.68f;
        float sy_m = 0.68f;
        float sz_m = 0.68f;
        float modelWidth = 0f;
        float modelHeight = 0f;
        float minX = 0f;
        float maxX = 1f;
        float minY = 0f;
        float maxY = 1f;
        float minZ = 0f;
        float maxZ = 0.0625f;
        float scaleRatio = 1.0f;

        Quaternionf modelSpaceRot = new Quaternionf();
        if (isWeapon) {
            net.minecraft.client.render.model.BakedModel activeModel = null;
            try {
                activeModel = client.getItemRenderer().getModel(stack, null, null, 0);
                net.minecraft.client.render.model.json.ModelTransformation modelTransform = activeModel.getTransformation();
                net.minecraft.client.render.model.json.Transformation transform = modelTransform.getTransformation(isLeftHand ? net.minecraft.client.render.model.json.ModelTransformationMode.FIRST_PERSON_LEFT_HAND : net.minecraft.client.render.model.json.ModelTransformationMode.FIRST_PERSON_RIGHT_HAND);
                if (transform != null) {
                    tx_m = transform.translation.x() / 16.0f;
                    ty_m = transform.translation.y() / 16.0f;
                    tz_m = transform.translation.z() / 16.0f;
                    rx_m = transform.rotation.x();
                    ry_m = transform.rotation.y();
                    rz_m = transform.rotation.z();
                    sx_m = transform.scale.x();
                    sy_m = transform.scale.y();
                    sz_m = transform.scale.z();
                }
            } catch (Exception e) {
                // Fallback to defaults
            }
            
            // Dynamically analyze the model's bounding box using BakedQuad vertex data to adapt to texture/mesh size
            float[] bounds = getModelBounds(activeModel);
            minX = bounds[0];
            maxX = bounds[1];
            minY = bounds[2];
            maxY = bounds[3];
            minZ = bounds[4];
            maxZ = bounds[5];
            
            modelWidth = maxX - minX;
            modelHeight = maxY - minY;
            scaleRatio = Math.max(sx_m, 0.68f) / 0.68f;
            
            float rx_rad = (float)Math.toRadians(rx_m);
            float ry_rad = (float)Math.toRadians(isLeftHand ? -ry_m : ry_m);
            float rz_rad = (float)Math.toRadians(isLeftHand ? -rz_m : rz_m);
            modelSpaceRot.rotationX(rx_rad)
                         .mul(new Quaternionf().rotationY(ry_rad))
                         .mul(new Quaternionf().rotationZ(rz_rad));
        }

        float dx = modelWidth > 0 ? modelWidth : 0.62f;
        float dy = modelHeight > 0 ? modelHeight : 0.62f;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        float ax = dx / len;
        float ay = dy / len;
        float px = -ay;
        float py = ax;

        // 3. DYNAMIC ORBIT PARAMS
        // Dynamically lerp orbit properties (radius, height, size) toward their targets depending on active item type
        float targetRadius = isBlock ? 0.38f : (isWeapon ? 0.28f * (modelWidth > 0 ? Math.max(modelWidth, 0.5f) : 1.0f) : 0.22f);
        float targetHeight = isBlock ? 0.45f : (isWeapon ? 0.55f * (modelHeight > 0 ? Math.max(modelHeight, 0.5f) : 1.0f) : 0.25f);
        float targetSize = isBlock ? 0.040f : (isWeapon ? 0.032f * (modelWidth > 0 ? Math.max(modelWidth, 0.5f) : 1.0f) * scaleRatio : 0.030f);

        state.currentOrbitRadius = MathHelper.lerp(0.15f, state.currentOrbitRadius, targetRadius);
        state.currentOrbitHeight = MathHelper.lerp(0.15f, state.currentOrbitHeight, targetHeight);
        state.currentBaseSize = MathHelper.lerp(0.15f, state.currentBaseSize, targetSize);

        // 4. UNIFIED PARTICLE GENERATION & KINEMATICS (Camera Space Targets)
        int trailCount = 2; // Helix strands
        int pPerTrail = total / trailCount;

        float posBlend = blend(0.85f, delta);
        float rotBlend = blend(0.75f, delta);

        for (int i = 0; i < total; i++) {
            Vector3f targetBlueprintPos = new Vector3f();
            Quaternionf targetBlueprintRot = new Quaternionf().identity();
            int pColorInt = 0xFFFFFF;
            boolean isCube = false;

            if (isLeftHand && stack.isEmpty()) {
                // COSMIC CRYSTALLINE RING VORTEX FORMATION (Off hand empty)
                targetBlueprintPos = CubeFormationEngine.getCubePos(i, animTime);
                
                // Organic jellyfish trailing: stretch vertices along trailing direction
                if (speedFactor > 0.05f) {
                    if (trailVec.lengthSquared() > 1e-5f) {
                        Vector3f localTrailVec = new Vector3f(trailVec).normalize();
                        Quaternionf invHandWorldRot = new Quaternionf(handWorldRot).invert();
                        localTrailVec.rotate(invHandWorldRot);
                        
                        float distFromCenter = targetBlueprintPos.length();
                        float dragAmount = distFromCenter * speedFactor * 0.22f;
                        targetBlueprintPos.add(localTrailVec.x() * dragAmount, localTrailVec.y() * dragAmount, localTrailVec.z() * dragAmount);
                    }
                }
                
                targetBlueprintRot = CubeFormationEngine.getCubeParticleRotation(i, animTime);
                pColorInt = CubeFormationEngine.getCubeColor(i, animTime);
                isCube = true;
            } else if (isWeapon) {
                float progress = i / (float) total;
                
                // Distribute particles along the blade diagonal axis (24% to 96% to fully cover but exclude hilt bottom)
                float startFactor = 0.24f;
                float endFactor = 0.96f;
                float bx = minX + dx * (startFactor + progress * (endFactor - startFactor));
                float by = minY + dy * (startFactor + progress * (endFactor - startFactor));
                float bz = (minZ + maxZ) * 0.5f; // Center along Z depth
                
                // Base radius increased from 0.22f to 0.28f to fully wrap/enclose the blade ("трубка больше, фул покрываться")
                // Adapt base radius to model width dynamically
                float baseRadius = 0.28f * (modelWidth > 0 ? Math.max(modelWidth, 0.5f) : 1.0f);
                float radius = baseRadius * (1.0f - progress * 0.20f) * scaleRatio;
                
                int strand = i % 2;
                float angle = progress * (float)(Math.PI * 2.0 * 2.5) + (float)animTime * 4.5f + strand * (float)Math.PI;
                
                float cosA = MathHelper.cos(angle);
                float sinA = MathHelper.sin(angle);
                float ox = cosA * radius * px;
                float oy = cosA * radius * py;
                float oz = sinA * radius;
                
                Vector3f modelPos = new Vector3f(bx + ox - 0.5f, by + oy - 0.5f, bz + oz - 0.5f);
                
                // Calculate tangent vector in model space
                float tx = ax - sinA * radius * px;
                float ty = ay - sinA * radius * py;
                float tz = cosA * radius;
                Vector3f tangent = new Vector3f(tx, ty, tz);
                if (tangent.lengthSquared() > 1e-5f) {
                    tangent.normalize();
                } else {
                    tangent.set(ax, ay, 0f);
                }
                Quaternionf modelRot = new Quaternionf().lookAlong(tangent, new Vector3f(0, 1, 0));
                
                // 1. Scale
                modelPos.mul(sx_m, sy_m, sz_m);
                
                // 2. Rotate
                modelPos.rotate(modelSpaceRot);
                
                // 3. Translate
                modelPos.add(isLeftHand ? -tx_m : tx_m, ty_m, tz_m);
                
                targetBlueprintPos.set(modelPos);
                targetBlueprintRot.set(modelSpaceRot).mul(modelRot);
            } else if (isBlock) {
                // scanning armillary sphere/ring vortex orbiting around the block
                int ring = i / 9; // 3 concentric rings of 9 particles (27 particles total)
                int pIndex = i % 9;
                float radius = 0.32f;
                double baseAngle = pIndex * (2.0 * Math.PI / 9.0);
                double orbitAngle;
                if (ring == 0) {
                    orbitAngle = baseAngle + 2.4 * animTime;
                } else if (ring == 1) {
                    orbitAngle = baseAngle - 1.8 * animTime;
                } else {
                    orbitAngle = baseAngle + 1.2 * animTime;
                }
                float rx = (float) Math.cos(orbitAngle) * radius;
                float ry = (float) Math.sin(orbitAngle) * radius;
                float rz = 0.0f;
                Vector3f blockOrbPos = new Vector3f(rx, ry, rz);
                
                // Unique tilt rotations to mimic nested armillary rings
                float tiltX = 0f, tiltY = 0f, tiltZ = 0f;
                if (ring == 0) {
                    tiltX = 0.785398f; tiltY = 0.523598f;
                } else if (ring == 1) {
                    tiltY = 0.785398f; tiltZ = 0.523598f;
                } else {
                    tiltX = 0.523598f; tiltZ = 0.785398f;
                }
                Quaternionf tilt = new Quaternionf().rotationXYZ(tiltX, tiltY, tiltZ);
                blockOrbPos.rotate(tilt);
                
                // Ambient slow global rotation
                Quaternionf ambient = new Quaternionf().rotationXYZ(
                    (float) animTime * 0.3f,
                    (float) animTime * 0.2f,
                    (float) animTime * 0.1f
                );
                blockOrbPos.rotate(ambient);
                
                targetBlueprintPos.set(blockOrbPos);
                targetBlueprintRot.identity(); // Flat droplets ("тупо капли")
            } else if (!isLeftHand && stack.isEmpty()) {
                // DNA double-helix closed loop when right hand is empty
                int strand = i % 2;
                int segmentIdx = i / 2;
                float progress = segmentIdx / 13.0f; // Continuous within strand
                
                float y = -0.22f + progress * 0.44f;
                float loopEnvelope = (float) Math.sin(progress * Math.PI);
                
                float radiusX = 0.24f * loopEnvelope;
                float radiusZ = 0.16f * loopEnvelope;
                
                double angle = progress * (Math.PI * 2.0) + animTime * 3.5 + strand * Math.PI;
                
                float x = (float) Math.cos(angle) * radiusX;
                float z = (float) Math.sin(angle) * radiusZ;
                
                targetBlueprintPos.set(x, y, z);
                targetBlueprintRot.identity(); // Flat droplets ("тупо капли")
            } else {
                // Dense continuous double helix spring wrapping the item from underneath with trailing delay
                int strand = i % 2;
                int segmentIdx = i / 2;
                float progress = segmentIdx / 13.0f; // Continuous within strand
                
                float y = -0.18f + progress * 0.46f;
                float radiusX = 0.25f * (1.0f - progress * 0.3f);
                float radiusZ = 0.14f * (1.0f - progress * 0.3f);
                
                // Trailing delay segment phase shift matching nebula motion physics
                double tSpring = animTime * 4.0 + (strand * Math.PI) - (segmentIdx * 0.18);
                
                float x = (float) Math.cos(tSpring) * radiusX;
                float z = (float) Math.sin(tSpring) * radiusZ;
                targetBlueprintPos.set(x, y, z);
                targetBlueprintRot.identity(); // Flat droplets ("тупо капли")
            }

            if (isRgb && !isCube) {
                float h = (float) (((now + i * 70) % 3000) / 3000.0);
                pColorInt = java.awt.Color.HSBtoRGB(h, 0.85f, 1.0f) & 0xFFFFFF;
            } else if (!isCube) {
                float h = 0.55f + 0.15f * MathHelper.sin((float) (animTime * 2.0f + i * 0.1f));
                pColorInt = java.awt.Color.HSBtoRGB(h, 0.75f, 1.0f) & 0xFFFFFF;
            }

            // --- SOURCE PATHWAY: Fluid Double Helix Nebula Trail ---
            int strand = i % 2;
            int segmentIdx = i / 2;
            double segmentDelay = segmentIdx * 0.14;
            double t = animTime * 4.2 + (strand * Math.PI) - segmentDelay;
            
            float helixRadius = state.currentOrbitRadius * (1.0f - (segmentIdx / (float)pPerTrail * 0.35f));
            float ox = (float) (Math.cos(t) * helixRadius);
            float oz = (float) (Math.sin(t) * helixRadius * 1.1f);
            float oy = (float) (-segmentIdx * 0.05f + Math.sin(t * 0.7f) * 0.08f);

            double dt = 0.01;
            double tPrev = t - dt;
            float pxPrev = (float) (Math.cos(tPrev) * helixRadius);
            float pzPrev = (float) (Math.sin(tPrev) * helixRadius * 1.1f);
            float pyPrev = (float) (-segmentIdx * 0.05f + Math.sin(tPrev * 0.7f) * 0.08f);
            Vector3f orbitDir = new Vector3f(ox - pxPrev, oy - pyPrev, oz - pzPrev);
            if (orbitDir.lengthSquared() > 1e-5f) orbitDir.normalize();
            Quaternionf orbitRot = new Quaternionf().lookAlong(orbitDir, new Vector3f(0, 1, 0));

            // --- MORPH: Interpolate between Nebula Double Helix and Target Blueprints ---
            float ease = state.summonProgress < 1.0f ? (float) (1.0 - Math.pow(1.0 - state.summonProgress, 3.5)) : 1.0f;
            
            float finalX = MathHelper.lerp(ease, ox, targetBlueprintPos.x());
            float finalY = MathHelper.lerp(ease, oy, targetBlueprintPos.y());
            float finalZ = MathHelper.lerp(ease, oz, targetBlueprintPos.z());

            // --- BURST: Radial expansion on attack ---
            if (state.burstProgress > 0.001f) {
                float burstForce = (float) Math.pow(state.burstProgress, 1.5) * 0.65f;
                Vector3f dirFromCenter = new Vector3f(finalX, finalY, finalZ);
                if (dirFromCenter.lengthSquared() > 1e-5f) {
                    dirFromCenter.normalize();
                } else {
                    dirFromCenter.set(MathHelper.cos(i * 1.5f), MathHelper.sin(i * 1.5f), MathHelper.sin(i * 2.0f));
                }
                finalX += dirFromCenter.x() * burstForce;
                finalY += dirFromCenter.y() * burstForce;
                finalZ += dirFromCenter.z() * burstForce;
            }

            Quaternionf finalRot = new Quaternionf().set(orbitRot).slerp(targetBlueprintRot, ease).normalize();

            // Target coordinates are already calculated in local hand space
            Vector3f targetLocalPos_i = new Vector3f(finalX, finalY, finalZ);
            Quaternionf targetLocalRot_i = new Quaternionf(finalRot);

            if (!state.initialized) {
                state.particlePositions[i].set(targetLocalPos_i);
                state.particleRotations[i].set(targetLocalRot_i);
            } else {
                state.particlePositions[i].lerp(targetLocalPos_i, posBlend);
                state.particleRotations[i].slerp(targetLocalRot_i, rotBlend).normalize();
            }
        }
        state.initialized = true;

        // 5. Setup Rendering State: Keep matrix stack in local hand space (no inversion or camera translations needed!)
        matrices.push();
        
        net.minecraft.client.gl.ShaderProgram previousShader = RenderSystem.getShader();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        // Simulated particles are already in camera space
        List<Particle> particles = new ArrayList<>();

        for (int i = 0; i < total; i++) {
            Vector3f relPos = state.particlePositions[i];
            Quaternionf pRot = state.particleRotations[i];

            int segmentIdx = i / 2;
            float ease = state.summonProgress < 1.0f ? (float) (1.0 - Math.pow(1.0 - state.summonProgress, 3.5)) : 1.0f;
            
            float summonedTargetSize = isBlock ? 0.040f : (isWeapon ? 0.032f * (modelWidth > 0 ? Math.max(modelWidth, 0.5f) : 1.0f) * scaleRatio : 0.030f);
            float pSize = MathHelper.lerp(ease, state.currentBaseSize * (1.0f - (segmentIdx / (float)pPerTrail * 0.45f)), summonedTargetSize);
            
            // Core Singularity Size Modification: index 26 of empty left hand is removed entirely
            if (isLeftHand && stack.isEmpty() && i == 26) {
                pSize = 0f;
            } else if (isWeapon) {
                pSize *= (1.1f + 0.3f * MathHelper.sin((float)(animTime * 12.0f + i)));
            }
            
            float pAlpha = MathHelper.lerp(ease, 0.90f * (1.0f - (segmentIdx / (float)pPerTrail * 0.75f)), 0.85f);
            // Core Singularity Alpha Boost: disabled for empty left hand to hide it completely
            if (isLeftHand && stack.isEmpty() && i == 26) {
                pAlpha = 0f;
            } else if (state.burstProgress > 0.001f) {
                pAlpha *= (1.0f - state.burstProgress * 0.6f);
            }

            int pColorInt = 0xFFFFFF;
            if (isLeftHand && stack.isEmpty()) {
                pColorInt = CubeFormationEngine.getCubeColor(i, animTime);
            } else if (isRgb) {
                float h = (float) (((now + i * 70) % 3000) / 3000.0);
                pColorInt = java.awt.Color.HSBtoRGB(h, 0.85f, 1.0f) & 0xFFFFFF;
            } else {
                float h = 0.55f + 0.15f * MathHelper.sin((float) (animTime * 2.0f + i * 0.1f));
                pColorInt = java.awt.Color.HSBtoRGB(h, 0.75f, 1.0f) & 0xFFFFFF;
            }

            particles.add(new Particle(relPos.x(), relPos.y(), relPos.z(), pSize, pAlpha, relPos.z(), pRot, pColorInt));
        }

        // Sort particles back-to-front for proper transparent alpha blending
        particles.sort(Comparator.comparingDouble(p -> p.depth));

        // 6. HIGH PERFORMANCE BATCH DRAWING (Exactly 1 Draw Call!)
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        Matrix4f baseMatrix = matrices.peek().getPositionMatrix();

        for (Particle p : particles) {
            Matrix4f particleMatrix = new Matrix4f(baseMatrix)
                .translate(p.x, p.y, p.z)
                .rotate(p.rotation);
            
            float r = ((p.color >> 16) & 0xFF) / 255f;
            float g = ((p.color >> 8) & 0xFF) / 255f;
            float b = (p.color & 0xFF) / 255f;
            
            draw3DDropletGeometry(buffer, particleMatrix, p.size, p.size * 2.5f, r, g, b, p.alpha);
            draw3DDropletGeometry(buffer, particleMatrix, p.size * 1.6f, p.size * 4.0f, r, g, b, p.alpha * 0.18f);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        if (previousShader != null) {
            RenderSystem.setShader(() -> previousShader);
        }
        matrices.pop();
    }

    private static void draw3DDropletGeometry(BufferBuilder buffer, Matrix4f matrix, float size, float length, float r, float g, float b, float alpha) {
        int segments = 12; // Balanced details for top notch visual and high performance
        float ringAlpha = alpha * 0.8f;
        float tailAlpha = 0f;

        float[] cos = new float[segments];
        float[] sin = new float[segments];
        for (int i = 0; i < segments; i++) {
            float angle = (float) (i * Math.PI * 2.0 / segments);
            cos[i] = MathHelper.cos(angle);
            sin[i] = MathHelper.sin(angle);
        }

        // 1. Spherical Front Dome (3D Sphere Head)
        float z1 = size * 0.6f;
        float r1 = size * 0.8f;
        for (int i = 0; i < segments; i++) {
            int next = (i + 1) % segments;
            buffer.vertex(matrix, 0, 0, size).color(r, g, b, alpha);
            buffer.vertex(matrix, cos[i] * r1, sin[i] * r1, z1).color(r, g, b, alpha * 0.9f);
            buffer.vertex(matrix, cos[next] * r1, sin[next] * r1, z1).color(r, g, b, alpha * 0.9f);
        }

        for (int i = 0; i < segments; i++) {
            int next = (i + 1) % segments;
            buffer.vertex(matrix, cos[i] * r1, sin[i] * r1, z1).color(r, g, b, alpha * 0.9f);
            buffer.vertex(matrix, cos[i] * size, sin[i] * size, 0).color(r, g, b, ringAlpha);
            buffer.vertex(matrix, cos[next] * size, sin[next] * size, 0).color(r, g, b, ringAlpha);

            buffer.vertex(matrix, cos[i] * r1, sin[i] * r1, z1).color(r, g, b, alpha * 0.9f);
            buffer.vertex(matrix, cos[next] * size, sin[next] * size, 0).color(r, g, b, ringAlpha);
            buffer.vertex(matrix, cos[next] * r1, sin[next] * r1, z1).color(r, g, b, alpha * 0.9f);
        }

        // 2. Tapered Tearing Tail (Fading along Z Depth)
        float tZ = -length;
        for (int i = 0; i < segments; i++) {
            int next = (i + 1) % segments;
            buffer.vertex(matrix, 0, 0, tZ).color(r, g, b, tailAlpha);
            buffer.vertex(matrix, cos[next] * size, sin[next] * size, 0).color(r, g, b, ringAlpha);
            buffer.vertex(matrix, cos[i] * size, sin[i] * size, 0).color(r, g, b, ringAlpha);
        }
    }

    private static float[] getModelBounds(net.minecraft.client.render.model.BakedModel model) {
        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE;
        float maxZ = -Float.MAX_VALUE;
        
        if (model == null) {
            return new float[]{0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0625f};
        }
        
        net.minecraft.util.math.random.Random random = net.minecraft.util.math.random.Random.create(42L);
        List<net.minecraft.client.render.model.BakedQuad> quads = new ArrayList<>();
        try {
            for (net.minecraft.util.math.Direction dir : net.minecraft.util.math.Direction.values()) {
                quads.addAll(model.getQuads(null, dir, random));
            }
            quads.addAll(model.getQuads(null, null, random));
        } catch (Exception e) {
            // Ignore
        }
        
        if (quads.isEmpty()) {
            return new float[]{0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0625f}; // Fallback bounds
        }
        
        for (net.minecraft.client.render.model.BakedQuad quad : quads) {
            int[] vertexData = quad.getVertexData();
            if (vertexData == null || vertexData.length < 32) continue;
            int step = vertexData.length / 4;
            if (step < 8) continue;
            for (int i = 0; i < 4; i++) {
                int offset = i * step;
                float x = Float.intBitsToFloat(vertexData[offset]);
                float y = Float.intBitsToFloat(vertexData[offset + 1]);
                float z = Float.intBitsToFloat(vertexData[offset + 2]);
                
                // Exclude extreme values or NaNs just in case
                if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) continue;
                
                if (x < minX) minX = x;
                if (x > maxX) maxX = x;
                if (y < minY) minY = y;
                if (y > maxY) maxY = y;
                if (z < minZ) minZ = z;
                if (z > maxZ) maxZ = z;
            }
        }
        
        // Final sanity fallback check
        if (minX >= maxX || minY >= maxY) {
            return new float[]{0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0625f};
        }
        
        return new float[]{minX, maxX, minY, maxY, minZ, maxZ};
    }
}
