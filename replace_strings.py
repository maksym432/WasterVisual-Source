import re

with open("src/main/java/com/example/glassmenu/screen/LiquidGlassScreen.java", "r") as f:
    code = f.read()

# Add import if missing
if "import com.example.glassmenu.util.MenuTranslator;" not in code:
    code = code.replace("import com.example.glassmenu.GlassMenuClient;", "import com.example.glassmenu.GlassMenuClient;\nimport com.example.glassmenu.util.MenuTranslator;")

# Replace context.drawTextWithShadow(textRenderer, "...",
code = re.sub(
    r'context\.drawTextWithShadow\([^,]+,\s*"([^"]+)"',
    lambda m: f'context.drawTextWithShadow(textRenderer, MenuTranslator.tr("{m.group(1)}")',
    code
)

# Replace context.drawCenteredTextWithShadow(textRenderer, "...",
code = re.sub(
    r'context\.drawCenteredTextWithShadow\([^,]+,\s*"([^"]+)"',
    lambda m: f'context.drawCenteredTextWithShadow(textRenderer, MenuTranslator.tr("{m.group(1)}")',
    code
)

# Replace context.drawText(textRenderer, "...",
code = re.sub(
    r'context\.drawText\([^,]+,\s*"([^"]+)"',
    lambda m: f'context.drawText(client.textRenderer, MenuTranslator.tr("{m.group(1)}")',
    code
)

# Replace Text.literal("...")
code = re.sub(
    r'Text\.literal\("([^"]+)"\)',
    lambda m: f'Text.literal(MenuTranslator.tr("{m.group(1)}"))',
    code
)

# Also fix the topTabs enum names (GENERAL, POSITIONS, VISUALS, COMBAT) 
# which is context.drawText(textRenderer, topTabs[i].name(), ...
code = code.replace(
    'context.drawText(textRenderer, topTabs[i].name()',
    'context.drawText(textRenderer, MenuTranslator.tr(topTabs[i].name())'
)
code = code.replace(
    'textRenderer.getWidth(topTabs[i].name())',
    'textRenderer.getWidth(MenuTranslator.tr(topTabs[i].name()))'
)

with open("src/main/java/com/example/glassmenu/screen/LiquidGlassScreen.java", "w") as f:
    f.write(code)

print("Strings replaced with MenuTranslator calls.")
