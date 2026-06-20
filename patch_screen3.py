import re

with open("src/main/java/com/example/glassmenu/screen/LiquidGlassScreen.java", "r") as f:
    code = f.read()

# Add init call to init() method
init_search = "        initVisualsAttackRangeTab(x, y);"
init_replace = init_search + "\n        initVisualsRadarTab(x, y);"
code = code.replace(init_search, init_replace)

# Render call
render_search = "        else if (currentTab == Tab.VISUALS_ATTACK_RANGE) renderVisualsAttackRangeTab(context, x, y, mouseX, mouseY, delta);"
render_replace = render_search + "\n        else if (currentTab == Tab.VISUALS_RADAR) renderVisualsRadarTab(context, x, y, mouseX, mouseY, delta);"
code = code.replace(render_search, render_replace)

# Inputs and Tab Definition
tab_code = """
    private TextFieldWidget radarHexInput, radarRgbInput;
    private LiquidGlassSlider radarSliderR, radarSliderG, radarSliderB, radarRadiusSlider;

    private void initVisualsRadarTab(float x, float y) {
        LiquidGlassButton backBtn = new LiquidGlassButton((int)x + 40, (int)y + 223, 80, 22, Text.literal(MenuTranslator.tr("Back")), b -> {
            currentTab = Tab.VISUALS; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        visualsRadarWidgets.add(backBtn);

        radarHexInput = new TextFieldWidget(textRenderer, (int)x + 195, (int)y + 117, 70, 16, Text.literal(""));
        radarHexInput.setMaxLength(6);
        radarHexInput.setText(String.format("%06X", GlassMenuClient.CONFIG.crosshairRadarColor() & 0xFFFFFF));
        radarHexInput.setChangedListener(s -> {
            try {
                if (s.length() == 6) {
                    GlassMenuClient.CONFIG.crosshairRadarColor(Integer.parseInt(s, 16) | 0xFF000000);
                    int r = (GlassMenuClient.CONFIG.crosshairRadarColor() >> 16) & 0xFF;
                    int g = (GlassMenuClient.CONFIG.crosshairRadarColor() >> 8) & 0xFF;
                    int b = GlassMenuClient.CONFIG.crosshairRadarColor() & 0xFF;
                    radarSliderR.setValue(r); radarSliderG.setValue(g); radarSliderB.setValue(b);
                    updateRgbInput(radarRgbInput, GlassMenuClient.CONFIG.crosshairRadarColor());
                    GlassMenuClient.saveConfig();
                }
            } catch (NumberFormatException ignored) {}
        });
        visualsRadarWidgets.add(radarHexInput);

        radarRgbInput = new TextFieldWidget(textRenderer, (int)x + 295, (int)y + 117, 95, 16, Text.literal(""));
        radarRgbInput.setMaxLength(11);
        updateRgbInput(radarRgbInput, GlassMenuClient.CONFIG.crosshairRadarColor());
        radarRgbInput.setChangedListener(s -> {
            String[] parts = s.split(",");
            if (parts.length == 3) {
                try {
                    int r = MathHelper.clamp(Integer.parseInt(parts[0].trim()), 0, 255);
                    int g = MathHelper.clamp(Integer.parseInt(parts[1].trim()), 0, 255);
                    int bl = MathHelper.clamp(Integer.parseInt(parts[2].trim()), 0, 255);
                    GlassMenuClient.CONFIG.crosshairRadarColor(0xFF000000 | (r << 16) | (g << 8) | bl);
                    radarHexInput.setText(String.format("%06X", GlassMenuClient.CONFIG.crosshairRadarColor() & 0xFFFFFF));
                    radarSliderR.setValue(r); radarSliderG.setValue(g); radarSliderB.setValue(bl);
                    GlassMenuClient.saveConfig();
                } catch (NumberFormatException ignored) {}
            }
        });
        visualsRadarWidgets.add(radarRgbInput);

        radarSliderR = new LiquidGlassSlider((int)x + 195, (int)y + 145, 195, 14, Text.literal("R"), 0, 255, (GlassMenuClient.CONFIG.crosshairRadarColor() >> 16) & 0xFF, val -> {
            int c = GlassMenuClient.CONFIG.crosshairRadarColor();
            GlassMenuClient.CONFIG.crosshairRadarColor((c & 0xFF00FFFF) | ((int)val << 16));
            updateColorInputs(radarHexInput, radarRgbInput, GlassMenuClient.CONFIG.crosshairRadarColor());
        });
        visualsRadarWidgets.add(radarSliderR);

        radarSliderG = new LiquidGlassSlider((int)x + 195, (int)y + 165, 195, 14, Text.literal("G"), 0, 255, (GlassMenuClient.CONFIG.crosshairRadarColor() >> 8) & 0xFF, val -> {
            int c = GlassMenuClient.CONFIG.crosshairRadarColor();
            GlassMenuClient.CONFIG.crosshairRadarColor((c & 0xFFFF00FF) | ((int)val << 8));
            updateColorInputs(radarHexInput, radarRgbInput, GlassMenuClient.CONFIG.crosshairRadarColor());
        });
        visualsRadarWidgets.add(radarSliderG);

        radarSliderB = new LiquidGlassSlider((int)x + 195, (int)y + 185, 195, 14, Text.literal("B"), 0, 255, GlassMenuClient.CONFIG.crosshairRadarColor() & 0xFF, val -> {
            int c = GlassMenuClient.CONFIG.crosshairRadarColor();
            GlassMenuClient.CONFIG.crosshairRadarColor((c & 0xFFFFFF00) | (int)val);
            updateColorInputs(radarHexInput, radarRgbInput, GlassMenuClient.CONFIG.crosshairRadarColor());
        });
        visualsRadarWidgets.add(radarSliderB);

        radarRadiusSlider = new LiquidGlassSlider((int)x + 160, (int)y + 223, 230, 22, Text.literal(MenuTranslator.tr("Radius: ") + (int)(float)GlassMenuClient.CONFIG.crosshairRadarRadius()), 30, 150, GlassMenuClient.CONFIG.crosshairRadarRadius(), val -> {
            GlassMenuClient.CONFIG.crosshairRadarRadius((float)val);
            radarRadiusSlider.setMessage(Text.literal(MenuTranslator.tr("Radius: ") + (int)(float)val));
        });
        visualsRadarWidgets.add(radarRadiusSlider);
    }

    private void renderVisualsRadarTab(DrawContext context, float x, float y, int mouseX, int mouseY, float delta) {
        context.drawCenteredTextWithShadow(textRenderer, MenuTranslator.tr("Player Radar Settings"), (int)(x + 80), (int)(y + 20), 0xFFFFFFFF);
        
        drawToggle(context, x + 20, y + 45, mouseX, mouseY, MenuTranslator.tr("Enable Feature"), GlassMenuClient.CONFIG.enableCrosshairRadar(), 
            state -> { GlassMenuClient.CONFIG.enableCrosshairRadar(state); GlassMenuClient.saveConfig(); });
        
        drawToggle(context, x + 20, y + 80, mouseX, mouseY, MenuTranslator.tr("RGB Arrows"), GlassMenuClient.CONFIG.crosshairRadarRgb(), 
            state -> { GlassMenuClient.CONFIG.crosshairRadarRgb(state); GlassMenuClient.saveConfig(); });

        if (!GlassMenuClient.CONFIG.crosshairRadarRgb()) {
            RenderUtils.drawSdfRoundedRect(context.getMatrices(), x + 160, y + 45, 230, 165, 8f, 0x1AFFFFFF, 0);
            context.drawTextWithShadow(textRenderer, MenuTranslator.tr("Color Config"), (int)x + 170, (int)y + 55, 0xFFFFFFFF);
            
            RenderUtils.drawSdfRoundedRect(context.getMatrices(), x + 175, y + 80, 200, 20, 4f, GlassMenuClient.CONFIG.crosshairRadarColor(), 0);
            
            context.drawTextWithShadow(textRenderer, "HEX", (int)x + 175, (int)y + 121, 0xFFFFFFFF);
            context.drawTextWithShadow(textRenderer, "RGB", (int)x + 270, (int)y + 121, 0xFFFFFFFF);
            
            radarHexInput.render(context, mouseX, mouseY, delta);
            radarRgbInput.render(context, mouseX, mouseY, delta);
            radarSliderR.render(context, mouseX, mouseY, delta);
            radarSliderG.render(context, mouseX, mouseY, delta);
            radarSliderB.render(context, mouseX, mouseY, delta);
        }

        radarRadiusSlider.render(context, mouseX, mouseY, delta);
        visualsRadarWidgets.get(0).render(context, mouseX, mouseY, delta); // Back Button
    }
"""

# Insert definition before initVisualsAttackRangeTab
code = code.replace("    private void initVisualsAttackRangeTab(float x, float y) {", tab_code + "\n    private void initVisualsAttackRangeTab(float x, float y) {")

with open("src/main/java/com/example/glassmenu/screen/LiquidGlassScreen.java", "w") as f:
    f.write(code)
