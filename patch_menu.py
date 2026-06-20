import re

with open("src/main/java/com/example/glassmenu/screen/LiquidGlassScreen.java", "r") as f:
    code = f.read()

# 1. Tab Enum
code = code.replace("VISUALS_NAMETAGS, POSITION, BRIDGE }", "VISUALS_NAMETAGS, VISUALS_ATTACK_RANGE, POSITION, BRIDGE }")

# 2. Widgets list declaration
code = code.replace("private final List<ClickableWidget> visualsNametagsWidgets = new ArrayList<>();",
"""private final List<ClickableWidget> visualsNametagsWidgets = new ArrayList<>();
    private final List<ClickableWidget> visualsAttackRangeWidgets = new ArrayList<>();""")

# 3. getPanelW, getPanelH, panelWidthProgress, panelHeightProgress, targetW, targetH
# They all end with `|| currentTab == Tab.BRIDGE) ?` or something similar.
code = code.replace("|| currentTab == Tab.BRIDGE)", "|| currentTab == Tab.VISUALS_ATTACK_RANGE || currentTab == Tab.BRIDGE)")

# 4. clear
code = code.replace("visualsNametagsWidgets.clear();", "visualsNametagsWidgets.clear(); visualsAttackRangeWidgets.clear();")

# 5. init
code = code.replace("initVisualsNametagsTab(x, y);", "initVisualsNametagsTab(x, y);\n        initVisualsAttackRangeTab(x, y);")

# 6. initVisualsTab buttons
btn_code = """        LiquidGlassButton nametagsBtn = new LiquidGlassButton(0, 0, 160, 22, Text.literal("Custom Nametags"), b -> {
            currentTab = Tab.VISUALS_NAMETAGS; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        visualsWidgets.add(nametagsBtn);

        LiquidGlassButton attackRangeBtn = new LiquidGlassButton(0, 0, 160, 22, Text.literal("Attack Range ESP"), b -> {
            currentTab = Tab.VISUALS_ATTACK_RANGE; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        visualsWidgets.add(attackRangeBtn);"""
code = code.replace("""        LiquidGlassButton nametagsBtn = new LiquidGlassButton(0, 0, 160, 22, Text.literal("Custom Nametags"), b -> {
            currentTab = Tab.VISUALS_NAMETAGS; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        visualsWidgets.add(nametagsBtn);""", btn_code)

# 7. updateVisibleWidgets
code = code.replace("case VISUALS_NAMETAGS -> visualsNametagsWidgets;", "case VISUALS_NAMETAGS -> visualsNametagsWidgets;\n                case VISUALS_ATTACK_RANGE -> visualsAttackRangeWidgets;")

# 8. render() dispatch
code = code.replace("else if (currentTab == Tab.VISUALS_NAMETAGS) renderVisualsNametagsTab(context, x, y, mouseX, mouseY, delta);", "else if (currentTab == Tab.VISUALS_NAMETAGS) renderVisualsNametagsTab(context, x, y, mouseX, mouseY, delta);\n        else if (currentTab == Tab.VISUALS_ATTACK_RANGE) renderVisualsAttackRangeTab(context, x, y, mouseX, mouseY, delta);")


