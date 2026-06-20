import re

with open("src/main/java/com/example/glassmenu/screen/LiquidGlassScreen.java", "r") as f:
    code = f.read()

# I will add a toggle button in `initVisualsAttackRangeTab`
# Let's find: visualsAttackRangeWidgets.add(toggle);
# and add the new toggle right after it.

new_toggle = """        LiquidGlassSwitch toggle = new LiquidGlassSwitch((int)x + 330, (int)y + 45, 40, 20, GlassMenuClient.CONFIG.enableAttackRange());
        toggle.setOnToggle(enabled -> {
            GlassMenuClient.CONFIG.enableAttackRange(enabled); GlassMenuClient.CONFIG.save();
        });
        visualsAttackRangeWidgets.add(toggle);

        LiquidGlassSwitch gearToggle = new LiquidGlassSwitch((int)x + 330, (int)y + 75, 40, 20, GlassMenuClient.CONFIG.attackRangeGear());
        gearToggle.setOnToggle(enabled -> {
            GlassMenuClient.CONFIG.attackRangeGear(enabled); GlassMenuClient.CONFIG.save();
        });
        visualsAttackRangeWidgets.add(gearToggle);"""

code = code.replace("""        LiquidGlassSwitch toggle = new LiquidGlassSwitch((int)x + 330, (int)y + 45, 40, 20, GlassMenuClient.CONFIG.enableAttackRange());
        toggle.setOnToggle(enabled -> {
            GlassMenuClient.CONFIG.enableAttackRange(enabled); GlassMenuClient.CONFIG.save();
        });
        visualsAttackRangeWidgets.add(toggle);""", new_toggle)

# Update renderVisualsAttackRangeTab to draw the text "Enable Gear/Clock Effect"
new_render_text = """        context.drawTextWithShadow(textRenderer, "Enable Attack Range", x + 40, y + 80 - (int)slideOffset, colorAlpha | 0xAAAAAA);
        context.drawTextWithShadow(textRenderer, "Gear / Clock Effect", x + 40, y + 110 - (int)slideOffset, colorAlpha | 0xAAAAAA);"""

code = code.replace("""        context.drawTextWithShadow(textRenderer, "Enable Attack Range", x + 40, y + 80 - (int)slideOffset, colorAlpha | 0xAAAAAA);""", new_render_text)

# Also need to move color sliders down so they don't overlap with the new Gear toggle!
# Originally they were at y+90, y+125, y+160.
# I need to shift everything down if there is space.
# Wait, the panel has 280 height. We can shift color sliders to y+110, y+145, y+180
# and shift the "Mode" button to y+215. Wait, backBtn is at y+223. That might be tight.
# Instead of moving sliders down, I can put the Gear toggle on the left column or shift color sliders to right?
# Let's just put the Gear toggle at x+330, y+75.
# And text at x+40, y+110? No, y+110 is where the color inputs are!
# Look at my previous code:
# color sliders were at y+90, y+125, y+160.
# hex input was at x+40, y+90.
# rgb input was at x+40, y+125.
# mode btn was at x+40, y+160.

# If I put Gear toggle at y+75, it might overlap with hex input at y+90 (diff 15px, slider is 20px).
# Let's move hex input to y+100, rgb to y+135, mode to y+175.
# And sliders to y+100, y+135, y+170.

code = code.replace("attackRangeSliderR = new LiquidGlassSlider((int)x + 230, (int)y + 90, 140, 16,", "attackRangeSliderR = new LiquidGlassSlider((int)x + 230, (int)y + 110, 140, 16,")
code = code.replace("attackRangeSliderG = new LiquidGlassSlider((int)x + 230, (int)y + 125, 140, 16,", "attackRangeSliderG = new LiquidGlassSlider((int)x + 230, (int)y + 145, 140, 16,")
code = code.replace("attackRangeSliderB = new LiquidGlassSlider((int)x + 230, (int)y + 160, 140, 16,", "attackRangeSliderB = new LiquidGlassSlider((int)x + 230, (int)y + 180, 140, 16,")

code = code.replace("attackRangeHexInput = createColorTextField((int)x + 40, (int)y + 90, 60);", "attackRangeHexInput = createColorTextField((int)x + 40, (int)y + 110, 60);")
code = code.replace("attackRangeRgbInput = createColorTextField((int)x + 40, (int)y + 125, 100);", "attackRangeRgbInput = createColorTextField((int)x + 40, (int)y + 145, 100);")
code = code.replace("LiquidGlassButton modeBtn = new LiquidGlassButton((int)x + 40, (int)y + 160, 150, 22,", "LiquidGlassButton modeBtn = new LiquidGlassButton((int)x + 40, (int)y + 180, 150, 22,")

# Oh wait! "Enable Attack Range" text was at `y + 80`. Let's change its y to `y + 50`!
code = code.replace("x + 40, y + 80 - (int)slideOffset", "x + 40, y + 50 - (int)slideOffset")
# Gear text at `y + 80`
code = code.replace("x + 40, y + 110 - (int)slideOffset", "x + 40, y + 80 - (int)slideOffset")

with open("src/main/java/com/example/glassmenu/screen/LiquidGlassScreen.java", "w") as f:
    f.write(code)

print("Menu GUI updated for Gear effect")
