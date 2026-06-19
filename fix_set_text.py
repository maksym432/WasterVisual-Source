import re

with open("src/main/java/com/example/glassmenu/screen/LiquidGlassScreen.java", "r") as f:
    content = f.read()

# Pattern: if (var != null) var.setText(String.format(...));
def replacer(m):
    var_name = m.group(1)
    format_expr = m.group(2)
    return f'if ({var_name} != null) {{ String s = {format_expr}; if (!{var_name}.getText().equals(s)) {var_name}.setText(s); }}'

new_content = re.sub(r'if \(([a-zA-Z0-9_]+) != null\) \1\.setText\((String\.format\([^)]+\))\);', replacer, content)

with open("src/main/java/com/example/glassmenu/screen/LiquidGlassScreen.java", "w") as f:
    f.write(new_content)

print("Done")
