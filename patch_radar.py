import re

with open("src/main/java/com/example/glassmenu/screen/LiquidGlassScreen.java", "r") as f:
    code = f.read()

# Tab enum
code = code.replace("VISUALS_NAMETAGS, VISUALS_ATTACK_RANGE, POSITION, BRIDGE", "VISUALS_NAMETAGS, VISUALS_ATTACK_RANGE, VISUALS_RADAR, POSITION, BRIDGE")

# getPanelW and getPanelH and panelWidthProgress
for search in ["VISUALS_ATTACK_RANGE || currentTab == Tab.BRIDGE", "VISUALS_ATTACK_RANGE || currentTab == Tab.BRIDGE) ? PANEL_W_EXPANDED", "VISUALS_ATTACK_RANGE || currentTab == Tab.BRIDGE) ? PANEL_H_EXPANDED"]:
    code = code.replace(search, search.replace("VISUALS_ATTACK_RANGE", "VISUALS_ATTACK_RANGE || currentTab == Tab.VISUALS_RADAR"))

# Active tab hover state
tab_hover_search = "currentTab == Tab.VISUALS_ATTACK_RANGE || currentTab == Tab.POSITION"
tab_hover_replace = "currentTab == Tab.VISUALS_ATTACK_RANGE || currentTab == Tab.VISUALS_RADAR || currentTab == Tab.POSITION"
code = code.replace(tab_hover_search, tab_hover_replace)

# Widget list & inputs
widget_search = "private final List<ClickableWidget> visualsAttackRangeWidgets = new ArrayList<>();"
widget_replace = widget_search + """
    private final List<ClickableWidget> visualsRadarWidgets = new ArrayList<>();
    private TextFieldWidget radarHexInput, radarRgbInput;
    private LiquidGlassSlider radarSliderR, radarSliderG, radarSliderB;
"""
code = code.replace(widget_search, widget_replace)

# Clear widgets
code = code.replace("visualsAttackRangeWidgets.clear();", "visualsAttackRangeWidgets.clear();\n        visualsRadarWidgets.clear();")

# Button definition
btn_search = "private LiquidGlassButton attackRangeBtn;"
btn_replace = btn_search + "\n    private LiquidGlassButton radarBtn;"
code = code.replace(btn_search, btn_replace)

# Add button to initVisualsTab
visuals_btn_search = """        attackRangeBtn = new LiquidGlassButton(0, 0, 160, 22, Text.literal(MenuTranslator.tr("Attack Range")), b -> {
            currentTab = Tab.VISUALS_ATTACK_RANGE; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        visualsWidgets.add(attackRangeBtn);"""
visuals_btn_replace = visuals_btn_search + """
        
        radarBtn = new LiquidGlassButton(0, 0, 160, 22, Text.literal(MenuTranslator.tr("Player Radar")), b -> {
            currentTab = Tab.VISUALS_RADAR; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        visualsWidgets.add(radarBtn);"""
code = code.replace(visuals_btn_search, visuals_btn_replace)

# Add updateVisibleWidgets logic
uvw_search = "        } else if (currentTab == Tab.VISUALS_ATTACK_RANGE) {\n            visualsAttackRangeWidgets.forEach(w -> w.visible = true);\n        }"
uvw_replace = uvw_search + " else if (currentTab == Tab.VISUALS_RADAR) {\n            visualsRadarWidgets.forEach(w -> w.visible = true);\n        }"
code = code.replace(uvw_search, uvw_replace)

# Add init call to init() method
init_search = "        initVisualsAttackRangeTab(x, y);"
init_replace = init_search + "\n        initVisualsRadarTab(x, y);"
code = code.replace(init_search, init_replace)

# Render call
render_search = "        else if (currentTab == Tab.VISUALS_ATTACK_RANGE) renderVisualsAttackRangeTab(context, x, y, mouseX, mouseY, delta);"
render_replace = render_search + "\n        else if (currentTab == Tab.VISUALS_RADAR) renderVisualsRadarTab(context, x, y, mouseX, mouseY, delta);"
code = code.replace(render_search, render_replace)

