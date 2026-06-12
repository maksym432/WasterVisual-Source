/**
 * LiquidGlassScreen.java
 *
 * Primary Responsibility:
 * Main configuration GUI screen for WasterVisual.
 *
 * Architectural Role:
 * Draws the premium Liquid Glass tabbed menu, letting players adjust settings (Item Effects,
 * Movement, Combat, Visuals, and Bridge options). Manages interactive 3D item/hand previews,
 * Target ESP spiral previews, widget layout animations, and text input bindings.
 *
 * Mathematical & Visual Styling:
 * - Fluid sizing: smoothly transitions between Normal (340x220) and Expanded (420x280) menu formats
 *   using lerp-interpolated progress steps driven by system delta-time (ticks).
 * - Implements premium SDF rounded border panels (LiquidLensView) and custom blurred backgrounds.
 * - Restructured Tab.BRIDGE to utilize Expanded mode, preventing input widget overlaps.
 */
package com.example.glassmenu.screen;

import com.example.glassmenu.GlassMenuClient;
import com.example.glassmenu.GlassMenuConfigModel;
import com.example.glassmenu.render.HandParticleRenderer;
import com.example.glassmenu.render.RenderUtils;
import com.example.glassmenu.render.TargetESPManager;
import com.example.glassmenu.widget.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.util.Identifier;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;

public class LiquidGlassScreen extends Screen {
    private LiquidGlassEffectView effectView;
    private LiquidLensView lensView;
    
    private enum Tab { GENERAL, MOVEMENT, COMBAT, VISUALS, VISUALS_JUMP, VISUALS_INV_HUD, VISUALS_PLAYER_CARD, VISUALS_BEDWARS, VISUALS_INDICATOR, VISUALS_ARMOR_HUD, VISUALS_FAST_ITEM, VISUALS_USER_HUD, VISUALS_EFFECTS, VISUALS_LEFT_HAND_ITEM, VISUALS_ISLAND, VISUALS_HIT, VISUALS_GHOST, POSITION, BRIDGE }
    private Tab currentTab = Tab.GENERAL;
    
    private final List<ClickableWidget> generalWidgets = new ArrayList<>();
    private final List<ClickableWidget> movementWidgets = new ArrayList<>();
    private final List<ClickableWidget> combatWidgets = new ArrayList<>();
    private final List<ClickableWidget> visualsWidgets = new ArrayList<>();
    private final List<ClickableWidget> visualsJumpWidgets = new ArrayList<>();
    private final List<ClickableWidget> visualsInvHudWidgets = new ArrayList<>();
    private final List<ClickableWidget> visualsPlayerCardWidgets = new ArrayList<>();
    private final List<ClickableWidget> visualsBedWarsWidgets = new ArrayList<>();
    private final List<ClickableWidget> visualsIndicatorWidgets = new ArrayList<>();
    private final List<ClickableWidget> visualsArmorHudWidgets = new ArrayList<>();
    private final List<ClickableWidget> visualsFastItemWidgets = new ArrayList<>();
    private final List<ClickableWidget> visualsUserHudWidgets = new ArrayList<>();
    private final List<ClickableWidget> visualsEffectsWidgets = new ArrayList<>();
    private final List<ClickableWidget> visualsLeftHandItemWidgets = new ArrayList<>();
    private final List<ClickableWidget> visualsIslandWidgets = new ArrayList<>();
    private final List<ClickableWidget> visualsHitWidgets  = new ArrayList<>();
    private final List<ClickableWidget> visualsGhostWidgets = new ArrayList<>();
    private final List<ClickableWidget> positionWidgets = new ArrayList<>();
    private final List<ClickableWidget> bridgeWidgets = new ArrayList<>();

    // UI Constants
    private static final float PANEL_W_NORMAL = 340f;
    private static final float PANEL_W_EXPANDED = 420f; 
    private static final float PANEL_H_NORMAL = 220f;
    private static final float PANEL_H_EXPANDED = 280f;

    private float getPanelW() { return (currentTab == Tab.MOVEMENT || currentTab == Tab.GENERAL || currentTab == Tab.VISUALS || currentTab == Tab.VISUALS_JUMP || currentTab == Tab.VISUALS_INV_HUD || currentTab == Tab.VISUALS_PLAYER_CARD || currentTab == Tab.VISUALS_BEDWARS || currentTab == Tab.VISUALS_INDICATOR || currentTab == Tab.VISUALS_ARMOR_HUD || currentTab == Tab.VISUALS_FAST_ITEM || currentTab == Tab.VISUALS_USER_HUD || currentTab == Tab.VISUALS_EFFECTS || currentTab == Tab.VISUALS_LEFT_HAND_ITEM || currentTab == Tab.VISUALS_ISLAND || currentTab == Tab.VISUALS_HIT || currentTab == Tab.VISUALS_GHOST || currentTab == Tab.BRIDGE) ? PANEL_W_EXPANDED : PANEL_W_NORMAL; }
    private float getPanelH() { return (currentTab == Tab.MOVEMENT || currentTab == Tab.GENERAL || currentTab == Tab.VISUALS || currentTab == Tab.VISUALS_JUMP || currentTab == Tab.VISUALS_INV_HUD || currentTab == Tab.VISUALS_PLAYER_CARD || currentTab == Tab.VISUALS_BEDWARS || currentTab == Tab.VISUALS_INDICATOR || currentTab == Tab.VISUALS_ARMOR_HUD || currentTab == Tab.VISUALS_FAST_ITEM || currentTab == Tab.VISUALS_USER_HUD || currentTab == Tab.VISUALS_EFFECTS || currentTab == Tab.VISUALS_LEFT_HAND_ITEM || currentTab == Tab.VISUALS_ISLAND || currentTab == Tab.VISUALS_HIT || currentTab == Tab.VISUALS_GHOST || currentTab == Tab.BRIDGE) ? PANEL_H_EXPANDED : PANEL_H_NORMAL; }

    // State
    private TextFieldWidget hexInput, rgbInput, visHexInput, visRgbInput, invHexInput, invRgbInput, bridgeHexInput, bridgeRgbInput, hitHexInput, hitRgbInput, ghostHexInput, ghostRgbInput;
    private LiquidGlassSlider sliderR, sliderG, sliderB, visSliderR, visSliderG, visSliderB, invSliderR, invSliderG, invSliderB, bridgeSliderR, bridgeSliderG, bridgeSliderB, cardSliderR, cardSliderG, cardSliderB, indSliderR, indSliderG, indSliderB, armSliderR, armSliderG, armSliderB, fastItemSliderR, fastItemSliderG, fastItemSliderB, userHudSliderR, userHudSliderG, userHudSliderB, effectsSliderR, effectsSliderG, effectsSliderB, leftHandSliderR, leftHandSliderG, leftHandSliderB, hitSliderR, hitSliderG, hitSliderB, hitSliderCount, ghostSliderR, ghostSliderG, ghostSliderB;
    private double scrollY = 0;
    private double maxScroll = 0;
    private boolean isUpdating = false;

    // Position Editor State
    private enum PositionObject { NONE, ISLAND, INVENTORY, PLAYER, INDICATOR, ARMOR, FAST_ITEM, USER_HUD, EFFECTS, LEFT_HAND_ITEM }
    private PositionObject selectedObject = PositionObject.NONE;
    private boolean isDraggingObject = false;
    private double dragOffsetX = 0;
    private double dragOffsetY = 0;

    // Animation State
    private float panelWidthProgress = 0f;
    private float panelHeightProgress = 0f;
    private float panelXOffsetProgress = 0f;
    private float contentAlpha = 0f;
    private final float[] tabHoverProgress = new float[Tab.values().length];
    private long lastAnimTime;

    public LiquidGlassScreen() {
        super(Text.literal("WasterVisual"));
        this.effectView = new LiquidGlassEffectView();
        this.lastAnimTime = System.currentTimeMillis();
        for (int i = 0; i < tabHoverProgress.length; i++) tabHoverProgress[i] = 0f;
    }

    @Override
    protected void init() {
        if (GlassMenuClient.CONFIG.glassEffect()) {
            this.effectView.enableBlur();
        } else {
            this.effectView.disableBlur();
        }
        panelWidthProgress = (currentTab == Tab.MOVEMENT || currentTab == Tab.GENERAL || currentTab == Tab.VISUALS || currentTab == Tab.VISUALS_JUMP || currentTab == Tab.VISUALS_INV_HUD || currentTab == Tab.VISUALS_PLAYER_CARD || currentTab == Tab.VISUALS_BEDWARS || currentTab == Tab.VISUALS_INDICATOR || currentTab == Tab.VISUALS_ARMOR_HUD || currentTab == Tab.VISUALS_FAST_ITEM || currentTab == Tab.VISUALS_USER_HUD || currentTab == Tab.VISUALS_EFFECTS || currentTab == Tab.VISUALS_LEFT_HAND_ITEM || currentTab == Tab.VISUALS_ISLAND || currentTab == Tab.VISUALS_HIT || currentTab == Tab.BRIDGE) ? 1.0f : 0.0f;
        panelHeightProgress = (currentTab == Tab.MOVEMENT || currentTab == Tab.GENERAL || currentTab == Tab.VISUALS || currentTab == Tab.VISUALS_JUMP || currentTab == Tab.VISUALS_INV_HUD || currentTab == Tab.VISUALS_PLAYER_CARD || currentTab == Tab.VISUALS_BEDWARS || currentTab == Tab.VISUALS_INDICATOR || currentTab == Tab.VISUALS_ARMOR_HUD || currentTab == Tab.VISUALS_FAST_ITEM || currentTab == Tab.VISUALS_USER_HUD || currentTab == Tab.VISUALS_EFFECTS || currentTab == Tab.VISUALS_LEFT_HAND_ITEM || currentTab == Tab.VISUALS_ISLAND || currentTab == Tab.VISUALS_HIT || currentTab == Tab.BRIDGE) ? 1.0f : 0.0f;
        contentAlpha = 1.0f;

        float x = (this.width - getPanelW()) / 2f;
        float y = (this.height - getPanelH()) / 2f;
        this.lensView = new LiquidLensView(x, y, getPanelW(), getPanelH());

        // 1. Initialize Lists once per screen init (resize/open)
        generalWidgets.clear(); movementWidgets.clear(); combatWidgets.clear(); visualsWidgets.clear(); visualsJumpWidgets.clear(); visualsInvHudWidgets.clear(); visualsPlayerCardWidgets.clear(); visualsBedWarsWidgets.clear(); visualsIndicatorWidgets.clear(); visualsArmorHudWidgets.clear(); visualsFastItemWidgets.clear(); visualsUserHudWidgets.clear(); visualsEffectsWidgets.clear(); visualsLeftHandItemWidgets.clear(); visualsIslandWidgets.clear(); visualsHitWidgets.clear(); visualsGhostWidgets.clear(); positionWidgets.clear(); bridgeWidgets.clear();
        
        initGeneralTab(x, y);
        initCombatTab(x, y);
        initMovementTab(x, y);
        initVisualsTab(x, y);
        initVisualsJumpTab(x, y);
        initVisualsInvHudTab(x, y);
        initVisualsPlayerCardTab(x, y);
        initVisualsBedWarsTab(x, y);
        initVisualsIndicatorTab(x, y);
        initVisualsArmorHudTab(x, y);
        initVisualsFastItemTab(x, y);
        initVisualsUserHudTab(x, y);
        initVisualsEffectsTab(x, y);
        initVisualsLeftHandItemTab(x, y);
        initVisualsIslandTab(x, y);
        initVisualsHitTab(x, y);
        initVisualsGhostTab(x, y);
        initPositionTab();
        initBridgeTab(x, y);

        updateVisibleWidgets();
    }

