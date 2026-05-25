/*
 * SwordFormationEngine - Architecture & Primary Responsibility:
 * Sword Formation Engine.
 * Precomputes 3D structural blueprints for a high-fidelity energy mini-dagger shape,
 * used during summon mode morph animations in hand particle trails.
 */
package com.example.glassmenu.render;

import org.joml.Vector3f;

public class SwordFormationEngine {
    public static final int TOTAL_PARTICLES = 27; // 3 groups of 9 or just 27 total
    public static final Vector3f[] BLUEPRINT = new Vector3f[TOTAL_PARTICLES];

    static {
        // 1. Generate Mini-Dagger Blueprint
        int idx = 0;

        // Core / Hilt Base (4 points)
        BLUEPRINT[idx++] = new Vector3f(0.04f, 0.05f, 0.02f);
        BLUEPRINT[idx++] = new Vector3f(-0.04f, 0.05f, 0.02f);
        BLUEPRINT[idx++] = new Vector3f(-0.04f, 0.05f, -0.02f);
        BLUEPRINT[idx++] = new Vector3f(0.04f, 0.05f, -0.02f);

        // Blade (23 points) - Diamond section, tapering up to Y=0.38
        for (int i = 0; i < 23; i++) {
            float y = 0.05f + (i * 0.015f); // Tapers up to ~0.395
            float width = 0.06f * (1.0f - (i / 23.0f)); // Taper
            
            float x = 0, z = 0;
            switch (i % 4) {
                case 0: x = width; break;
                case 1: x = -width; break;
                case 2: z = width * 0.4f; break;
                case 3: z = -width * 0.4f; break;
            }
            
            BLUEPRINT[idx++] = new Vector3f(x, y, z);
        }
    }

    public static Vector3f getSwordPos(int index) {
        return BLUEPRINT[index];
    }
    
    public static org.joml.Quaternionf getSwordRotation() {
        // Oriented vertically for the blade
        return new org.joml.Quaternionf().rotationXYZ(0, 0, 0);
    }
}
