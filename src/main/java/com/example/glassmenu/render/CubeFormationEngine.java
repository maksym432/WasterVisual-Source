/**
 * CubeFormationEngine.java
 *
 * Primary Responsibility:
 * Precomputes 3D structural blueprints for hand particles in summon mode (off-hand empty state).
 *
 * Architectural & Rendering Pipeline Role:
 * - Serves as a pure stateless math engine that generates positions, orientations, and colors for 27 particles.
 * - Replaces the legacy Rubik's Cube with a highly premium concentric Armillary Ring Vortex.
 * - Integrates a central pulsing celestial singularity core at index 26 to act as the energetic anchor of the vortex.
 * - Communicates coordinate-space geometry to {@link com.example.glassmenu.render.HandParticleRenderer}.
 *
 * Key Mathematical & Technical Aspects:
 * - 27 particles are partitioned: 26 outer ring droplets + 1 central singularity (index 26) at (0, 0, 0).
 * - The 26 outer particles are split into 3 concentric rings:
 *   - Ring 0: 8 particles, radius 0.10f, speed 2.8, tilted around X & Y.
 *   - Ring 1: 9 particles, radius 0.16f, speed -2.0 (clockwise), tilted around Y & Z.
 *   - Ring 2: 9 particles, radius 0.22f, speed 1.4, tilted around X & Z.
 * - Tilt transformations utilize static Euler-angle quaternions to produce nested, intersecting 3D orbits.
 * - Velocity-tangent alignment: Computes the 2D tangent vector of the orbit, applies the tilt and global rotations,
 *   and uses `lookAlong` to orient particles perfectly flat along their path (avoiding static flat angles).
 * - Shifting color gradient:
 *   - Singularity Core (index 26): Pulses dynamically in a royal violet-magenta to warm amber gold range.
 *   - Ring Particles: A galactic blend smoothly flowing between bright electric cyan (0.52), deep indigo (0.68),
 *     cosmic magenta/rose (0.88), and warm gold highlights (0.12).
 */
package com.example.glassmenu.render;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.minecraft.util.math.MathHelper;

public class CubeFormationEngine {
    
    /**
     * Calculates the 3D coordinate offset of a particle in the Armillary Ring Vortex.
     * Index 26 acts as a stationary central core singularity at (0, 0, 0).
     *
     * @param index The particle index (0 to 26).
     * @param time  The continuous animation time.
     * @return The 3D position vector in local hand coordinates.
     */
    public static Vector3f getCubePos(int index, double time) {
        // 1. Core Singularity: Anchored at the absolute center
        if (index == 26) {
            return new Vector3f(0f, 0f, 0f);
        }

        // 2. Partition 26 particles into 3 concentric rings
        int ring;
        int pIndex;
        if (index < 8) {
            ring = 0;
            pIndex = index;
        } else if (index < 17) {
            ring = 1;
            pIndex = index - 8;
        } else {
            ring = 2;
            pIndex = index - 17;
        }
        
        int ringParticles = (ring == 0) ? 8 : 9;
        
        // 3. Nested radii and alternate orbital speeds
        float radius = 0.10f + ring * 0.06f; // Ring 0: 0.10f, Ring 1: 0.16f, Ring 2: 0.22f
        double baseAngle = pIndex * (2.0 * Math.PI / ringParticles);
        double orbitAngle;
        
        // Alternate directions and speeds for dynamic complexity
        if (ring == 0) {
            orbitAngle = baseAngle + 2.8 * time;
        } else if (ring == 1) {
            orbitAngle = baseAngle - 2.0 * time;
        } else {
            orbitAngle = baseAngle + 1.4 * time;
        }
        
        // 4. Compute local 2D plane coordinates
        float localX = (float) (Math.cos(orbitAngle) * radius);
        float localY = (float) (Math.sin(orbitAngle) * radius);
        float localZ = 0.0f;
        
        Vector3f pos = new Vector3f(localX, localY, localZ);
        
        // 5. Apply unique tilt rotations to mimic an armillary sphere
        Quaternionf tilt = getRingTilt(ring);
        pos.rotate(tilt);
        
        // 6. Apply a slow ambient global rotation to the entire system
        Quaternionf ambient = new Quaternionf().rotationXYZ(
            (float) time * 0.4f,
            (float) time * 0.25f,
            (float) time * 0.15f
        );
        pos.rotate(ambient);
        
        return pos;
    }

