import re

with open("src/main/java/com/example/glassmenu/screen/LiquidGlassScreen.java", "r") as f:
    code = f.read()

btn_search = """        LiquidGlassButton attackRangeBtn = new LiquidGlassButton(0, 0, 160, 22, Text.literal(MenuTranslator.tr("Attack Range ESP")), b -> {
            currentTab = Tab.VISUALS_ATTACK_RANGE; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        visualsWidgets.add(attackRangeBtn);"""
        
btn_replace = btn_search + """
        
        radarBtn = new LiquidGlassButton(0, 0, 160, 22, Text.literal(MenuTranslator.tr("Player Radar")), b -> {
            currentTab = Tab.VISUALS_RADAR; contentAlpha = 0.0f; updateVisibleWidgets();
        });
        visualsWidgets.add(radarBtn);"""

code = code.replace(btn_search, btn_replace)

with open("src/main/java/com/example/glassmenu/screen/LiquidGlassScreen.java", "w") as f:
    f.write(code)
