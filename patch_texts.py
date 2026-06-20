import re

# 1. Fix LiquidGlassScreen.java text rendering
with open("src/main/java/com/example/glassmenu/screen/LiquidGlassScreen.java", "r") as f:
    code = f.read()

bad_text = """        context.drawTextWithShadow(textRenderer, "Attack Range ESP Settings", x + 30, y + 45 - (int)slideOffset, colorAlpha | 0xFFFFFF);
        
        context.drawTextWithShadow(textRenderer, "Enable Attack Range", x + 40, y + 50 - (int)slideOffset, colorAlpha | 0xAAAAAA);"""

good_text = """        context.drawCenteredTextWithShadow(textRenderer, "Attack Range ESP Settings", x + 200, y + 20 - (int)slideOffset, colorAlpha | 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, "Enable Attack Range", x + 40, y + 50 - (int)slideOffset, colorAlpha | 0xAAAAAA);"""

code = code.replace(bad_text, good_text)

with open("src/main/java/com/example/glassmenu/screen/LiquidGlassScreen.java", "w") as f:
    f.write(code)


# 2. Fix AttackRangeRenderer.java smoothing and filled mode outline
with open("src/main/java/com/example/glassmenu/render/AttackRangeRenderer.java", "r") as f:
    renderer = f.read()

renderer = renderer.replace("private static final int CIRCLE_SEGMENTS = 64;", "private static final int CIRCLE_SEGMENTS = 360;")
renderer = renderer.replace("outlineBuf.vertex(posMatrix, cx, 0, cz).color(r, g, b, 0.8f);", "outlineBuf.vertex(posMatrix, cx, 0, cz).color(r, g, b, 0.35f);")

with open("src/main/java/com/example/glassmenu/render/AttackRangeRenderer.java", "w") as f:
    f.write(renderer)

print("Patch applied successfully")