# 9. Add the new methods
new_methods = """
    private TextFieldWidget attackRangeHexInput, attackRangeRgbInput;
    private LiquidGlassSlider attackRangeSliderR, attackRangeSliderG, attackRangeSliderB;

    private void initVisualsAttackRangeTab(float x, float y) {
        LiquidGlassButton backBtn = new LiquidGlassButton((int)x + 40, (int)y + 223, 80, 22, Text.literal("Back"), b -> {
            currentTab = Tab.VISUALS; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        visualsAttackRangeWidgets.add(backBtn);

        LiquidGlassSwitch toggle = new LiquidGlassSwitch((int)x + 330, (int)y + 45, 40, 20, GlassMenuClient.CONFIG.enableAttackRange());
        toggle.setOnToggle(enabled -> {
            GlassMenuClient.CONFIG.enableAttackRange(enabled); GlassMenuClient.CONFIG.save();
        });
        visualsAttackRangeWidgets.add(toggle);
        
        int currentColor = GlassMenuClient.CONFIG.attackRangeColor();
        attackRangeSliderR = new LiquidGlassSlider((int)x + 230, (int)y + 90, 140, 16, ((currentColor >> 16) & 0xFF) / 255f);
        attackRangeSliderG = new LiquidGlassSlider((int)x + 230, (int)y + 125, 140, 16, ((currentColor >> 8) & 0xFF) / 255f);
        attackRangeSliderB = new LiquidGlassSlider((int)x + 230, (int)y + 160, 140, 16, (currentColor & 0xFF) / 255f);

        Runnable updateColor = () -> {
            if (isUpdating) return; isUpdating = true;
            int r = (int)(attackRangeSliderR.getValue() * 255), g = (int)(attackRangeSliderG.getValue() * 255), b = (int)(attackRangeSliderB.getValue() * 255);
            int color = 0xFF000000 | (r << 16) | (g << 8) | b;
            GlassMenuClient.CONFIG.attackRangeColor(color); GlassMenuClient.CONFIG.save();
            if (attackRangeHexInput != null) { String s = String.format("#%06X", color & 0xFFFFFF); if (!attackRangeHexInput.getText().equals(s)) attackRangeHexInput.setText(s); }
            if (attackRangeRgbInput != null) { String s = String.format("%d, %d, %d", r, g, b); if (!attackRangeRgbInput.getText().equals(s)) attackRangeRgbInput.setText(s); }
            isUpdating = false;
        };

        attackRangeSliderR.setOnValueChange(v -> updateColor.run());
        attackRangeSliderG.setOnValueChange(v -> updateColor.run());
        attackRangeSliderB.setOnValueChange(v -> updateColor.run());
        visualsAttackRangeWidgets.add(attackRangeSliderR); visualsAttackRangeWidgets.add(attackRangeSliderG); visualsAttackRangeWidgets.add(attackRangeSliderB);

        attackRangeHexInput = createColorTextField((int)x + 40, (int)y + 90, 60);
        attackRangeHexInput.setText(String.format("#%06X", currentColor & 0xFFFFFF));
        attackRangeHexInput.setChangedListener(text -> {
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
                    attackRangeSliderR.setValue(r / 255.0f);
                    attackRangeSliderG.setValue(g / 255.0f);
                    attackRangeSliderB.setValue(b / 255.0f);
                    GlassMenuClient.CONFIG.attackRangeColor(0xFF000000 | color);
                    GlassMenuClient.CONFIG.save();
                    if (attackRangeRgbInput != null) { String s = String.format("%d, %d, %d", r, g, b); if (!attackRangeRgbInput.getText().equals(s)) attackRangeRgbInput.setText(s); }
                    isUpdating = false;
                }
            } catch (NumberFormatException ignored) {}
        });
        visualsAttackRangeWidgets.add(attackRangeHexInput);

        attackRangeRgbInput = createColorTextField((int)x + 40, (int)y + 125, 100);
        attackRangeRgbInput.setText(String.format("%d, %d, %d", (currentColor >> 16) & 0xFF, (currentColor >> 8) & 0xFF, currentColor & 0xFF));
        attackRangeRgbInput.setChangedListener(text -> {
            if (isUpdating) return;
            try {
                String[] parts = text.split(",");
                if (parts.length == 3) {
                    int r = Integer.parseInt(parts[0].trim());
                    int g = Integer.parseInt(parts[1].trim());
                    int b = Integer.parseInt(parts[2].trim());
                    if (r >= 0 && r <= 255 && g >= 0 && g <= 255 && b >= 0 && b <= 255) {
                        isUpdating = true;
                        attackRangeSliderR.setValue(r / 255.0f);
                        attackRangeSliderG.setValue(g / 255.0f);
                        attackRangeSliderB.setValue(b / 255.0f);
                        int color = 0xFF000000 | (r << 16) | (g << 8) | b;
                        GlassMenuClient.CONFIG.attackRangeColor(color);
                        GlassMenuClient.CONFIG.save();
                        if (attackRangeHexInput != null) { String s = String.format("#%06X", color & 0xFFFFFF); if (!attackRangeHexInput.getText().equals(s)) attackRangeHexInput.setText(s); }
                        isUpdating = false;
                    }
                }
            } catch (NumberFormatException ignored) {}
        });
        visualsAttackRangeWidgets.add(attackRangeRgbInput);

        String modeText = "Mode: " + GlassMenuClient.CONFIG.attackRangeMode().name();
        LiquidGlassButton modeBtn = new LiquidGlassButton((int)x + 40, (int)y + 160, 150, 22, Text.literal(modeText), b -> {
            GlassMenuConfigModel.AttackRangeMode currentMode = GlassMenuClient.CONFIG.attackRangeMode();
            GlassMenuConfigModel.AttackRangeMode nextMode;
            if (currentMode == GlassMenuConfigModel.AttackRangeMode.SOLID_OUTLINE) nextMode = GlassMenuConfigModel.AttackRangeMode.GLOW_OUTLINE;
            else if (currentMode == GlassMenuConfigModel.AttackRangeMode.GLOW_OUTLINE) nextMode = GlassMenuConfigModel.AttackRangeMode.FILLED;
            else nextMode = GlassMenuConfigModel.AttackRangeMode.SOLID_OUTLINE;
            GlassMenuClient.CONFIG.attackRangeMode(nextMode);
            GlassMenuClient.CONFIG.save();
            b.setMessage(Text.literal("Mode: " + nextMode.name()));
        });
        visualsAttackRangeWidgets.add(modeBtn);
    }

    private void renderVisualsAttackRangeTab(DrawContext context, int x, int y, int mouseX, int mouseY, float delta) {
        int alphaInt = (int)(255 * contentAlpha); int colorAlpha = alphaInt << 24;
        float slideOffset = (1.0f - contentAlpha) * 12f;
        context.drawTextWithShadow(textRenderer, "Attack Range ESP Settings", x + 30, y + 45 - (int)slideOffset, colorAlpha | 0xFFFFFF);
        
        context.drawTextWithShadow(textRenderer, "Enable Attack Range", x + 40, y + 80 - (int)slideOffset, colorAlpha | 0xAAAAAA);
        
        for (ClickableWidget w : visualsAttackRangeWidgets) {
            w.setAlpha(contentAlpha);
            if (w == attackRangeSliderR) context.drawTextWithShadow(textRenderer, "R", x + 215, w.getY() + 4, colorAlpha | 0xFF5555);
            if (w == attackRangeSliderG) context.drawTextWithShadow(textRenderer, "G", x + 215, w.getY() + 4, colorAlpha | 0x55FF55);
            if (w == attackRangeSliderB) context.drawTextWithShadow(textRenderer, "B", x + 215, w.getY() + 4, colorAlpha | 0x5555FF);
            w.render(context, mouseX, mouseY, delta);
        }
    }
"""

code = code.replace("private void renderVisualsNametagsTab", new_methods + "\n    private void renderVisualsNametagsTab")

with open("src/main/java/com/example/glassmenu/screen/LiquidGlassScreen.java", "w") as f:
    f.write(code)

print("Patch applied")
