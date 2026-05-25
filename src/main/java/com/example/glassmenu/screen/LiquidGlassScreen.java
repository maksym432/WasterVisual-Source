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
    
    private enum Tab { GENERAL, MOVEMENT, COMBAT, VISUALS, BRIDGE }
    private Tab currentTab = Tab.GENERAL;
    
    private final List<ClickableWidget> generalWidgets = new ArrayList<>();
    private final List<ClickableWidget> movementWidgets = new ArrayList<>();
    private final List<ClickableWidget> combatWidgets = new ArrayList<>();
    private final List<ClickableWidget> visualsWidgets = new ArrayList<>();
    private final List<ClickableWidget> bridgeWidgets = new ArrayList<>();

    // UI Constants
    private static final float PANEL_W_NORMAL = 340f;
    private static final float PANEL_W_EXPANDED = 420f; 
    private static final float PANEL_H_NORMAL = 220f;
    private static final float PANEL_H_EXPANDED = 280f;

    private float getPanelW() { return (currentTab == Tab.MOVEMENT || currentTab == Tab.GENERAL || currentTab == Tab.VISUALS || currentTab == Tab.BRIDGE) ? PANEL_W_EXPANDED : PANEL_W_NORMAL; }
    private float getPanelH() { return (currentTab == Tab.MOVEMENT || currentTab == Tab.GENERAL || currentTab == Tab.VISUALS || currentTab == Tab.BRIDGE) ? PANEL_H_EXPANDED : PANEL_H_NORMAL; }

    // State
    private TextFieldWidget hexInput, rgbInput, visHexInput, visRgbInput, bridgeHexInput, bridgeRgbInput;
    private LiquidGlassSlider sliderR, sliderG, sliderB, visSliderR, visSliderG, visSliderB, bridgeSliderR, bridgeSliderG, bridgeSliderB;
    private double scrollY = 0;
    private double maxScroll = 0;
    private boolean isUpdating = false;

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
        panelWidthProgress = (currentTab == Tab.MOVEMENT || currentTab == Tab.GENERAL || currentTab == Tab.VISUALS || currentTab == Tab.BRIDGE) ? 1.0f : 0.0f;
        panelHeightProgress = (currentTab == Tab.MOVEMENT || currentTab == Tab.GENERAL || currentTab == Tab.VISUALS || currentTab == Tab.BRIDGE) ? 1.0f : 0.0f;
        contentAlpha = 1.0f;

        float x = (this.width - getPanelW()) / 2f;
        float y = (this.height - getPanelH()) / 2f;
        this.lensView = new LiquidLensView(x, y, getPanelW(), getPanelH());

        // 1. Initialize Lists once per screen init (resize/open)
        generalWidgets.clear(); movementWidgets.clear(); combatWidgets.clear(); visualsWidgets.clear(); bridgeWidgets.clear();
        
        initGeneralTab(x, y);
        initCombatTab(x, y);
        initMovementTab(x, y);
        initVisualsTab(x, y);
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
        LiquidGlassSwitch ringToggle = new LiquidGlassSwitch((int)x + 330, (int)y + 45, 40, 20, GlassMenuClient.CONFIG.enableJumpRings());
        ringToggle.setOnToggle(enabled -> {
            GlassMenuClient.CONFIG.enableJumpRings(enabled); GlassMenuClient.CONFIG.save();
        });
        visualsWidgets.add(ringToggle);
        
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
        visualsWidgets.add(visSliderR); visualsWidgets.add(visSliderG); visualsWidgets.add(visSliderB);

        visHexInput = createColorTextField((int)x + 40, (int)y + 38, 60);
        visHexInput.setText(String.format("#%06X", currentColor & 0xFFFFFF));
        
        // Add dynamic text listener to parse and update jump ring color using hex format
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
        
        // Add dynamic text listener to parse and update jump ring color using comma-separated RGB
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
            visualsWidgets.add(btn);
        }
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
        if (currentTab == Tab.MOVEMENT) {
            for (ClickableWidget w : movementWidgets) this.addSelectableChild(w);
        } else {
            List<ClickableWidget> list = switch (currentTab) {
                case GENERAL -> generalWidgets; case COMBAT -> combatWidgets;
                case VISUALS -> visualsWidgets; case MOVEMENT -> movementWidgets;
                case BRIDGE -> bridgeWidgets;
            };
            for (ClickableWidget w : list) this.addDrawableChild(w);
            if (currentTab == Tab.COMBAT) {
                this.addDrawableChild(hexInput); this.addDrawableChild(rgbInput);
                this.addSelectableChild(hexInput); this.addSelectableChild(rgbInput);
            } else if (currentTab == Tab.VISUALS) {
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
        long now = System.currentTimeMillis(); float dt = (now - lastAnimTime) / 1000f; lastAnimTime = now;
        
        float targetW = (currentTab == Tab.MOVEMENT || currentTab == Tab.GENERAL || currentTab == Tab.VISUALS || currentTab == Tab.BRIDGE) ? 1.0f : 0.0f;
        float targetH = (currentTab == Tab.MOVEMENT || currentTab == Tab.GENERAL || currentTab == Tab.VISUALS || currentTab == Tab.BRIDGE) ? 1.0f : 0.0f;
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
        this.lensView.setX(x); this.lensView.setY(y); this.lensView.setWidth(currentW); this.lensView.setHeight(currentH);
        this.lensView.render(context);
        
        Tab[] topTabs = { Tab.GENERAL, Tab.MOVEMENT, Tab.COMBAT, Tab.VISUALS };
        float tabW = (float)currentW / topTabs.length;
        for (int i = 0; i < topTabs.length; i++) {
            boolean isHovered = mouseX >= x + tabW * i && mouseX <= x + tabW * (i + 1) && mouseY >= y && mouseY <= y + 30;
            tabHoverProgress[i] = MathHelper.lerp(dt * 8.0f, tabHoverProgress[i], isHovered ? 1.0f : 0.0f);
            float swell = 1.0f + tabHoverProgress[i] * 0.12f;
            
            // Highlight GENERAL if we are in BRIDGE
            boolean isActive = (topTabs[i] == currentTab) || (topTabs[i] == Tab.GENERAL && currentTab == Tab.BRIDGE);
            int color = ((int)(255 * contentAlpha) << 24) | ((isActive ? 0xFFFFFFFF : 0x88FFFFFF) & 0xFFFFFF);
            
            context.getMatrices().push(); context.getMatrices().translate(x + tabW * i + tabW / 2f, y + 15, 0);
            context.getMatrices().scale(swell, swell, 1.0f); context.drawCenteredTextWithShadow(textRenderer, topTabs[i].name(), 0, -5, color);
            context.getMatrices().pop();
        }

        if (currentTab == Tab.GENERAL || currentTab == Tab.BRIDGE) renderGeneralTab(context, x, y, mouseX, mouseY, delta);
        else if (currentTab == Tab.COMBAT) renderCombatTab(context, x, y, mouseX, mouseY, delta);
        else if (currentTab == Tab.MOVEMENT) renderMovementTab(context, x, y, mouseX, mouseY, delta, currentW, currentH);
        else if (currentTab == Tab.VISUALS) renderVisualsTab(context, x, y, mouseX, mouseY, delta);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderVisualsTab(DrawContext context, int x, int y, int mouseX, int mouseY, float delta) {
        int alphaInt = (int)(255 * contentAlpha); int colorAlpha = alphaInt << 24;
        float slideOffset = (1.0f - contentAlpha) * 12f;
        context.drawTextWithShadow(textRenderer, "Jump Pulse Rings", x + 230, y + 50 - (int)slideOffset, colorAlpha | 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, "Mode", x + 40, y + 85 - (int)slideOffset, colorAlpha | 0xAAAAAA);

        int btnIdx = 0;
        for (ClickableWidget w : visualsWidgets) {
            w.setAlpha(contentAlpha);
            if (w == visSliderR) w.setY((int)y + 90 - (int)slideOffset);
            else if (w == visSliderG) w.setY((int)y + 125 - (int)slideOffset);
            else if (w == visSliderB) w.setY((int)y + 160 - (int)slideOffset);
            else if (w instanceof LiquidGlassButton) {
                w.setY((int)y + 100 + (btnIdx++) * 28 - (int)slideOffset);
            } else w.setY((int)y + 45 - (int)slideOffset);
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
        RenderUtils.drawSdfRoundedRect(context.getMatrices(), subX, subY, subW, subH, 15, 0xAA111111, 0);
        RenderUtils.drawLine(context.getMatrices(), subX + 15, subY + 20, subX + subW - 15, subY + 20, 0.8f, 0x44FFFFFF);
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
        RenderUtils.drawSdfRoundedRect(c.getMatrices(), x - 0.7f, y - 0.7f, w + 1.4f, h + 1.4f, 5.7f, 0xFFFFFFFF, 0);
        RenderUtils.drawSdfRoundedRect(c.getMatrices(), x, y, w, h, 5, 0xFF000000, 0);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
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

    @Override public boolean mouseReleased(double mX, double mY, int b) { draggingPreview = false; return super.mouseReleased(mX, mY, b); }
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (currentTab == Tab.MOVEMENT) { scrollY = MathHelper.clamp(scrollY - verticalAmount * 20, 0, maxScroll); return true; }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    @Override public void renderBackground(DrawContext c, int mX, int mY, float d) {}
    @Override public boolean shouldPause() { return false; }
}
