import re

with open("src/main/java/com/example/glassmenu/render/CrosshairRadarRenderer.java", "r") as f:
    code = f.read()

code = code.replace("float arrowSize = 6.0f;", "float arrowSize = GlassMenuClient.CONFIG.crosshairRadarIconSize();")

with open("src/main/java/com/example/glassmenu/render/CrosshairRadarRenderer.java", "w") as f:
    f.write(code)
