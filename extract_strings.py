import re
import json

with open("src/main/java/com/example/glassmenu/screen/LiquidGlassScreen.java", "r") as f:
    code = f.read()

strings = set()

# Find context.drawTextWithShadow(textRenderer, "...",
for match in re.finditer(r'context\.drawTextWithShadow\([^,]+,\s*"([^"]+)"', code):
    strings.add(match.group(1))

# Find context.drawCenteredTextWithShadow(textRenderer, "...",
for match in re.finditer(r'context\.drawCenteredTextWithShadow\([^,]+,\s*"([^"]+)"', code):
    strings.add(match.group(1))

# Find Text.literal("...")
for match in re.finditer(r'Text\.literal\("([^"]+)"\)', code):
    strings.add(match.group(1))

print("Found", len(strings), "unique strings.")
print(list(strings))
