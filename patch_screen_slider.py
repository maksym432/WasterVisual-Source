import re

with open("src/main/java/com/example/glassmenu/screen/LiquidGlassScreen.java", "r") as f:
    code = f.read()

# In initVisualsRadarTab, after radiusSlider
search_init = """        visualsRadarWidgets.add(radiusSlider);
    }"""
replace_init = """        visualsRadarWidgets.add(radiusSlider);
        
        LiquidGlassSlider sizeSlider = new LiquidGlassSlider((int)x + 195, (int)y + 235, 140, 16, (GlassMenuClient.CONFIG.crosshairRadarIconSize() - 2f) / 18f);
        sizeSlider.setOnValueChange(v -> {
            float size = 2f + (float)(double)v * 18f;
            GlassMenuClient.CONFIG.crosshairRadarIconSize(size); GlassMenuClient.CONFIG.save();
        });
        visualsRadarWidgets.add(sizeSlider);
    }"""
code = code.replace(search_init, replace_init)

# In renderVisualsRadarTab, after "Radius"
search_render = """        context.drawTextWithShadow(textRenderer, "Radius", (int)x + 180, (int)y + 209, 0xFFFFFFFF);
        visualsRadarWidgets.get(7).render(context, mouseX, mouseY, delta); // Radius slider"""
replace_render = """        context.drawTextWithShadow(textRenderer, MenuTranslator.tr("Radius"), (int)x + 130, (int)y + 209, 0xFFFFFFFF);
        visualsRadarWidgets.get(7).render(context, mouseX, mouseY, delta); // Radius slider
        
        context.drawTextWithShadow(textRenderer, MenuTranslator.tr("Icon Size"), (int)x + 130, (int)y + 239, 0xFFFFFFFF);
        visualsRadarWidgets.get(8).render(context, mouseX, mouseY, delta); // Icon size slider"""
code = code.replace(search_render, replace_render)

with open("src/main/java/com/example/glassmenu/screen/LiquidGlassScreen.java", "w") as f:
    f.write(code)
