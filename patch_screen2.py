import re

with open("src/main/java/com/example/glassmenu/screen/LiquidGlassScreen.java", "r") as f:
    code = f.read()

# Add to getPanelW/H
for search in ["VISUALS_ATTACK_RANGE || currentTab == Tab.BRIDGE", "VISUALS_ATTACK_RANGE || currentTab == Tab.BRIDGE) ? PANEL_W_EXPANDED", "VISUALS_ATTACK_RANGE || currentTab == Tab.BRIDGE) ? PANEL_H_EXPANDED"]:
    code = code.replace(search, search.replace("VISUALS_ATTACK_RANGE", "VISUALS_ATTACK_RANGE || currentTab == Tab.VISUALS_RADAR"))

# Add active status for main VISUALS tab to keep it highlighted
tab_hover_search = "currentTab == Tab.VISUALS_ATTACK_RANGE || currentTab == Tab.POSITION"
tab_hover_replace = "currentTab == Tab.VISUALS_ATTACK_RANGE || currentTab == Tab.VISUALS_RADAR || currentTab == Tab.POSITION"
code = code.replace(tab_hover_search, tab_hover_replace)

# Add widget list and button
code = code.replace("private final List<ClickableWidget> visualsAttackRangeWidgets = new ArrayList<>();", "private final List<ClickableWidget> visualsAttackRangeWidgets = new ArrayList<>();\n    private final List<ClickableWidget> visualsRadarWidgets = new ArrayList<>();")
code = code.replace("private LiquidGlassButton attackRangeBtn;", "private LiquidGlassButton attackRangeBtn;\n    private LiquidGlassButton radarBtn;")

# Clear widgets
code = code.replace("visualsAttackRangeWidgets.clear();", "visualsAttackRangeWidgets.clear();\n        visualsRadarWidgets.clear();")

# Add button in initVisualsTab
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

with open("src/main/java/com/example/glassmenu/screen/LiquidGlassScreen.java", "w") as f:
    f.write(code)
