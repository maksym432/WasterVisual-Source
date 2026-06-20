import re

with open("src/main/java/com/example/glassmenu/screen/LiquidGlassScreen.java", "r") as f:
    code = f.read()

# Add to Tab enum
code = code.replace("VISUALS_NAMETAGS, VISUALS_ATTACK_RANGE, POSITION, BRIDGE", "VISUALS_NAMETAGS, VISUALS_ATTACK_RANGE, VISUALS_RADAR, POSITION, BRIDGE")

# Add to getPanelW and getPanelH and panelWidthProgress
for search in ["VISUALS_ATTACK_RANGE || currentTab == Tab.BRIDGE", "VISUALS_ATTACK_RANGE || currentTab == Tab.BRIDGE) ? PANEL_W_EXPANDED", "VISUALS_ATTACK_RANGE || currentTab == Tab.BRIDGE) ? PANEL_H_EXPANDED"]:
    code = code.replace(search, search.replace("VISUALS_ATTACK_RANGE", "VISUALS_ATTACK_RANGE || currentTab == Tab.VISUALS_RADAR"))

# Add active status for main VISUALS tab to keep it highlighted
tab_hover_search = "currentTab == Tab.VISUALS_NAMETAGS || currentTab == Tab.POSITION"
tab_hover_replace = "currentTab == Tab.VISUALS_NAMETAGS || currentTab == Tab.VISUALS_RADAR || currentTab == Tab.POSITION"
code = code.replace(tab_hover_search, tab_hover_replace)

# Add widget list
code = code.replace("private final List<ClickableWidget> visualsAttackRangeWidgets = new ArrayList<>();", "private final List<ClickableWidget> visualsAttackRangeWidgets = new ArrayList<>();\n    private final List<ClickableWidget> visualsRadarWidgets = new ArrayList<>();")

# Add button
code = code.replace("private LiquidGlassButton visualsAttackRangeBtn;", "private LiquidGlassButton visualsAttackRangeBtn;\n    private LiquidGlassButton visualsRadarBtn;")

# Clear widgets
code = code.replace("visualsAttackRangeWidgets.clear();", "visualsAttackRangeWidgets.clear();\n        visualsRadarWidgets.clear();")

# Add to initVisualsTab (add the button for Radar)
# First we need to find the layout loops or button creations. Let's see how other buttons are created in initVisualsTab.
