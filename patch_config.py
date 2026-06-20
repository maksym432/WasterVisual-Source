import re

with open("src/main/java/com/example/glassmenu/GlassMenuConfigModel.java", "r") as f:
    code = f.read()

if "crosshairRadarIconSize" not in code:
    code = code.replace("public float crosshairRadarRadius = 60.0f;", "public float crosshairRadarRadius = 60.0f;\n    public float crosshairRadarIconSize = 6.0f;")

with open("src/main/java/com/example/glassmenu/GlassMenuConfigModel.java", "w") as f:
    f.write(code)
