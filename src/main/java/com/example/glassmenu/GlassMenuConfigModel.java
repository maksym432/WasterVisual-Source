/**
 * WasterVisual Configuration Model.
 * Defines the settings schema using owo-lib, including Dynamic Island dimensions,
 * colors, Target ESP, and Hand Swing customization.
 */
package com.example.glassmenu;

import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.Modmenu;
import io.wispforest.owo.config.annotation.RangeConstraint;
import io.wispforest.owo.config.annotation.RestartRequired;

@Modmenu(modId = "glassmenu")
@Config(name = "glassmenu-config", wrapperName = "GlassMenuConfig")
public class GlassMenuConfigModel {

    @RangeConstraint(min = 80, max = 300)
    public int capsuleWidth = 110;

    @RangeConstraint(min = 150, max = 500)
    public int expandedWidth = 190;

    @RangeConstraint(min = 10, max = 50)
    public int capsuleHeight = 24;

    @RangeConstraint(min = 0.1f, max = 5.0f)
    public float fadeSpeed = 3.0f;

    public int capsuleColor = 0xEE1C1C1E;

    @RestartRequired
    public boolean enableShaders = true;

    // --- HAND ITEM EFFECTS ---
    public boolean enableItemEffects = true;
    public ItemEffect itemEffect = ItemEffect.NONE;

    public enum ItemEffect {
        NONE, RAINBOW, PARTICLES, RGB_PARTICLES
    }

    public boolean enableTargetEsp = true;
    public int targetEspColor = 0xBF00FF; // Default Purple

    // --- HAND SWING SETTINGS ---
    public boolean enableCustomSwing = true;
    public SwingType swingType = SwingType.SWING_DOWN;
    public float swingSpeed = 1.0f;

    public float handPosX = 0.4f;
    public float handPosY = -0.3f;
    public float handPosZ = -0.7f;

    public float handScaleX = 0.5f;
    public float handScaleY = 0.5f;
    public float handScaleZ = 0.5f;

    public float handRotX = 0.0f;
    public float handRotY = 0.0f;
    public float handRotZ = 0.0f;

    public float swingRotX = -80.0f;
    public float swingRotY = 90.0f;
    public float swingRotZ = 0.0f;

    public enum SwingType {
        DEFAULT, SWING_DOWN, SWING_UP, SWING_CENTER
    }

    // --- VISUALS ---
    public boolean enableJumpRings = true;
    public int jumpRingsColor = 0xFFFFFF; // Default White
    public JumpRingMode jumpRingMode = JumpRingMode.CIRCLE;

    public enum JumpRingMode {
        CIRCLE, BLOCK_OUTLINE
    }

    // --- BRIDGE ---
    public boolean enableBridgeVortex = true;
    public int bridgeVortexColor = 0x00BFFF; // Default Cyan
}