# Tab definition
tab_code = """
    private void initVisualsRadarTab(float x, float y) {
        LiquidGlassButton backBtn = new LiquidGlassButton((int)x + 40, (int)y + 223, 80, 22, Text.literal(MenuTranslator.tr("Back")), b -> {
            currentTab = Tab.VISUALS; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        visualsRadarWidgets.add(backBtn);

        LiquidGlassSwitch toggleRadar = new LiquidGlassSwitch((int)x + 100, (int)y + 40, 40, 20, GlassMenuClient.CONFIG.enableCrosshairRadar());
        toggleRadar.setOnToggle(enabled -> { GlassMenuClient.CONFIG.enableCrosshairRadar(enabled); GlassMenuClient.CONFIG.save(); });
        visualsRadarWidgets.add(toggleRadar);

        LiquidGlassSwitch toggleRgb = new LiquidGlassSwitch((int)x + 100, (int)y + 75, 40, 20, GlassMenuClient.CONFIG.crosshairRadarRgb());
        toggleRgb.setOnToggle(enabled -> { GlassMenuClient.CONFIG.crosshairRadarRgb(enabled); GlassMenuClient.CONFIG.save(); });
        visualsRadarWidgets.add(toggleRgb);

        int radarColor = GlassMenuClient.CONFIG.crosshairRadarColor();
        radarSliderR = new LiquidGlassSlider((int)x + 195, (int)y + 115, 140, 16, ((radarColor >> 16) & 0xFF) / 255f);
        radarSliderG = new LiquidGlassSlider((int)x + 195, (int)y + 145, 140, 16, ((radarColor >> 8) & 0xFF) / 255f);
        radarSliderB = new LiquidGlassSlider((int)x + 195, (int)y + 175, 140, 16, (radarColor & 0xFF) / 255f);

        Runnable updateColor = () -> {
            if (isUpdating) return; isUpdating = true;
            int r = (int)(radarSliderR.getValue() * 255), g = (int)(radarSliderG.getValue() * 255), b = (int)(radarSliderB.getValue() * 255);
            int color = 0xFF000000 | (r << 16) | (g << 8) | b;
            GlassMenuClient.CONFIG.crosshairRadarColor(color); GlassMenuClient.CONFIG.save();
            if (radarHexInput != null) { String s = String.format("#%06X", color & 0xFFFFFF); if (!radarHexInput.getText().equals(s)) radarHexInput.setText(s); }
            if (radarRgbInput != null) { String s = String.format("%d, %d, %d", r, g, b); if (!radarRgbInput.getText().equals(s)) radarRgbInput.setText(s); }
            isUpdating = false;
        };

        radarSliderR.setChangeListener(v -> updateColor.run());
        radarSliderG.setChangeListener(v -> updateColor.run());
        radarSliderB.setChangeListener(v -> updateColor.run());
        
        visualsRadarWidgets.add(radarSliderR); visualsRadarWidgets.add(radarSliderG); visualsRadarWidgets.add(radarSliderB);

        radarHexInput = new TextFieldWidget(textRenderer, (int)x + 185, (int)y + 80, 60, 16, Text.literal(""));
        radarHexInput.setMaxLength(7); radarHexInput.setText(String.format("#%06X", radarColor & 0xFFFFFF));
        radarHexInput.setChangedListener(s -> {
            if (isUpdating || s.length() < 7 || !s.startsWith("#")) return;
            try {
                int color = Integer.parseInt(s.substring(1), 16) | 0xFF000000;
                isUpdating = true;
                GlassMenuClient.CONFIG.crosshairRadarColor(color); GlassMenuClient.CONFIG.save();
                radarSliderR.setValue(((color >> 16) & 0xFF) / 255f); radarSliderG.setValue(((color >> 8) & 0xFF) / 255f); radarSliderB.setValue((color & 0xFF) / 255f);
                if (radarRgbInput != null) radarRgbInput.setText(String.format("%d, %d, %d", (color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF));
                isUpdating = false;
            } catch (Exception ignored) {}
        });
        visualsRadarWidgets.add(radarHexInput);

        radarRgbInput = new TextFieldWidget(textRenderer, (int)x + 280, (int)y + 80, 80, 16, Text.literal(""));
        radarRgbInput.setMaxLength(11); radarRgbInput.setText(String.format("%d, %d, %d", (radarColor >> 16) & 0xFF, (radarColor >> 8) & 0xFF, radarColor & 0xFF));
        radarRgbInput.setChangedListener(s -> {
            if (isUpdating) return;
            String[] parts = s.split(","); if (parts.length != 3) return;
            try {
                int r = MathHelper.clamp(Integer.parseInt(parts[0].trim()), 0, 255), g = MathHelper.clamp(Integer.parseInt(parts[1].trim()), 0, 255), bl = MathHelper.clamp(Integer.parseInt(parts[2].trim()), 0, 255);
                int color = 0xFF000000 | (r << 16) | (g << 8) | bl;
                isUpdating = true;
                GlassMenuClient.CONFIG.crosshairRadarColor(color); GlassMenuClient.CONFIG.save();
                radarSliderR.setValue(r / 255f); radarSliderG.setValue(g / 255f); radarSliderB.setValue(bl / 255f);
                if (radarHexInput != null) radarHexInput.setText(String.format("#%06X", color & 0xFFFFFF));
                isUpdating = false;
            } catch (Exception ignored) {}
        });
        visualsRadarWidgets.add(radarRgbInput);
        
        LiquidGlassSlider radiusSlider = new LiquidGlassSlider((int)x + 195, (int)y + 205, 140, 16, (GlassMenuClient.CONFIG.crosshairRadarRadius() - 30f) / 120f);
        radiusSlider.setChangeListener(v -> {
            float rad = 30f + (float)v * 120f;
            GlassMenuClient.CONFIG.crosshairRadarRadius(rad); GlassMenuClient.CONFIG.save();
        });
        visualsRadarWidgets.add(radiusSlider);
    }

    private void renderVisualsRadarTab(DrawContext context, float x, float y, int mouseX, int mouseY, float delta) {
        context.drawCenteredTextWithShadow(textRenderer, MenuTranslator.tr("Player Radar Settings"), (int)(x + 80), (int)(y + 20), 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, MenuTranslator.tr("Enable Feature"), (int)(x + 20), (int)(y + 45), 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, MenuTranslator.tr("RGB Mode"), (int)(x + 20), (int)(y + 80), 0xFFFFFFFF);
        
        if (!GlassMenuClient.CONFIG.crosshairRadarRgb()) {
            RenderUtils.drawSdfRoundedRect(context.getMatrices(), x + 160, y + 45, 230, 195, 8f, 0x1AFFFFFF, 0);
            context.drawTextWithShadow(textRenderer, MenuTranslator.tr("Arrow Color"), (int)x + 170, (int)y + 55, 0xFFFFFFFF);
            
            RenderUtils.drawSdfRoundedRect(context.getMatrices(), x + 260, y + 55, 110, 10, 4f, GlassMenuClient.CONFIG.crosshairRadarColor(), 0);
            
            context.drawTextWithShadow(textRenderer, "HEX", (int)x + 185, (int)y + 70, 0xFFFFFFFF);
            context.drawTextWithShadow(textRenderer, "RGB", (int)x + 280, (int)y + 70, 0xFFFFFFFF);
            
            context.drawTextWithShadow(textRenderer, "R", (int)x + 180, (int)y + 119, 0xFFFF5555);
            context.drawTextWithShadow(textRenderer, "G", (int)x + 180, (int)y + 149, 0xFF55FF55);
            context.drawTextWithShadow(textRenderer, "B", (int)x + 180, (int)y + 179, 0xFF5555FF);
            
            radarHexInput.render(context, mouseX, mouseY, delta);
            radarRgbInput.render(context, mouseX, mouseY, delta);
            radarSliderR.render(context, mouseX, mouseY, delta);
            radarSliderG.render(context, mouseX, mouseY, delta);
            radarSliderB.render(context, mouseX, mouseY, delta);
        }
        
        context.drawTextWithShadow(textRenderer, "Radius", (int)x + 180, (int)y + 209, 0xFFFFFFFF);
        visualsRadarWidgets.get(7).render(context, mouseX, mouseY, delta); // Radius slider
        visualsRadarWidgets.get(0).render(context, mouseX, mouseY, delta); // Back Button
        visualsRadarWidgets.get(1).render(context, mouseX, mouseY, delta); // Toggle 1
        visualsRadarWidgets.get(2).render(context, mouseX, mouseY, delta); // Toggle 2
    }
"""

# Insert definition before initVisualsAttackRangeTab
code = code.replace("    private void initVisualsAttackRangeTab(float x, float y) {", tab_code + "\n    private void initVisualsAttackRangeTab(float x, float y) {")

with open("src/main/java/com/example/glassmenu/screen/LiquidGlassScreen.java", "w") as f:
    f.write(code)
