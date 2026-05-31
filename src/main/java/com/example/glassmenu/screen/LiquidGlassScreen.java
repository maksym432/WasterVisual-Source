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
    
    private enum Tab { GENERAL, MOVEMENT, COMBAT, VISUALS, VISUALS_JUMP, VISUALS_INV_HUD, VISUALS_PLAYER_CARD, VISUALS_BEDWARS, VISUALS_INDICATOR, VISUALS_ARMOR_HUD, VISUALS_FAST_ITEM, VISUALS_USER_HUD, POSITION, BRIDGE }
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
    private final List<ClickableWidget> positionWidgets = new ArrayList<>();
    private final List<ClickableWidget> bridgeWidgets = new ArrayList<>();

    // UI Constants
    private static final float PANEL_W_NORMAL = 340f;
    private static final float PANEL_W_EXPANDED = 420f; 
    private static final float PANEL_H_NORMAL = 220f;
    private static final float PANEL_H_EXPANDED = 280f;

    private float getPanelW() { return (currentTab == Tab.MOVEMENT || currentTab == Tab.GENERAL || currentTab == Tab.VISUALS || currentTab == Tab.VISUALS_JUMP || currentTab == Tab.VISUALS_INV_HUD || currentTab == Tab.VISUALS_PLAYER_CARD || currentTab == Tab.VISUALS_BEDWARS || currentTab == Tab.VISUALS_INDICATOR || currentTab == Tab.VISUALS_ARMOR_HUD || currentTab == Tab.VISUALS_FAST_ITEM || currentTab == Tab.VISUALS_USER_HUD || currentTab == Tab.BRIDGE) ? PANEL_W_EXPANDED : PANEL_W_NORMAL; }
    private float getPanelH() { return (currentTab == Tab.MOVEMENT || currentTab == Tab.GENERAL || currentTab == Tab.VISUALS || currentTab == Tab.VISUALS_JUMP || currentTab == Tab.VISUALS_INV_HUD || currentTab == Tab.VISUALS_PLAYER_CARD || currentTab == Tab.VISUALS_BEDWARS || currentTab == Tab.VISUALS_INDICATOR || currentTab == Tab.VISUALS_ARMOR_HUD || currentTab == Tab.VISUALS_FAST_ITEM || currentTab == Tab.VISUALS_USER_HUD || currentTab == Tab.BRIDGE) ? PANEL_H_EXPANDED : PANEL_H_NORMAL; }

    // State
    private TextFieldWidget hexInput, rgbInput, visHexInput, visRgbInput, invHexInput, invRgbInput, bridgeHexInput, bridgeRgbInput;
    private LiquidGlassSlider sliderR, sliderG, sliderB, visSliderR, visSliderG, visSliderB, invSliderR, invSliderG, invSliderB, bridgeSliderR, bridgeSliderG, bridgeSliderB, cardSliderR, cardSliderG, cardSliderB, indSliderR, indSliderG, indSliderB, armSliderR, armSliderG, armSliderB, fastItemSliderR, fastItemSliderG, fastItemSliderB, userHudSliderR, userHudSliderG, userHudSliderB;
    private double scrollY = 0;
    private double maxScroll = 0;
    private boolean isUpdating = false;

    // Position Editor State
    private enum PositionObject { NONE, ISLAND, INVENTORY, PLAYER, INDICATOR, ARMOR, FAST_ITEM, USER_HUD }
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
        this.effectView.enableBlur();
        panelWidthProgress = (currentTab == Tab.MOVEMENT || currentTab == Tab.GENERAL || currentTab == Tab.VISUALS || currentTab == Tab.VISUALS_JUMP || currentTab == Tab.VISUALS_INV_HUD || currentTab == Tab.VISUALS_PLAYER_CARD || currentTab == Tab.VISUALS_BEDWARS || currentTab == Tab.VISUALS_INDICATOR || currentTab == Tab.VISUALS_ARMOR_HUD || currentTab == Tab.VISUALS_FAST_ITEM || currentTab == Tab.VISUALS_USER_HUD || currentTab == Tab.BRIDGE) ? 1.0f : 0.0f;
        panelHeightProgress = (currentTab == Tab.MOVEMENT || currentTab == Tab.GENERAL || currentTab == Tab.VISUALS || currentTab == Tab.VISUALS_JUMP || currentTab == Tab.VISUALS_INV_HUD || currentTab == Tab.VISUALS_PLAYER_CARD || currentTab == Tab.VISUALS_BEDWARS || currentTab == Tab.VISUALS_INDICATOR || currentTab == Tab.VISUALS_ARMOR_HUD || currentTab == Tab.VISUALS_FAST_ITEM || currentTab == Tab.VISUALS_USER_HUD || currentTab == Tab.BRIDGE) ? 1.0f : 0.0f;
        contentAlpha = 1.0f;

        float x = (this.width - getPanelW()) / 2f;
        float y = (this.height - getPanelH()) / 2f;
        this.lensView = new LiquidLensView(x, y, getPanelW(), getPanelH());

        // 1. Initialize Lists once per screen init (resize/open)
        generalWidgets.clear(); movementWidgets.clear(); combatWidgets.clear(); visualsWidgets.clear(); visualsJumpWidgets.clear(); visualsInvHudWidgets.clear(); visualsPlayerCardWidgets.clear(); visualsBedWarsWidgets.clear(); visualsIndicatorWidgets.clear(); visualsArmorHudWidgets.clear(); visualsFastItemWidgets.clear(); visualsUserHudWidgets.clear(); positionWidgets.clear(); bridgeWidgets.clear();
        
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
        LiquidGlassButton fastItemBtn = new LiquidGlassButton(0, 0, 160, 22, Text.literal("Fast Item Wheel"), b -> {
            currentTab = Tab.VISUALS_FAST_ITEM; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        LiquidGlassButton userHudBtn = new LiquidGlassButton(0, 0, 160, 22, Text.literal("User HUD"), b -> {
            currentTab = Tab.VISUALS_USER_HUD; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        LiquidGlassButton positionBtn = new LiquidGlassButton(0, 0, 160, 22, Text.literal("Position Editor"), b -> {
            currentTab = Tab.POSITION; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        visualsWidgets.add(jumpEffectsBtn);
        visualsWidgets.add(invHudBtn);
        visualsWidgets.add(playerCardBtn);
        visualsWidgets.add(bedwarsBtn);
        visualsWidgets.add(indicatorBtn);
        visualsWidgets.add(armorHudBtn);
        visualsWidgets.add(fastItemBtn);
        visualsWidgets.add(userHudBtn);
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
            LiquidGlassButton btn = new LiquidGlassButton((int)x + 40, (int)y + 90 + i * 28, 80, 22, Text.literal(modeLabels[i]), b -> {
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

        String btnText = GlassMenuClient.CONFIG.transparentBackground() ? "Transparent: ON" : "Transparent: OFF";
        LiquidGlassButton transparentBtn = new LiquidGlassButton((int)x + 40, (int)y + 50, 120, 22, Text.literal(btnText), b -> {
            boolean current = GlassMenuClient.CONFIG.transparentBackground();
            GlassMenuClient.CONFIG.transparentBackground(!current);
            GlassMenuClient.CONFIG.save();
            b.setMessage(Text.literal(GlassMenuClient.CONFIG.transparentBackground() ? "Transparent: ON" : "Transparent: OFF"));
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

        String btnText = GlassMenuClient.CONFIG.transparentPlayerCard() ? "Transparent: ON" : "Transparent: OFF";
        LiquidGlassButton transparentBtn = new LiquidGlassButton((int)x + 40, (int)y + 80, 120, 22, Text.literal(btnText), b -> {
            boolean current = GlassMenuClient.CONFIG.transparentPlayerCard();
            GlassMenuClient.CONFIG.transparentPlayerCard(!current);
            GlassMenuClient.CONFIG.save();
            b.setMessage(Text.literal(GlassMenuClient.CONFIG.transparentPlayerCard() ? "Transparent: ON" : "Transparent: OFF"));
        });
        visualsPlayerCardWidgets.add(transparentBtn);
    }

    private void initVisualsBedWarsTab(float x, float y) {
        LiquidGlassButton backBtn = new LiquidGlassButton((int)x + 40, (int)y + 240, 80, 22, Text.literal("Back"), b -> {
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

        String btnText = GlassMenuClient.CONFIG.transparentUserIndicator() ? "Transparent: ON" : "Transparent: OFF";
        LiquidGlassButton transparentBtn = new LiquidGlassButton((int)x + 40, (int)y + 80, 120, 22, Text.literal(btnText), b -> {
            boolean current = GlassMenuClient.CONFIG.transparentUserIndicator();
            GlassMenuClient.CONFIG.transparentUserIndicator(!current);
            GlassMenuClient.CONFIG.save();
            b.setMessage(Text.literal(GlassMenuClient.CONFIG.transparentUserIndicator() ? "Transparent: ON" : "Transparent: OFF"));
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

        String btnText = GlassMenuClient.CONFIG.transparentArmorHud() ? "Transparent: ON" : "Transparent: OFF";
        LiquidGlassButton transparentBtn = new LiquidGlassButton((int)x + 40, (int)y + 80, 120, 22, Text.literal(btnText), b -> {
            boolean current = GlassMenuClient.CONFIG.transparentArmorHud();
            GlassMenuClient.CONFIG.transparentArmorHud(!current);
            GlassMenuClient.CONFIG.save();
            b.setMessage(Text.literal(GlassMenuClient.CONFIG.transparentArmorHud() ? "Transparent: ON" : "Transparent: OFF"));
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

        String btnText = GlassMenuClient.CONFIG.transparentFastItem() ? "Transparent: ON" : "Transparent: OFF";
        LiquidGlassButton transparentBtn = new LiquidGlassButton((int)x + 40, (int)y + 80, 120, 22, Text.literal(btnText), b -> {
            boolean current = GlassMenuClient.CONFIG.transparentFastItem();
            GlassMenuClient.CONFIG.transparentFastItem(!current);
            GlassMenuClient.CONFIG.save();
            b.setMessage(Text.literal(GlassMenuClient.CONFIG.transparentFastItem() ? "Transparent: ON" : "Transparent: OFF"));
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

        String btnText = GlassMenuClient.CONFIG.transparentUserHud() ? "Transparent: ON" : "Transparent: OFF";
        LiquidGlassButton transparentBtn = new LiquidGlassButton((int)x + 40, (int)y + 80, 120, 22, Text.literal(btnText), b -> {
            boolean current = GlassMenuClient.CONFIG.transparentUserHud();
            GlassMenuClient.CONFIG.transparentUserHud(!current);
            GlassMenuClient.CONFIG.save();
            b.setMessage(Text.literal(GlassMenuClient.CONFIG.transparentUserHud() ? "Transparent: ON" : "Transparent: OFF"));
        });
        visualsUserHudWidgets.add(transparentBtn);
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
        LiquidGlassSlider sX = new LiquidGlassSlider((int)x + 105, y + 25, 40, 16, vx); sX.setOnValueChange(ax); movementWidgets.add(sX);
        LiquidGlassSlider sY = new LiquidGlassSlider((int)x + 155, y - 10, 20, 60, vy); sY.setVertical(true); sY.setOnValueChange(ay); movementWidgets.add(sY);
        LiquidGlassSlider sZ = new LiquidGlassSlider((int)x + 185, y + 25, 40, 16, vz); sZ.setOnValueChange(az); movementWidgets.add(sZ);
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
            maxScroll = Math.max(0, (visualsWidgets.size() * 36) + 10 - (PANEL_H_EXPANDED - 65));
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
        
        float targetW = (currentTab == Tab.MOVEMENT || currentTab == Tab.GENERAL || currentTab == Tab.VISUALS || currentTab == Tab.VISUALS_JUMP || currentTab == Tab.VISUALS_INV_HUD || currentTab == Tab.VISUALS_PLAYER_CARD || currentTab == Tab.VISUALS_BEDWARS || currentTab == Tab.VISUALS_INDICATOR || currentTab == Tab.VISUALS_ARMOR_HUD || currentTab == Tab.VISUALS_FAST_ITEM || currentTab == Tab.VISUALS_USER_HUD || currentTab == Tab.BRIDGE) ? 1.0f : 0.0f;
        float targetH = (currentTab == Tab.MOVEMENT || currentTab == Tab.GENERAL || currentTab == Tab.VISUALS || currentTab == Tab.VISUALS_JUMP || currentTab == Tab.VISUALS_INV_HUD || currentTab == Tab.VISUALS_PLAYER_CARD || currentTab == Tab.VISUALS_BEDWARS || currentTab == Tab.VISUALS_INDICATOR || currentTab == Tab.VISUALS_ARMOR_HUD || currentTab == Tab.VISUALS_FAST_ITEM || currentTab == Tab.VISUALS_USER_HUD || currentTab == Tab.BRIDGE) ? 1.0f : 0.0f;
        float targetXOff = (currentTab == Tab.COMBAT) ? 40f : 0f;

        panelWidthProgress = MathHelper.lerp(dt * 10.0f, panelWidthProgress, targetW);
        panelHeightProgress = MathHelper.lerp(dt * 10.0f, panelHeightProgress, targetH);
        panelXOffsetProgress = MathHelper.lerp(dt * 10.0f, panelXOffsetProgress, targetXOff);
        contentAlpha = MathHelper.lerp(dt * 12.0f, contentAlpha, 1.0f);

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
                tabHoverProgress[i] = MathHelper.lerp(dt * 8.0f, tabHoverProgress[i], isHovered ? 1.0f : 0.0f);
                float swell = 1.0f + tabHoverProgress[i] * 0.12f;
                
                // Highlight GENERAL if we are in BRIDGE, or VISUALS if we are in VISUALS_JUMP/VISUALS_INV_HUD/VISUALS_PLAYER_CARD/POSITION
                boolean isActive = (topTabs[i] == currentTab) 
                    || (topTabs[i] == Tab.GENERAL && currentTab == Tab.BRIDGE)
                    || (topTabs[i] == Tab.VISUALS && (currentTab == Tab.VISUALS_JUMP || currentTab == Tab.VISUALS_INV_HUD || currentTab == Tab.VISUALS_PLAYER_CARD || currentTab == Tab.VISUALS_BEDWARS || currentTab == Tab.VISUALS_INDICATOR || currentTab == Tab.VISUALS_ARMOR_HUD || currentTab == Tab.VISUALS_FAST_ITEM || currentTab == Tab.VISUALS_USER_HUD || currentTab == Tab.POSITION));
                int color = ((int)(255 * contentAlpha) << 24) | ((isActive ? 0xFFFFFFFF : 0x88FFFFFF) & 0xFFFFFF);
                
                context.getMatrices().push(); context.getMatrices().translate(x + tabW * i + tabW / 2f, y + 15, 0);
                context.getMatrices().scale(swell, swell, 1.0f); context.drawCenteredTextWithShadow(textRenderer, topTabs[i].name(), 0, -5, color);
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
        else if (currentTab == Tab.POSITION) renderPositionTab(context, mouseX, mouseY, delta);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderVisualsTab(DrawContext context, int x, int y, int mouseX, int mouseY, float delta, int currentW, int currentH) {
        int alphaInt = (int)(255 * contentAlpha); int colorAlpha = alphaInt << 24;
        float slideOffset = (1.0f - contentAlpha) * 12f;

        context.drawTextWithShadow(textRenderer, "Visuals Settings", x + 30, y + 40 - (int)slideOffset, colorAlpha | 0xFFFFFF);

        double sc = MinecraftClient.getInstance().getWindow().getScaleFactor();
        int scissorY = (int)((this.height - y - currentH + 25) * sc);
        int scissorH = (int)((currentH - 65) * sc);
        RenderSystem.enableScissor((int)(x * sc), Math.max(0, scissorY), (int)(currentW * sc), Math.max(0, scissorH));

        int startY = y + 60 - (int)scrollY - (int)slideOffset;
        for (int i = 0; i < visualsWidgets.size(); i++) {
            ClickableWidget w = visualsWidgets.get(i);
            w.setAlpha(contentAlpha);
            w.setX(x + (currentW - 160) / 2);
            w.setY(startY + i * 36);

            if (w.getY() + w.getHeight() > y + 40 && w.getY() < y + currentH - 25) {
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
                w.setX(x + 40); w.setY((int)y + 240 - (int)slideOffset);
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
        context.drawTextWithShadow(textRenderer, "Mode", x + 40, y + 85 - (int)slideOffset, colorAlpha | 0xAAAAAA);

        int btnIdx = 0;
        for (ClickableWidget w : visualsJumpWidgets) {
            w.setAlpha(contentAlpha);
            if (w == visSliderR) { w.setX(x + 230); w.setY((int)y + 90 - (int)slideOffset); }
            else if (w == visSliderG) { w.setX(x + 230); w.setY((int)y + 125 - (int)slideOffset); }
            else if (w == visSliderB) { w.setX(x + 230); w.setY((int)y + 160 - (int)slideOffset); }
            else if (w instanceof LiquidGlassButton lgb && lgb.getMessage().getString().equals("Back")) {
                w.setX(x + 40); w.setY((int)y + 210 - (int)slideOffset);
            } else if (w instanceof LiquidGlassButton) {
                w.setX(x + 40); w.setY((int)y + 90 + (btnIdx++) * 28 - (int)slideOffset);
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
            } else if (w instanceof LiquidGlassButton lgb && lgb.getMessage().getString().startsWith("Transparent")) {
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
            } else if (w instanceof LiquidGlassButton lgb && lgb.getMessage().getString().startsWith("Transparent")) {
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
            } else if (w instanceof LiquidGlassButton lgb && lgb.getMessage().getString().startsWith("Transparent")) {
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
            } else if (w instanceof LiquidGlassButton lgb && lgb.getMessage().getString().startsWith("Transparent")) {
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
        context.drawTextWithShadow(textRenderer, "Enable Fast Item", x + 40, y + 50 - (int)slideOffset, colorAlpha | 0xAAAAAA);
        context.drawTextWithShadow(textRenderer, "Transparent", x + 40, y + 84 - (int)slideOffset, colorAlpha | 0xAAAAAA);
        context.drawTextWithShadow(textRenderer, "Item Slots",  x + 40, y + 116 - (int)slideOffset, colorAlpha | 0xAAAAAA);

        // Right column title (short enough to not overlap switch at x+370)
        context.drawTextWithShadow(textRenderer, "Fast Item", x + 230, y + 50 - (int)slideOffset, colorAlpha | 0xFFFFFF);

        for (ClickableWidget w : visualsFastItemWidgets) {
            w.setAlpha(contentAlpha);
            if (w == fastItemSliderR || w == fastItemSliderG || w == fastItemSliderB) {
                if (isTransparent) { w.setX(-9999); w.setY(-9999); }
                else if (w == fastItemSliderR) { w.setX(x + 230); w.setY((int)y + 85 - (int)slideOffset); }
                else if (w == fastItemSliderG) { w.setX(x + 230); w.setY((int)y + 118 - (int)slideOffset); }
                else if (w == fastItemSliderB) { w.setX(x + 230); w.setY((int)y + 151 - (int)slideOffset); }
            } else if (w instanceof LiquidGlassButton lgb && lgb.getMessage().getString().equals("Back")) {
                w.setX(x + 40); w.setY((int)y + 205 - (int)slideOffset);
            } else if (w instanceof LiquidGlassButton lgb && lgb.getMessage().getString().startsWith("Transparent")) {
                w.setX(x + 130); w.setY((int)y + 79 - (int)slideOffset);
            } else if (w instanceof LiquidGlassButton lgb && lgb.getMessage().getString().startsWith("Item Slots")) {
                w.setX(x + 130); w.setY((int)y + 111 - (int)slideOffset);
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
        context.drawTextWithShadow(textRenderer, "Transparent", x + 40, y + 84 - (int)slideOffset, colorAlpha | 0xAAAAAA);

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
            } else if (w instanceof LiquidGlassButton lgb && lgb.getMessage().getString().startsWith("Transparent")) {
                w.setX(x + 130); w.setY((int)y + 79 - (int)slideOffset);
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

        // Slot 3: XP (Green)
        String xpText = "12";
        int xpW = textRenderer.getWidth(xpText);
        context.drawText(textRenderer, xpText, 149 - xpW / 2, textY, 0xFF55FF55, false);

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
        int cardW = 160;
        int cardH = 38;
        int cardX = GlassMenuClient.CONFIG.playerCardX() == -1 ? (this.width - cardW) / 2 : GlassMenuClient.CONFIG.playerCardX();
        int cardY;
        if (GlassMenuClient.CONFIG.playerCardY() == -1) {
            cardY = islandY + islandH + 8;
        } else {
            cardY = GlassMenuClient.CONFIG.playerCardY();
        }

        int panelColor = GlassMenuClient.CONFIG.playerCardColor();
        int borderColor = 0x2AFFFFFF;

        if (!GlassMenuClient.CONFIG.transparentPlayerCard()) {
            RenderUtils.drawSdfRoundedOutline(context.getMatrices(), cardX, cardY, cardW, cardH, 8f, 0.8f, borderColor);
            RenderUtils.drawSdfRoundedRect(context.getMatrices(), cardX, cardY, cardW, cardH, 8f, panelColor, 0);
        } else {
            RenderUtils.drawSdfRoundedOutline(context.getMatrices(), cardX, cardY, cardW, cardH, 8f, 0.8f, 0x33FFFFFF);
        }
        context.draw(); // Flush background

        int slotOutlineColor = GlassMenuClient.CONFIG.transparentPlayerCard() ? 0x22FFFFFF : 0x1AFFFFFF;
        int slotFillColor = GlassMenuClient.CONFIG.transparentPlayerCard() ? 0x0F000000 : 0x12FFFFFF;

        // Head slot frame
        RenderUtils.drawSdfRoundedOutline(context.getMatrices(), cardX + 3f, cardY + 3f, 32f, 32f, 5f, 0.6f, slotOutlineColor);
        RenderUtils.drawSdfRoundedRect(context.getMatrices(), cardX + 3f, cardY + 3f, 32f, 32f, 5f, slotFillColor, 0);
        context.draw();

        if (client.player != null) {
            Identifier dummySkin = client.player.getSkinTextures().texture();
            PlayerSkinDrawer.draw(context, dummySkin, cardX + 4, cardY + 4, 30);
        }

        String hpText = "20.0 HP";
        int hpTextWidth = textRenderer.getWidth(hpText);
        int hpTextX = cardX + cardW - 8 - hpTextWidth;

        if (client.player != null) {
            String displayName = client.player.getGameProfile().getName();
            int maxNameWidth = hpTextX - (cardX + 38) - 6;
            if (textRenderer.getWidth(displayName) > maxNameWidth) {
                displayName = textRenderer.trimToWidth(displayName, maxNameWidth - 8) + "...";
            }
            context.drawTextWithShadow(textRenderer, displayName, cardX + 38, cardY + 6, 0xFFFFFFFF);
        }
        context.drawTextWithShadow(textRenderer, hpText, hpTextX, cardY + 6, 0xFFFF8888);
        
        // Health bar slot frame
        int healthFrameX = cardX + 37;
        int healthFrameY = cardY + 21;
        int healthFrameW = cardW - 37 - 7;
        int healthFrameH = 12;

        RenderUtils.drawSdfRoundedOutline(context.getMatrices(), healthFrameX, healthFrameY, healthFrameW, healthFrameH, 4f, 0.6f, slotOutlineColor);
        RenderUtils.drawSdfRoundedRect(context.getMatrices(), healthFrameX, healthFrameY, healthFrameW, healthFrameH, 4f, slotFillColor, 0);
        context.draw();

        // Inner health bar
        int barX = healthFrameX + 3;
        int barY = healthFrameY + 3;
        int barW = healthFrameW - 6;
        int barH = 6;
        RenderUtils.drawSdfRoundedRect(context.getMatrices(), barX, barY, barW, barH, 2f, 0xFF505050, 0);
        RenderUtils.drawSdfRoundedRect(context.getMatrices(), barX, barY, Math.round(barW * 0.8f), barH, 2f, 0xFFE03030, 0);
        context.draw();

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
            for (int i = 0; i < generalWidgets.size(); i++) {
                ClickableWidget w = generalWidgets.get(i);
                w.setAlpha(contentAlpha);
                if (w instanceof LiquidGlassButton lgb && lgb.getMessage().getString().equals("Bridge")) {
                    w.setX(x + 280); w.setY(y + 60 - (int)slideOffset);
                } else {
                    w.setX(x + 30); w.setY((int)y + 65 + i * 32 - (int)slideOffset);
                }
            }
            drawItemPreview(context, x + 260, y + 140 - (int)slideOffset, 60);
        } else if (currentTab == Tab.BRIDGE) {
            context.drawTextWithShadow(textRenderer, "Bridge Box", x + 30, y + 45 - (int)slideOffset, colorAlpha | 0xFFFFFF);
            for (ClickableWidget w : bridgeWidgets) {
                w.setAlpha(contentAlpha);
                if (w == bridgeSliderR) { w.setX(x + 30); w.setY((int)y + 90 - (int)slideOffset); }
                else if (w == bridgeSliderG) { w.setX(x + 30); w.setY((int)y + 125 - (int)slideOffset); }
                else if (w == bridgeSliderB) { w.setX(x + 30); w.setY((int)y + 160 - (int)slideOffset); }
                else if (w instanceof LiquidGlassButton lgb && lgb.getMessage().getString().equals("Back")) {
                    w.setX(x + 230); w.setY((int)y + 50 - (int)slideOffset);
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
            
            // Nudge first two widgets (Switch and Speed Slider) slightly right
            int xOffset = (isVert ? 165 : (i == 0 ? 160 : (i == 1 ? 110 : ( (i-2)%3 == 0 ? 105 : 195))));
            if (i >= 2 && !isVert) { // Position group sliders
                 xOffset = ( (i-2)%3 == 0 ? 105 : 195);
            } else if (i < 2) {
                 xOffset = (i == 0 ? 160 : 110);
            }
            
            w.setY(yPos + itemOffset); w.setX(x + xOffset);
            
            if (lIdx < labelMapping.length && i == labelMapping[lIdx]) {
                int labelY = (i < 2) ? w.getY() + 5 : yPos - 45;
                if (labelY > y + 40 && labelY < y + currentH - 25) {
                    context.drawTextWithShadow(textRenderer, labels[lIdx], x + 20, labelY, colorAlpha | 0xFFFFFF);
                    if (lIdx >= 2) {
                        context.drawTextWithShadow(textRenderer, "X", x + 125, yPos + 35, colorAlpha | 0x88FFFFFF);
                        context.drawTextWithShadow(textRenderer, "Y", x + 170, yPos + 35, colorAlpha | 0x88FFFFFF);
                        context.drawTextWithShadow(textRenderer, "Z", x + 215, yPos + 35, colorAlpha | 0x88FFFFFF);
                    }
                }
                lIdx++;
            }
            if (w.getY() + w.getHeight() > y + 40 && w.getY() < y + currentH - 25) {
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

            int cardW = 160;
            int cardH = 38;
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
                    newW = MathHelper.clamp(newW, 150, 350);
                    newH = MathHelper.clamp(newH, 56, 120);
                    if (resizeLeft) newX = initialX + (initialWidth - newW);
                    if (resizeTop) newY = initialY + (initialHeight - newH);

                    GlassMenuClient.CONFIG.userHudWidth(newW);
                    GlassMenuClient.CONFIG.userHudHeight(newH);
                    GlassMenuClient.CONFIG.userHudX(newX);
                    GlassMenuClient.CONFIG.userHudY(newY);
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
                    int cardW = 160;
                    int cardH = 38;
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
}
