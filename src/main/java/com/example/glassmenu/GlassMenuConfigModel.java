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

    
    public MenuLanguage menuLanguage = MenuLanguage.AUTO;

    public enum MenuLanguage {
        AUTO, ENGLISH, RUSSIAN
    }

    public boolean enableDynamicIsland = false;

    public boolean transparentIsland = false;

    @RestartRequired
    public boolean enableShaders = false;

    public boolean glassEffect = false;

    // --- HAND ITEM EFFECTS ---
    public boolean enableItemEffects = false;
    public ItemEffect itemEffect = ItemEffect.NONE;

    public enum ItemEffect {
        NONE, RAINBOW, PARTICLES, RGB_PARTICLES
    }

    public boolean enableTargetEsp = false;
    public int targetEspColor = 0xBF00FF; // Default Purple

    // --- HAND SWING SETTINGS ---
    public boolean enableCustomSwing = false;
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
    public boolean enableJumpRings = false;
    public int jumpRingsColor = 0xFFFFFF; // Default White
    public JumpRingMode jumpRingMode = JumpRingMode.CIRCLE;

    public enum JumpRingMode {
        CIRCLE, BLOCK_OUTLINE
    }

    // --- INVENTORY HUD ---
    public boolean enableInventoryHud = false;
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
    public boolean enableBridgeVortex = false;
    public int bridgeVortexColor = 0x00BFFF; // Default Cyan
    public boolean transparentBridge = false;

    // --- PLAYER CARD ---
    public boolean enablePlayerCard = false;
    public int playerCardColor = 0xEE1C1C24;
    public boolean transparentPlayerCard = false;

    // --- BEDWARS ---
    public boolean enableBedWarsEsp = false;
    public boolean enableBedWarsNames = false;
    public boolean enableBedWarsHearts = false;
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

    // --- GLASS HOTBAR (Standard Hotbar replacement) ---
    public boolean glassHotbar = false;
    public int glassHotbarColor = 0xEE1C1C24;
    public boolean glassHotbarTransparent = true;
    public boolean glassHotbarSlots = true;
    public boolean glassHotbarVertical = false;
    public boolean glassHotbarNumbers = true;
    public int glassHotbarWidth = 220;
    public int glassHotbarHeight = 34;
    public int glassHotbarX = -1;
    public int glassHotbarY = -1;

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

    // --- STRETCH RESOLUTION ---
    public boolean enableStretch = false;
    public float stretchHorizontal = 1.0f;
    public float stretchVertical = 1.0f;

    // --- COLOR GRADING ---
    public boolean enableColorGrading = false;
    public float cgSaturation = 1.0f;
    public float cgContrast = 1.0f;
    public int cgTint = 0xFFFFFF; // default white

    // --- CUSTOM CROSSHAIR ---
    public boolean enableCustomCrosshair = false;
    public int crosshairMode = 0; // 0 = Rounded Cross, 1 = Dot, 2 = Circle, 3 = 4-Corners
    public int crosshairColor = 0xFF00FF00; // default green
    public float crosshairSize = 1.0f;
    public float crosshairThickness = 1.5f;
    public float crosshairGap = 2.0f;
    public boolean crosshairRainbow = false;
    public float crosshairRainbowSpeed = 1.0f;

    // Custom Nametags
    public boolean enableCustomNametags = false;
    public int customNametagColor = 0xFFFFFFFF;

    // Fall Prediction
    public boolean enableFallPrediction = false;
    public int fallPredictionColor = 0x8800BFFF;

    // --- ATTACK RANGE ---
    public boolean enableAttackRange = false;
    public int attackRangeColor = 0xFF00BFFF; // Default Cyan
    public AttackRangeMode attackRangeMode = AttackRangeMode.GLOW_OUTLINE;
    public boolean attackRangeGear = false;

    public enum AttackRangeMode {
        SOLID_OUTLINE, GLOW_OUTLINE, FILLED
    }

    // --- CROSSHAIR RADAR ---
    public boolean enableCrosshairRadar = false;
    public int crosshairRadarColor = 0xFF00FF00; // Default green
    public boolean crosshairRadarRgb = false;
    public float crosshairRadarRadius = 60.0f;
    public float crosshairRadarIconSize = 6.0f;
    public float crosshairRadarSearchDistance = 64.0f;

    // --- KEYSTROKES ---
    public boolean enableKeystrokes = false;
    public boolean transparentKeystrokes = false;
    public int keystrokesColor = 0xEE1C1C24;
    public int keystrokesX = -1;
    public int keystrokesY = -1;
    public int keystrokesWidth = 76;
    public int keystrokesHeight = 68;
}
