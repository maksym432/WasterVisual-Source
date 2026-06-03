/*
 * CustomHitParticleManager - Architecture & Primary Responsibility:
 * Custom Hit Particle Manager.
 * Spawns and manages puff-star particles in world space on attack hits.
 *
 * Rendering strategy (world-space safe):
 *   Pure POSITION_COLOR geometry via getPositionColorProgram.
 *   NO custom shader, NO texture binding → atlas is never corrupted.
 *
 *   The puff-star shape is computed in Java via smin (smooth-minimum) of 5 circles.
 *   Binary-search finds the exact boundary radius at each of 64 angular samples.
 *   Three geometry layers per particle:
 *     1. Outer transparent glow disc (TRIANGLE_FAN, alpha→0 at rim)
 *     2. Star body with inner-brightness gradient (bright centre → base colour at edge)
 *     3. Tiny specular highlight dot (white, fades to transparent)
 *
 * Physics:
 *   - Ring spawn (surrounds enemy), fast burst → heavy drag → gentle float-up.
 *   - Smooth fade-in 20% of life, ease-out fade-out.
 *   - Painter's-algorithm sort. Count configurable (1-30).
 */
package com.example.glassmenu.render;

import com.example.glassmenu.GlassMenuClient;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class CustomHitParticleManager {

    // ─── Puff-star shape parameters ──────────────────────────────────────────
    // orbitR/circleR ratio controls "how star-like" the shape is.
    // orbitR > circleR/2  →  visible 5 tips; the higher the ratio, the sharper tips.
    // blend                →  smin smoothness at joins (0 = no blending/sharp, 0.3 = very soft)
    private static final float STAR_ORBIT_R  = 0.40f;  // tip-centre distance (normalised)
    private static final float STAR_CIRCLE_R = 0.34f;  // each tip-circle radius
    private static final float STAR_BLEND    = 0.11f;  // smin blend radius
    private static final int   STAR_SEGMENTS = 64;     // boundary polygon resolution

    /** Precomputed normalised boundary radii for one full revolution (0..2π). */
    private static final float[] BOUNDARY_R = precomputeBoundary();

    private static float[] precomputeBoundary() {
        float[] r = new float[STAR_SEGMENTS + 1];
        for (int i = 0; i <= STAR_SEGMENTS; i++) {
            float angle = (float)(Math.PI * 2.0 * i / STAR_SEGMENTS - Math.PI / 2.0);
            r[i] = findBoundaryRadius(angle);
        }
        return r;
    }

    /** smin of 5 circles (same formula as the FSH). */
    private static float sminSDF(float px, float py) {
        float result = 1e6f;
        for (int i = 0; i < 5; i++) {
            float a  = (float)(Math.PI * 2.0 * i / 5.0 - Math.PI / 2.0);
            float cx = (float)Math.cos(a) * STAR_ORBIT_R;
            float cy = (float)Math.sin(a) * STAR_ORBIT_R;
            float d  = (float)Math.sqrt((px-cx)*(px-cx) + (py-cy)*(py-cy)) - STAR_CIRCLE_R;
            // smooth minimum
            float h = Math.max(0f, Math.min(1f, 0.5f + 0.5f*(result-d)/STAR_BLEND));
            result = result*(1-h) + d*h - STAR_BLEND*h*(1-h);
        }
        return result;
    }

    /** Binary search: find r such that sminSDF(r·cos θ, r·sin θ) ≈ 0. */
    private static float findBoundaryRadius(float angle) {
        float cos = (float)Math.cos(angle), sin = (float)Math.sin(angle);
        float lo = 0f, hi = 1.2f;
        for (int k = 0; k < 20; k++) {
            float mid = (lo + hi) * 0.5f;
            if (sminSDF(mid*cos, mid*sin) < 0f) lo = mid; else hi = mid;
        }
        return (lo + hi) * 0.5f;
    }

    // ─── Particle physics constants ───────────────────────────────────────────
    private static final float  FADE_IN_FRACTION = 0.15f;
    private static final float  SPAWN_RADIUS_MIN = 0.10f;
    private static final float  SPAWN_RADIUS_MAX = 0.35f;
    private static final float  SPAWN_Y_SPREAD   = 0.25f;
    private static final double BURST_SPEED      = 0.26;   // faster outward burst
    private static final double DRAG_XZ          = 0.86;   // less drag → travels further sideways
    private static final double DRAG_Y           = 0.80;
    private static final double FLOAT_ACCEL      = 0.0008; // gentler upward drift

    private static final List<StarParticle> particles = new ArrayList<>();
    private static final Random RNG = new Random();

    // ─────────────────────────────────────────────────────────────────────────

    public static class StarParticle {
        public double x, y, z, vx, vy, vz;
        public float  size, rotation, rotationSpeed;
        public int    maxAge, age;
        public double distSq;

        public StarParticle(double x, double y, double z,
                            double vx, double vy, double vz,
                            float size, int maxAge) {
            this.x = x; this.y = y; this.z = z;
            this.vx = vx; this.vy = vy; this.vz = vz;
            this.size = size; this.maxAge = maxAge;
            this.rotation      = (float)(RNG.nextDouble() * Math.PI * 2.0);
            this.rotationSpeed = (float)((RNG.nextDouble() - 0.5) * 0.8);
        }

        public void tick() {
            x += vx; y += vy; z += vz;
            vx *= DRAG_XZ; vy *= DRAG_Y; vz *= DRAG_XZ;
            vy += FLOAT_ACCEL;
            rotation += rotationSpeed * 0.05f;
            age++;
        }

        public float alpha() {
            float p = (float)age / maxAge;
            if (p < FADE_IN_FRACTION) { float t = p/FADE_IN_FRACTION; return t*t*(3-2*t); }
            float t = (p - FADE_IN_FRACTION) / (1f - FADE_IN_FRACTION);
            return 1f - t*t;
        }

        public float scale() {
            float p = (float)age / maxAge;
            if (p < FADE_IN_FRACTION) { float t = p/FADE_IN_FRACTION; return size*(0.3f+0.7f*t); }
            float t = (p - FADE_IN_FRACTION) / (1f - FADE_IN_FRACTION);
            return size*(1f - 0.45f*t);
        }
    }

    private static long lastHitTime = 0;

    public static void spawnStarBurst(double hitX, double hitY, double hitZ,
                                      double velX, double velY, double velZ) {
        long now = System.currentTimeMillis();
        if (now - lastHitTime < 80) return;
        lastHitTime = now;

        int count = Math.max(1, Math.min(30, GlassMenuClient.CONFIG.customHitCount()));
        synchronized (particles) {
            for (int i = 0; i < count; i++) {
                double angle  = (Math.PI * 2.0 / count) * i
                              + (RNG.nextDouble() - 0.5) * (Math.PI * 2.0 / count) * 0.6;
                double radius = SPAWN_RADIUS_MIN + RNG.nextDouble() * (SPAWN_RADIUS_MAX - SPAWN_RADIUS_MIN);
                double sx = hitX + Math.cos(angle)*radius;
                double sy = hitY + (RNG.nextDouble()-0.5)*2.0*SPAWN_Y_SPREAD;
                double sz = hitZ + Math.sin(angle)*radius;
                double bvx = Math.cos(angle)*BURST_SPEED*(0.7+RNG.nextDouble()*0.6);
                double bvz = Math.sin(angle)*BURST_SPEED*(0.7+RNG.nextDouble()*0.6);
                double bvy = 0.04 + RNG.nextDouble()*0.06;
                float  sz2 = 0.20f + (float)RNG.nextDouble()*0.12f;
                int    age = 40 + (int)(RNG.nextDouble()*20); // longer lifetime
                particles.add(new StarParticle(sx, sy, sz, bvx, bvy, bvz, sz2, age));
            }
        }
    }

    public static void tick() {
        synchronized (particles) {
            Iterator<StarParticle> it = particles.iterator();
            while (it.hasNext()) { StarParticle p = it.next(); p.tick(); if (p.age >= p.maxAge) it.remove(); }
        }
    }

    public static void render(WorldRenderContext context) {
        if (!GlassMenuClient.CONFIG.enableCustomHit()) return;

        List<StarParticle> active;
        synchronized (particles) {
            if (particles.isEmpty()) return;
            active = new ArrayList<>(particles);
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || context.camera() == null) return;

        Vec3d camPos = context.camera().getPos();
        for (StarParticle p : active) p.distSq = camPos.squaredDistanceTo(p.x, p.y, p.z);
        active.sort(Comparator.comparingDouble(p -> -p.distSq));

        MatrixStack matrices = context.matrixStack();
        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        // ── Save ALL relevant GL state ─────────────────────────────────────────
        ShaderProgram prevShader    = RenderSystem.getShader();
        boolean prevBlend           = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean prevDepth           = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        // Save the actual OpenGL texture binding on unit 0 before we do anything.
        // drawWithGlobalProgram / position_color can desync Minecraft's texture tracking
        // even without an explicit setShaderTexture call, so we always save+restore.
        int prevActiveTexture = com.mojang.blaze3d.platform.GlStateManager._getActiveTexture();
        com.mojang.blaze3d.platform.GlStateManager._activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
        int prevTexture0 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        // Standard built-in shader — safest option for world-space geometry.
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        Quaternionf camRot  = context.camera().getRotation();
        int   baseCol = GlassMenuClient.CONFIG.customHitColor();
        float cr = ((baseCol>>16)&0xFF)/255f;
        float cg = ((baseCol>> 8)&0xFF)/255f;
        float cb = ( baseCol     &0xFF)/255f;
        boolean rgb = GlassMenuClient.CONFIG.customHitRgb();
        float tHue  = (System.currentTimeMillis() % 6000L) / 6000f;

        for (StarParticle p : active) {
            float alpha = p.alpha(), scale = p.scale();
            if (alpha <= 0.002f || scale <= 0.001f) continue;

            float pr = cr, pg = cg, pb = cb;
            if (rgb) {
                float hue = (tHue + p.rotation * 0.05f) % 1f;
                int c = hsvToRgb(hue, 1f, 1f);
                pr = ((c>>16)&0xFF)/255f; pg = ((c>>8)&0xFF)/255f; pb = (c&0xFF)/255f;
            }

            matrices.push();
            matrices.translate(p.x, p.y, p.z);
            matrices.multiply(camRot);
            matrices.multiply(new Quaternionf().rotateZ(p.rotation));

            drawPuffStar(matrices.peek().getPositionMatrix(), scale, pr, pg, pb, alpha);
            matrices.pop();
        }

        // ── Restore ALL GL state ──────────────────────────────────────────────
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        if (!prevDepth) RenderSystem.disableDepthTest();
        if (!prevBlend) RenderSystem.disableBlend();
        if (prevShader != null) RenderSystem.setShader(() -> prevShader);
        // Restore texture slot 0 — both the raw GL binding and Minecraft's tracked state.
        com.mojang.blaze3d.platform.GlStateManager._activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
        org.lwjgl.opengl.GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTexture0);
        RenderSystem.setShaderTexture(0, prevTexture0);
        // Restore active texture unit.
        com.mojang.blaze3d.platform.GlStateManager._activeTexture(prevActiveTexture);

        matrices.pop();
    }

    /**
     * Draws one puff-star using precomputed boundary radii.
     * Three layers:
     *   1. Wide outer glow ring (transparent at rim — looks like an aura).
     *   2. Star body: bright centre (self-illumination) → base colour at edge.
     *   3. Tiny specular dot (white → transparent) near the centre.
     */
    private static void drawPuffStar(Matrix4f mat, float size,
                                     float r, float g, float b, float alpha) {
        Tessellator tess = Tessellator.getInstance();

        // ── 1. Outer glow ────────────────────────────────────────────────────
        {
            BufferBuilder buf = tess.begin(VertexFormat.DrawMode.TRIANGLE_FAN,
                                           VertexFormats.POSITION_COLOR);
            // Glow centre: base colour, semi-transparent
            buf.vertex(mat, 0, 0, 0).color(r, g, b, alpha * 0.40f);
            for (int i = 0; i <= STAR_SEGMENTS; i++) {
                float angle   = (float)(Math.PI * 2.0 * i / STAR_SEGMENTS - Math.PI / 2.0);
                float rad     = BOUNDARY_R[i] * size * 2.0f; // glow extends 2× beyond body
                float vx = (float)Math.cos(angle) * rad;
                float vy = (float)Math.sin(angle) * rad;
                buf.vertex(mat, vx, vy, 0).color(r, g, b, 0f);
            }
            BufferRenderer.drawWithGlobalProgram(buf.end());
        }

        // ── 2. Star body with inner-volume gradient ───────────────────────────
        {
            BufferBuilder buf = tess.begin(VertexFormat.DrawMode.TRIANGLE_FAN,
                                           VertexFormats.POSITION_COLOR);
            // Bright centre = self-illumination (clamped to 1.0)
            float bc = 1.0f;
            buf.vertex(mat, 0, 0, 0)
               .color(Math.min(1f, r*1.9f + bc*0.05f),
                      Math.min(1f, g*1.9f + bc*0.05f),
                      Math.min(1f, b*1.9f + bc*0.05f), alpha);
            for (int i = 0; i <= STAR_SEGMENTS; i++) {
                float angle = (float)(Math.PI * 2.0 * i / STAR_SEGMENTS - Math.PI / 2.0);
                float rad   = BOUNDARY_R[i] * size;
                float vx = (float)Math.cos(angle) * rad;
                float vy = (float)Math.sin(angle) * rad;
                // Edge: slightly darker than base (creates the volume illusion)
                buf.vertex(mat, vx, vy, 0)
                   .color(r * 0.72f, g * 0.72f, b * 0.72f, alpha);
            }
            BufferRenderer.drawWithGlobalProgram(buf.end());
        }

        // ── 3. Specular highlight (small bright disc, off-centre) ─────────────
        {
            float specR = size * 0.14f;
            // Offset specular towards upper-left
            float ox = -size * 0.12f;
            float oy =  size * 0.15f;
            int   segs = 12;
            BufferBuilder buf = tess.begin(VertexFormat.DrawMode.TRIANGLE_FAN,
                                           VertexFormats.POSITION_COLOR);
            buf.vertex(mat, ox, oy, 0).color(1f, 1f, 1f, alpha * 0.75f);
            for (int i = 0; i <= segs; i++) {
                float a = (float)(Math.PI * 2.0 * i / segs);
                buf.vertex(mat, ox + (float)Math.cos(a)*specR,
                                oy + (float)Math.sin(a)*specR, 0)
                   .color(1f, 1f, 1f, 0f);
            }
            BufferRenderer.drawWithGlobalProgram(buf.end());
        }
    }

    private static int hsvToRgb(float h, float s, float v) {
        float c = v*s, x = c*(1f - Math.abs(h*6f%2f - 1f)), m = v-c;
        float r, g, b;
        switch ((int)(h*6f)%6) {
            case 0: r=c; g=x; b=0; break; case 1: r=x; g=c; b=0; break;
            case 2: r=0; g=c; b=x; break; case 3: r=0; g=x; b=c; break;
            case 4: r=x; g=0; b=c; break; default: r=c; g=0; b=x; break;
        }
        return ((int)((r+m)*255)<<16)|((int)((g+m)*255)<<8)|(int)((b+m)*255);
    }
}
