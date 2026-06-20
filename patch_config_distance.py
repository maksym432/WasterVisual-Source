import re

with open("src/main/java/com/example/glassmenu/GlassMenuConfigModel.java", "r") as f:
    code = f.read()

if "crosshairRadarSearchDistance" not in code:
    code = code.replace("public float crosshairRadarIconSize = 6.0f;", "public float crosshairRadarIconSize = 6.0f;\n    public float crosshairRadarSearchDistance = 64.0f;")

with open("src/main/java/com/example/glassmenu/GlassMenuConfigModel.java", "w") as f:
    f.write(code)