    /**
     * Determines the shifting HSB color of a particle based on its index and time.
     * Transitions smoothly between electric cyan, royal indigo, rose, and amber gold.
     *
     * @param index The particle index (0 to 26).
     * @param time  The continuous animation time.
     * @return RGB color integer format.
     */
    public static int getCubeColor(int index, double time) {
        // 1. Color shift for the Central Singularity (deep violet-magenta to warm gold)
        if (index == 26) {
            float pulse = (float) Math.sin(time * 4.0);
            float hue = MathHelper.lerp(pulse * 0.5f + 0.5f, 0.78f, 0.12f);
            return java.awt.Color.HSBtoRGB(hue, 0.95f, 1.0f) & 0xFFFFFF;
        }
        
        // 2. Galactic color flow for outer rings
        double wave = Math.sin(time * 2.2 + (index * 0.4));
        float hue;
        if (wave > 0) {
            hue = MathHelper.lerp((float)wave, 0.52f, 0.88f); // Cyan to Rose
        } else {
            hue = MathHelper.lerp((float)-wave, 0.88f, 0.12f); // Rose to Gold
        }
        
        return java.awt.Color.HSBtoRGB(hue, 0.90f, 1.0f) & 0xFFFFFF;
    }

    /**
     * Calculates the tangent-aligned rotation quaternion for a particle.
     * Keeps flat droplets oriented dynamically along their speed trajectory.
     *
     * @param index The particle index (0 to 26).
     * @param time  The continuous animation time.
     * @return The rotation quaternion.
     */
    public static Quaternionf getCubeParticleRotation(int index, double time) {
        // 1. Stationary core requires no alignment
        if (index == 26) {
            return new Quaternionf().identity();
        }

        int ring;
        int pIndex;
        if (index < 8) {
            ring = 0;
            pIndex = index;
        } else if (index < 17) {
            ring = 1;
            pIndex = index - 8;
        } else {
            ring = 2;
            pIndex = index - 17;
        }
        
        int ringParticles = (ring == 0) ? 8 : 9;
        
        double baseAngle = pIndex * (2.0 * Math.PI / ringParticles);
        double orbitAngle;
        
        if (ring == 0) {
            orbitAngle = baseAngle + 2.8 * time;
        } else if (ring == 1) {
            orbitAngle = baseAngle - 2.0 * time;
        } else {
            orbitAngle = baseAngle + 1.4 * time;
        }
        
        // 2. Compute the 2D tangent velocity vector in the plane
        float tx = (float) (-Math.sin(orbitAngle));
        float ty = (float) (Math.cos(orbitAngle));
        float tz = 0.0f;
        
        Vector3f tangent = new Vector3f(tx, ty, tz);
        
        // 3. Rotate the tangent vector by the ring's tilt and global ambient rotation
        Quaternionf tilt = getRingTilt(ring);
        tangent.rotate(tilt);
        
        Quaternionf ambient = new Quaternionf().rotationXYZ(
            (float) time * 0.4f,
            (float) time * 0.25f,
            (float) time * 0.15f
        );
        tangent.rotate(ambient);
        
        if (tangent.lengthSquared() > 1e-5f) {
            tangent.normalize();
        } else {
            tangent.set(0, 0, 1);
        }
        
        // 4. Build look-along quaternion (aligning Z axis with motion tangent)
        return new Quaternionf().lookAlong(tangent, new Vector3f(0, 1, 0));
    }
    
    /**
     * Helper method to retrieve unique tilt angles for the three concentric rings.
     */
    private static Quaternionf getRingTilt(int ring) {
        if (ring == 0) {
            // Tilted 45 degrees around X, 30 degrees around Y
            return new Quaternionf().rotationXYZ(0.785398f, 0.523598f, 0.0f);
        } else if (ring == 1) {
            // Tilted 45 degrees around Y, 30 degrees around Z
            return new Quaternionf().rotationXYZ(0.0f, 0.785398f, 0.523598f);
        } else {
            // Tilted 30 degrees around X, 45 degrees around Z
            return new Quaternionf().rotationXYZ(0.523598f, 0.0f, 0.785398f);
        }
    }
}