    private void initBridgeTab(float x, float y) {
        LiquidGlassButton exitBtn = new LiquidGlassButton((int)x + 230, (int)y + 50, 80, 22, Text.literal("Back"), b -> {
            currentTab = Tab.GENERAL; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        bridgeWidgets.add(exitBtn);

        LiquidGlassSwitch bridgeToggle = new LiquidGlassSwitch((int)x + 330, (int)y + 45, 40, 20, GlassMenuClient.CONFIG.enableBridgeVortex());
        bridgeToggle.setOnToggle(enabled -> {
            GlassMenuClient.CONFIG.enableBridgeVortex(enabled); GlassMenuClient.CONFIG.save();
        });
        bridgeWidgets.add(bridgeToggle);
        
        int currentColor = GlassMenuClient.CONFIG.bridgeVortexColor();
        bridgeSliderR = new LiquidGlassSlider((int)x + 30, (int)y + 90, 140, 16, ((currentColor >> 16) & 0xFF) / 255f);
        bridgeSliderG = new LiquidGlassSlider((int)x + 30, (int)y + 125, 140, 16, ((currentColor >> 8) & 0xFF) / 255f);
        bridgeSliderB = new LiquidGlassSlider((int)x + 30, (int)y + 160, 140, 16, (currentColor & 0xFF) / 255f);

        Runnable updateColor = () -> {
            if (isUpdating) return; isUpdating = true;
            int r = (int)(bridgeSliderR.getValue() * 255), g = (int)(bridgeSliderG.getValue() * 255), b = (int)(bridgeSliderB.getValue() * 255);
            int color = 0xFF000000 | (r << 16) | (g << 8) | b;
            GlassMenuClient.CONFIG.bridgeVortexColor(color); GlassMenuClient.CONFIG.save();
            if (bridgeHexInput != null) bridgeHexInput.setText(String.format("#%06X", color & 0xFFFFFF));
            if (bridgeRgbInput != null) bridgeRgbInput.setText(String.format("%d, %d, %d", r, g, b));
            isUpdating = false;
        };

        bridgeSliderR.setOnValueChange(v -> updateColor.run());
        bridgeSliderG.setOnValueChange(v -> updateColor.run());
        bridgeSliderB.setOnValueChange(v -> updateColor.run());
        bridgeWidgets.add(bridgeSliderR); bridgeWidgets.add(bridgeSliderG); bridgeWidgets.add(bridgeSliderB);

        bridgeHexInput = createColorTextField((int)x + 235, (int)y + 100, 60);
        bridgeHexInput.setText(String.format("#%06X", currentColor & 0xFFFFFF));
        
        // Add dynamic text listener to parse and update bridge vortex color using hex format
        bridgeHexInput.setChangedListener(text -> {
            if (isUpdating) return;
            try {
                String clean = text.trim();
                if (clean.startsWith("#")) clean = clean.substring(1);
                if (clean.length() == 6) {
                    int color = Integer.parseInt(clean, 16);
                    isUpdating = true;
                    int r = (color >> 16) & 0xFF;
                    int g = (color >> 8) & 0xFF;
                    int b = color & 0xFF;
                    bridgeSliderR.setValue(r / 255.0f);
                    bridgeSliderG.setValue(g / 255.0f);
                    bridgeSliderB.setValue(b / 255.0f);
                    GlassMenuClient.CONFIG.bridgeVortexColor(0xFF000000 | color);
                    GlassMenuClient.CONFIG.save();
                    if (bridgeRgbInput != null) bridgeRgbInput.setText(String.format("%d, %d, %d", r, g, b));
                    isUpdating = false;
                }
            } catch (NumberFormatException ignored) {}
        });

        bridgeRgbInput = createColorTextField((int)x + 235, (int)y + 130, 100);
        bridgeRgbInput.setText(String.format("%d, %d, %d", (currentColor >> 16) & 0xFF, (currentColor >> 8) & 0xFF, currentColor & 0xFF));

        // Add dynamic text listener to parse and update bridge vortex color using comma-separated RGB
        bridgeRgbInput.setChangedListener(text -> {
            if (isUpdating) return;
            try {
                String[] parts = text.split(",");
                if (parts.length == 3) {
                    int r = Integer.parseInt(parts[0].trim());
                    int g = Integer.parseInt(parts[1].trim());
                    int b = Integer.parseInt(parts[2].trim());
                    if (r >= 0 && r <= 255 && g >= 0 && g <= 255 && b >= 0 && b <= 255) {
                        isUpdating = true;
                        bridgeSliderR.setValue(r / 255.0f);
                        bridgeSliderG.setValue(g / 255.0f);
                        bridgeSliderB.setValue(b / 255.0f);
                        int color = 0xFF000000 | (r << 16) | (g << 8) | b;
                        GlassMenuClient.CONFIG.bridgeVortexColor(color);
                        GlassMenuClient.CONFIG.save();
                        if (bridgeHexInput != null) bridgeHexInput.setText(String.format("#%06X", color & 0xFFFFFF));
                        isUpdating = false;
                    }
                }
            } catch (NumberFormatException ignored) {}
        });

        // Add Glass Effect button
        String glassText = GlassMenuClient.CONFIG.transparentBridge() ? "Glass Effect: ON" : "Glass Effect: OFF";
        LiquidGlassButton glassBtn = new LiquidGlassButton((int)x + 230, (int)y + 160, 120, 22, Text.literal(glassText), b -> {
            boolean current = GlassMenuClient.CONFIG.transparentBridge();
            GlassMenuClient.CONFIG.transparentBridge(!current);
            GlassMenuClient.CONFIG.save();
            b.setMessage(Text.literal(GlassMenuClient.CONFIG.transparentBridge() ? "Glass Effect: ON" : "Glass Effect: OFF"));
        });
        bridgeWidgets.add(glassBtn);
    }

    private void initVisualsTab(float x, float y) {
        LiquidGlassButton jumpEffectsBtn = new LiquidGlassButton(0, 0, 160, 22, Text.literal("Jump Effects"), b -> {
            currentTab = Tab.VISUALS_JUMP; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        LiquidGlassButton invHudBtn = new LiquidGlassButton(0, 0, 160, 22, Text.literal("Inventory HUD"), b -> {
            currentTab = Tab.VISUALS_INV_HUD; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        LiquidGlassButton playerCardBtn = new LiquidGlassButton(0, 0, 160, 22, Text.literal("Player Card"), b -> {
            currentTab = Tab.VISUALS_PLAYER_CARD; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        LiquidGlassButton bedwarsBtn = new LiquidGlassButton(0, 0, 160, 22, Text.literal("BedWars ESP"), b -> {
            currentTab = Tab.VISUALS_BEDWARS; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        LiquidGlassButton indicatorBtn = new LiquidGlassButton(0, 0, 160, 22, Text.literal("User Indicator"), b -> {
            currentTab = Tab.VISUALS_INDICATOR; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        LiquidGlassButton armorHudBtn = new LiquidGlassButton(0, 0, 160, 22, Text.literal("Armor HUD"), b -> {
            currentTab = Tab.VISUALS_ARMOR_HUD; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        LiquidGlassButton fastItemBtn = new LiquidGlassButton(0, 0, 160, 22, Text.literal("Item HUD"), b -> {
            currentTab = Tab.VISUALS_FAST_ITEM; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        LiquidGlassButton userHudBtn = new LiquidGlassButton(0, 0, 160, 22, Text.literal("User HUD"), b -> {
            currentTab = Tab.VISUALS_USER_HUD; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        LiquidGlassButton effectsBtn = new LiquidGlassButton(0, 0, 160, 22, Text.literal("Potion Effects"), b -> {
            currentTab = Tab.VISUALS_EFFECTS; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        LiquidGlassButton leftHandItemBtn = new LiquidGlassButton(0, 0, 160, 22, Text.literal("Left Hand Item"), b -> {
            currentTab = Tab.VISUALS_LEFT_HAND_ITEM; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        LiquidGlassButton islandBtn = new LiquidGlassButton(0, 0, 160, 22, Text.literal("Dynamic Island"), b -> {
            currentTab = Tab.VISUALS_ISLAND; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        LiquidGlassButton hitBtn = new LiquidGlassButton(0, 0, 160, 22, Text.literal("Hit Effects"), b -> {
            currentTab = Tab.VISUALS_HIT; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        LiquidGlassButton positionBtn = new LiquidGlassButton(0, 0, 160, 22, Text.literal("Position Editor"), b -> {
            currentTab = Tab.POSITION; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        // Left Column: HUD & Overlays
        visualsWidgets.add(effectsBtn);
        visualsWidgets.add(armorHudBtn);
        visualsWidgets.add(userHudBtn);
        visualsWidgets.add(indicatorBtn);
        visualsWidgets.add(leftHandItemBtn);
        visualsWidgets.add(invHudBtn);

        // Right Column: World & Interaction
        visualsWidgets.add(jumpEffectsBtn);
        visualsWidgets.add(fastItemBtn);
        visualsWidgets.add(bedwarsBtn);
        visualsWidgets.add(playerCardBtn);
        visualsWidgets.add(islandBtn);
        visualsWidgets.add(hitBtn);
        LiquidGlassButton ghostBtn = new LiquidGlassButton(0, 0, 160, 22, Text.literal("Afterimage"), b -> {
            currentTab = Tab.VISUALS_GHOST; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        visualsWidgets.add(ghostBtn);
        visualsWidgets.add(positionBtn);
    }

    private void initVisualsJumpTab(float x, float y) {
        LiquidGlassButton backBtn = new LiquidGlassButton((int)x + 40, (int)y + 210, 80, 22, Text.literal("Back"), b -> {
            currentTab = Tab.VISUALS; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        visualsJumpWidgets.add(backBtn);

        LiquidGlassSwitch ringToggle = new LiquidGlassSwitch((int)x + 330, (int)y + 45, 40, 20, GlassMenuClient.CONFIG.enableJumpRings());
        ringToggle.setOnToggle(enabled -> {
            GlassMenuClient.CONFIG.enableJumpRings(enabled); GlassMenuClient.CONFIG.save();
        });
        visualsJumpWidgets.add(ringToggle);
        
        int currentColor = GlassMenuClient.CONFIG.jumpRingsColor();
        visSliderR = new LiquidGlassSlider((int)x + 230, (int)y + 90, 140, 16, ((currentColor >> 16) & 0xFF) / 255f);
        visSliderG = new LiquidGlassSlider((int)x + 230, (int)y + 125, 140, 16, ((currentColor >> 8) & 0xFF) / 255f);
        visSliderB = new LiquidGlassSlider((int)x + 230, (int)y + 160, 140, 16, (currentColor & 0xFF) / 255f);

        Runnable updateColor = () -> {
            if (isUpdating) return; isUpdating = true;
            int r = (int)(visSliderR.getValue() * 255), g = (int)(visSliderG.getValue() * 255), b = (int)(visSliderB.getValue() * 255);
            int color = 0xFF000000 | (r << 16) | (g << 8) | b;
            GlassMenuClient.CONFIG.jumpRingsColor(color); GlassMenuClient.CONFIG.save();
            if (visHexInput != null) visHexInput.setText(String.format("#%06X", color & 0xFFFFFF));
            if (visRgbInput != null) visRgbInput.setText(String.format("%d, %d, %d", r, g, b));
            isUpdating = false;
        };

        visSliderR.setOnValueChange(v -> updateColor.run());
        visSliderG.setOnValueChange(v -> updateColor.run());
        visSliderB.setOnValueChange(v -> updateColor.run());
        visualsJumpWidgets.add(visSliderR); visualsJumpWidgets.add(visSliderG); visualsJumpWidgets.add(visSliderB);

        visHexInput = createColorTextField((int)x + 40, (int)y + 38, 60);
        visHexInput.setText(String.format("#%06X", currentColor & 0xFFFFFF));
        
        visHexInput.setChangedListener(text -> {
            if (isUpdating) return;
            try {
                String clean = text.trim();
                if (clean.startsWith("#")) clean = clean.substring(1);
                if (clean.length() == 6) {
                    int color = Integer.parseInt(clean, 16);
                    isUpdating = true;
                    int r = (color >> 16) & 0xFF;
                    int g = (color >> 8) & 0xFF;
                    int b = color & 0xFF;
                    visSliderR.setValue(r / 255.0f);
                    visSliderG.setValue(g / 255.0f);
                    visSliderB.setValue(b / 255.0f);
                    GlassMenuClient.CONFIG.jumpRingsColor(0xFF000000 | color);
                    GlassMenuClient.CONFIG.save();
                    if (visRgbInput != null) visRgbInput.setText(String.format("%d, %d, %d", r, g, b));
                    isUpdating = false;
                }
            } catch (NumberFormatException ignored) {}
        });

        visRgbInput = createColorTextField((int)x + 40, (int)y + 63, 100);
        visRgbInput.setText(String.format("%d, %d, %d", (currentColor >> 16) & 0xFF, (currentColor >> 8) & 0xFF, currentColor & 0xFF));
        
        visRgbInput.setChangedListener(text -> {
            if (isUpdating) return;
            try {
                String[] parts = text.split(",");
                if (parts.length == 3) {
                    int r = Integer.parseInt(parts[0].trim());
                    int g = Integer.parseInt(parts[1].trim());
                    int b = Integer.parseInt(parts[2].trim());
                    if (r >= 0 && r <= 255 && g >= 0 && g <= 255 && b >= 0 && b <= 255) {
                        isUpdating = true;
                        visSliderR.setValue(r / 255.0f);
                        visSliderG.setValue(g / 255.0f);
                        visSliderB.setValue(b / 255.0f);
                        int color = 0xFF000000 | (r << 16) | (g << 8) | b;
                        GlassMenuClient.CONFIG.jumpRingsColor(color);
                        GlassMenuClient.CONFIG.save();
                        if (visHexInput != null) visHexInput.setText(String.format("#%06X", color & 0xFFFFFF));
                        isUpdating = false;
                    }
                }
            } catch (NumberFormatException ignored) {}
        });

        String[] modeLabels = {"CIRCLE", "SQUARES"};
        GlassMenuConfigModel.JumpRingMode[] modes = GlassMenuConfigModel.JumpRingMode.values();
        for (int i = 0; i < modeLabels.length; i++) {
            final GlassMenuConfigModel.JumpRingMode mode = modes[i];
            LiquidGlassButton btn = new LiquidGlassButton((int)x + 40, (int)y + 130 + i * 28, 80, 22, Text.literal(modeLabels[i]), b -> {
                GlassMenuClient.CONFIG.jumpRingMode(mode); GlassMenuClient.CONFIG.save();
            });
            visualsJumpWidgets.add(btn);
        }
    }

    private void initVisualsInvHudTab(float x, float y) {
        LiquidGlassButton backBtn = new LiquidGlassButton((int)x + 40, (int)y + 210, 80, 22, Text.literal("Back"), b -> {
            currentTab = Tab.VISUALS; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        visualsInvHudWidgets.add(backBtn);

        LiquidGlassSwitch hudToggle = new LiquidGlassSwitch((int)x + 330, (int)y + 45, 40, 20, GlassMenuClient.CONFIG.enableInventoryHud());
        hudToggle.setOnToggle(enabled -> {
            GlassMenuClient.CONFIG.enableInventoryHud(enabled); GlassMenuClient.CONFIG.save();
        });
        visualsInvHudWidgets.add(hudToggle);
        
        int currentColor = GlassMenuClient.CONFIG.inventoryHudColor();
        invSliderR = new LiquidGlassSlider((int)x + 230, (int)y + 90, 140, 16, ((currentColor >> 16) & 0xFF) / 255f);
        invSliderG = new LiquidGlassSlider((int)x + 230, (int)y + 125, 140, 16, ((currentColor >> 8) & 0xFF) / 255f);
        invSliderB = new LiquidGlassSlider((int)x + 230, (int)y + 160, 140, 16, (currentColor & 0xFF) / 255f);

        Runnable updateColor = () -> {
            if (isUpdating) return; isUpdating = true;
            int r = (int)(invSliderR.getValue() * 255), g = (int)(invSliderG.getValue() * 255), b = (int)(invSliderB.getValue() * 255);
            int currentVal = GlassMenuClient.CONFIG.inventoryHudColor();
            int alpha = (currentVal >> 24) & 0xFF;
            if (alpha == 0) alpha = 0xEE; // make it visible if adjusted
            int color = (alpha << 24) | (r << 16) | (g << 8) | b;
            GlassMenuClient.CONFIG.inventoryHudColor(color); GlassMenuClient.CONFIG.save();
            isUpdating = false;
        };

        invSliderR.setOnValueChange(v -> updateColor.run());
        invSliderG.setOnValueChange(v -> updateColor.run());
        invSliderB.setOnValueChange(v -> updateColor.run());
        visualsInvHudWidgets.add(invSliderR); visualsInvHudWidgets.add(invSliderG); visualsInvHudWidgets.add(invSliderB);

        String btnText = GlassMenuClient.CONFIG.transparentBackground() ? "Glass Effect: ON" : "Glass Effect: OFF";
        LiquidGlassButton transparentBtn = new LiquidGlassButton((int)x + 40, (int)y + 50, 120, 22, Text.literal(btnText), b -> {
            boolean current = GlassMenuClient.CONFIG.transparentBackground();
            GlassMenuClient.CONFIG.transparentBackground(!current);
            GlassMenuClient.CONFIG.save();
            b.setMessage(Text.literal(GlassMenuClient.CONFIG.transparentBackground() ? "Glass Effect: ON" : "Glass Effect: OFF"));
        });
        visualsInvHudWidgets.add(transparentBtn);
    }

    private void initVisualsPlayerCardTab(float x, float y) {
        LiquidGlassButton backBtn = new LiquidGlassButton((int)x + 40, (int)y + 210, 80, 22, Text.literal("Back"), b -> {
            currentTab = Tab.VISUALS; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        visualsPlayerCardWidgets.add(backBtn);

        LiquidGlassSwitch cardToggle = new LiquidGlassSwitch((int)x + 330, (int)y + 45, 40, 20, GlassMenuClient.CONFIG.enablePlayerCard());
        cardToggle.setOnToggle(enabled -> {
            GlassMenuClient.CONFIG.enablePlayerCard(enabled); GlassMenuClient.CONFIG.save();
        });
        visualsPlayerCardWidgets.add(cardToggle);
        
        int currentColor = GlassMenuClient.CONFIG.playerCardColor();
        cardSliderR = new LiquidGlassSlider((int)x + 230, (int)y + 90, 140, 16, ((currentColor >> 16) & 0xFF) / 255f);
        cardSliderG = new LiquidGlassSlider((int)x + 230, (int)y + 125, 140, 16, ((currentColor >> 8) & 0xFF) / 255f);
        cardSliderB = new LiquidGlassSlider((int)x + 230, (int)y + 160, 140, 16, (currentColor & 0xFF) / 255f);

        Runnable updateColor = () -> {
            if (isUpdating) return; isUpdating = true;
            int r = (int)(cardSliderR.getValue() * 255), g = (int)(cardSliderG.getValue() * 255), b = (int)(cardSliderB.getValue() * 255);
            int currentVal = GlassMenuClient.CONFIG.playerCardColor();
            int alpha = (currentVal >> 24) & 0xFF;
            if (alpha == 0) alpha = 0xEE;
            int color = (alpha << 24) | (r << 16) | (g << 8) | b;
            GlassMenuClient.CONFIG.playerCardColor(color); GlassMenuClient.CONFIG.save();
            isUpdating = false;
        };

        cardSliderR.setOnValueChange(v -> updateColor.run());
        cardSliderG.setOnValueChange(v -> updateColor.run());
        cardSliderB.setOnValueChange(v -> updateColor.run());
        visualsPlayerCardWidgets.add(cardSliderR); visualsPlayerCardWidgets.add(cardSliderG); visualsPlayerCardWidgets.add(cardSliderB);

        String btnText = GlassMenuClient.CONFIG.transparentPlayerCard() ? "Glass Effect: ON" : "Glass Effect: OFF";
        LiquidGlassButton transparentBtn = new LiquidGlassButton((int)x + 40, (int)y + 80, 120, 22, Text.literal(btnText), b -> {
            boolean current = GlassMenuClient.CONFIG.transparentPlayerCard();
            GlassMenuClient.CONFIG.transparentPlayerCard(!current);
            GlassMenuClient.CONFIG.save();
            b.setMessage(Text.literal(GlassMenuClient.CONFIG.transparentPlayerCard() ? "Glass Effect: ON" : "Glass Effect: OFF"));
        });
        visualsPlayerCardWidgets.add(transparentBtn);
    }

    private void initVisualsBedWarsTab(float x, float y) {
        LiquidGlassButton backBtn = new LiquidGlassButton((int)x + 40, (int)y + 223, 80, 22, Text.literal("Back"), b -> {
            currentTab = Tab.VISUALS; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        visualsBedWarsWidgets.add(backBtn);

        LiquidGlassSwitch espToggle = new LiquidGlassSwitch((int)x + 330, (int)y + 75, 40, 20, GlassMenuClient.CONFIG.enableBedWarsEsp());
        espToggle.setOnToggle(enabled -> {
            GlassMenuClient.CONFIG.enableBedWarsEsp(enabled); GlassMenuClient.CONFIG.save();
        });
        visualsBedWarsWidgets.add(espToggle);

        LiquidGlassSwitch heartsToggle = new LiquidGlassSwitch((int)x + 330, (int)y + 115, 40, 20, GlassMenuClient.CONFIG.enableBedWarsHearts());
        heartsToggle.setOnToggle(enabled -> {
            GlassMenuClient.CONFIG.enableBedWarsHearts(enabled); GlassMenuClient.CONFIG.save();
        });
        visualsBedWarsWidgets.add(heartsToggle);

        LiquidGlassSwitch namesToggle = new LiquidGlassSwitch((int)x + 330, (int)y + 155, 40, 20, GlassMenuClient.CONFIG.enableBedWarsNames());
        namesToggle.setOnToggle(enabled -> {
            GlassMenuClient.CONFIG.enableBedWarsNames(enabled); GlassMenuClient.CONFIG.save();
        });
        visualsBedWarsWidgets.add(namesToggle);

        String modeText = "Mode: " + GlassMenuClient.CONFIG.bedWarsEspMode().name();
        LiquidGlassButton modeBtn = new LiquidGlassButton((int)x + 40, (int)y + 200, 120, 22, Text.literal(modeText), b -> {
            GlassMenuConfigModel.BedWarsEspMode currentMode = GlassMenuClient.CONFIG.bedWarsEspMode();
            GlassMenuConfigModel.BedWarsEspMode nextMode = (currentMode == GlassMenuConfigModel.BedWarsEspMode.HITBOX)
                ? GlassMenuConfigModel.BedWarsEspMode.ALERT
                : GlassMenuConfigModel.BedWarsEspMode.HITBOX;
            GlassMenuClient.CONFIG.bedWarsEspMode(nextMode);
            GlassMenuClient.CONFIG.save();
            b.setMessage(Text.literal("Mode: " + nextMode.name()));
        });
        visualsBedWarsWidgets.add(modeBtn);
    }

    private void initVisualsIndicatorTab(float x, float y) {
        LiquidGlassButton backBtn = new LiquidGlassButton((int)x + 40, (int)y + 210, 80, 22, Text.literal("Back"), b -> {
            currentTab = Tab.VISUALS; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        visualsIndicatorWidgets.add(backBtn);

        LiquidGlassSwitch indToggle = new LiquidGlassSwitch((int)x + 330, (int)y + 45, 40, 20, GlassMenuClient.CONFIG.enableUserIndicator());
        indToggle.setOnToggle(enabled -> {
            GlassMenuClient.CONFIG.enableUserIndicator(enabled); GlassMenuClient.CONFIG.save();
        });
        visualsIndicatorWidgets.add(indToggle);
        
        int currentColor = GlassMenuClient.CONFIG.userIndicatorColor();
        indSliderR = new LiquidGlassSlider((int)x + 230, (int)y + 90, 140, 16, ((currentColor >> 16) & 0xFF) / 255f);
        indSliderG = new LiquidGlassSlider((int)x + 230, (int)y + 125, 140, 16, ((currentColor >> 8) & 0xFF) / 255f);
        indSliderB = new LiquidGlassSlider((int)x + 230, (int)y + 160, 140, 16, (currentColor & 0xFF) / 255f);

        Runnable updateColor = () -> {
            if (isUpdating) return; isUpdating = true;
            int r = (int)(indSliderR.getValue() * 255), g = (int)(indSliderG.getValue() * 255), b = (int)(indSliderB.getValue() * 255);
            int currentVal = GlassMenuClient.CONFIG.userIndicatorColor();
            int alpha = (currentVal >> 24) & 0xFF;
            if (alpha == 0) alpha = 0xEE;
            int color = (alpha << 24) | (r << 16) | (g << 8) | b;
            GlassMenuClient.CONFIG.userIndicatorColor(color); GlassMenuClient.CONFIG.save();
            isUpdating = false;
        };

        indSliderR.setOnValueChange(v -> updateColor.run());
        indSliderG.setOnValueChange(v -> updateColor.run());
        indSliderB.setOnValueChange(v -> updateColor.run());
        visualsIndicatorWidgets.add(indSliderR); visualsIndicatorWidgets.add(indSliderG); visualsIndicatorWidgets.add(indSliderB);

        String btnText = GlassMenuClient.CONFIG.transparentUserIndicator() ? "Glass Effect: ON" : "Glass Effect: OFF";
        LiquidGlassButton transparentBtn = new LiquidGlassButton((int)x + 40, (int)y + 80, 120, 22, Text.literal(btnText), b -> {
            boolean current = GlassMenuClient.CONFIG.transparentUserIndicator();
            GlassMenuClient.CONFIG.transparentUserIndicator(!current);
            GlassMenuClient.CONFIG.save();
            b.setMessage(Text.literal(GlassMenuClient.CONFIG.transparentUserIndicator() ? "Glass Effect: ON" : "Glass Effect: OFF"));
        });
        visualsIndicatorWidgets.add(transparentBtn);
    }

    private void initVisualsArmorHudTab(float x, float y) {
        LiquidGlassButton backBtn = new LiquidGlassButton((int)x + 40, (int)y + 210, 80, 22, Text.literal("Back"), b -> {
            currentTab = Tab.VISUALS; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        visualsArmorHudWidgets.add(backBtn);

        LiquidGlassSwitch armToggle = new LiquidGlassSwitch((int)x + 330, (int)y + 45, 40, 20, GlassMenuClient.CONFIG.enableArmorHud());
        armToggle.setOnToggle(enabled -> {
            GlassMenuClient.CONFIG.enableArmorHud(enabled); GlassMenuClient.CONFIG.save();
        });
        visualsArmorHudWidgets.add(armToggle);
        
        int currentColor = GlassMenuClient.CONFIG.armorHudColor();
        armSliderR = new LiquidGlassSlider((int)x + 230, (int)y + 90, 140, 16, ((currentColor >> 16) & 0xFF) / 255f);
        armSliderG = new LiquidGlassSlider((int)x + 230, (int)y + 125, 140, 16, ((currentColor >> 8) & 0xFF) / 255f);
        armSliderB = new LiquidGlassSlider((int)x + 230, (int)y + 160, 140, 16, (currentColor & 0xFF) / 255f);

        Runnable updateColor = () -> {
            if (isUpdating) return; isUpdating = true;
            int r = (int)(armSliderR.getValue() * 255), g = (int)(armSliderG.getValue() * 255), b = (int)(armSliderB.getValue() * 255);
            int currentVal = GlassMenuClient.CONFIG.armorHudColor();
            int alpha = (currentVal >> 24) & 0xFF;
            if (alpha == 0) alpha = 0xEE;
            int color = (alpha << 24) | (r << 16) | (g << 8) | b;
            GlassMenuClient.CONFIG.armorHudColor(color); GlassMenuClient.CONFIG.save();
            isUpdating = false;
        };

        armSliderR.setOnValueChange(v -> updateColor.run());
        armSliderG.setOnValueChange(v -> updateColor.run());
        armSliderB.setOnValueChange(v -> updateColor.run());
        visualsArmorHudWidgets.add(armSliderR); visualsArmorHudWidgets.add(armSliderG); visualsArmorHudWidgets.add(armSliderB);

        String btnText = GlassMenuClient.CONFIG.transparentArmorHud() ? "Glass Effect: ON" : "Glass Effect: OFF";
        LiquidGlassButton transparentBtn = new LiquidGlassButton((int)x + 40, (int)y + 80, 120, 22, Text.literal(btnText), b -> {
            boolean current = GlassMenuClient.CONFIG.transparentArmorHud();
            GlassMenuClient.CONFIG.transparentArmorHud(!current);
            GlassMenuClient.CONFIG.save();
            b.setMessage(Text.literal(GlassMenuClient.CONFIG.transparentArmorHud() ? "Glass Effect: ON" : "Glass Effect: OFF"));
        });
        visualsArmorHudWidgets.add(transparentBtn);

        String orientText = GlassMenuClient.CONFIG.armorHudVertical() ? "Orientation: Vertical" : "Orientation: Horizontal";
        LiquidGlassButton orientationBtn = new LiquidGlassButton((int)x + 40, (int)y + 115, 150, 22, Text.literal(orientText), b -> {
            boolean current = GlassMenuClient.CONFIG.armorHudVertical();
            GlassMenuClient.CONFIG.armorHudVertical(!current);
            int w = GlassMenuClient.CONFIG.armorHudWidth();
            int h = GlassMenuClient.CONFIG.armorHudHeight();
            GlassMenuClient.CONFIG.armorHudWidth(h);
            GlassMenuClient.CONFIG.armorHudHeight(w);
            GlassMenuClient.CONFIG.save();
            b.setMessage(Text.literal(GlassMenuClient.CONFIG.armorHudVertical() ? "Orientation: Vertical" : "Orientation: Horizontal"));
        });
        visualsArmorHudWidgets.add(orientationBtn);
    }

    private void initVisualsFastItemTab(float x, float y) {
        LiquidGlassButton backBtn = new LiquidGlassButton((int)x + 40, (int)y + 210, 80, 22, Text.literal("Back"), b -> {
            currentTab = Tab.VISUALS; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        visualsFastItemWidgets.add(backBtn);

        LiquidGlassSwitch fastToggle = new LiquidGlassSwitch((int)x + 330, (int)y + 45, 40, 20, GlassMenuClient.CONFIG.enableFastItem());
        fastToggle.setOnToggle(enabled -> {
            GlassMenuClient.CONFIG.enableFastItem(enabled); GlassMenuClient.CONFIG.save();
        });
        visualsFastItemWidgets.add(fastToggle);
        
        int currentColor = GlassMenuClient.CONFIG.fastItemColor();
        fastItemSliderR = new LiquidGlassSlider((int)x + 230, (int)y + 90, 140, 16, ((currentColor >> 16) & 0xFF) / 255f);
        fastItemSliderG = new LiquidGlassSlider((int)x + 230, (int)y + 125, 140, 16, ((currentColor >> 8) & 0xFF) / 255f);
        fastItemSliderB = new LiquidGlassSlider((int)x + 230, (int)y + 160, 140, 16, (currentColor & 0xFF) / 255f);

        Runnable updateColor = () -> {
            if (isUpdating) return; isUpdating = true;
            int r = (int)(fastItemSliderR.getValue() * 255), g = (int)(fastItemSliderG.getValue() * 255), b = (int)(fastItemSliderB.getValue() * 255);
            int currentVal = GlassMenuClient.CONFIG.fastItemColor();
            int alpha = (currentVal >> 24) & 0xFF;
            if (alpha == 0) alpha = 0xEE;
            int color = (alpha << 24) | (r << 16) | (g << 8) | b;
            GlassMenuClient.CONFIG.fastItemColor(color); GlassMenuClient.CONFIG.save();
            isUpdating = false;
        };

        fastItemSliderR.setOnValueChange(v -> updateColor.run());
        fastItemSliderG.setOnValueChange(v -> updateColor.run());
        fastItemSliderB.setOnValueChange(v -> updateColor.run());
        visualsFastItemWidgets.add(fastItemSliderR); visualsFastItemWidgets.add(fastItemSliderG); visualsFastItemWidgets.add(fastItemSliderB);

        String btnText = GlassMenuClient.CONFIG.transparentFastItem() ? "Glass Effect: ON" : "Glass Effect: OFF";
        LiquidGlassButton transparentBtn = new LiquidGlassButton((int)x + 40, (int)y + 80, 120, 22, Text.literal(btnText), b -> {
            boolean current = GlassMenuClient.CONFIG.transparentFastItem();
            GlassMenuClient.CONFIG.transparentFastItem(!current);
            GlassMenuClient.CONFIG.save();
            b.setMessage(Text.literal(GlassMenuClient.CONFIG.transparentFastItem() ? "Glass Effect: ON" : "Glass Effect: OFF"));
        });
        visualsFastItemWidgets.add(transparentBtn);

        String slotsText = GlassMenuClient.CONFIG.fastItemSlots() ? "Item Slots: ON" : "Item Slots: OFF";
        LiquidGlassButton slotsBtn = new LiquidGlassButton((int)x + 40, (int)y + 115, 120, 22, Text.literal(slotsText), b -> {
            boolean current = GlassMenuClient.CONFIG.fastItemSlots();
            GlassMenuClient.CONFIG.fastItemSlots(!current);
            GlassMenuClient.CONFIG.save();
            b.setMessage(Text.literal(GlassMenuClient.CONFIG.fastItemSlots() ? "Item Slots: ON" : "Item Slots: OFF"));
        });
        visualsFastItemWidgets.add(slotsBtn);

        String hotbarText = GlassMenuClient.CONFIG.glassHotbar() ? "Glass Hotbar: ON" : "Glass Hotbar: OFF";
        LiquidGlassButton hotbarBtn = new LiquidGlassButton((int)x + 40, (int)y + 150, 120, 22, Text.literal(hotbarText), b -> {
            boolean current = GlassMenuClient.CONFIG.glassHotbar();
            GlassMenuClient.CONFIG.glassHotbar(!current);
            GlassMenuClient.CONFIG.save();
            b.setMessage(Text.literal(GlassMenuClient.CONFIG.glassHotbar() ? "Glass Hotbar: ON" : "Glass Hotbar: OFF"));
        });
        visualsFastItemWidgets.add(hotbarBtn);
    }

    private void initVisualsUserHudTab(float x, float y) {
        LiquidGlassButton backBtn = new LiquidGlassButton((int)x + 40, (int)y + 210, 80, 22, Text.literal("Back"), b -> {
            currentTab = Tab.VISUALS; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        visualsUserHudWidgets.add(backBtn);

        LiquidGlassSwitch userHudToggle = new LiquidGlassSwitch((int)x + 370, (int)y + 45, 40, 20, GlassMenuClient.CONFIG.enableUserHud());
        userHudToggle.setOnToggle(enabled -> {
            GlassMenuClient.CONFIG.enableUserHud(enabled); GlassMenuClient.CONFIG.save();
        });
        visualsUserHudWidgets.add(userHudToggle);
        
        int currentColor = GlassMenuClient.CONFIG.userHudColor();
        userHudSliderR = new LiquidGlassSlider((int)x + 230, (int)y + 90, 140, 16, ((currentColor >> 16) & 0xFF) / 255f);
        userHudSliderG = new LiquidGlassSlider((int)x + 230, (int)y + 125, 140, 16, ((currentColor >> 8) & 0xFF) / 255f);
        userHudSliderB = new LiquidGlassSlider((int)x + 230, (int)y + 160, 140, 16, (currentColor & 0xFF) / 255f);

        Runnable updateColor = () -> {
            if (isUpdating) return; isUpdating = true;
            int r = (int)(userHudSliderR.getValue() * 255), g = (int)(userHudSliderG.getValue() * 255), b = (int)(userHudSliderB.getValue() * 255);
            int currentVal = GlassMenuClient.CONFIG.userHudColor();
            int alpha = (currentVal >> 24) & 0xFF;
            if (alpha == 0) alpha = 0xEE;
            int color = (alpha << 24) | (r << 16) | (g << 8) | b;
            GlassMenuClient.CONFIG.userHudColor(color); GlassMenuClient.CONFIG.save();
            isUpdating = false;
        };

        userHudSliderR.setOnValueChange(v -> updateColor.run());
        userHudSliderG.setOnValueChange(v -> updateColor.run());
        userHudSliderB.setOnValueChange(v -> updateColor.run());
        visualsUserHudWidgets.add(userHudSliderR); visualsUserHudWidgets.add(userHudSliderG); visualsUserHudWidgets.add(userHudSliderB);

        String btnText = GlassMenuClient.CONFIG.transparentUserHud() ? "Glass Effect: ON" : "Glass Effect: OFF";
        LiquidGlassButton transparentBtn = new LiquidGlassButton((int)x + 40, (int)y + 80, 120, 22, Text.literal(btnText), b -> {
            boolean current = GlassMenuClient.CONFIG.transparentUserHud();
            GlassMenuClient.CONFIG.transparentUserHud(!current);
            GlassMenuClient.CONFIG.save();
            b.setMessage(Text.literal(GlassMenuClient.CONFIG.transparentUserHud() ? "Glass Effect: ON" : "Glass Effect: OFF"));
        });
        visualsUserHudWidgets.add(transparentBtn);
    }

    private void initVisualsEffectsTab(float x, float y) {
        LiquidGlassButton backBtn = new LiquidGlassButton((int)x + 40, (int)y + 210, 80, 22, Text.literal("Back"), b -> {
            currentTab = Tab.VISUALS; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        visualsEffectsWidgets.add(backBtn);

        LiquidGlassSwitch effectsToggle = new LiquidGlassSwitch((int)x + 370, (int)y + 45, 40, 20, GlassMenuClient.CONFIG.enableEffectsHud());
        effectsToggle.setOnToggle(enabled -> {
            GlassMenuClient.CONFIG.enableEffectsHud(enabled); GlassMenuClient.CONFIG.save();
        });
        visualsEffectsWidgets.add(effectsToggle);
        
        int currentColor = GlassMenuClient.CONFIG.effectsHudColor();
        effectsSliderR = new LiquidGlassSlider((int)x + 230, (int)y + 90, 140, 16, ((currentColor >> 16) & 0xFF) / 255f);
        effectsSliderG = new LiquidGlassSlider((int)x + 230, (int)y + 125, 140, 16, ((currentColor >> 8) & 0xFF) / 255f);
        effectsSliderB = new LiquidGlassSlider((int)x + 230, (int)y + 160, 140, 16, (currentColor & 0xFF) / 255f);

        Runnable updateColor = () -> {
            if (isUpdating) return; isUpdating = true;
            int r = (int)(effectsSliderR.getValue() * 255), g = (int)(effectsSliderG.getValue() * 255), b = (int)(effectsSliderB.getValue() * 255);
            int currentVal = GlassMenuClient.CONFIG.effectsHudColor();
            int alpha = (currentVal >> 24) & 0xFF;
            if (alpha == 0) alpha = 0xEE;
            int color = (alpha << 24) | (r << 16) | (g << 8) | b;
            GlassMenuClient.CONFIG.effectsHudColor(color); GlassMenuClient.CONFIG.save();
            isUpdating = false;
        };

        effectsSliderR.setOnValueChange(v -> updateColor.run());
        effectsSliderG.setOnValueChange(v -> updateColor.run());
        effectsSliderB.setOnValueChange(v -> updateColor.run());
        visualsEffectsWidgets.add(effectsSliderR); visualsEffectsWidgets.add(effectsSliderG); visualsEffectsWidgets.add(effectsSliderB);

        String btnText = GlassMenuClient.CONFIG.transparentEffectsHud() ? "Glass Effect: ON" : "Glass Effect: OFF";
        LiquidGlassButton transparentBtn = new LiquidGlassButton((int)x + 40, (int)y + 80, 120, 22, Text.literal(btnText), b -> {
            boolean current = GlassMenuClient.CONFIG.transparentEffectsHud();
            GlassMenuClient.CONFIG.transparentEffectsHud(!current);
            GlassMenuClient.CONFIG.save();
            b.setMessage(Text.literal(GlassMenuClient.CONFIG.transparentEffectsHud() ? "Glass Effect: ON" : "Glass Effect: OFF"));
        });
        visualsEffectsWidgets.add(transparentBtn);

        String orientText = GlassMenuClient.CONFIG.effectsHudVertical() ? "Orientation: Vertical" : "Orientation: Horizontal";
        LiquidGlassButton orientBtn = new LiquidGlassButton((int)x + 40, (int)y + 115, 150, 22, Text.literal(orientText), b -> {
            boolean current = GlassMenuClient.CONFIG.effectsHudVertical();
            GlassMenuClient.CONFIG.effectsHudVertical(!current);
            int w = GlassMenuClient.CONFIG.effectsHudWidth();
            int h = GlassMenuClient.CONFIG.effectsHudHeight();
            GlassMenuClient.CONFIG.effectsHudWidth(h);
            GlassMenuClient.CONFIG.effectsHudHeight(w);
            GlassMenuClient.CONFIG.save();
            b.setMessage(Text.literal(GlassMenuClient.CONFIG.effectsHudVertical() ? "Orientation: Vertical" : "Orientation: Horizontal"));
        });
        visualsEffectsWidgets.add(orientBtn);
    }

    private void initPositionTab() {
        LiquidGlassButton posBackBtn = new LiquidGlassButton((this.width - 100) / 2, this.height - 40, 100, 22, Text.literal("Save & Back"), b -> {
            currentTab = Tab.VISUALS; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        positionWidgets.add(posBackBtn);
    }

    private void initGeneralTab(float x, float y) {
        String[] labels = {"NONE", "RAINBOW", "PARTICLES", "RGB_PARTICLES"};
        GlassMenuConfigModel.ItemEffect[] values = GlassMenuConfigModel.ItemEffect.values();
        for (int i = 0; i < labels.length; i++) {
            final GlassMenuConfigModel.ItemEffect effect = values[i];
            LiquidGlassButton btn = new LiquidGlassButton((int)x + 30, (int)y + 60 + i * 32, 110, 22, Text.literal(labels[i]), b -> {
                GlassMenuClient.CONFIG.itemEffect(effect);
                GlassMenuClient.CONFIG.save();
            });
            generalWidgets.add(btn);
        }

        // Add Bridge button on the right
        LiquidGlassButton bridgeBtn = new LiquidGlassButton((int)x + 280, (int)y + 60, 80, 22, Text.literal("Bridge"), b -> {
            currentTab = Tab.BRIDGE; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        generalWidgets.add(bridgeBtn);

        // Add Island Glass Effect button on the right
        String islandGlassText = GlassMenuClient.CONFIG.transparentIsland() ? "Glass Effect: ON" : "Glass Effect: OFF";
        LiquidGlassButton islandGlassBtn = new LiquidGlassButton((int)x + 280, (int)y + 90, 120, 22, Text.literal(islandGlassText), b -> {
            boolean current = GlassMenuClient.CONFIG.transparentIsland();
            GlassMenuClient.CONFIG.transparentIsland(!current);
            GlassMenuClient.CONFIG.save();
            b.setMessage(Text.literal(GlassMenuClient.CONFIG.transparentIsland() ? "Glass Effect: ON" : "Glass Effect: OFF"));
        });
        generalWidgets.add(islandGlassBtn);

        // Add Menu Glass Effect button on the right
        String menuGlassText = GlassMenuClient.CONFIG.glassEffect() ? "Menu Glass: ON" : "Menu Glass: OFF";
        LiquidGlassButton menuGlassBtn = new LiquidGlassButton((int)x + 280, (int)y + 120, 120, 22, Text.literal(menuGlassText), b -> {
            boolean current = GlassMenuClient.CONFIG.glassEffect();
            GlassMenuClient.CONFIG.glassEffect(!current);
            GlassMenuClient.CONFIG.save();
            b.setMessage(Text.literal(GlassMenuClient.CONFIG.glassEffect() ? "Menu Glass: ON" : "Menu Glass: OFF"));
            if (GlassMenuClient.CONFIG.glassEffect()) {
                this.effectView.enableBlur();
            } else {
                this.effectView.disableBlur();
            }
        });
        generalWidgets.add(menuGlassBtn);
    }

    private void initCombatTab(float x, float y) {
        LiquidGlassSwitch espToggle = new LiquidGlassSwitch((int)x + 370, (int)y + 45, 40, 20, GlassMenuClient.CONFIG.enableTargetEsp());
        espToggle.setOnToggle(enabled -> {
            GlassMenuClient.CONFIG.enableTargetEsp(enabled); GlassMenuClient.CONFIG.save();
        });
        combatWidgets.add(espToggle);
        
        int currentColor = GlassMenuClient.CONFIG.targetEspColor();
        sliderR = new LiquidGlassSlider((int)x + 110, (int)y + 90, 140, 16, ((currentColor >> 16) & 0xFF) / 255f);
        sliderG = new LiquidGlassSlider((int)x + 110, (int)y + 125, 140, 16, ((currentColor >> 8) & 0xFF) / 255f);
        sliderB = new LiquidGlassSlider((int)x + 110, (int)y + 160, 140, 16, (currentColor & 0xFF) / 255f);

        Runnable updateColor = () -> {
            if (isUpdating) return; isUpdating = true;
            int r = (int)(sliderR.getValue() * 255), g = (int)(sliderG.getValue() * 255), b = (int)(sliderB.getValue() * 255);
            int color = 0xFF000000 | (r << 16) | (g << 8) | b;
            GlassMenuClient.CONFIG.targetEspColor(color); GlassMenuClient.CONFIG.save();
            if (hexInput != null) hexInput.setText(String.format("#%06X", color & 0xFFFFFF));
            if (rgbInput != null) rgbInput.setText(String.format("%d, %d, %d", r, g, b));
            isUpdating = false;
        };

        sliderR.setOnValueChange(v -> updateColor.run());
        sliderG.setOnValueChange(v -> updateColor.run());
        sliderB.setOnValueChange(v -> updateColor.run());
        combatWidgets.add(sliderR); combatWidgets.add(sliderG); combatWidgets.add(sliderB);

        hexInput = createColorTextField((int)x + 30, (int)y + 38, 60);
        hexInput.setText(String.format("#%06X", currentColor & 0xFFFFFF));
        
        // Add dynamic text listener to parse and update target ESP color using hex format
        hexInput.setChangedListener(text -> {
            if (isUpdating) return;
            try {
                String clean = text.trim();
                if (clean.startsWith("#")) clean = clean.substring(1);
                if (clean.length() == 6) {
                    int color = Integer.parseInt(clean, 16);
                    isUpdating = true;
                    int r = (color >> 16) & 0xFF;
                    int g = (color >> 8) & 0xFF;
                    int b = color & 0xFF;
                    sliderR.setValue(r / 255.0f);
                    sliderG.setValue(g / 255.0f);
                    sliderB.setValue(b / 255.0f);
                    GlassMenuClient.CONFIG.targetEspColor(0xFF000000 | color);
                    GlassMenuClient.CONFIG.save();
                    if (rgbInput != null) rgbInput.setText(String.format("%d, %d, %d", r, g, b));
                    isUpdating = false;
                }
            } catch (NumberFormatException ignored) {}
        });

        rgbInput = createColorTextField((int)x + 30, (int)y + 63, 100);
        rgbInput.setText(String.format("%d, %d, %d", (currentColor >> 16) & 0xFF, (currentColor >> 8) & 0xFF, currentColor & 0xFF));
        
        // Add dynamic text listener to parse and update target ESP color using comma-separated RGB
        rgbInput.setChangedListener(text -> {
            if (isUpdating) return;
            try {
                String[] parts = text.split(",");
                if (parts.length == 3) {
                    int r = Integer.parseInt(parts[0].trim());
                    int g = Integer.parseInt(parts[1].trim());
                    int b = Integer.parseInt(parts[2].trim());
                    if (r >= 0 && r <= 255 && g >= 0 && g <= 255 && b >= 0 && b <= 255) {
                        isUpdating = true;
                        sliderR.setValue(r / 255.0f);
                        sliderG.setValue(g / 255.0f);
                        sliderB.setValue(b / 255.0f);
                        int color = 0xFF000000 | (r << 16) | (g << 8) | b;
                        GlassMenuClient.CONFIG.targetEspColor(color);
                        GlassMenuClient.CONFIG.save();
                        if (hexInput != null) hexInput.setText(String.format("#%06X", color & 0xFFFFFF));
                        isUpdating = false;
                    }
                }
            } catch (NumberFormatException ignored) {}
        });
    }

    private void initMovementTab(float x, float y) {
        int startY = (int)y + 40; int spacing = 40; int curY = 0;
        movementWidgets.add(createLabeledSwitch((int)x + 30, startY + curY, "Enable Custom", GlassMenuClient.CONFIG.enableCustomSwing(), val -> { 
            GlassMenuClient.CONFIG.enableCustomSwing(val); GlassMenuClient.CONFIG.save(); 
        }));
        curY += spacing;

        movementWidgets.add(createLabeledSlider((int)x + 130, startY + curY, "Speed", GlassMenuClient.CONFIG.swingSpeed() / 10f, val -> { 
            GlassMenuClient.CONFIG.swingSpeed(val.floatValue() * 10f); GlassMenuClient.CONFIG.save(); 
        }));
        curY += 100;

        addMovementSliderGroup(x + 30, startY + curY, "Position", 
            (GlassMenuClient.CONFIG.handPosX() + 2f) / 4f, v -> { GlassMenuClient.CONFIG.handPosX((v.floatValue() * 4f) - 2f); GlassMenuClient.CONFIG.save(); },
            (GlassMenuClient.CONFIG.handPosY() + 2f) / 4f, v -> { GlassMenuClient.CONFIG.handPosY((v.floatValue() * 4f) - 2f); GlassMenuClient.CONFIG.save(); },
            (GlassMenuClient.CONFIG.handPosZ() + 2f) / 4f, v -> { GlassMenuClient.CONFIG.handPosZ((v.floatValue() * 4f) - 2f); GlassMenuClient.CONFIG.save(); }
        );
        curY += 100;

        addMovementSliderGroup(x + 30, startY + curY, "Scale", 
            GlassMenuClient.CONFIG.handScaleX(), v -> { GlassMenuClient.CONFIG.handScaleX(v.floatValue()); GlassMenuClient.CONFIG.save(); },
            GlassMenuClient.CONFIG.handScaleY(), v -> { GlassMenuClient.CONFIG.handScaleY(v.floatValue()); GlassMenuClient.CONFIG.save(); },
            GlassMenuClient.CONFIG.handScaleZ(), v -> { GlassMenuClient.CONFIG.handScaleZ(v.floatValue()); GlassMenuClient.CONFIG.save(); }
        );
        curY += 100;

        addMovementSliderGroup(x + 30, startY + curY, "Rotation", 
            (GlassMenuClient.CONFIG.handRotX() + 180f) / 360f, v -> { GlassMenuClient.CONFIG.handRotX((v.floatValue() * 360f) - 180f); GlassMenuClient.CONFIG.save(); },
            (GlassMenuClient.CONFIG.handRotY() + 180f) / 360f, v -> { GlassMenuClient.CONFIG.handRotY((v.floatValue() * 360f) - 180f); GlassMenuClient.CONFIG.save(); },
            (GlassMenuClient.CONFIG.handRotZ() + 180f) / 360f, v -> { GlassMenuClient.CONFIG.handRotZ((v.floatValue() * 360f) - 180f); GlassMenuClient.CONFIG.save(); }
        );
        curY += spacing + 40;
        maxScroll = Math.max(0, curY);
    }

    private void addMovementSliderGroup(float x, int y, String label, double vx, java.util.function.Consumer<Double> ax, double vy, java.util.function.Consumer<Double> ay, double vz, java.util.function.Consumer<Double> az) {
        LiquidGlassSlider sX = new LiquidGlassSlider((int)x + 100, y + 25, 40, 16, vx); sX.setOnValueChange(ax); movementWidgets.add(sX);
        LiquidGlassSlider sY = new LiquidGlassSlider((int)x + 160, y - 10, 20, 60, vy); sY.setVertical(true); sY.setOnValueChange(ay); movementWidgets.add(sY);
        LiquidGlassSlider sZ = new LiquidGlassSlider((int)x + 200, y + 25, 40, 16, vz); sZ.setOnValueChange(az); movementWidgets.add(sZ);
    }

    private TextFieldWidget createColorTextField(int x, int y, int w) {
        TextFieldWidget tf = new TextFieldWidget(textRenderer, x, y, w, 12, Text.empty());
        tf.setEditableColor(0xFFFFFFFF); tf.setDrawsBackground(false); return tf;
    }

    private ClickableWidget createLabeledSwitch(int x, int y, String label, boolean initial, java.util.function.Consumer<Boolean> action) {
        LiquidGlassSwitch s = new LiquidGlassSwitch(x + 150, y, 40, 20, initial);
        s.setOnToggle(action); return s;
    }

    private ClickableWidget createLabeledSlider(int x, int y, String label, double initial, java.util.function.Consumer<Double> action) {
        LiquidGlassSlider s = new LiquidGlassSlider(x + 100, y, 60, 16, initial);
        s.setOnValueChange(action); return s;
    }

    private void updateVisibleWidgets() {
        this.clearChildren();
        scrollY = 0;
        if (currentTab == Tab.MOVEMENT) {
            maxScroll = 380;
            for (ClickableWidget w : movementWidgets) this.addSelectableChild(w);
        } else if (currentTab == Tab.VISUALS) {
            int leftCount = 6;
            int rightCount = visualsWidgets.size() - leftCount;
            int maxRows = Math.max(leftCount, Math.max(0, rightCount));
            maxScroll = Math.max(0, (maxRows * 34) - 170);
            for (ClickableWidget w : visualsWidgets) this.addSelectableChild(w);
        } else {
            maxScroll = 0;
            List<ClickableWidget> list = switch (currentTab) {
                case GENERAL -> generalWidgets; case COMBAT -> combatWidgets;
                case VISUALS -> visualsWidgets; case VISUALS_JUMP -> visualsJumpWidgets;
                case VISUALS_INV_HUD -> visualsInvHudWidgets; case VISUALS_PLAYER_CARD -> visualsPlayerCardWidgets;
                case VISUALS_BEDWARS -> visualsBedWarsWidgets; case VISUALS_INDICATOR -> visualsIndicatorWidgets;
                case VISUALS_ARMOR_HUD -> visualsArmorHudWidgets; case VISUALS_FAST_ITEM -> visualsFastItemWidgets;
                case VISUALS_USER_HUD -> visualsUserHudWidgets;
                case VISUALS_EFFECTS -> visualsEffectsWidgets;
                case VISUALS_LEFT_HAND_ITEM -> visualsLeftHandItemWidgets;
                case VISUALS_ISLAND -> visualsIslandWidgets;
                case VISUALS_HIT   -> visualsHitWidgets;
                case VISUALS_GHOST -> visualsGhostWidgets;
                case POSITION -> positionWidgets;
                case MOVEMENT -> movementWidgets; case BRIDGE -> bridgeWidgets;
            };
            for (ClickableWidget w : list) this.addDrawableChild(w);
            if (currentTab == Tab.COMBAT) {
                this.addDrawableChild(hexInput); this.addDrawableChild(rgbInput);
                this.addSelectableChild(hexInput); this.addSelectableChild(rgbInput);
            } else if (currentTab == Tab.VISUALS_JUMP) {
                this.addDrawableChild(visHexInput); this.addDrawableChild(visRgbInput);
                this.addSelectableChild(visHexInput); this.addSelectableChild(visRgbInput);
            } else if (currentTab == Tab.BRIDGE) {
                this.addDrawableChild(bridgeHexInput); this.addDrawableChild(bridgeRgbInput);
                this.addSelectableChild(bridgeHexInput); this.addSelectableChild(bridgeRgbInput);
            } else if (currentTab == Tab.VISUALS_HIT) {
                this.addDrawableChild(hitHexInput); this.addDrawableChild(hitRgbInput);
                this.addSelectableChild(hitHexInput); this.addSelectableChild(hitRgbInput);
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        float dt = (now - lastAnimTime) / 1000f;
        lastAnimTime = now;
        if (dt < 0f) dt = 0f;
        dt = Math.min(dt, 0.1f);
        
        float targetW = (currentTab == Tab.MOVEMENT || currentTab == Tab.GENERAL || currentTab == Tab.VISUALS || currentTab == Tab.VISUALS_JUMP || currentTab == Tab.VISUALS_INV_HUD || currentTab == Tab.VISUALS_PLAYER_CARD || currentTab == Tab.VISUALS_BEDWARS || currentTab == Tab.VISUALS_INDICATOR || currentTab == Tab.VISUALS_ARMOR_HUD || currentTab == Tab.VISUALS_FAST_ITEM || currentTab == Tab.VISUALS_USER_HUD || currentTab == Tab.VISUALS_EFFECTS || currentTab == Tab.VISUALS_LEFT_HAND_ITEM || currentTab == Tab.VISUALS_ISLAND || currentTab == Tab.VISUALS_HIT || currentTab == Tab.VISUALS_GHOST || currentTab == Tab.BRIDGE) ? 1.0f : 0.0f;
        float targetH = (currentTab == Tab.MOVEMENT || currentTab == Tab.GENERAL || currentTab == Tab.VISUALS || currentTab == Tab.VISUALS_JUMP || currentTab == Tab.VISUALS_INV_HUD || currentTab == Tab.VISUALS_PLAYER_CARD || currentTab == Tab.VISUALS_BEDWARS || currentTab == Tab.VISUALS_INDICATOR || currentTab == Tab.VISUALS_ARMOR_HUD || currentTab == Tab.VISUALS_FAST_ITEM || currentTab == Tab.VISUALS_USER_HUD || currentTab == Tab.VISUALS_EFFECTS || currentTab == Tab.VISUALS_LEFT_HAND_ITEM || currentTab == Tab.VISUALS_ISLAND || currentTab == Tab.VISUALS_HIT || currentTab == Tab.VISUALS_GHOST || currentTab == Tab.BRIDGE) ? 1.0f : 0.0f;
        float targetXOff = (currentTab == Tab.COMBAT) ? 40f : 0f;

        panelWidthProgress = MathHelper.lerp(MathHelper.clamp(dt * 10.0f, 0f, 1f), panelWidthProgress, targetW);
        panelWidthProgress = MathHelper.clamp(panelWidthProgress, 0f, 1f);
        panelHeightProgress = MathHelper.lerp(MathHelper.clamp(dt * 10.0f, 0f, 1f), panelHeightProgress, targetH);
        panelHeightProgress = MathHelper.clamp(panelHeightProgress, 0f, 1f);
        panelXOffsetProgress = MathHelper.lerp(MathHelper.clamp(dt * 10.0f, 0f, 1f), panelXOffsetProgress, targetXOff);
        contentAlpha = MathHelper.lerp(MathHelper.clamp(dt * 12.0f, 0f, 1f), contentAlpha, 1.0f);
        contentAlpha = MathHelper.clamp(contentAlpha, 0f, 1f);

        int currentW = Math.round(MathHelper.lerp(panelWidthProgress, PANEL_W_NORMAL, PANEL_W_EXPANDED));
        int currentH = Math.round(MathHelper.lerp(panelHeightProgress, PANEL_H_NORMAL, PANEL_H_EXPANDED));
        int x = (this.width - currentW) / 2 + Math.round(panelXOffsetProgress); 
        int y = (this.height - currentH) / 2;

        this.effectView.render(context, this.width, this.height);
        
        if (currentTab != Tab.POSITION) {
            this.lensView.setX(x); this.lensView.setY(y); this.lensView.setWidth(currentW); this.lensView.setHeight(currentH);
            this.lensView.render(context);
            
            Tab[] topTabs = { Tab.GENERAL, Tab.MOVEMENT, Tab.COMBAT, Tab.VISUALS };
            float tabW = (float)currentW / topTabs.length;
            for (int i = 0; i < topTabs.length; i++) {
                boolean isHovered = mouseX >= x + tabW * i && mouseX <= x + tabW * (i + 1) && mouseY >= y && mouseY <= y + 30;
                tabHoverProgress[i] = MathHelper.lerp(MathHelper.clamp(dt * 8.0f, 0f, 1f), tabHoverProgress[i], isHovered ? 1.0f : 0.0f);
                tabHoverProgress[i] = MathHelper.clamp(tabHoverProgress[i], 0f, 1f);
                
                boolean isActive = (topTabs[i] == currentTab) 
                    || (topTabs[i] == Tab.GENERAL && currentTab == Tab.BRIDGE)
                    || (topTabs[i] == Tab.VISUALS && (currentTab == Tab.VISUALS_JUMP || currentTab == Tab.VISUALS_INV_HUD || currentTab == Tab.VISUALS_PLAYER_CARD || currentTab == Tab.VISUALS_BEDWARS || currentTab == Tab.VISUALS_INDICATOR || currentTab == Tab.VISUALS_ARMOR_HUD || currentTab == Tab.VISUALS_FAST_ITEM || currentTab == Tab.VISUALS_USER_HUD || currentTab == Tab.VISUALS_EFFECTS || currentTab == Tab.VISUALS_LEFT_HAND_ITEM || currentTab == Tab.VISUALS_ISLAND || currentTab == Tab.VISUALS_HIT || currentTab == Tab.VISUALS_GHOST || currentTab == Tab.POSITION));
                
                float swell = 1.0f + tabHoverProgress[i] * 0.04f; // Subtle scale swell on hover

                int fillColor, borderColor, textColor;
                if (GlassMenuClient.CONFIG.glassEffect()) {
                    int baseFill = isActive ? 0x3DFFFFFF : (isHovered ? 0x26FFFFFF : 0x10FFFFFF);
                    int hoverFill = isActive ? 0x4DFFFFFF : 0x2EFFFFFF;
                    fillColor = interpolateColor(baseFill, hoverFill, tabHoverProgress[i]);

                    int baseBorder = isActive ? 0x55FFFFFF : (isHovered ? 0x33FFFFFF : 0x1AFFFFFF);
                    int hoverBorder = isActive ? 0x66FFFFFF : 0x40FFFFFF;
                    borderColor = interpolateColor(baseBorder, hoverBorder, tabHoverProgress[i]);
                    
                    textColor = ((int)(255 * contentAlpha) << 24) | 0xFFFFFF;
                } else {
                    int baseFill = isActive ? 0xFFFFFFFF : (isHovered ? 0xFFF2F2F7 : 0xFFE5E5EA);
                    int hoverFill = 0xFFFFFFFF;
                    fillColor = interpolateColor(baseFill, hoverFill, tabHoverProgress[i]);

                    int baseBorder = isActive ? 0xFFC7C7CC : (isHovered ? 0xFFD1D1D6 : 0xFFE5E5EA);
                    int hoverBorder = 0xFFC7C7CC;
                    borderColor = interpolateColor(baseBorder, hoverBorder, tabHoverProgress[i]);
                    
                    int textRGB = isActive ? 0xFF1C1C1E : 0xFF8E8E93;
                    textColor = ((int)(255 * contentAlpha) << 24) | (textRGB & 0xFFFFFF);
                }

                // Apply alpha to colors
                int fillAlpha = Math.round(((fillColor >> 24) & 0xFF) * contentAlpha);
                int finalFillColor = (fillAlpha << 24) | (fillColor & 0x00FFFFFF);

                int borderAlpha = Math.round(((borderColor >> 24) & 0xFF) * contentAlpha);
                int finalBorderColor = (borderAlpha << 24) | (borderColor & 0x00FFFFFF);

                float boxW = tabW - 20f;
                float boxH = 16f;
                float boxX = x + tabW * i + 10f;
                float boxY = y + 7f;

                context.getMatrices().push();
                context.getMatrices().translate(boxX + boxW / 2f, boxY + boxH / 2f, 0);
                context.getMatrices().scale(swell, swell, 1.0f);
                context.getMatrices().translate(-(boxX + boxW / 2f), -(boxY + boxH / 2f), 0);

                // Draw background and outline
                RenderUtils.drawSdfRoundedOutline(context.getMatrices(), boxX, boxY, boxW, boxH, 6f, 0.6f, finalBorderColor);
                RenderUtils.drawSdfRoundedRect(context.getMatrices(), boxX, boxY, boxW, boxH, 6f, finalFillColor, 0);
                context.draw(); // Flush to render SDF backgrounds

                // Draw centered text
                if (GlassMenuClient.CONFIG.glassEffect()) {
                    context.drawCenteredTextWithShadow(textRenderer, topTabs[i].name(), (int)(boxX + boxW / 2f), (int)(boxY + (boxH - 8) / 2f), textColor);
                } else {
                    context.drawText(textRenderer, topTabs[i].name(), (int)(boxX + (boxW - textRenderer.getWidth(topTabs[i].name())) / 2f), (int)(boxY + (boxH - 8) / 2f), textColor, false);
                }

                context.getMatrices().pop();
            }
        }

        if (currentTab == Tab.GENERAL || currentTab == Tab.BRIDGE) renderGeneralTab(context, x, y, mouseX, mouseY, delta);
        else if (currentTab == Tab.COMBAT) renderCombatTab(context, x, y, mouseX, mouseY, delta);
        else if (currentTab == Tab.MOVEMENT) renderMovementTab(context, x, y, mouseX, mouseY, delta, currentW, currentH);
        else if (currentTab == Tab.VISUALS) renderVisualsTab(context, x, y, mouseX, mouseY, delta, currentW, currentH);
        else if (currentTab == Tab.VISUALS_JUMP) renderVisualsJumpTab(context, x, y, mouseX, mouseY, delta);
        else if (currentTab == Tab.VISUALS_INV_HUD) renderVisualsInvHudTab(context, x, y, mouseX, mouseY, delta);
        else if (currentTab == Tab.VISUALS_PLAYER_CARD) renderVisualsPlayerCardTab(context, x, y, mouseX, mouseY, delta);
        else if (currentTab == Tab.VISUALS_BEDWARS) renderVisualsBedWarsTab(context, x, y, mouseX, mouseY, delta);
        else if (currentTab == Tab.VISUALS_INDICATOR) renderVisualsIndicatorTab(context, x, y, mouseX, mouseY, delta);
        else if (currentTab == Tab.VISUALS_ARMOR_HUD) renderVisualsArmorHudTab(context, x, y, mouseX, mouseY, delta);
        else if (currentTab == Tab.VISUALS_FAST_ITEM) renderVisualsFastItemTab(context, x, y, mouseX, mouseY, delta);
        else if (currentTab == Tab.VISUALS_USER_HUD) renderVisualsUserHudTab(context, x, y, mouseX, mouseY, delta);
        else if (currentTab == Tab.VISUALS_EFFECTS) renderVisualsEffectsTab(context, x, y, mouseX, mouseY, delta);
        else if (currentTab == Tab.VISUALS_LEFT_HAND_ITEM) renderVisualsLeftHandItemTab(context, x, y, mouseX, mouseY, delta);
        else if (currentTab == Tab.VISUALS_ISLAND) renderVisualsIslandTab(context, x, y, mouseX, mouseY, delta);
        else if (currentTab == Tab.VISUALS_HIT)   renderVisualsHitTab(context, x, y, mouseX, mouseY, delta);
        else if (currentTab == Tab.VISUALS_GHOST)  renderVisualsGhostTab(context, x, y, mouseX, mouseY, delta);
        else if (currentTab == Tab.POSITION) renderPositionTab(context, mouseX, mouseY, delta);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderVisualsTab(DrawContext context, int x, int y, int mouseX, int mouseY, float delta, int currentW, int currentH) {
        int alphaInt = (int)(255 * contentAlpha); int colorAlpha = alphaInt << 24;
        float slideOffset = (1.0f - contentAlpha) * 12f;

        context.drawTextWithShadow(textRenderer, "Visuals Settings", x + 30, y + 25 - (int)slideOffset, colorAlpha | 0xFFFFFF);

        double sc = MinecraftClient.getInstance().getWindow().getScaleFactor();
        // Clip exactly between the top separator line (y + 25) and bottom separator line (y + currentH - 25)
        int scissorY = (int)((this.height - y - currentH + 25) * sc);
        int scissorH = (int)((currentH - 50) * sc);
        RenderSystem.enableScissor((int)(x * sc), Math.max(0, scissorY), (int)(currentW * sc), Math.max(0, scissorH));

        int colW = 160;
        int colGap = 30;
        int totalW = colW * 2 + colGap;
        int startX = x + (currentW - totalW) / 2;
        int startY = y + 70 - (int)scrollY - (int)slideOffset;

        // Draw Column Headers
        context.drawTextWithShadow(textRenderer, "HUD & OVERLAYS", startX + 5, y + 54 - (int)scrollY - (int)slideOffset, colorAlpha | 0xAAAAAA);
        context.drawTextWithShadow(textRenderer, "WORLD & RENDER", startX + colW + colGap + 5, y + 54 - (int)scrollY - (int)slideOffset, colorAlpha | 0xAAAAAA);

        for (int i = 0; i < visualsWidgets.size(); i++) {
            ClickableWidget w = visualsWidgets.get(i);
            w.setAlpha(contentAlpha);

            int col = (i < 6) ? 0 : 1;
            int row = (i < 6) ? i : (i - 6);

            int bx = startX + col * (colW + colGap);
            int by = startY + row * 34;

            w.setX(bx);
            w.setY(by);

            boolean visible = w.getY() + w.getHeight() > y + 25 && w.getY() < y + currentH - 25;
            w.visible = visible;
            w.active = visible;

            if (visible) {
                float swell = (w.isHovered()) ? 1.05f : 1.0f;
                context.getMatrices().push();
                context.getMatrices().translate(w.getX() + w.getWidth() / 2f, w.getY() + w.getHeight() / 2f, 0);
                context.getMatrices().scale(swell, swell, 1.0f);
                context.getMatrices().translate(-(w.getX() + w.getWidth() / 2f), -(w.getY() + w.getHeight() / 2f), 0);
                w.render(context, mouseX, mouseY, delta);
                context.getMatrices().pop();
            }
        }
        RenderSystem.disableScissor();
    }

    private void renderVisualsBedWarsTab(DrawContext context, int x, int y, int mouseX, int mouseY, float delta) {
        int alphaInt = (int)(255 * contentAlpha); int colorAlpha = alphaInt << 24;
        float slideOffset = (1.0f - contentAlpha) * 12f;
        context.drawTextWithShadow(textRenderer, "BedWars ESP Settings", x + 30, y + 45 - (int)slideOffset, colorAlpha | 0xFFFFFF);
        
        context.drawTextWithShadow(textRenderer, "Enable ESP (Hitbox/Alert)", x + 40, y + 80 - (int)slideOffset, colorAlpha | 0xAAAAAA);
        context.drawTextWithShadow(textRenderer, "Show Teammate Hearts", x + 40, y + 120 - (int)slideOffset, colorAlpha | 0xAAAAAA);
        context.drawTextWithShadow(textRenderer, "Enable Nametags through walls", x + 40, y + 160 - (int)slideOffset, colorAlpha | 0xAAAAAA);

        for (ClickableWidget w : visualsBedWarsWidgets) {
            w.setAlpha(contentAlpha);
            if (w == visualsBedWarsWidgets.get(0)) { // Back Button
                w.setX(x + 40); w.setY((int)y + 223 - (int)slideOffset);
            } else if (w == visualsBedWarsWidgets.get(1)) { // ESP Switch
                w.setX(x + 330); w.setY((int)y + 75 - (int)slideOffset);
            } else if (w == visualsBedWarsWidgets.get(2)) { // Hearts Switch
                w.setX(x + 330); w.setY((int)y + 115 - (int)slideOffset);
            } else if (w == visualsBedWarsWidgets.get(3)) { // Names Switch
                w.setX(x + 330); w.setY((int)y + 155 - (int)slideOffset);
            } else if (w == visualsBedWarsWidgets.get(4)) { // Mode Button
                w.setX(x + 40); w.setY((int)y + 195 - (int)slideOffset);
            }
        }
    }

    private void renderVisualsJumpTab(DrawContext context, int x, int y, int mouseX, int mouseY, float delta) {
        int alphaInt = (int)(255 * contentAlpha); int colorAlpha = alphaInt << 24;
        float slideOffset = (1.0f - contentAlpha) * 12f;
        context.drawTextWithShadow(textRenderer, "Jump Pulse Rings", x + 230, y + 50 - (int)slideOffset, colorAlpha | 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, "Mode", x + 40, y + 110 - (int)slideOffset, colorAlpha | 0xAAAAAA);

        int btnIdx = 0;
        for (ClickableWidget w : visualsJumpWidgets) {
            w.setAlpha(contentAlpha);
            if (w == visSliderR) { w.setX(x + 230); w.setY((int)y + 90 - (int)slideOffset); }
            else if (w == visSliderG) { w.setX(x + 230); w.setY((int)y + 125 - (int)slideOffset); }
            else if (w == visSliderB) { w.setX(x + 230); w.setY((int)y + 160 - (int)slideOffset); }
            else if (w instanceof LiquidGlassButton lgb && lgb.getMessage().getString().equals("Back")) {
                w.setX(x + 40); w.setY((int)y + 210 - (int)slideOffset);
            } else if (w instanceof LiquidGlassButton) {
                w.setX(x + 40); w.setY((int)y + 130 + (btnIdx++) * 28 - (int)slideOffset);
            } else {
                w.setX(x + 330); w.setY((int)y + 45 - (int)slideOffset);
            }
        }

        if (visHexInput != null) { 
            visHexInput.setAlpha(contentAlpha); 
            visHexInput.setX(x + 40);
            visHexInput.setY((int)y + 38 - (int)slideOffset); 
        }
        if (visRgbInput != null) { 
            visRgbInput.setAlpha(contentAlpha); 
            visRgbInput.setX(x + 40);
            visRgbInput.setY((int)y + 63 - (int)slideOffset); 
        }

        drawInputBox(context, x + 35, y + 35 - (int)slideOffset, 70, 18); drawInputBox(context, x + 35, y + 60 - (int)slideOffset, 110, 18);
        context.drawTextWithShadow(textRenderer, "Red", x + 230, y + 80 - (int)slideOffset, colorAlpha | 0xAAAAAA);
        context.drawTextWithShadow(textRenderer, "Green", x + 230, y + 115 - (int)slideOffset, colorAlpha | 0xAAAAAA);
        context.drawTextWithShadow(textRenderer, "Blue", x + 230, y + 150 - (int)slideOffset, colorAlpha | 0xAAAAAA);
    }

    private void renderVisualsInvHudTab(DrawContext context, int x, int y, int mouseX, int mouseY, float delta) {
        int alphaInt = (int)(255 * contentAlpha); int colorAlpha = alphaInt << 24;
        float slideOffset = (1.0f - contentAlpha) * 12f;
        boolean isTransparent = GlassMenuClient.CONFIG.transparentBackground();

        context.drawTextWithShadow(textRenderer, "Inventory HUD", x + 230, y + 50 - (int)slideOffset, colorAlpha | 0xFFFFFF);

        for (ClickableWidget w : visualsInvHudWidgets) {
            w.setAlpha(contentAlpha);
            if (w == invSliderR || w == invSliderG || w == invSliderB) {
                if (isTransparent) { w.setX(-9999); w.setY(-9999); }
                else if (w == invSliderR) { w.setX(x + 230); w.setY((int)y + 90 - (int)slideOffset); }
                else if (w == invSliderG) { w.setX(x + 230); w.setY((int)y + 125 - (int)slideOffset); }
                else if (w == invSliderB) { w.setX(x + 230); w.setY((int)y + 160 - (int)slideOffset); }
            } else if (w instanceof LiquidGlassButton lgb && lgb.getMessage().getString().equals("Back")) {
                w.setX(x + 40); w.setY((int)y + 210 - (int)slideOffset);
            } else if (w instanceof LiquidGlassButton lgb && lgb.getMessage().getString().startsWith("Glass Effect")) {
                w.setX(x + 40); w.setY((int)y + 50 - (int)slideOffset);
            } else {
                w.setX(x + 370); w.setY((int)y + 45 - (int)slideOffset);
            }
        }

        if (!isTransparent) {
            context.drawTextWithShadow(textRenderer, "Red",   x + 230, y + 80  - (int)slideOffset, colorAlpha | 0xAAAAAA);
            context.drawTextWithShadow(textRenderer, "Green", x + 230, y + 115 - (int)slideOffset, colorAlpha | 0xAAAAAA);
            context.drawTextWithShadow(textRenderer, "Blue",  x + 230, y + 150 - (int)slideOffset, colorAlpha | 0xAAAAAA);
        }
    }

    private void renderVisualsPlayerCardTab(DrawContext context, int x, int y, int mouseX, int mouseY, float delta) {
        int alphaInt = (int)(255 * contentAlpha); int colorAlpha = alphaInt << 24;
        float slideOffset = (1.0f - contentAlpha) * 12f;
        boolean isTransparent = GlassMenuClient.CONFIG.transparentPlayerCard();
        context.drawTextWithShadow(textRenderer, "Player Card", x + 230, y + 50 - (int)slideOffset, colorAlpha | 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, "Enable Card", x + 40, y + 50 - (int)slideOffset, colorAlpha | 0xAAAAAA);

        for (ClickableWidget w : visualsPlayerCardWidgets) {
            w.setAlpha(contentAlpha);
            if (w == cardSliderR || w == cardSliderG || w == cardSliderB) {
                if (isTransparent) { w.setX(-9999); w.setY(-9999); }
                else if (w == cardSliderR) { w.setX(x + 230); w.setY((int)y + 90 - (int)slideOffset); }
                else if (w == cardSliderG) { w.setX(x + 230); w.setY((int)y + 125 - (int)slideOffset); }
                else if (w == cardSliderB) { w.setX(x + 230); w.setY((int)y + 160 - (int)slideOffset); }
            } else if (w instanceof LiquidGlassButton lgb && lgb.getMessage().getString().equals("Back")) {
                w.setX(x + 40); w.setY((int)y + 210 - (int)slideOffset);
            } else if (w instanceof LiquidGlassButton lgb && lgb.getMessage().getString().startsWith("Glass Effect")) {
                w.setX(x + 40); w.setY((int)y + 80 - (int)slideOffset);
            } else {
                w.setX(x + 370); w.setY((int)y + 45 - (int)slideOffset);
            }
        }

        if (!isTransparent) {
            context.drawTextWithShadow(textRenderer, "Red",   x + 230, y + 80  - (int)slideOffset, colorAlpha | 0xAAAAAA);
            context.drawTextWithShadow(textRenderer, "Green", x + 230, y + 115 - (int)slideOffset, colorAlpha | 0xAAAAAA);
            context.drawTextWithShadow(textRenderer, "Blue",  x + 230, y + 150 - (int)slideOffset, colorAlpha | 0xAAAAAA);
        }
    }

    private void renderVisualsIndicatorTab(DrawContext context, int x, int y, int mouseX, int mouseY, float delta) {
        int alphaInt = (int)(255 * contentAlpha); int colorAlpha = alphaInt << 24;
        float slideOffset = (1.0f - contentAlpha) * 12f;
        boolean isTransparent = GlassMenuClient.CONFIG.transparentUserIndicator();
        context.drawTextWithShadow(textRenderer, "User Indicator", x + 230, y + 50 - (int)slideOffset, colorAlpha | 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, "Enable Indicator", x + 40, y + 50 - (int)slideOffset, colorAlpha | 0xAAAAAA);

        for (ClickableWidget w : visualsIndicatorWidgets) {
            w.setAlpha(contentAlpha);
            if (w == indSliderR || w == indSliderG || w == indSliderB) {
                if (isTransparent) { w.setX(-9999); w.setY(-9999); }
                else if (w == indSliderR) { w.setX(x + 230); w.setY((int)y + 90 - (int)slideOffset); }
                else if (w == indSliderG) { w.setX(x + 230); w.setY((int)y + 125 - (int)slideOffset); }
                else if (w == indSliderB) { w.setX(x + 230); w.setY((int)y + 160 - (int)slideOffset); }
            } else if (w instanceof LiquidGlassButton lgb && lgb.getMessage().getString().equals("Back")) {
                w.setX(x + 40); w.setY((int)y + 210 - (int)slideOffset);
            } else if (w instanceof LiquidGlassButton lgb && lgb.getMessage().getString().startsWith("Glass Effect")) {
                w.setX(x + 40); w.setY((int)y + 80 - (int)slideOffset);
            } else {
                w.setX(x + 370); w.setY((int)y + 45 - (int)slideOffset);
            }
        }

        if (!isTransparent) {
            context.drawTextWithShadow(textRenderer, "Red",   x + 230, y + 80  - (int)slideOffset, colorAlpha | 0xAAAAAA);
            context.drawTextWithShadow(textRenderer, "Green", x + 230, y + 115 - (int)slideOffset, colorAlpha | 0xAAAAAA);
            context.drawTextWithShadow(textRenderer, "Blue",  x + 230, y + 150 - (int)slideOffset, colorAlpha | 0xAAAAAA);
        }
    }

    private void renderVisualsArmorHudTab(DrawContext context, int x, int y, int mouseX, int mouseY, float delta) {
        int alphaInt = (int)(255 * contentAlpha); int colorAlpha = alphaInt << 24;
        float slideOffset = (1.0f - contentAlpha) * 12f;
        boolean isTransparent = GlassMenuClient.CONFIG.transparentArmorHud();
        context.drawTextWithShadow(textRenderer, "Armor HUD", x + 230, y + 50 - (int)slideOffset, colorAlpha | 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, "Enable Armor HUD", x + 40, y + 50 - (int)slideOffset, colorAlpha | 0xAAAAAA);

        for (ClickableWidget w : visualsArmorHudWidgets) {
            w.setAlpha(contentAlpha);
            if (w == armSliderR || w == armSliderG || w == armSliderB) {
                if (isTransparent) { w.setX(-9999); w.setY(-9999); }
                else if (w == armSliderR) { w.setX(x + 230); w.setY((int)y + 90 - (int)slideOffset); }
                else if (w == armSliderG) { w.setX(x + 230); w.setY((int)y + 125 - (int)slideOffset); }
                else if (w == armSliderB) { w.setX(x + 230); w.setY((int)y + 160 - (int)slideOffset); }
            } else if (w instanceof LiquidGlassButton lgb && lgb.getMessage().getString().equals("Back")) {
                w.setX(x + 40); w.setY((int)y + 210 - (int)slideOffset);
            } else if (w instanceof LiquidGlassButton lgb && lgb.getMessage().getString().startsWith("Glass Effect")) {
                w.setX(x + 40); w.setY((int)y + 80 - (int)slideOffset);
            } else if (w instanceof LiquidGlassButton lgb && lgb.getMessage().getString().startsWith("Orientation")) {
                w.setX(x + 40); w.setY((int)y + 115 - (int)slideOffset);
            } else {
                w.setX(x + 370); w.setY((int)y + 45 - (int)slideOffset);
            }
        }

        if (!isTransparent) {
            context.drawTextWithShadow(textRenderer, "Red",   x + 230, y + 80  - (int)slideOffset, colorAlpha | 0xAAAAAA);
            context.drawTextWithShadow(textRenderer, "Green", x + 230, y + 115 - (int)slideOffset, colorAlpha | 0xAAAAAA);
            context.drawTextWithShadow(textRenderer, "Blue",  x + 230, y + 150 - (int)slideOffset, colorAlpha | 0xAAAAAA);
        }
    }

    private void renderVisualsFastItemTab(DrawContext context, int x, int y, int mouseX, int mouseY, float delta) {
        int alphaInt = (int)(255 * contentAlpha); int colorAlpha = alphaInt << 24;
        float slideOffset = (1.0f - contentAlpha) * 12f;
        boolean isTransparent = GlassMenuClient.CONFIG.transparentFastItem();

        // Left column: labels + buttons
        context.drawTextWithShadow(textRenderer, "Circle Hotbar", x + 40, y + 50 - (int)slideOffset, colorAlpha | 0xAAAAAA);

        // Right column title (short enough to not overlap switch at x+370)
        context.drawTextWithShadow(textRenderer, "Item HUD", x + 230, y + 50 - (int)slideOffset, colorAlpha | 0xFFFFFF);

        for (ClickableWidget w : visualsFastItemWidgets) {
            w.setAlpha(contentAlpha);
            if (w == fastItemSliderR || w == fastItemSliderG || w == fastItemSliderB) {
                if (isTransparent) { w.setX(-9999); w.setY(-9999); }
                else if (w == fastItemSliderR) { w.setX(x + 230); w.setY((int)y + 85 - (int)slideOffset); }
                else if (w == fastItemSliderG) { w.setX(x + 230); w.setY((int)y + 118 - (int)slideOffset); }
                else if (w == fastItemSliderB) { w.setX(x + 230); w.setY((int)y + 151 - (int)slideOffset); }
            } else if (w instanceof LiquidGlassButton lgb && lgb.getMessage().getString().equals("Back")) {
                w.setX(x + 40); w.setY((int)y + 205 - (int)slideOffset);
            } else if (w instanceof LiquidGlassButton lgb && lgb.getMessage().getString().startsWith("Glass Effect")) {
                w.setX(x + 40); w.setY((int)y + 80 - (int)slideOffset);
            } else if (w instanceof LiquidGlassButton lgb && lgb.getMessage().getString().startsWith("Item Slots")) {
                w.setX(x + 40); w.setY((int)y + 115 - (int)slideOffset);
            } else if (w instanceof LiquidGlassButton lgb && lgb.getMessage().getString().startsWith("Glass Hotbar")) {
                w.setX(x + 40); w.setY((int)y + 150 - (int)slideOffset);
            } else {
                // Enable toggle switch — at x+370 to avoid overlap with title
                w.setX(x + 370); w.setY((int)y + 45 - (int)slideOffset);
            }
        }

        if (!isTransparent) {
            context.drawTextWithShadow(textRenderer, "Red",   x + 230, y + 73  - (int)slideOffset, colorAlpha | 0xAAAAAA);
            context.drawTextWithShadow(textRenderer, "Green", x + 230, y + 106 - (int)slideOffset, colorAlpha | 0xAAAAAA);
            context.drawTextWithShadow(textRenderer, "Blue",  x + 230, y + 139 - (int)slideOffset, colorAlpha | 0xAAAAAA);
        }
    }

    private void renderVisualsUserHudTab(DrawContext context, int x, int y, int mouseX, int mouseY, float delta) {
        int alphaInt = (int)(255 * contentAlpha); int colorAlpha = alphaInt << 24;
        float slideOffset = (1.0f - contentAlpha) * 12f;
        boolean isTransparent = GlassMenuClient.CONFIG.transparentUserHud();

        // Left column: labels + buttons
        context.drawTextWithShadow(textRenderer, "Enable User HUD", x + 40, y + 50 - (int)slideOffset, colorAlpha | 0xAAAAAA);

        // Right column title
        context.drawTextWithShadow(textRenderer, "User HUD Color", x + 230, y + 50 - (int)slideOffset, colorAlpha | 0xFFFFFF);

        for (ClickableWidget w : visualsUserHudWidgets) {
            w.setAlpha(contentAlpha);
            if (w == userHudSliderR || w == userHudSliderG || w == userHudSliderB) {
                if (isTransparent) { w.setX(-9999); w.setY(-9999); }
                else if (w == userHudSliderR) { w.setX(x + 230); w.setY((int)y + 85 - (int)slideOffset); }
                else if (w == userHudSliderG) { w.setX(x + 230); w.setY((int)y + 118 - (int)slideOffset); }
                else if (w == userHudSliderB) { w.setX(x + 230); w.setY((int)y + 151 - (int)slideOffset); }
            } else if (w instanceof LiquidGlassButton lgb && lgb.getMessage().getString().equals("Back")) {
                w.setX(x + 40); w.setY((int)y + 205 - (int)slideOffset);
            } else if (w instanceof LiquidGlassButton lgb && lgb.getMessage().getString().startsWith("Glass Effect")) {
                w.setX(x + 40); w.setY((int)y + 80 - (int)slideOffset);
            } else {
                // Enable toggle switch at x+370
                w.setX(x + 370); w.setY((int)y + 45 - (int)slideOffset);
            }
        }

        if (!isTransparent) {
            context.drawTextWithShadow(textRenderer, "Red",   x + 230, y + 73  - (int)slideOffset, colorAlpha | 0xAAAAAA);
            context.drawTextWithShadow(textRenderer, "Green", x + 230, y + 106 - (int)slideOffset, colorAlpha | 0xAAAAAA);
            context.drawTextWithShadow(textRenderer, "Blue",  x + 230, y + 139 - (int)slideOffset, colorAlpha | 0xAAAAAA);
        }
    }

    private void renderVisualsEffectsTab(DrawContext context, int x, int y, int mouseX, int mouseY, float delta) {
        int alphaInt = (int)(255 * contentAlpha); int colorAlpha = alphaInt << 24;
        float slideOffset = (1.0f - contentAlpha) * 12f;
        boolean isTransparent = GlassMenuClient.CONFIG.transparentEffectsHud();

        // Left column: labels + buttons
        context.drawTextWithShadow(textRenderer, "Enable Effects HUD", x + 40, y + 50 - (int)slideOffset, colorAlpha | 0xAAAAAA);

        // Right column title
        context.drawTextWithShadow(textRenderer, "Effects HUD Color", x + 230, y + 50 - (int)slideOffset, colorAlpha | 0xFFFFFF);

        for (ClickableWidget w : visualsEffectsWidgets) {
            w.setAlpha(contentAlpha);
            if (w == effectsSliderR || w == effectsSliderG || w == effectsSliderB) {
                if (isTransparent) { w.setX(-9999); w.setY(-9999); }
                else if (w == effectsSliderR) { w.setX(x + 230); w.setY((int)y + 85 - (int)slideOffset); }
                else if (w == effectsSliderG) { w.setX(x + 230); w.setY((int)y + 118 - (int)slideOffset); }
                else if (w == effectsSliderB) { w.setX(x + 230); w.setY((int)y + 151 - (int)slideOffset); }
            } else if (w instanceof LiquidGlassButton lgb && lgb.getMessage().getString().equals("Back")) {
                w.setX(x + 40); w.setY((int)y + 205 - (int)slideOffset);
            } else if (w instanceof LiquidGlassButton lgb && lgb.getMessage().getString().startsWith("Glass Effect")) {
                w.setX(x + 40); w.setY((int)y + 80 - (int)slideOffset);
            } else if (w instanceof LiquidGlassButton lgb && lgb.getMessage().getString().startsWith("Orientation")) {
                w.setX(x + 40); w.setY((int)y + 115 - (int)slideOffset);
            } else {
                // Enable toggle switch at x+370
                w.setX(x + 370); w.setY((int)y + 45 - (int)slideOffset);
            }
        }

        if (!isTransparent) {
            context.drawTextWithShadow(textRenderer, "Red",   x + 230, y + 73  - (int)slideOffset, colorAlpha | 0xAAAAAA);
            context.drawTextWithShadow(textRenderer, "Green", x + 230, y + 106 - (int)slideOffset, colorAlpha | 0xAAAAAA);
            context.drawTextWithShadow(textRenderer, "Blue",  x + 230, y + 139 - (int)slideOffset, colorAlpha | 0xAAAAAA);
        }
    }

    private void initVisualsLeftHandItemTab(float x, float y) {
        LiquidGlassButton backBtn = new LiquidGlassButton((int)x + 40, (int)y + 210, 80, 22, Text.literal("Back"), b -> {
            currentTab = Tab.VISUALS; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        visualsLeftHandItemWidgets.add(backBtn);

        LiquidGlassSwitch leftHandToggle = new LiquidGlassSwitch((int)x + 370, (int)y + 45, 40, 20, GlassMenuClient.CONFIG.enableLeftHandItem());
        leftHandToggle.setOnToggle(enabled -> {
            GlassMenuClient.CONFIG.enableLeftHandItem(enabled); GlassMenuClient.CONFIG.save();
        });
        visualsLeftHandItemWidgets.add(leftHandToggle);

        String btnText = GlassMenuClient.CONFIG.transparentLeftHandItem() ? "Glass Effect: ON" : "Glass Effect: OFF";
        LiquidGlassButton transparentBtn = new LiquidGlassButton((int)x + 40, (int)y + 80, 120, 22, Text.literal(btnText), b -> {
            boolean current = GlassMenuClient.CONFIG.transparentLeftHandItem();
            GlassMenuClient.CONFIG.transparentLeftHandItem(!current);
            GlassMenuClient.CONFIG.save();
            b.setMessage(Text.literal(GlassMenuClient.CONFIG.transparentLeftHandItem() ? "Glass Effect: ON" : "Glass Effect: OFF"));
        });
        visualsLeftHandItemWidgets.add(transparentBtn);

        int currentColor = GlassMenuClient.CONFIG.leftHandItemColor();
        leftHandSliderR = new LiquidGlassSlider((int)x + 230, (int)y + 90, 140, 16, ((currentColor >> 16) & 0xFF) / 255f);
        leftHandSliderG = new LiquidGlassSlider((int)x + 230, (int)y + 125, 140, 16, ((currentColor >> 8) & 0xFF) / 255f);
        leftHandSliderB = new LiquidGlassSlider((int)x + 230, (int)y + 160, 140, 16, (currentColor & 0xFF) / 255f);

        Runnable updateColor = () -> {
            if (isUpdating) return; isUpdating = true;
            int r = (int)(leftHandSliderR.getValue() * 255), g = (int)(leftHandSliderG.getValue() * 255), b = (int)(leftHandSliderB.getValue() * 255);
            int currentVal = GlassMenuClient.CONFIG.leftHandItemColor();
            int alpha = (currentVal >> 24) & 0xFF;
            if (alpha == 0) alpha = 0xEE;
            int color = (alpha << 24) | (r << 16) | (g << 8) | b;
            GlassMenuClient.CONFIG.leftHandItemColor(color); GlassMenuClient.CONFIG.save();
            isUpdating = false;
        };

        leftHandSliderR.setOnValueChange(v -> updateColor.run());
        leftHandSliderG.setOnValueChange(v -> updateColor.run());
        leftHandSliderB.setOnValueChange(v -> updateColor.run());
        visualsLeftHandItemWidgets.add(leftHandSliderR); visualsLeftHandItemWidgets.add(leftHandSliderG); visualsLeftHandItemWidgets.add(leftHandSliderB);
    }

    private void renderVisualsLeftHandItemTab(DrawContext context, int x, int y, int mouseX, int mouseY, float delta) {
        int alphaInt = (int)(255 * contentAlpha); int colorAlpha = alphaInt << 24;
        float slideOffset = (1.0f - contentAlpha) * 12f;
        boolean isTransparent = GlassMenuClient.CONFIG.transparentLeftHandItem();

        context.drawTextWithShadow(textRenderer, "Left Hand Item", x + 40, y + 50 - (int)slideOffset, colorAlpha | 0xAAAAAA);
        context.drawTextWithShadow(textRenderer, "Overlay Color", x + 230, y + 50 - (int)slideOffset, colorAlpha | 0xFFFFFF);

        for (ClickableWidget w : visualsLeftHandItemWidgets) {
            w.setAlpha(contentAlpha);
            if (w == leftHandSliderR || w == leftHandSliderG || w == leftHandSliderB) {
                if (isTransparent) { w.setX(-9999); w.setY(-9999); }
                else if (w == leftHandSliderR) { w.setX(x + 230); w.setY((int)y + 85 - (int)slideOffset); }
                else if (w == leftHandSliderG) { w.setX(x + 230); w.setY((int)y + 118 - (int)slideOffset); }
                else if (w == leftHandSliderB) { w.setX(x + 230); w.setY((int)y + 151 - (int)slideOffset); }
            } else if (w instanceof LiquidGlassButton lgb && lgb.getMessage().getString().equals("Back")) {
                w.setX(x + 40); w.setY((int)y + 205 - (int)slideOffset);
            } else if (w instanceof LiquidGlassButton lgb && lgb.getMessage().getString().startsWith("Glass Effect")) {
                w.setX(x + 40); w.setY((int)y + 80 - (int)slideOffset);
            } else {
                w.setX(x + 370); w.setY((int)y + 45 - (int)slideOffset);
            }
        }

        if (!isTransparent) {
            context.drawTextWithShadow(textRenderer, "Red",   x + 230, y + 73  - (int)slideOffset, colorAlpha | 0xAAAAAA);
            context.drawTextWithShadow(textRenderer, "Green", x + 230, y + 106 - (int)slideOffset, colorAlpha | 0xAAAAAA);
            context.drawTextWithShadow(textRenderer, "Blue",  x + 230, y + 139 - (int)slideOffset, colorAlpha | 0xAAAAAA);
        }

        // Draw a live HUD preview on the left side under the button
        int previewW = 48;
        int previewH = 48;
        int previewX = (int)x + 60;
        int previewY = (int)y + 130 - (int)slideOffset;

        context.getMatrices().push();
        int origW = GlassMenuClient.CONFIG.leftHandItemWidth();
        int origH = GlassMenuClient.CONFIG.leftHandItemHeight();
        int origX = GlassMenuClient.CONFIG.leftHandItemX();
        int origY = GlassMenuClient.CONFIG.leftHandItemY();

        GlassMenuClient.CONFIG.leftHandItemWidth(previewW);
        GlassMenuClient.CONFIG.leftHandItemHeight(previewH);
        GlassMenuClient.CONFIG.leftHandItemX(previewX);
        GlassMenuClient.CONFIG.leftHandItemY(previewY);

        com.example.glassmenu.render.LeftHandItemRenderer.render(context, this.width, this.height, true);

        GlassMenuClient.CONFIG.leftHandItemWidth(origW);
        GlassMenuClient.CONFIG.leftHandItemHeight(origH);
        GlassMenuClient.CONFIG.leftHandItemX(origX);
        GlassMenuClient.CONFIG.leftHandItemY(origY);

        context.getMatrices().pop();
        context.draw();
    }

    private void drawUserHudPreview(DrawContext context, int x, int y, int w, int h) {
        float scaleX = (float) w / 180f;
        float scaleY = (float) h / 26f;

        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(scaleX, scaleY, 1.0f);

        boolean transparent = GlassMenuClient.CONFIG.transparentUserHud();
        
        if (!transparent) {
            RenderUtils.drawSdfRoundedOutline(context.getMatrices(), 0f, 0f, 180f, 26f, 6f, 0.8f, 0x33FFFFFF);
            RenderUtils.drawSdfRoundedRect(context.getMatrices(), 0f, 0f, 180f, 26f, 6f, 0xFF000000, 0f);
        } else {
            RenderUtils.drawSdfRoundedOutline(context.getMatrices(), 0f, 0f, 180f, 26f, 6f, 0.8f, 0x33FFFFFF);
        }
        context.draw(); // Flush background

        int slotOutlineColor = transparent ? 0x22FFFFFF : 0x1AFFFFFF;
        int slotFillColor = transparent ? 0x0F000000 : 0x12FFFFFF;

        for (int i = 0; i < 3; i++) {
            float bx = 3 + i * 59;
            float by = 3;
            RenderUtils.drawSdfRoundedOutline(context.getMatrices(), bx, by, 56f, 20f, 4f, 0.6f, slotOutlineColor);
            RenderUtils.drawSdfRoundedRect(context.getMatrices(), bx, by, 56f, 20f, 4f, slotFillColor, 0f);
        }
        context.draw(); // Flush slots

        int textY = 9;

        // Slot 1: HP (Green)
        String hpText = "20";
        int hpW = textRenderer.getWidth(hpText);
        context.drawText(textRenderer, hpText, 31 - hpW / 2, textY, 0xFF44FF44, false);

        // Slot 2: FD (Yellow)
        String fdText = "16";
        int fdW = textRenderer.getWidth(fdText);
        context.drawText(textRenderer, fdText, 90 - fdW / 2, textY, 0xFFFFCC00, false);

        // Slot 3: XP (Red)
        String xpText = "12";
        int xpW = textRenderer.getWidth(xpText);
        context.drawText(textRenderer, xpText, 149 - xpW / 2, textY, 0xFFFF4444, false);

        context.getMatrices().pop();
        context.draw(); // Flush!
    }

    private void drawFastItemPreview(DrawContext context, int x, int y, int w, int h) {
        float baseW = 160f;
        float baseH = 160f;
        float scaleX = (float) w / baseW;
        float scaleY = (float) h / baseH;
        float radius = baseW / 2f;
        float distributionRadius = radius - 20f;

        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(scaleX, scaleY, 1.0f);

        int panelColor = GlassMenuClient.CONFIG.fastItemColor();
        int alpha = (panelColor >> 24) & 0xFF;

        if (!GlassMenuClient.CONFIG.transparentFastItem()) {
            if (alpha > 0) {
                int borderColor = (alpha * 0x2A / 0xEE) << 24 | 0x00FFFFFF;
                RenderUtils.drawSdfRoundedOutline(context.getMatrices(), 0, 0, baseW, baseH, radius, 0.8f, borderColor);
                RenderUtils.drawSdfRoundedRect(context.getMatrices(), 0, 0, baseW, baseH, radius, panelColor, 0);
            }
        } else {
            RenderUtils.drawSdfRoundedOutline(context.getMatrices(), 0, 0, baseW, baseH, radius, 0.8f, 0x33FFFFFF);
        }
        context.draw();

        if (GlassMenuClient.CONFIG.fastItemSlots()) {
            int slotOutlineColor = GlassMenuClient.CONFIG.transparentFastItem() ? 0x22FFFFFF : 0x1AFFFFFF;
            int slotFillColor = GlassMenuClient.CONFIG.transparentFastItem() ? 0x0F000000 : 0x12FFFFFF;

            for (int i = 0; i < 9; i++) {
                double angle = -Math.PI / 2.0 + i * (2.0 * Math.PI / 9.0);
                float slotCenterX = baseW / 2f + (float) Math.cos(angle) * distributionRadius;
                float slotCenterY = baseH / 2f + (float) Math.sin(angle) * distributionRadius;

                float slotSize = (i == 0) ? 24f : 20f;
                float rx = slotCenterX - slotSize / 2f;
                float ry = slotCenterY - slotSize / 2f;

                RenderUtils.drawSdfRoundedOutline(context.getMatrices(), rx, ry, slotSize, slotSize, slotSize / 2f, 0.6f, slotOutlineColor);
                RenderUtils.drawSdfRoundedRect(context.getMatrices(), rx, ry, slotSize, slotSize, slotSize / 2f, slotFillColor, 0);

                if (i == 0) {
                    RenderUtils.drawSdfRoundedOutline(context.getMatrices(), rx - 1f, ry - 1f, slotSize + 2f, slotSize + 2f, (slotSize + 2f) / 2f, 1.0f, 0xFFFFFFFF);
                }
            }
            context.draw();
        }
        context.getMatrices().pop();
        context.draw();
    }

    private void drawCornerBrackets(DrawContext context, float x, float y, float w, float h, float gap, float len, float t, float radius, int color) {
        MinecraftClient client = MinecraftClient.getInstance();
        double sc = client.getWindow().getScaleFactor();
        int screenHeight = client.getWindow().getScaledHeight();

        // Coordinates of the outer outline rectangle
        float ox = x - gap;
        float oy = y - gap;
        float ow = w + 2 * gap;
        float oh = h + 2 * gap;
        float or = radius + gap;

        // Draw top-left corner
        int tlX = (int) Math.round((ox - t) * sc);
        int tlY = (int) Math.round((screenHeight - (oy + len)) * sc);
        int tlW = (int) Math.round((len + t) * sc);
        int tlH = (int) Math.round((len + t) * sc);
        RenderSystem.enableScissor(tlX, Math.max(0, tlY), tlW, tlH);
        RenderUtils.drawSdfRoundedOutline(context.getMatrices(), ox, oy, ow, oh, or, t, color);
        context.draw();

        // Draw top-right corner
        int trX = (int) Math.round((ox + ow - len) * sc);
        int trY = (int) Math.round((screenHeight - (oy + len)) * sc);
        int trW = (int) Math.round((len + t) * sc);
        int trH = (int) Math.round((len + t) * sc);
        RenderSystem.enableScissor(trX, Math.max(0, trY), trW, trH);
        RenderUtils.drawSdfRoundedOutline(context.getMatrices(), ox, oy, ow, oh, or, t, color);
        context.draw();

        // Draw bottom-left corner
        int blX = (int) Math.round((ox - t) * sc);
        int blY = (int) Math.round((screenHeight - (oy + oh + t)) * sc);
        int blW = (int) Math.round((len + t) * sc);
        int blH = (int) Math.round((len + t) * sc);
        RenderSystem.enableScissor(blX, Math.max(0, blY), blW, blH);
        RenderUtils.drawSdfRoundedOutline(context.getMatrices(), ox, oy, ow, oh, or, t, color);
        context.draw();

        // Draw bottom-right corner
        int brX = (int) Math.round((ox + ow - len) * sc);
        int brY = (int) Math.round((screenHeight - (oy + oh + t)) * sc);
        int brW = (int) Math.round((len + t) * sc);
        int brH = (int) Math.round((len + t) * sc);
        RenderSystem.enableScissor(brX, Math.max(0, brY), brW, brH);
        RenderUtils.drawSdfRoundedOutline(context.getMatrices(), ox, oy, ow, oh, or, t, color);
        context.draw();

        RenderSystem.disableScissor();
    }

    private void renderPositionTab(DrawContext context, int mouseX, int mouseY, float delta) {
        context.drawCenteredTextWithShadow(textRenderer, "DRAG ELEMENTS TO POSITION / EDGES TO RESIZE", this.width / 2, 20, 0xFFFFFFFF);

        // 1. Dynamic Island
        int islandW = GlassMenuClient.CONFIG.capsuleWidth();
        int islandH = GlassMenuClient.CONFIG.capsuleHeight();
        int islandX = GlassMenuClient.CONFIG.islandX() == -1 ? (this.width - islandW) / 2 : GlassMenuClient.CONFIG.islandX();
        int islandY = GlassMenuClient.CONFIG.islandY();
        RenderUtils.drawSdfRoundedRect(context.getMatrices(), islandX, islandY, islandW, islandH, 8f, 0xEE1C1C1E, 0);
        context.draw(); // Flush
        context.drawCenteredTextWithShadow(textRenderer, "Dynamic Island", islandX + islandW / 2, islandY + (islandH - 8) / 2, 0xFFFFFFFF);
        
        boolean islandHovered = mouseX >= islandX && mouseX <= islandX + islandW && mouseY >= islandY && mouseY <= islandY + islandH;
        if (islandHovered || selectedObject == PositionObject.ISLAND) {
            drawCornerBrackets(context, islandX, islandY, islandW, islandH, 3.0f, 12.0f, 2.0f, 8.0f, 0xEE00FF00);
        }

        // 2. Inventory HUD
        int invW = GlassMenuClient.CONFIG.inventoryHudWidth();
        int invH = GlassMenuClient.CONFIG.inventoryHudHeight();
        int invX = GlassMenuClient.CONFIG.inventoryHudX() == -1 ? 10 : GlassMenuClient.CONFIG.inventoryHudX();
        int invY = GlassMenuClient.CONFIG.inventoryHudY() == -1 ? this.height - invH - 10 : GlassMenuClient.CONFIG.inventoryHudY();
        drawInventoryHudPreview(context, invX, invY, invW, invH);
        
        boolean invHovered = mouseX >= invX && mouseX <= invX + invW && mouseY >= invY && mouseY <= invY + invH;
        if (invHovered || selectedObject == PositionObject.INVENTORY) {
            drawCornerBrackets(context, invX, invY, invW, invH, 3.0f, 15.0f, 2.0f, 8.0f, 0xEE00FF00);
        }

        // 3. Player Card
        // 3. Player Card
        int cardW = GlassMenuClient.CONFIG.playerCardWidth();
        int cardH = GlassMenuClient.CONFIG.playerCardHeight();
        int cardX = GlassMenuClient.CONFIG.playerCardX() == -1 ? (this.width - cardW) / 2 : GlassMenuClient.CONFIG.playerCardX();
        int cardY;
        if (GlassMenuClient.CONFIG.playerCardY() == -1) {
            cardY = islandY + islandH + 8;
        } else {
            cardY = GlassMenuClient.CONFIG.playerCardY();
        }

        int panelColor = GlassMenuClient.CONFIG.playerCardColor();
        int borderColor = 0x2AFFFFFF;

        float scaleX = (float) cardW / 140f;
        float scaleY = (float) cardH / 54f;

        float centerX = cardX + cardW / 2f;
        float centerY = cardY + cardH / 2f;
        float virtualCenterX = cardX + 70f;
        float virtualCenterY = cardY + 27f;

        context.getMatrices().push();
        // Pivot scale transformation around the center of the card
        context.getMatrices().translate(centerX, centerY, 300.0f);
        context.getMatrices().scale(scaleX, scaleY, 1.0f);
        context.getMatrices().translate(-virtualCenterX, -virtualCenterY, 0f);

        if (!GlassMenuClient.CONFIG.transparentPlayerCard()) {
            RenderUtils.drawSdfRoundedOutline(context.getMatrices(), cardX, cardY, 140, 54, 8f, 0.8f, borderColor);
            RenderUtils.drawSdfRoundedRect(context.getMatrices(), cardX, cardY, 140, 54, 8f, panelColor, 0);
        } else {
            RenderUtils.drawSdfRoundedOutline(context.getMatrices(), cardX, cardY, 140, 54, 8f, 0.8f, 0x33FFFFFF);
        }
        context.draw(); // Flush background

        int slotOutlineColor = GlassMenuClient.CONFIG.transparentPlayerCard() ? 0x22FFFFFF : 0x1AFFFFFF;
        int slotFillColor = GlassMenuClient.CONFIG.transparentPlayerCard() ? 0x0F000000 : 0x12FFFFFF;

        // Head slot frame
        float headFrameX = cardX + 5f;
        float headFrameY = cardY + 5f;
        float headFrameSize = 20f;

        RenderUtils.drawSdfRoundedOutline(context.getMatrices(), headFrameX, headFrameY, headFrameSize, headFrameSize, 4f, 0.6f, slotOutlineColor);
        RenderUtils.drawSdfRoundedRect(context.getMatrices(), headFrameX, headFrameY, headFrameSize, headFrameSize, 4f, slotFillColor, 0);
        context.draw();

        if (client.player != null) {
            Identifier dummySkin = client.player.getSkinTextures().texture();
            RenderUtils.drawSdfRoundedTexture(context.getMatrices(), headFrameX + 1f, headFrameY + 1f, headFrameSize - 2f, headFrameSize - 2f, 3.5f, dummySkin, 0xFFFFFFFF, 0.125f, 0.125f, 0.25f, 0.25f);
            RenderUtils.drawSdfRoundedTexture(context.getMatrices(), headFrameX + 1f, headFrameY + 1f, headFrameSize - 2f, headFrameSize - 2f, 3.5f, dummySkin, 0xFFFFFFFF, 0.625f, 0.125f, 0.75f, 0.25f);
        }

        String hpText = "20.0 HP";
        int hpTextWidth = textRenderer.getWidth(hpText);
        int hpTextX = cardX + 140 - 5 - hpTextWidth;

        if (client.player != null) {
            String displayName = client.player.getGameProfile().getName();
            int maxNameWidth = hpTextX - (cardX + 29) - 6;
            if (textRenderer.getWidth(displayName) > maxNameWidth) {
                displayName = textRenderer.trimToWidth(displayName, maxNameWidth - 8) + "...";
            }
            context.drawTextWithShadow(textRenderer, displayName, cardX + 29, cardY + 5, 0xFFFFFFFF);
        }
        context.drawTextWithShadow(textRenderer, hpText, hpTextX, cardY + 5, 0xFFFF8888);
        
        // Health bar slot frame (Raised and thinner: 106x6)
        int healthFrameX = cardX + 29;
        int healthFrameY = cardY + 16;
        int healthFrameW = 106;
        int healthFrameH = 6;

        RenderUtils.drawSdfRoundedOutline(context.getMatrices(), healthFrameX, healthFrameY, healthFrameW, healthFrameH, 2.5f, 0.6f, slotOutlineColor);
        RenderUtils.drawSdfRoundedRect(context.getMatrices(), healthFrameX, healthFrameY, healthFrameW, healthFrameH, 2.5f, slotFillColor, 0);
        context.draw();

        // Inner health bar
        int barX = healthFrameX + 1;
        int barY = healthFrameY + 1;
        int barW = healthFrameW - 2;
        int barH = 4;
        RenderUtils.drawSdfRoundedRect(context.getMatrices(), barX, barY, barW, barH, 1.5f, 0xFF505050, 0);
        RenderUtils.drawSdfRoundedRect(context.getMatrices(), barX, barY, Math.round(barW * 0.8f), barH, 1.5f, 0xFFE03030, 0);
        context.draw();

        // Render Armor & Held Items Slots (Sized down to 18x18, gap 2px)
        float slotSize = 18f;
        float gap = 2f;
        float totalW = 6f * slotSize + 5f * gap;
        float startX = cardX + (140 - totalW) / 2f;
        float slotsY = cardY + 31f;

        // Draw slots background/outlines
        for (int i = 0; i < 6; i++) {
            float currentSlotX = startX + i * (slotSize + gap);
            RenderUtils.drawSdfRoundedOutline(context.getMatrices(), currentSlotX, slotsY, slotSize, slotSize, 3f, 0.6f, slotOutlineColor);
            RenderUtils.drawSdfRoundedRect(context.getMatrices(), currentSlotX, slotsY, slotSize, slotSize, 3f, slotFillColor, 0);
        }
        context.draw();

        if (client.player != null) {
            net.minecraft.item.ItemStack[] items = new net.minecraft.item.ItemStack[6];
            items[0] = client.player.getMainHandStack();
            items[1] = client.player.getOffHandStack();
            items[2] = client.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD);
            items[3] = client.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST);
            items[4] = client.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.LEGS);
            items[5] = client.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.FEET);

            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            net.minecraft.client.render.DiffuseLighting.enableGuiDepthLighting();

            for (int i = 0; i < 6; i++) {
                net.minecraft.item.ItemStack stack = items[i];
                if (!stack.isEmpty()) {
                    float currentSlotX = startX + i * (slotSize + gap);
                    context.getMatrices().push();
                    // Place 16x16 item centered inside the 18x18 slot (+1px offset)
                    context.getMatrices().translate(currentSlotX + 1f, slotsY + 1f, 100.0f);
                    
                    context.drawItem(stack, 0, 0);
                    context.drawItemInSlot(client.textRenderer, stack, 0, 0);
                    
                    context.getMatrices().pop();
                }
            }

            net.minecraft.client.render.DiffuseLighting.disableGuiDepthLighting();
            RenderSystem.disableDepthTest();
        }
        context.getMatrices().pop();

        boolean cardHovered = mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= cardY && mouseY <= cardY + cardH;
        if (cardHovered || selectedObject == PositionObject.PLAYER) {
            drawCornerBrackets(context, cardX, cardY, cardW, cardH, 3.0f, 12.0f, 2.0f, 8.0f, 0xEE00FF00);
        }

        // 4. User Indicator
        int indW = GlassMenuClient.CONFIG.userIndicatorWidth();
        int indH = GlassMenuClient.CONFIG.userIndicatorHeight();
        int indX = GlassMenuClient.CONFIG.userIndicatorX() == -1 ? (this.width - indW) / 2 : GlassMenuClient.CONFIG.userIndicatorX();
        int indY = GlassMenuClient.CONFIG.userIndicatorY() == -1 ? 35 : GlassMenuClient.CONFIG.userIndicatorY();

        com.example.glassmenu.render.UserIndicatorRenderer.renderIndicator(
            context, indX, indY, indW, indH, 120, 15, 0, 0,
            GlassMenuClient.CONFIG.transparentUserIndicator(),
            GlassMenuClient.CONFIG.userIndicatorColor()
        );

        boolean indHovered = mouseX >= indX && mouseX <= indX + indW && mouseY >= indY && mouseY <= indY + indH;
        if (indHovered || selectedObject == PositionObject.INDICATOR) {
            drawCornerBrackets(context, indX, indY, indW, indH, 3.0f, 12.0f, 2.0f, 8.0f, 0xEE00FF00);
        }

        // 5. Armor HUD
        int armW = GlassMenuClient.CONFIG.armorHudWidth();
        int armH = GlassMenuClient.CONFIG.armorHudHeight();
        int armX = GlassMenuClient.CONFIG.armorHudX() == -1 ? (this.width - armW) / 2 : GlassMenuClient.CONFIG.armorHudX();
        int armY = GlassMenuClient.CONFIG.armorHudY() == -1 ? this.height - armH - 45 : GlassMenuClient.CONFIG.armorHudY();

        drawArmorHudPreview(context, armX, armY, armW, armH);

        boolean armHovered = mouseX >= armX && mouseX <= armX + armW && mouseY >= armY && mouseY <= armY + armH;
        if (armHovered || selectedObject == PositionObject.ARMOR) {
            drawCornerBrackets(context, armX, armY, armW, armH, 3.0f, 12.0f, 2.0f, 8.0f, 0xEE00FF00);
        }

        // 6. Fast Item Wheel
        int fiW = GlassMenuClient.CONFIG.fastItemWidth();
        int fiH = GlassMenuClient.CONFIG.fastItemHeight();
        int fiX = GlassMenuClient.CONFIG.fastItemX() == -1 ? (this.width - fiW) / 2 : GlassMenuClient.CONFIG.fastItemX();
        int fiY = GlassMenuClient.CONFIG.fastItemY() == -1 ? this.height - fiH - 10 : GlassMenuClient.CONFIG.fastItemY();

        drawFastItemPreview(context, fiX, fiY, fiW, fiH);

        boolean fiHovered = mouseX >= fiX && mouseX <= fiX + fiW && mouseY >= fiY && mouseY <= fiY + fiH;
        if (fiHovered || selectedObject == PositionObject.FAST_ITEM) {
            drawCornerBrackets(context, fiX, fiY, fiW, fiH, 3.0f, 15.0f, 2.0f, (float)fiW / 2f, 0xEE00FF00);
        }

        // 7. User HUD
        int userHudW = GlassMenuClient.CONFIG.userHudWidth();
        int userHudHeightVal = GlassMenuClient.CONFIG.userHudHeight();
        int userHudX = GlassMenuClient.CONFIG.userHudX() == -1 ? (this.width - userHudW) / 2 : GlassMenuClient.CONFIG.userHudX();
        int userHudY = GlassMenuClient.CONFIG.userHudY() == -1 ? this.height - userHudHeightVal - 10 : GlassMenuClient.CONFIG.userHudY();

        drawUserHudPreview(context, userHudX, userHudY, userHudW, userHudHeightVal);

        boolean userHudHovered = mouseX >= userHudX && mouseX <= userHudX + userHudW && mouseY >= userHudY && mouseY <= userHudY + userHudHeightVal;
        if (userHudHovered || selectedObject == PositionObject.USER_HUD) {
            drawCornerBrackets(context, userHudX, userHudY, userHudW, userHudHeightVal, 3.0f, 15.0f, 2.0f, 8.0f, 0xEE00FF00);
        }

        // 8. Potion Effects HUD
        int effSize = GlassMenuClient.CONFIG.effectsHudHeight();
        effSize = MathHelper.clamp(effSize, 18, 50);
        int effPadding = 3;
        int effGap = 4;
        int effBoxSize = effSize - effPadding * 2;
        int effLength = effPadding * 2 + 3 * (effBoxSize + effGap) - effGap; // 3 items for preview

        boolean vertical = GlassMenuClient.CONFIG.effectsHudVertical();
        int effW = vertical ? effSize : effLength;
        int effH = vertical ? effLength : effSize;
        
        int effX = GlassMenuClient.CONFIG.effectsHudX() == -1 ? this.width - effW - 10 : GlassMenuClient.CONFIG.effectsHudX();
        int effY = GlassMenuClient.CONFIG.effectsHudY() == -1 ? 10 : GlassMenuClient.CONFIG.effectsHudY();

        com.example.glassmenu.render.EffectsHudRenderer.renderEffects(context, this.width, this.height, true);

        boolean effHovered = mouseX >= effX && mouseX <= effX + effW && mouseY >= effY && mouseY <= effY + effH;
        if (effHovered || selectedObject == PositionObject.EFFECTS) {
            drawCornerBrackets(context, effX, effY, effW, effH, 3.0f, 12.0f, 2.0f, 8.0f, 0xEE00FF00);
        }

        // 9. Left Hand Item HUD
        int leftW = GlassMenuClient.CONFIG.leftHandItemWidth();
        int leftH = GlassMenuClient.CONFIG.leftHandItemHeight();
        int leftX = GlassMenuClient.CONFIG.leftHandItemX() == -1 ? 10 : GlassMenuClient.CONFIG.leftHandItemX();
        int leftY = GlassMenuClient.CONFIG.leftHandItemY() == -1 ? (this.height - leftH) / 2 : GlassMenuClient.CONFIG.leftHandItemY();

        com.example.glassmenu.render.LeftHandItemRenderer.render(context, this.width, this.height, true);

        boolean leftHovered = mouseX >= leftX && mouseX <= leftX + leftW && mouseY >= leftY && mouseY <= leftY + leftH;
        if (leftHovered || selectedObject == PositionObject.LEFT_HAND_ITEM) {
            drawCornerBrackets(context, leftX, leftY, leftW, leftH, 3.0f, 12.0f, 2.0f, 8.0f, 0xEE00FF00);
        }

        for (ClickableWidget w : positionWidgets) {
            w.setAlpha(w.isHovered() ? 1.0f : 0.5f);
            w.render(context, mouseX, mouseY, delta);
        }
    }

    private void drawInnerOutline(DrawContext context, float x, float y, float w, float h) {
        float hue = (System.currentTimeMillis() % 3000L) / 3000.0f;
        int color = java.awt.Color.HSBtoRGB(hue, 0.9f, 1.0f);
        float offset = 1.0f;
        RenderUtils.drawSdfRoundedOutline(context.getMatrices(), x + offset, y + offset, w - offset * 2, h - offset * 2, 7f, 1.5f, 0xFF000000 | color);
    }

    private boolean isResizing = false;
    private boolean resizeLeft = false;
    private boolean resizeRight = false;
    private boolean resizeTop = false;
    private boolean resizeBottom = false;
    private int initialWidth = 0;
    private int initialHeight = 0;
    private int initialX = 0;
    private int initialY = 0;

    private boolean checkClickOrResize(PositionObject obj, int x, int y, int w, int h, double mouseX, double mouseY) {
        int margin = 8;
        if (mouseX >= x - margin && mouseX <= x + w + margin && mouseY >= y - margin && mouseY <= y + h + margin) {
            selectedObject = obj;
            
            // Check if we are close to edges for resizing
            resizeLeft = Math.abs(mouseX - x) <= margin;
            resizeRight = Math.abs(mouseX - (x + w)) <= margin;
            resizeTop = Math.abs(mouseY - y) <= margin;
            resizeBottom = Math.abs(mouseY - (y + h)) <= margin;
            
            if (resizeLeft || resizeRight || resizeTop || resizeBottom) {
                isResizing = true;
                isDraggingObject = false;
                dragOffsetX = mouseX;
                dragOffsetY = mouseY;
                initialWidth = w;
                initialHeight = h;
                initialX = x;
                initialY = y;
            } else {
                isResizing = false;
                isDraggingObject = true;
                dragOffsetX = mouseX - x;
                dragOffsetY = mouseY - y;
            }
            return true;
        }
        return false;
    }

    private void drawArmorHudPreview(DrawContext context, int x, int y, int w, int h) {
        boolean isVertical = GlassMenuClient.CONFIG.armorHudVertical();
        float baseW = isVertical ? 32f : 120f;
        float baseH = isVertical ? 120f : 32f;
        float scaleX = (float) w / baseW;
        float scaleY = (float) h / baseH;

        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(scaleX, scaleY, 1.0f);

        int panelColor = GlassMenuClient.CONFIG.armorHudColor();
        int alpha = (panelColor >> 24) & 0xFF;
        
        if (!GlassMenuClient.CONFIG.transparentArmorHud()) {
            if (alpha == 0) {
                RenderUtils.drawSdfRoundedOutline(context.getMatrices(), 0, 0, baseW, baseH, 8f, 0.8f, 0x55FFFFFF);
            } else {
                int borderColor = (alpha * 0x2A / 0xEE) << 24 | 0x00FFFFFF;
                RenderUtils.drawSdfRoundedOutline(context.getMatrices(), 0, 0, baseW, baseH, 8f, 0.8f, borderColor);
                RenderUtils.drawSdfRoundedRect(context.getMatrices(), 0, 0, baseW, baseH, 8f, panelColor, 0);
            }
            context.draw(); // Flush SDF states!
        } else {
            RenderUtils.drawSdfRoundedOutline(context.getMatrices(), 0, 0, baseW, baseH, 8f, 0.8f, 0x33FFFFFF);
            context.draw(); // Flush SDF states!
        }

        int slotOutlineColor = GlassMenuClient.CONFIG.transparentArmorHud() ? 0x22FFFFFF : 0x1AFFFFFF;
        int slotFillColor = GlassMenuClient.CONFIG.transparentArmorHud() ? 0x0F000000 : 0x12FFFFFF;

        for (int i = 0; i < 4; i++) {
            int localX = isVertical ? 4 : 4 + i * 28;
            int localY = isVertical ? 4 + i * 28 : 4;

            float slotSquareX = localX + 1f;
            float slotSquareY = localY + 1f;
            float slotSquareSize = 22f;

            RenderUtils.drawSdfRoundedOutline(context.getMatrices(), slotSquareX, slotSquareY, slotSquareSize, slotSquareSize, 5f, 0.6f, slotOutlineColor);
            RenderUtils.drawSdfRoundedRect(context.getMatrices(), slotSquareX, slotSquareY, slotSquareSize, slotSquareSize, 5f, slotFillColor, 0);
        }
        context.draw();

        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        com.mojang.blaze3d.systems.RenderSystem.clear(org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
        net.minecraft.client.render.DiffuseLighting.enableGuiDepthLighting();

        ItemStack[] previewArmor = {
            new ItemStack(Items.DIAMOND_HELMET),
            new ItemStack(Items.DIAMOND_CHESTPLATE),
            new ItemStack(Items.DIAMOND_LEGGINGS),
            new ItemStack(Items.DIAMOND_BOOTS)
        };

        for (int i = 0; i < 4; i++) {
            int localX = isVertical ? 4 : 4 + i * 28;
            int localY = isVertical ? 4 + i * 28 : 4;

            ItemStack stack = previewArmor[i];
            context.draw();
            context.getMatrices().push();
            context.getMatrices().translate(localX + 12, localY + 12, 150.0f);
            context.getMatrices().translate(8f, 8f, 0f);
            context.getMatrices().scale(1.25f, 1.25f, 1.0f);
            context.getMatrices().translate(-8f, -8f, 0f);
            context.drawItemInSlot(client.textRenderer, stack, 0, 0);
            context.getMatrices().pop();
            context.draw();
        }

        context.getMatrices().pop();
        context.draw();
        net.minecraft.client.render.DiffuseLighting.disableGuiDepthLighting();
    }

    private void drawInventoryHudPreview(DrawContext context, int x, int y, int w, int h) {
        float scaleX = (float) w / 260f;
        float scaleY = (float) h / 92f;

        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(scaleX, scaleY, 1.0f);

        int panelColor = GlassMenuClient.CONFIG.inventoryHudColor();
        int alpha = (panelColor >> 24) & 0xFF;
        
        if (!GlassMenuClient.CONFIG.transparentBackground()) {
            if (alpha == 0) {
                RenderUtils.drawSdfRoundedOutline(context.getMatrices(), 0, 0, 260, 92, 8f, 0.8f, 0x55FFFFFF);
            } else {
                int borderColor = 0x2AFFFFFF;
                RenderUtils.drawSdfRoundedOutline(context.getMatrices(), 0, 0, 260, 92, 8f, 0.8f, borderColor);
                RenderUtils.drawSdfRoundedRect(context.getMatrices(), 0, 0, 260, 92, 8f, panelColor, 0);
            }
            context.draw(); // Flush SDF states!
        } else {
            // Draw a very subtle outline in position mode so they can see the boundary
            RenderUtils.drawSdfRoundedOutline(context.getMatrices(), 0, 0, 260, 92, 8f, 0.8f, 0x33FFFFFF);
            context.draw(); // Flush SDF states!
        }
        
        context.draw(); // Flush previous UI elements first
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        com.mojang.blaze3d.systems.RenderSystem.clear(org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, net.minecraft.screen.PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
        net.minecraft.client.render.DiffuseLighting.enableGuiDepthLighting();

        int slotSize = 24;
        int padding = 4;
        
        ItemStack[] previewStacks = {
            new ItemStack(Items.GRASS_BLOCK),
            new ItemStack(Items.DIRT),
            new ItemStack(Items.COBBLESTONE),
            new ItemStack(Items.OAK_PLANKS),
            new ItemStack(Items.DIAMOND_ORE),
            new ItemStack(Items.GOLD_INGOT),
            new ItemStack(Items.IRON_SWORD),
            new ItemStack(Items.BOW),
            new ItemStack(Items.APPLE)
        };

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int itemX = 6 + col * (slotSize + padding) + 4;
                int itemY = 6 + row * (slotSize + padding) + 4;
                
                int slotIndex = row * 9 + col;
                if (slotIndex < previewStacks.length) {
                    ItemStack stack = previewStacks[slotIndex];
                    context.draw(); // Flush before rendering item to reset shaders!
                    context.getMatrices().push();
                    context.getMatrices().translate(itemX, itemY, 150.0f);
                    context.getMatrices().translate(8f, 8f, 0f);
                    context.getMatrices().scale(1.25f, 1.25f, 1.0f);
                    context.getMatrices().translate(-8f, -8f, 0f);
                    context.drawItemInSlot(client.textRenderer, stack, 0, 0);
                    context.getMatrices().pop();
                    context.draw(); // Flush item renderer!
                } else if (!GlassMenuClient.CONFIG.transparentBackground()) {
                    RenderUtils.drawSdfRoundedRect(context.getMatrices(), itemX - 2, itemY - 2, 20, 20, 4f, 0x15FFFFFF, 0);
                    context.draw(); // Flush!
                }
            }
        }
        
        context.getMatrices().pop();
        context.draw(); // Flush finally
        net.minecraft.client.render.DiffuseLighting.disableGuiDepthLighting();
    }

    private void renderGeneralTab(DrawContext context, int x, int y, int mouseX, int mouseY, float delta) {
        int alphaInt = (int)(255 * contentAlpha); int colorAlpha = alphaInt << 24;
        float slideOffset = (1.0f - contentAlpha) * 12f;
        
        if (currentTab == Tab.GENERAL) {
            context.drawTextWithShadow(textRenderer, "Item Effects", x + 30, y + 45 - (int)slideOffset, colorAlpha | 0xFFFFFF);
            int leftBtnIndex = 0;
            for (int i = 0; i < generalWidgets.size(); i++) {
                ClickableWidget w = generalWidgets.get(i);
                w.setAlpha(contentAlpha);
                if (w instanceof LiquidGlassButton lgb && lgb.getMessage().getString().equals("Bridge")) {
                    w.setX(x + 280); w.setY(y + 60 - (int)slideOffset);
                } else if (w instanceof LiquidGlassButton lgb && lgb.getMessage().getString().contains("Glass Effect")) {
                    w.setX(x + 280); w.setY(y + 90 - (int)slideOffset);
                } else if (w instanceof LiquidGlassButton lgb && lgb.getMessage().getString().startsWith("Menu Glass")) {
                    w.setX(x + 280); w.setY(y + 120 - (int)slideOffset);
                } else {
                    w.setX(x + 30); w.setY((int)y + 65 + leftBtnIndex * 32 - (int)slideOffset);
                    leftBtnIndex++;
                }
            }
            drawItemPreview(context, x + 225, y + 140 - (int)slideOffset, 60);
        } else if (currentTab == Tab.BRIDGE) {
            context.drawTextWithShadow(textRenderer, "Bridge Box", x + 30, y + 45 - (int)slideOffset, colorAlpha | 0xFFFFFF);
            for (ClickableWidget w : bridgeWidgets) {
                w.setAlpha(contentAlpha);
                if (w == bridgeSliderR) { w.setX(x + 30); w.setY((int)y + 90 - (int)slideOffset); }
                else if (w == bridgeSliderG) { w.setX(x + 30); w.setY((int)y + 125 - (int)slideOffset); }
                else if (w == bridgeSliderB) { w.setX(x + 30); w.setY((int)y + 160 - (int)slideOffset); }
                else if (w instanceof LiquidGlassButton lgb && lgb.getMessage().getString().equals("Back")) {
                    w.setX(x + 230); w.setY((int)y + 50 - (int)slideOffset);
                } else if (w instanceof LiquidGlassButton lgb && lgb.getMessage().getString().contains("Glass Effect")) {
                    w.setX(x + 230); w.setY((int)y + 160 - (int)slideOffset);
                } else if (w instanceof LiquidGlassSwitch) {
                    w.setX(x + 330); w.setY((int)y + 45 - (int)slideOffset);
                }
            }
            if (bridgeHexInput != null) { 
                bridgeHexInput.setAlpha(contentAlpha); bridgeHexInput.setX(x + 235); bridgeHexInput.setY((int)y + 100 - (int)slideOffset); 
            }
            if (bridgeRgbInput != null) { 
                bridgeRgbInput.setAlpha(contentAlpha); bridgeRgbInput.setX(x + 235); bridgeRgbInput.setY((int)y + 130 - (int)slideOffset); 
            }
            // Draw input boxes for hex/rgb
            drawInputBox(context, x + 230, y + 97 - (int)slideOffset, 70, 18); 
            drawInputBox(context, x + 230, y + 127 - (int)slideOffset, 110, 18);
            
            // Hex and RGB labels
            context.drawTextWithShadow(textRenderer, "Hex", x + 230, y + 85 - (int)slideOffset, colorAlpha | 0xAAAAAA);
            context.drawTextWithShadow(textRenderer, "RGB", x + 230, y + 115 - (int)slideOffset, colorAlpha | 0xAAAAAA);

            context.drawTextWithShadow(textRenderer, "Red", x + 180, y + 95 - (int)slideOffset, colorAlpha | 0xAAAAAA);
            context.drawTextWithShadow(textRenderer, "Green", x + 180, y + 130 - (int)slideOffset, colorAlpha | 0xAAAAAA);
            context.drawTextWithShadow(textRenderer, "Blue", x + 180, y + 165 - (int)slideOffset, colorAlpha | 0xAAAAAA);
        }
    }

    private void drawItemPreview(DrawContext context, int x, int y, int size) {
        ItemStack stack = client.player != null ? client.player.getMainHandStack() : new ItemStack(Items.IRON_SWORD);
        if (stack.isEmpty()) stack = new ItemStack(Items.IRON_SWORD);
        context.getMatrices().push(); context.getMatrices().translate(x, y, 250); context.getMatrices().scale(size, -size, size);
        float time = (System.currentTimeMillis() % 4000L) / 4000.0f;
        context.getMatrices().multiply(RotationAxis.POSITIVE_Y.rotationDegrees(time * 360f));
        context.getMatrices().multiply(RotationAxis.POSITIVE_X.rotationDegrees(15f));

        GlassMenuConfigModel.ItemEffect effect = GlassMenuClient.CONFIG.itemEffect();
        net.minecraft.client.render.VertexConsumerProvider provider = context.getVertexConsumers();
        
        if (effect == GlassMenuConfigModel.ItemEffect.RAINBOW) {
            int rgb = java.awt.Color.HSBtoRGB((System.currentTimeMillis() % 4000 / 4000f), 0.8f, 1.0f);
            float r = ((rgb >> 16) & 0xFF)/255f; float g = ((rgb >> 8) & 0xFF)/255f; float b = (rgb & 0xFF)/255f;
            provider = new RenderUtils.TintedVertexConsumerProvider(provider, r, g, b, 1.0f);
        }

        net.minecraft.client.render.DiffuseLighting.disableGuiDepthLighting();
        context.draw(); // Flush to apply current state
        client.getItemRenderer().renderItem(stack, net.minecraft.client.render.model.json.ModelTransformationMode.FIXED, 0xF000F0, net.minecraft.client.render.OverlayTexture.DEFAULT_UV, context.getMatrices(), provider, client.world, 0);
        context.draw(); // Flush item

        if (effect == GlassMenuConfigModel.ItemEffect.PARTICLES || effect == GlassMenuConfigModel.ItemEffect.RGB_PARTICLES) {
            RenderSystem.disableDepthTest();
            HandParticleRenderer.render(context.getMatrices(), effect == GlassMenuConfigModel.ItemEffect.RGB_PARTICLES, stack, false, true);
            context.draw(); 
            RenderSystem.enableDepthTest();
        }

        context.draw(); RenderSystem.setShaderColor(1, 1, 1, 1); context.getMatrices().pop();
    }

    private void renderCombatTab(DrawContext context, int x, int y, int mouseX, int mouseY, float delta) {
        int alphaInt = (int)(255 * contentAlpha); int colorAlpha = alphaInt << 24;
        float slideOffset = (1.0f - contentAlpha) * 12f;

        // --- PREVIEW PANEL (DOCKED TOP-LEFT) ---
        float subX = x - 150;
        float subY = y - slideOffset;
        float subW = 140;
        float subH = 220;
        // Glass border outline (drawn first to act as a proper border highlight under the fill)
        RenderUtils.drawSdfRoundedOutline(context.getMatrices(), subX, subY, subW, subH, 15, 1.0f, 0x2AFFFFFF);
        
        // Preview Background Fill
        RenderUtils.drawSdfRoundedRect(context.getMatrices(), subX, subY, subW, subH, 15, 0x2C1C1C24, 0);
        RenderUtils.drawLine(context.getMatrices(), subX + 15, subY + 20, subX + subW - 15, subY + 20, 0.8f, 0x33FFFFFF);
        context.drawTextWithShadow(textRenderer, "PREVIEW", (int)(subX + 45), (int)(subY + 8), colorAlpha | 0x88FFFFFF);

        drawPlayerPreview(context, (int)(subX + subW/2), (int)(subY + subH - 35), 55, mouseX, mouseY);
        // ----------------------------

        context.drawTextWithShadow(textRenderer, "Target ESP", x + 230, y + 50 - (int)slideOffset, colorAlpha | 0xFFFFFF);

        
        for (ClickableWidget w : combatWidgets) {
            w.setAlpha(contentAlpha);
            if (w == sliderR) w.setY((int)y + 90 - (int)slideOffset);
            else if (w == sliderG) w.setY((int)y + 125 - (int)slideOffset);
            else if (w == sliderB) w.setY((int)y + 160 - (int)slideOffset);
            else w.setY((int)y + 45 - (int)slideOffset);
        }

        if (hexInput != null) { 
            hexInput.setAlpha(contentAlpha); 
            hexInput.setX(x + 30);
            hexInput.setY((int)y + 38 - (int)slideOffset); 
        }
        if (rgbInput != null) { 
            rgbInput.setAlpha(contentAlpha); 
            rgbInput.setX(x + 30);
            rgbInput.setY((int)y + 63 - (int)slideOffset); 
        }

        drawInputBox(context, x + 25, y + 35 - (int)slideOffset, 70, 18); drawInputBox(context, x + 25, y + 60 - (int)slideOffset, 110, 18);
        context.drawTextWithShadow(textRenderer, "Red", x + 110, y + 80 - (int)slideOffset, colorAlpha | 0xAAAAAA);
        context.drawTextWithShadow(textRenderer, "Green", x + 110, y + 115 - (int)slideOffset, colorAlpha | 0xAAAAAA);
        context.drawTextWithShadow(textRenderer, "Blue", x + 110, y + 150 - (int)slideOffset, colorAlpha | 0xAAAAAA);
    }

    private void renderMovementTab(DrawContext context, int x, int y, int mouseX, int mouseY, float delta, int currentW, int currentH) {
        int alphaInt = (int)(255 * contentAlpha); int colorAlpha = alphaInt << 24;
        float slideOffset = (1.0f - contentAlpha) * 12f;
        context.drawTextWithShadow(textRenderer, "Hand Customization", x + 30, y + 40 - (int)slideOffset, colorAlpha | 0xFFFFFF);
        
        double sc = MinecraftClient.getInstance().getWindow().getScaleFactor();
        int scissorY = (int)((this.height - y - currentH + 25) * sc);
        int scissorH = (int)((currentH - 65) * sc);
        RenderSystem.enableScissor((int)(x * sc), Math.max(0, scissorY), (int)(currentW * sc), Math.max(0, scissorH));
        
        int startY = y + 60 - (int)scrollY - (int)slideOffset;
        String[] labels = {"Enable Custom", "Speed", "Position", "Scale", "Rotation"};
        int[] labelMapping = {0, 1, 2, 5, 8}; int lIdx = 0;
        for (int i = 0; i < movementWidgets.size(); i++) {
            ClickableWidget w = movementWidgets.get(i);
            w.setAlpha(contentAlpha);
            int groupIdx = (i < 2 ? i : 2 + (i-2)/3);
            int yPos = (groupIdx < 2) ? startY + groupIdx * 40 : startY + 100 + (groupIdx - 2) * 100;
            int itemOffset = (i < 2) ? 0 : ((i-2)%3 == 1 ? -22 : 0);
            boolean isVert = (w instanceof LiquidGlassSlider lgs) && lgs.isVertical();
            
            // Symmetrically layout X, Y, Z widgets (gaps 20px on both sides of vertical Y slider)
            int xOffset;
            if (i < 2) {
                xOffset = (i == 0 ? 160 : 110);
            } else {
                int rem = (i - 2) % 3;
                if (rem == 0) {
                    xOffset = 100;
                } else if (rem == 1) {
                    xOffset = 160;
                } else {
                    xOffset = 200;
                }
            }
            
            w.setY(yPos + itemOffset); w.setX(x + xOffset);
            
            if (lIdx < labelMapping.length && i == labelMapping[lIdx]) {
                int labelY = (i < 2) ? w.getY() + 5 : yPos - 45;
                if (labelY > y + 40 && labelY < y + currentH - 25) {
                    context.drawTextWithShadow(textRenderer, labels[lIdx], x + 20, labelY, colorAlpha | 0xFFFFFF);
                    if (lIdx >= 2) {
                        // Position labels above sliders to avoid overlap with vertical slider Y
                        context.drawTextWithShadow(textRenderer, "X", x + 117, yPos - 28, colorAlpha | 0x88FFFFFF);
                        context.drawTextWithShadow(textRenderer, "Y", x + 167, yPos - 28, colorAlpha | 0x88FFFFFF);
                        context.drawTextWithShadow(textRenderer, "Z", x + 217, yPos - 28, colorAlpha | 0x88FFFFFF);
                    }
                }
                lIdx++;
            }
            boolean visible = w.getY() + w.getHeight() > y + 40 && w.getY() < y + currentH - 25;
            w.visible = visible;
            w.active = visible;

            if (visible) {
                float swell = (w.isHovered()) ? 1.05f : 1.0f;
                context.getMatrices().push(); context.getMatrices().translate(w.getX() + w.getWidth() / 2f, w.getY() + w.getHeight() / 2f, 0);
                context.getMatrices().scale(swell, swell, 1.0f); context.getMatrices().translate(-(w.getX() + w.getWidth() / 2f), -(w.getY() + w.getHeight() / 2f), 0);
                w.render(context, mouseX, mouseY, delta); context.getMatrices().pop();
            }
        }
        RenderSystem.disableScissor();
    }

    private void drawHandPreview(DrawContext context, int x, int y, int size) {
        ItemStack stack = client.player != null ? client.player.getMainHandStack() : new ItemStack(Items.IRON_SWORD);
        if (stack.isEmpty()) stack = new ItemStack(Items.IRON_SWORD);
        context.getMatrices().push(); context.getMatrices().translate(x, y, 250); context.getMatrices().scale(size, -size, size);
        
        float time = (System.currentTimeMillis() % 4000L) / 4000.0f;
        context.getMatrices().multiply(RotationAxis.POSITIVE_Y.rotationDegrees(time * 360f));
        
        // Apply hand position/scale/rotation from config for preview
        float px = GlassMenuClient.CONFIG.handPosX(); float py = GlassMenuClient.CONFIG.handPosY(); float pz = GlassMenuClient.CONFIG.handPosZ();
        float sx = GlassMenuClient.CONFIG.handScaleX(); float sy = GlassMenuClient.CONFIG.handScaleY(); float sz = GlassMenuClient.CONFIG.handScaleZ();
        float rx = GlassMenuClient.CONFIG.handRotX(); float ry = GlassMenuClient.CONFIG.handRotY(); float rz = GlassMenuClient.CONFIG.handRotZ();
        
        context.getMatrices().translate(px, py, pz);
        context.getMatrices().scale(sx, sy, sz);
        context.getMatrices().multiply(RotationAxis.POSITIVE_X.rotationDegrees(rx));
        context.getMatrices().multiply(RotationAxis.POSITIVE_Y.rotationDegrees(ry));
        context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rz));

        net.minecraft.client.render.DiffuseLighting.disableGuiDepthLighting();
        client.getItemRenderer().renderItem(stack, net.minecraft.client.render.model.json.ModelTransformationMode.FIRST_PERSON_RIGHT_HAND, 0xF000F0, net.minecraft.client.render.OverlayTexture.DEFAULT_UV, context.getMatrices(), context.getVertexConsumers(), client.world, 0);
        context.draw(); context.getMatrices().pop();
    }

    private void drawPlayerPreview(DrawContext context, int x, int y, int size, int mouseX, int mouseY) {
        MinecraftClient client = MinecraftClient.getInstance(); if (client.player == null) return;
        if (draggingPreview) { previewRotation += (float)(mouseX - lastDragMouseX) * 2.0f; lastDragMouseX = mouseX; } else lastDragMouseX = mouseX;
        context.draw(); Quaternionf rotation = new Quaternionf().rotateZ((float)Math.PI).rotateY(previewRotation * (float)Math.PI / 180f);
        InventoryScreen.drawEntity(context, (float)x, (float)y, (float)size, new Vector3f(), rotation, null, client.player);
        context.draw(); 
        RenderSystem.enableDepthTest();
        TargetESPManager.renderGuiTargetSight(context.getMatrices(), x, y, 1.0f, previewRotation);
        context.draw();
    }

    private float previewRotation = 0; private boolean draggingPreview = false; private double lastDragMouseX = 0;

    private void drawInputBox(DrawContext c, int x, int y, int w, int h) {
        RenderUtils.drawSdfRoundedRect(c.getMatrices(), x - 0.7f, y - 0.7f, w + 1.4f, h + 1.4f, 5.7f, 0x2AFFFFFF, 0);
        RenderUtils.drawSdfRoundedRect(c.getMatrices(), x, y, w, h, 5, 0x1F222226, 0);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (currentTab == Tab.POSITION) {
            if (super.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }

            int islandW = GlassMenuClient.CONFIG.capsuleWidth();
            int islandH = GlassMenuClient.CONFIG.capsuleHeight();
            int islandX = GlassMenuClient.CONFIG.islandX() == -1 ? (this.width - islandW) / 2 : GlassMenuClient.CONFIG.islandX();
            int islandY = GlassMenuClient.CONFIG.islandY();

            int invW = GlassMenuClient.CONFIG.inventoryHudWidth();
            int invH = GlassMenuClient.CONFIG.inventoryHudHeight();
            int invX = GlassMenuClient.CONFIG.inventoryHudX() == -1 ? 10 : GlassMenuClient.CONFIG.inventoryHudX();
            int invY = GlassMenuClient.CONFIG.inventoryHudY() == -1 ? this.height - invH - 10 : GlassMenuClient.CONFIG.inventoryHudY();

            int cardW = GlassMenuClient.CONFIG.playerCardWidth();
            int cardH = GlassMenuClient.CONFIG.playerCardHeight();
            int cardX = GlassMenuClient.CONFIG.playerCardX() == -1 ? (this.width - cardW) / 2 : GlassMenuClient.CONFIG.playerCardX();
            int cardY = GlassMenuClient.CONFIG.playerCardY() == -1 ? islandY + islandH + 8 : GlassMenuClient.CONFIG.playerCardY();

            int indW = GlassMenuClient.CONFIG.userIndicatorWidth();
            int indH = GlassMenuClient.CONFIG.userIndicatorHeight();
            int indX = GlassMenuClient.CONFIG.userIndicatorX() == -1 ? (this.width - indW) / 2 : GlassMenuClient.CONFIG.userIndicatorX();
            int indY = GlassMenuClient.CONFIG.userIndicatorY() == -1 ? 35 : GlassMenuClient.CONFIG.userIndicatorY();

            if (checkClickOrResize(PositionObject.ISLAND, islandX, islandY, islandW, islandH, mouseX, mouseY)) return true;
            if (checkClickOrResize(PositionObject.INVENTORY, invX, invY, invW, invH, mouseX, mouseY)) return true;
            if (checkClickOrResize(PositionObject.PLAYER, cardX, cardY, cardW, cardH, mouseX, mouseY)) return true;
            if (checkClickOrResize(PositionObject.INDICATOR, indX, indY, indW, indH, mouseX, mouseY)) return true;

            int armW = GlassMenuClient.CONFIG.armorHudWidth();
            int armH = GlassMenuClient.CONFIG.armorHudHeight();
            int armX = GlassMenuClient.CONFIG.armorHudX() == -1 ? (this.width - armW) / 2 : GlassMenuClient.CONFIG.armorHudX();
            int armY = GlassMenuClient.CONFIG.armorHudY() == -1 ? this.height - armH - 45 : GlassMenuClient.CONFIG.armorHudY();
            if (checkClickOrResize(PositionObject.ARMOR, armX, armY, armW, armH, mouseX, mouseY)) return true;

            int fiW = GlassMenuClient.CONFIG.fastItemWidth();
            int fiH = GlassMenuClient.CONFIG.fastItemHeight();
            int fiX = GlassMenuClient.CONFIG.fastItemX() == -1 ? (this.width - fiW) / 2 : GlassMenuClient.CONFIG.fastItemX();
            int fiY = GlassMenuClient.CONFIG.fastItemY() == -1 ? this.height - fiH - 10 : GlassMenuClient.CONFIG.fastItemY();
            if (checkClickOrResize(PositionObject.FAST_ITEM, fiX, fiY, fiW, fiH, mouseX, mouseY)) return true;

            int userHudWidthVal = GlassMenuClient.CONFIG.userHudWidth();
            int userHudHeightVal = GlassMenuClient.CONFIG.userHudHeight();
            int userHudX = GlassMenuClient.CONFIG.userHudX() == -1 ? (this.width - userHudWidthVal) / 2 : GlassMenuClient.CONFIG.userHudX();
            int userHudY = GlassMenuClient.CONFIG.userHudY() == -1 ? this.height - userHudHeightVal - 10 : GlassMenuClient.CONFIG.userHudY();
            if (checkClickOrResize(PositionObject.USER_HUD, userHudX, userHudY, userHudWidthVal, userHudHeightVal, mouseX, mouseY)) return true;

            int effSize_click = GlassMenuClient.CONFIG.effectsHudHeight();
            effSize_click = MathHelper.clamp(effSize_click, 18, 50);
            int effPadding_click = 3;
            int effGap_click = 4;
            int effBoxSize_click = effSize_click - effPadding_click * 2;
            int effLength_click = effPadding_click * 2 + 3 * (effBoxSize_click + effGap_click) - effGap_click;

            boolean vertical_click = GlassMenuClient.CONFIG.effectsHudVertical();
            int effW_click = vertical_click ? effSize_click : effLength_click;
            int effH_click = vertical_click ? effLength_click : effSize_click;

            int effX_click = GlassMenuClient.CONFIG.effectsHudX() == -1 ? this.width - effW_click - 10 : GlassMenuClient.CONFIG.effectsHudX();
            int effY_click = GlassMenuClient.CONFIG.effectsHudY() == -1 ? 10 : GlassMenuClient.CONFIG.effectsHudY();
            if (checkClickOrResize(PositionObject.EFFECTS, effX_click, effY_click, effW_click, effH_click, mouseX, mouseY)) return true;

            int leftW = GlassMenuClient.CONFIG.leftHandItemWidth();
            int leftH = GlassMenuClient.CONFIG.leftHandItemHeight();
            int leftX = GlassMenuClient.CONFIG.leftHandItemX() == -1 ? 10 : GlassMenuClient.CONFIG.leftHandItemX();
            int leftY = GlassMenuClient.CONFIG.leftHandItemY() == -1 ? (this.height - leftH) / 2 : GlassMenuClient.CONFIG.leftHandItemY();
            if (checkClickOrResize(PositionObject.LEFT_HAND_ITEM, leftX, leftY, leftW, leftH, mouseX, mouseY)) return true;

            selectedObject = PositionObject.NONE;
            return false;
        }

        int currentW = Math.round(MathHelper.lerp(panelWidthProgress, PANEL_W_NORMAL, PANEL_W_EXPANDED));
        int currentH = Math.round(MathHelper.lerp(panelHeightProgress, PANEL_H_NORMAL, PANEL_H_EXPANDED));
        int x = (this.width - currentW) / 2 + Math.round(panelXOffsetProgress); 
        int y = (this.height - currentH) / 2;
        if (mouseY >= y && mouseY <= y + 30) {
            Tab[] topTabs = { Tab.GENERAL, Tab.MOVEMENT, Tab.COMBAT, Tab.VISUALS };
            float tabW = (float)currentW / topTabs.length;
            for (int i = 0; i < topTabs.length; i++) {
                if (mouseX >= x + tabW * i && mouseX <= x + tabW * (i + 1)) {
                    if (currentTab != topTabs[i]) { currentTab = topTabs[i]; contentAlpha = 0.0f; updateVisibleWidgets(); }
                    return true;
                }
            }
        }
        if (currentTab == Tab.COMBAT && Math.abs(mouseX - (x - 80)) < 60 && Math.abs(mouseY - (y + 120)) < 100) {
            draggingPreview = true; lastDragMouseX = mouseX; return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (currentTab == Tab.POSITION && selectedObject != PositionObject.NONE) {
            if (isResizing) {
                int diffX = (int) (mouseX - dragOffsetX);
                int diffY = (int) (mouseY - dragOffsetY);

                int newW = initialWidth;
                int newH = initialHeight;
                int newX = initialX;
                int newY = initialY;

                if (resizeRight) {
                    newW = initialWidth + diffX;
                } else if (resizeLeft) {
                    newW = initialWidth - diffX;
                    newX = initialX + diffX;
                }

                if (resizeBottom) {
                    newH = initialHeight + diffY;
                } else if (resizeTop) {
                    newH = initialHeight - diffY;
                    newY = initialY + diffY;
                }

                if (selectedObject == PositionObject.ISLAND) {
                    newW = MathHelper.clamp(newW, 80, 300);
                    newH = MathHelper.clamp(newH, 10, 50);
                    if (resizeLeft) newX = initialX + (initialWidth - newW);
                    if (resizeTop) newY = initialY + (initialHeight - newH);

                    GlassMenuClient.CONFIG.capsuleWidth(newW);
                    GlassMenuClient.CONFIG.capsuleHeight(newH);
                    GlassMenuClient.CONFIG.islandX(newX);
                    GlassMenuClient.CONFIG.islandY(newY);
                } else if (selectedObject == PositionObject.INVENTORY) {
                    newW = MathHelper.clamp(newW, 150, 450);
                    newH = MathHelper.clamp(newH, 60, 250);
                    if (resizeLeft) newX = initialX + (initialWidth - newW);
                    if (resizeTop) newY = initialY + (initialHeight - newH);

                    GlassMenuClient.CONFIG.inventoryHudWidth(newW);
                    GlassMenuClient.CONFIG.inventoryHudHeight(newH);
                    GlassMenuClient.CONFIG.inventoryHudX(newX);
                    GlassMenuClient.CONFIG.inventoryHudY(newY);
                } else if (selectedObject == PositionObject.PLAYER) {
                    newW = MathHelper.clamp(newW, 70, 280);
                    newH = MathHelper.clamp(newH, 27, 108);
                    if (resizeLeft) newX = initialX + (initialWidth - newW);
                    if (resizeTop) newY = initialY + (initialHeight - newH);

                    GlassMenuClient.CONFIG.playerCardWidth(newW);
                    GlassMenuClient.CONFIG.playerCardHeight(newH);
                    GlassMenuClient.CONFIG.playerCardX(newX);
                    GlassMenuClient.CONFIG.playerCardY(newY);
                } else if (selectedObject == PositionObject.INDICATOR) {
                    newW = MathHelper.clamp(newW, 100, 350);
                    newH = MathHelper.clamp(newH, 15, 60);
                    if (resizeLeft) newX = initialX + (initialWidth - newW);
                    if (resizeTop) newY = initialY + (initialHeight - newH);

                    GlassMenuClient.CONFIG.userIndicatorWidth(newW);
                    GlassMenuClient.CONFIG.userIndicatorHeight(newH);
                    GlassMenuClient.CONFIG.userIndicatorX(newX);
                    GlassMenuClient.CONFIG.userIndicatorY(newY);
                } else if (selectedObject == PositionObject.ARMOR) {
                    boolean isVertical = GlassMenuClient.CONFIG.armorHudVertical();
                    int minW = isVertical ? 20 : 60;
                    int maxW = isVertical ? 80 : 350;
                    int minH = isVertical ? 60 : 20;
                    int maxH = isVertical ? 350 : 80;
                    newW = MathHelper.clamp(newW, minW, maxW);
                    newH = MathHelper.clamp(newH, minH, maxH);
                    if (resizeLeft) newX = initialX + (initialWidth - newW);
                    if (resizeTop) newY = initialY + (initialHeight - newH);

                    GlassMenuClient.CONFIG.armorHudWidth(newW);
                    GlassMenuClient.CONFIG.armorHudHeight(newH);
                    GlassMenuClient.CONFIG.armorHudX(newX);
                    GlassMenuClient.CONFIG.armorHudY(newY);
                } else if (selectedObject == PositionObject.FAST_ITEM) {
                    newW = MathHelper.clamp(newW, 60, 350);
                    newH = MathHelper.clamp(newH, 60, 350);
                    if (resizeLeft) newX = initialX + (initialWidth - newW);
                    if (resizeTop) newY = initialY + (initialHeight - newH);

                    GlassMenuClient.CONFIG.fastItemWidth(newW);
                    GlassMenuClient.CONFIG.fastItemHeight(newH);
                    GlassMenuClient.CONFIG.fastItemX(newX);
                    GlassMenuClient.CONFIG.fastItemY(newY);
                } else if (selectedObject == PositionObject.USER_HUD) {
                    newW = MathHelper.clamp(newW, 100, 350);
                    newH = MathHelper.clamp(newH, 15, 60);
                    if (resizeLeft) newX = initialX + (initialWidth - newW);
                    if (resizeTop) newY = initialY + (initialHeight - newH);

                    GlassMenuClient.CONFIG.userHudWidth(newW);
                    GlassMenuClient.CONFIG.userHudHeight(newH);
                    GlassMenuClient.CONFIG.userHudX(newX);
                    GlassMenuClient.CONFIG.userHudY(newY);
                } else if (selectedObject == PositionObject.EFFECTS) {
                    boolean isVert = GlassMenuClient.CONFIG.effectsHudVertical();
                    int newSize = isVert ? newW : newH;
                    newSize = MathHelper.clamp(newSize, 18, 50);

                    int padding_drag = 3;
                    int gap_drag = 4;
                    int boxSize_drag = newSize - padding_drag * 2;
                    int length_drag = padding_drag * 2 + 3 * (boxSize_drag + gap_drag) - gap_drag;

                    int finalW = isVert ? newSize : length_drag;
                    int finalH = isVert ? length_drag : newSize;

                    if (resizeLeft) newX = initialX + (initialWidth - finalW);
                    if (resizeTop) newY = initialY + (initialHeight - finalH);

                    GlassMenuClient.CONFIG.effectsHudHeight(newSize);
                    GlassMenuClient.CONFIG.effectsHudWidth(length_drag);
                    GlassMenuClient.CONFIG.effectsHudX(newX);
                    GlassMenuClient.CONFIG.effectsHudY(newY);
                } else if (selectedObject == PositionObject.LEFT_HAND_ITEM) {
                    newW = MathHelper.clamp(newW, 20, 200);
                    newH = MathHelper.clamp(newH, 20, 200);
                    if (resizeLeft) newX = initialX + (initialWidth - newW);
                    if (resizeTop) newY = initialY + (initialHeight - newH);

                    GlassMenuClient.CONFIG.leftHandItemWidth(newW);
                    GlassMenuClient.CONFIG.leftHandItemHeight(newH);
                    GlassMenuClient.CONFIG.leftHandItemX(newX);
                    GlassMenuClient.CONFIG.leftHandItemY(newY);
                }
                GlassMenuClient.CONFIG.save();
                return true;
            } else if (isDraggingObject) {
                int newX = (int) (mouseX - dragOffsetX);
                int newY = (int) (mouseY - dragOffsetY);

                if (selectedObject == PositionObject.ISLAND) {
                    int islandW = GlassMenuClient.CONFIG.capsuleWidth();
                    int islandH = GlassMenuClient.CONFIG.capsuleHeight();
                    newX = MathHelper.clamp(newX, 0, this.width - islandW);
                    newY = MathHelper.clamp(newY, 0, this.height - islandH);
                    GlassMenuClient.CONFIG.islandX(newX);
                    GlassMenuClient.CONFIG.islandY(newY);
                } else if (selectedObject == PositionObject.INVENTORY) {
                    int invW = GlassMenuClient.CONFIG.inventoryHudWidth();
                    int invH = GlassMenuClient.CONFIG.inventoryHudHeight();
                    newX = MathHelper.clamp(newX, 0, this.width - invW);
                    newY = MathHelper.clamp(newY, 0, this.height - invH);
                    GlassMenuClient.CONFIG.inventoryHudX(newX);
                    GlassMenuClient.CONFIG.inventoryHudY(newY);
                } else if (selectedObject == PositionObject.PLAYER) {
                    int cardW = GlassMenuClient.CONFIG.playerCardWidth();
                    int cardH = GlassMenuClient.CONFIG.playerCardHeight();
                    newX = MathHelper.clamp(newX, 0, this.width - cardW);
                    newY = MathHelper.clamp(newY, 0, this.height - cardH);
                    GlassMenuClient.CONFIG.playerCardX(newX);
                    GlassMenuClient.CONFIG.playerCardY(newY);
                } else if (selectedObject == PositionObject.INDICATOR) {
                    int indW = GlassMenuClient.CONFIG.userIndicatorWidth();
                    int indH = GlassMenuClient.CONFIG.userIndicatorHeight();
                    newX = MathHelper.clamp(newX, 0, this.width - indW);
                    newY = MathHelper.clamp(newY, 0, this.height - indH);
                    GlassMenuClient.CONFIG.userIndicatorX(newX);
                    GlassMenuClient.CONFIG.userIndicatorY(newY);
                } else if (selectedObject == PositionObject.ARMOR) {
                    int armW = GlassMenuClient.CONFIG.armorHudWidth();
                    int armH = GlassMenuClient.CONFIG.armorHudHeight();
                    newX = MathHelper.clamp(newX, 0, this.width - armW);
                    newY = MathHelper.clamp(newY, 0, this.height - armH);
                    GlassMenuClient.CONFIG.armorHudX(newX);
                    GlassMenuClient.CONFIG.armorHudY(newY);
                } else if (selectedObject == PositionObject.FAST_ITEM) {
                    int fastW = GlassMenuClient.CONFIG.fastItemWidth();
                    int fastH = GlassMenuClient.CONFIG.fastItemHeight();
                    newX = MathHelper.clamp(newX, 0, this.width - fastW);
                    newY = MathHelper.clamp(newY, 0, this.height - fastH);
                    GlassMenuClient.CONFIG.fastItemX(newX);
                    GlassMenuClient.CONFIG.fastItemY(newY);
                } else if (selectedObject == PositionObject.USER_HUD) {
                    int userW = GlassMenuClient.CONFIG.userHudWidth();
                    int userH = GlassMenuClient.CONFIG.userHudHeight();
                    newX = MathHelper.clamp(newX, 0, this.width - userW);
                    newY = MathHelper.clamp(newY, 0, this.height - userH);
                    GlassMenuClient.CONFIG.userHudX(newX);
                    GlassMenuClient.CONFIG.userHudY(newY);
                } else if (selectedObject == PositionObject.EFFECTS) {
                    boolean isVert = GlassMenuClient.CONFIG.effectsHudVertical();
                    int effSize_drag = GlassMenuClient.CONFIG.effectsHudHeight();
                    effSize_drag = MathHelper.clamp(effSize_drag, 18, 50);
                    int padding_d = 3;
                    int gap_d = 4;
                    int boxSize_d = effSize_drag - padding_d * 2;
                    int length_d = padding_d * 2 + 3 * (boxSize_d + gap_d) - gap_d;

                    int effW_drag = isVert ? effSize_drag : length_d;
                    int effH_drag = isVert ? length_d : effSize_drag;

                    newX = MathHelper.clamp(newX, 0, this.width - effW_drag);
                    newY = MathHelper.clamp(newY, 0, this.height - effH_drag);
                    GlassMenuClient.CONFIG.effectsHudX(newX);
                    GlassMenuClient.CONFIG.effectsHudY(newY);
                } else if (selectedObject == PositionObject.LEFT_HAND_ITEM) {
                    int leftW = GlassMenuClient.CONFIG.leftHandItemWidth();
                    int leftH = GlassMenuClient.CONFIG.leftHandItemHeight();
                    newX = MathHelper.clamp(newX, 0, this.width - leftW);
                    newY = MathHelper.clamp(newY, 0, this.height - leftH);
                    GlassMenuClient.CONFIG.leftHandItemX(newX);
                    GlassMenuClient.CONFIG.leftHandItemY(newY);
                }
                GlassMenuClient.CONFIG.save();
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override public boolean mouseReleased(double mX, double mY, int b) { 
        draggingPreview = false; 
        if (currentTab == Tab.POSITION) {
            isDraggingObject = false;
            isResizing = false;
        }
        return super.mouseReleased(mX, mY, b); 
    }
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (currentTab == Tab.MOVEMENT || currentTab == Tab.VISUALS) {
            scrollY = MathHelper.clamp(scrollY - verticalAmount * 20, 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    @Override public void renderBackground(DrawContext c, int mX, int mY, float d) {}
    @Override public boolean shouldPause() { return false; }

    @Override
    public void close() {
        if (this.effectView != null) {
            this.effectView.disableBlur();
        }
        super.close();
    }

    @Override
    public void removed() {
        if (this.effectView != null) {
            this.effectView.disableBlur();
        }
        super.removed();
    }

    private void initVisualsIslandTab(float x, float y) {
        LiquidGlassButton backBtn = new LiquidGlassButton((int)x + 40, (int)y + 210, 80, 22, Text.literal("Back"), b -> {
            currentTab = Tab.VISUALS; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        visualsIslandWidgets.add(backBtn);

        LiquidGlassSwitch islandToggle = new LiquidGlassSwitch((int)x + 370, (int)y + 45, 40, 20, GlassMenuClient.CONFIG.enableDynamicIsland());
        islandToggle.setOnToggle(enabled -> {
            GlassMenuClient.CONFIG.enableDynamicIsland(enabled); GlassMenuClient.CONFIG.save();
        });
        visualsIslandWidgets.add(islandToggle);

        String btnText = GlassMenuClient.CONFIG.transparentIsland() ? "Glass Effect: ON" : "Glass Effect: OFF";
        LiquidGlassButton transparentBtn = new LiquidGlassButton((int)x + 40, (int)y + 80, 120, 22, Text.literal(btnText), b -> {
            boolean current = GlassMenuClient.CONFIG.transparentIsland();
            GlassMenuClient.CONFIG.transparentIsland(!current);
            GlassMenuClient.CONFIG.save();
            b.setMessage(Text.literal(GlassMenuClient.CONFIG.transparentIsland() ? "Glass Effect: ON" : "Glass Effect: OFF"));
        });
        visualsIslandWidgets.add(transparentBtn);
    }

    private void renderVisualsIslandTab(DrawContext context, int x, int y, int mouseX, int mouseY, float delta) {
        int alphaInt = (int)(255 * contentAlpha); int colorAlpha = alphaInt << 24;
        float slideOffset = (1.0f - contentAlpha) * 12f;

        context.drawTextWithShadow(textRenderer, "Enable Dynamic Island", x + 40, y + 50 - (int)slideOffset, colorAlpha | 0xAAAAAA);

        for (ClickableWidget w : visualsIslandWidgets) {
            w.setAlpha(contentAlpha);
            if (w instanceof LiquidGlassButton lgb && lgb.getMessage().getString().equals("Back")) {
                w.setX(x + 40); w.setY((int)y + 205 - (int)slideOffset);
            } else if (w instanceof LiquidGlassButton lgb && lgb.getMessage().getString().startsWith("Glass Effect")) {
                w.setX(x + 40); w.setY((int)y + 80 - (int)slideOffset);
            } else {
                w.setX(x + 370); w.setY((int)y + 45 - (int)slideOffset);
            }
        }
    }

    private void initVisualsHitTab(float x, float y) {
        LiquidGlassButton backBtn = new LiquidGlassButton((int)x + 40, (int)y + 215, 80, 22, Text.literal("Back"), b -> {
            currentTab = Tab.VISUALS; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        visualsHitWidgets.add(backBtn);

        LiquidGlassSwitch customHitToggle = new LiquidGlassSwitch((int)x + 330, (int)y + 45, 40, 20, GlassMenuClient.CONFIG.enableCustomHit());
        customHitToggle.setOnToggle(enabled -> {
            GlassMenuClient.CONFIG.enableCustomHit(enabled); GlassMenuClient.CONFIG.save();
        });
        visualsHitWidgets.add(customHitToggle);

        LiquidGlassSwitch rgbToggle = new LiquidGlassSwitch((int)x + 330, (int)y + 80, 40, 20, GlassMenuClient.CONFIG.customHitRgb());
        rgbToggle.setOnToggle(enabled -> {
            GlassMenuClient.CONFIG.customHitRgb(enabled); GlassMenuClient.CONFIG.save();
        });
        visualsHitWidgets.add(rgbToggle);

        int currentColor = GlassMenuClient.CONFIG.customHitColor();
        hitSliderR = new LiquidGlassSlider((int)x + 230, (int)y + 118, 140, 16, ((currentColor >> 16) & 0xFF) / 255f);
        hitSliderG = new LiquidGlassSlider((int)x + 230, (int)y + 148, 140, 16, ((currentColor >> 8) & 0xFF) / 255f);
        hitSliderB = new LiquidGlassSlider((int)x + 230, (int)y + 178, 140, 16, (currentColor & 0xFF) / 255f);

        Runnable updateColor = () -> {
            if (isUpdating) return; isUpdating = true;
            int r = (int)(hitSliderR.getValue() * 255), g = (int)(hitSliderG.getValue() * 255), b = (int)(hitSliderB.getValue() * 255);
            int color = 0xFF000000 | (r << 16) | (g << 8) | b;
            GlassMenuClient.CONFIG.customHitColor(color); GlassMenuClient.CONFIG.save();
            if (hitHexInput != null) hitHexInput.setText(String.format("#%06X", color & 0xFFFFFF));
            if (hitRgbInput != null) hitRgbInput.setText(String.format("%d, %d, %d", r, g, b));
            isUpdating = false;
        };

        hitSliderR.setOnValueChange(v -> updateColor.run());
        hitSliderG.setOnValueChange(v -> updateColor.run());
        hitSliderB.setOnValueChange(v -> updateColor.run());
        
        visualsHitWidgets.add(hitSliderR);
        visualsHitWidgets.add(hitSliderG);
        visualsHitWidgets.add(hitSliderB);

        hitHexInput = createColorTextField((int)x + 110, (int)y + 112, 60);
        hitHexInput.setText(String.format("#%06X", currentColor & 0xFFFFFF));
        
        hitHexInput.setChangedListener(text -> {
            if (isUpdating) return;
            try {
                String clean = text.trim();
                if (clean.startsWith("#")) clean = clean.substring(1);
                if (clean.length() == 6) {
                    int color = Integer.parseInt(clean, 16);
                    isUpdating = true;
                    int r = (color >> 16) & 0xFF;
                    int g = (color >> 8) & 0xFF;
                    int b = color & 0xFF;
                    hitSliderR.setValue(r / 255.0f);
                    hitSliderG.setValue(g / 255.0f);
                    hitSliderB.setValue(b / 255.0f);
                    GlassMenuClient.CONFIG.customHitColor(0xFF000000 | color);
                    GlassMenuClient.CONFIG.save();
                    if (hitRgbInput != null) hitRgbInput.setText(String.format("%d, %d, %d", r, g, b));
                    isUpdating = false;
                }
            } catch (NumberFormatException ignored) {}
        });

        hitRgbInput = createColorTextField((int)x + 110, (int)y + 142, 100);
        hitRgbInput.setText(String.format("%d, %d, %d", (currentColor >> 16) & 0xFF, (currentColor >> 8) & 0xFF, currentColor & 0xFF));

        hitRgbInput.setChangedListener(text -> {
            if (isUpdating) return;
            try {
                String[] parts = text.split(",");
                if (parts.length == 3) {
                    int r = Integer.parseInt(parts[0].trim());
                    int g = Integer.parseInt(parts[1].trim());
                    int b = Integer.parseInt(parts[2].trim());
                    if (r >= 0 && r <= 255 && g >= 0 && g <= 255 && b >= 0 && b <= 255) {
                        isUpdating = true;
                        hitSliderR.setValue(r / 255.0f);
                        hitSliderG.setValue(g / 255.0f);
                        hitSliderB.setValue(b / 255.0f);
                        int color = 0xFF000000 | (r << 16) | (g << 8) | b;
                        GlassMenuClient.CONFIG.customHitColor(color);
                        GlassMenuClient.CONFIG.save();
                        if (hitHexInput != null) hitHexInput.setText(String.format("#%06X", color & 0xFFFFFF));
                        isUpdating = false;
                    }
                }
            } catch (NumberFormatException ignored) {}
        });

        // Count slider: 1–30 stars, stored as int
        float countNorm = (GlassMenuClient.CONFIG.customHitCount() - 1) / 29.0f;
        hitSliderCount = new LiquidGlassSlider((int)x + 230, (int)y + 213, 140, 16, countNorm);
        hitSliderCount.setOnValueChange(v -> {
            int count = 1 + (int) Math.round(v * 29);
            GlassMenuClient.CONFIG.customHitCount(count);
            GlassMenuClient.CONFIG.save();
        });
        visualsHitWidgets.add(hitSliderCount);
    }

    private void renderVisualsHitTab(DrawContext context, int x, int y, int mouseX, int mouseY, float delta) {
        int alphaInt = (int)(255 * contentAlpha); int colorAlpha = alphaInt << 24;
        float slideOffset = (1.0f - contentAlpha) * 12f;

        context.drawTextWithShadow(textRenderer, "Hit Star Settings", x + 30, y + 25 - (int)slideOffset, colorAlpha | 0xFFFFFF);

        context.drawTextWithShadow(textRenderer, "Enable Custom Hit Stars", x + 40, y + 50 - (int)slideOffset, colorAlpha | 0xAAAAAA);
        context.drawTextWithShadow(textRenderer, "RGB / Rainbow Effect", x + 40, y + 85 - (int)slideOffset, colorAlpha | 0xAAAAAA);

        boolean showColorPicker = !GlassMenuClient.CONFIG.customHitRgb();

        if (showColorPicker) {
            context.drawTextWithShadow(textRenderer, "HEX Color", x + 40, y + 115 - (int)slideOffset, colorAlpha | 0xAAAAAA);
            context.drawTextWithShadow(textRenderer, "RGB Value", x + 40, y + 145 - (int)slideOffset, colorAlpha | 0xAAAAAA);

            context.drawTextWithShadow(textRenderer, "Red", x + 230, y + 105 - (int)slideOffset, colorAlpha | 0xAAAAAA);
            context.drawTextWithShadow(textRenderer, "Green", x + 230, y + 135 - (int)slideOffset, colorAlpha | 0xAAAAAA);
            context.drawTextWithShadow(textRenderer, "Blue", x + 230, y + 165 - (int)slideOffset, colorAlpha | 0xAAAAAA);
        }

        for (ClickableWidget w : visualsHitWidgets) {
            w.setAlpha(contentAlpha);
            if (w instanceof LiquidGlassButton lgb && lgb.getMessage().getString().equals("Back")) {
                w.setX(x + 40); w.setY((int)y + 215 - (int)slideOffset);
            } else if (w == visualsHitWidgets.get(1)) {
                w.setX(x + 330); w.setY((int)y + 45 - (int)slideOffset);
            } else if (w == visualsHitWidgets.get(2)) {
                w.setX(x + 330); w.setY((int)y + 80 - (int)slideOffset);
            } else if (w == hitSliderCount) {
                w.setX(x + 230); w.setY((int)y + 213 - (int)slideOffset);
                w.visible = true; w.active = true;
            } else {
                w.visible = showColorPicker;
                w.active = showColorPicker;
                if (w == hitSliderR) {
                    w.setX(x + 230); w.setY((int)y + 118 - (int)slideOffset);
                } else if (w == hitSliderG) {
                    w.setX(x + 230); w.setY((int)y + 148 - (int)slideOffset);
                } else if (w == hitSliderB) {
                    w.setX(x + 230); w.setY((int)y + 178 - (int)slideOffset);
                }
            }
        }

        if (hitHexInput != null && hitRgbInput != null) {
            hitHexInput.visible = showColorPicker;
            hitHexInput.active = showColorPicker;
            hitRgbInput.visible = showColorPicker;
            hitRgbInput.active = showColorPicker;
            
            hitHexInput.setX(x + 110); hitHexInput.setY((int)y + 112 - (int)slideOffset);
            hitRgbInput.setX(x + 110); hitRgbInput.setY((int)y + 142 - (int)slideOffset);
        }

        // Count slider label (always visible)
        int countNow = GlassMenuClient.CONFIG.customHitCount();
        context.drawTextWithShadow(textRenderer, "Stars", x + 230, y + 200 - (int)slideOffset, colorAlpha | 0xAAAAAA);
        context.drawTextWithShadow(textRenderer, String.valueOf(countNow), x + 382, y + 200 - (int)slideOffset, colorAlpha | 0xFFFFFF);
    }
    // ─── AFTERIMAGE (Ghost Trail) ──────────────────────────────────────────────
    private void initVisualsGhostTab(float x, float y) {
        LiquidGlassButton backBtn = new LiquidGlassButton((int)x + 40, (int)y + 215, 80, 22, Text.literal("Back"), b -> {
            currentTab = Tab.VISUALS; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        visualsGhostWidgets.add(backBtn);

        // Enable toggle
        LiquidGlassSwitch enableToggle = new LiquidGlassSwitch((int)x + 330, (int)y + 45, 40, 20, GlassMenuClient.CONFIG.enableGhostTrail());
        enableToggle.setOnToggle(enabled -> { GlassMenuClient.CONFIG.enableGhostTrail(enabled); GlassMenuClient.CONFIG.save(); });
        visualsGhostWidgets.add(enableToggle);

        // RGB rainbow toggle
        LiquidGlassSwitch rgbToggle = new LiquidGlassSwitch((int)x + 330, (int)y + 80, 40, 20, GlassMenuClient.CONFIG.ghostTrailRgb());
        rgbToggle.setOnToggle(enabled -> { GlassMenuClient.CONFIG.ghostTrailRgb(enabled); GlassMenuClient.CONFIG.save(); });
        visualsGhostWidgets.add(rgbToggle);

        // RGB colour sliders
        int currentColor = GlassMenuClient.CONFIG.ghostTrailColor();
        ghostSliderR = new LiquidGlassSlider((int)x + 230, (int)y + 118, 140, 16, ((currentColor >> 16) & 0xFF) / 255f);
        ghostSliderG = new LiquidGlassSlider((int)x + 230, (int)y + 148, 140, 16, ((currentColor >>  8) & 0xFF) / 255f);
        ghostSliderB = new LiquidGlassSlider((int)x + 230, (int)y + 178, 140, 16, ( currentColor        & 0xFF) / 255f);

        Runnable updateColor = () -> {
            if (isUpdating) return; isUpdating = true;
            int r = (int)(ghostSliderR.getValue() * 255);
            int g = (int)(ghostSliderG.getValue() * 255);
            int b = (int)(ghostSliderB.getValue() * 255);
            int color = 0xFF000000 | (r << 16) | (g << 8) | b;
            GlassMenuClient.CONFIG.ghostTrailColor(color); GlassMenuClient.CONFIG.save();
            if (ghostHexInput != null) ghostHexInput.setText(String.format("#%06X", color & 0xFFFFFF));
            if (ghostRgbInput != null) ghostRgbInput.setText(String.format("%d, %d, %d", r, g, b));
            isUpdating = false;
        };
        ghostSliderR.setOnValueChange(v -> updateColor.run());
        ghostSliderG.setOnValueChange(v -> updateColor.run());
        ghostSliderB.setOnValueChange(v -> updateColor.run());
        visualsGhostWidgets.add(ghostSliderR);
        visualsGhostWidgets.add(ghostSliderG);
        visualsGhostWidgets.add(ghostSliderB);

        ghostHexInput = createColorTextField((int)x + 110, (int)y + 112, 60);
        ghostHexInput.setText(String.format("#%06X", currentColor & 0xFFFFFF));
        ghostHexInput.setChangedListener(text -> {
            if (isUpdating) return;
            try {
                String clean = text.trim(); if (clean.startsWith("#")) clean = clean.substring(1);
                if (clean.length() == 6) {
                    int color = Integer.parseInt(clean, 16); isUpdating = true;
                    int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
                    ghostSliderR.setValue(r / 255f); ghostSliderG.setValue(g / 255f); ghostSliderB.setValue(b / 255f);
                    GlassMenuClient.CONFIG.ghostTrailColor(0xFF000000 | color); GlassMenuClient.CONFIG.save();
                    if (ghostRgbInput != null) ghostRgbInput.setText(String.format("%d, %d, %d", r, g, b));
                    isUpdating = false;
                }
            } catch (NumberFormatException ignored) {}
        });

        ghostRgbInput = createColorTextField((int)x + 110, (int)y + 142, 100);
        ghostRgbInput.setText(String.format("%d, %d, %d", (currentColor>>16)&0xFF, (currentColor>>8)&0xFF, currentColor&0xFF));
        ghostRgbInput.setChangedListener(text -> {
            if (isUpdating) return;
            try {
                String[] parts = text.split(",");
                if (parts.length == 3) {
                    int r = Integer.parseInt(parts[0].trim()), g = Integer.parseInt(parts[1].trim()), b = Integer.parseInt(parts[2].trim());
                    if (r>=0&&r<=255&&g>=0&&g<=255&&b>=0&&b<=255) {
                        isUpdating = true;
                        ghostSliderR.setValue(r/255f); ghostSliderG.setValue(g/255f); ghostSliderB.setValue(b/255f);
                        int color = 0xFF000000|(r<<16)|(g<<8)|b;
                        GlassMenuClient.CONFIG.ghostTrailColor(color); GlassMenuClient.CONFIG.save();
                        if (ghostHexInput!=null) ghostHexInput.setText(String.format("#%06X",color&0xFFFFFF));
                        isUpdating = false;
                    }
                }
            } catch (NumberFormatException ignored) {}
        });
    }

    private void renderVisualsGhostTab(DrawContext context, int x, int y, int mouseX, int mouseY, float delta) {
        int alphaInt = (int)(255 * contentAlpha); int colorAlpha = alphaInt << 24;
        float slideOffset = (1f - contentAlpha) * 18f;

        // Panel title
        context.drawCenteredTextWithShadow(textRenderer, "Afterimage Trail", x + 200, y + 20 - (int)slideOffset, colorAlpha | 0xFFFFFF);

        // Enable row
        context.drawTextWithShadow(textRenderer, "Enable",  x + 230, y + 50  - (int)slideOffset, colorAlpha | 0xAAAAAA);
        // RGB row
        context.drawTextWithShadow(textRenderer, "Rainbow", x + 230, y + 85  - (int)slideOffset, colorAlpha | 0xAAAAAA);

        // Colour picker section header
        boolean rgbOn = GlassMenuClient.CONFIG.ghostTrailRgb();
        boolean showColorPicker = !rgbOn;
        if (showColorPicker) {
            context.drawTextWithShadow(textRenderer, "Color",  x + 110,  y + 98 - (int)slideOffset, colorAlpha | 0xAAAAAA);
            context.drawTextWithShadow(textRenderer, "Red",    x + 230,  y + 108 - (int)slideOffset, colorAlpha | 0xAAAAAA);
            context.drawTextWithShadow(textRenderer, "Green",  x + 230,  y + 138 - (int)slideOffset, colorAlpha | 0xAAAAAA);
            context.drawTextWithShadow(textRenderer, "Blue",   x + 230,  y + 168 - (int)slideOffset, colorAlpha | 0xAAAAAA);
        }

        // Reposition interactive widgets
        if (ghostSliderR != null) { ghostSliderR.setY((int)y + 118 - (int)slideOffset); ghostSliderR.visible = showColorPicker; ghostSliderR.active = showColorPicker; }
        if (ghostSliderG != null) { ghostSliderG.setY((int)y + 148 - (int)slideOffset); ghostSliderG.visible = showColorPicker; ghostSliderG.active = showColorPicker; }
        if (ghostSliderB != null) { ghostSliderB.setY((int)y + 178 - (int)slideOffset); ghostSliderB.visible = showColorPicker; ghostSliderB.active = showColorPicker; }

        if (ghostHexInput != null) {
            ghostHexInput.visible = showColorPicker; ghostHexInput.active = showColorPicker;
            ghostHexInput.setX(x + 110); ghostHexInput.setY((int)y + 112 - (int)slideOffset);
        }
        if (ghostRgbInput != null) {
            ghostRgbInput.visible = showColorPicker; ghostRgbInput.active = showColorPicker;
            ghostRgbInput.setX(x + 110); ghostRgbInput.setY((int)y + 142 - (int)slideOffset);
        }

        // Register text fields (need to be added every render frame like other tabs)
        if (showColorPicker) {
            if (ghostHexInput != null && !children().contains(ghostHexInput)) {
                addDrawableChild(ghostHexInput); addSelectableChild(ghostHexInput);
            }
            if (ghostRgbInput != null && !children().contains(ghostRgbInput)) {
                addDrawableChild(ghostRgbInput); addSelectableChild(ghostRgbInput);
            }
        }
    }

    private int interpolateColor(int c1, int c2, float p) {
        int a = (int) MathHelper.lerp(p, (c1 >> 24) & 0xFF, (c2 >> 24) & 0xFF);
        int r = (int) MathHelper.lerp(p, (c1 >> 16) & 0xFF, (c2 >> 16) & 0xFF);
        int g = (int) MathHelper.lerp(p, (c1 >> 8) & 0xFF, (c2 >> 8) & 0xFF);
        int b = (int) MathHelper.lerp(p, c1 & 0xFF, c2 & 0xFF);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}

