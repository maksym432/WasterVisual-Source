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

    public boolean enableDynamicIsland = true;

    public boolean transparentIsland = false;

    @RestartRequired
    public boolean enableShaders = true;

    public boolean glassEffect = true;

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

    // --- INVENTORY HUD ---
    public boolean enableInventoryHud = true;
    public int inventoryHudColor = 0xEE1C1C24; // Premium dark glassmorphic panel color
    public boolean transparentBackground = false;
    public int inventoryHudWidth = 260;
    public int inventoryHudHeight = 92;

    // --- POSITIONS ---
    public int islandX = -1;
    public int islandY = 4;
    public int inventoryHudX = 10;
    public int inventoryHudY = -1;
    public int playerCardX = -1;
    public int playerCardY = -1;
    public int playerCardWidth = 140;
    public int playerCardHeight = 54;

    // --- BRIDGE ---
    public boolean enableBridgeVortex = true;
    public int bridgeVortexColor = 0x00BFFF; // Default Cyan
    public boolean transparentBridge = false;

    // --- PLAYER CARD ---
    public boolean enablePlayerCard = true;
    public int playerCardColor = 0xEE1C1C24;
    public boolean transparentPlayerCard = false;

    // --- BEDWARS ---
    public boolean enableBedWarsEsp = false;
    public boolean enableBedWarsNames = false;
    public boolean enableBedWarsHearts = true;
    public BedWarsEspMode bedWarsEspMode = BedWarsEspMode.ALERT;

    public enum BedWarsEspMode {
        HITBOX, ALERT
    }

    // --- USER INDICATOR ---
    public boolean enableUserIndicator = false;
    public int userIndicatorColor = 0xEE1C1C24;
    public boolean transparentUserIndicator = false;
    public int userIndicatorWidth = 203;
    public int userIndicatorHeight = 26;
    public int userIndicatorX = -1;
    public int userIndicatorY = -1;

    // --- ARMOR HUD ---
    public boolean enableArmorHud = false;
    public int armorHudColor = 0xEE1C1C24;
    public boolean transparentArmorHud = false;
    public boolean armorHudVertical = false;
    public int armorHudWidth = 120;
    public int armorHudHeight = 32;
    public int armorHudX = -1;
    public int armorHudY = -1;

    // --- FAST ITEM CIRCULAR HOTBAR ---
    public boolean enableFastItem = false;
    public int fastItemColor = 0xEE1C1C24;
    public boolean transparentFastItem = false;
    public boolean fastItemSlots = true;
    public int fastItemWidth = 160;
    public int fastItemHeight = 160;
    public int fastItemX = -1;
    public int fastItemY = -1;
    public boolean glassHotbar = true;

    // --- USER HUD (custom HP / Food / XP bars) ---
    public boolean enableUserHud = false;
    public boolean transparentUserHud = false;
    public int userHudColor = 0xEE1C1C24;
    public int userHudX = -1;
    public int userHudY = -1;
    public int userHudWidth = 180;
    public int userHudHeight = 26;

    // --- EFFECTS HUD (custom status / potion effects HUD) ---
    public boolean enableEffectsHud = false;
    public boolean transparentEffectsHud = false;
    public boolean effectsHudVertical = false;
    public int effectsHudColor = 0xEE1C1C24;
    public int effectsHudX = -1;
    public int effectsHudY = -1;
    public int effectsHudWidth = 100;
    public int effectsHudHeight = 32;

    // --- LEFT HAND ITEM HUD ---
    public boolean enableLeftHandItem = false;
    public boolean transparentLeftHandItem = false;
    public int leftHandItemColor = 0xEE1C1C24;
    public int leftHandItemX = -1;
    public int leftHandItemY = -1;
    public int leftHandItemWidth = 32;
    public int leftHandItemHeight = 32;

    // --- CUSTOM HIT PARTICLES ---
    public boolean enableCustomHit = false;
    public boolean customHitRgb = false;
    public int customHitColor = 0xFFFFD700; // Default Gold/Yellow
    public int customHitCount = 8;          // Number of stars per burst (1-30)

    // --- GHOST TRAIL (Afterimage) ---
    public boolean enableGhostTrail = false;
    public boolean ghostTrailRgb = false;
    public int ghostTrailColor = 0xFF88CCFF; // Default light-blue
}
