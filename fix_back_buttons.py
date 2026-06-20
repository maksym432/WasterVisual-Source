import re

with open("src/main/java/com/example/glassmenu/screen/LiquidGlassScreen.java", "r") as f:
    code = f.read()

# Replace any backBtn with Y coordinate > 225 to 220
def fix_y(m):
    x_part = m.group(1)
    y_val = int(m.group(2))
    rest = m.group(3)
    if y_val > 225:
        y_val = 220
    return f"LiquidGlassButton((int)x + {x_part}, (int)y + {y_val}, {rest}"

code = re.sub(r'LiquidGlassButton\(\(int\)x \+ (\d+),\s*\(int\)y \+ (\d+),\s*(.*?Text\.literal\(MenuTranslator\.tr\("Back"\)[^\)]*\))', fix_y, code)

with open("src/main/java/com/example/glassmenu/screen/LiquidGlassScreen.java", "w") as f:
    f.write(code)

print("Fixed Back buttons Y coordinates.")
