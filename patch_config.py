import re

with open("src/main/java/com/example/glassmenu/GlassMenuConfigModel.java", "r") as f:
    code = f.read()

# Add configuration options for Crosshair Radar
radar_config = """
    // --- CROSSHAIR RADAR ---
    public boolean enableCrosshairRadar = false;
    public int crosshairRadarColor = 0xFF00FF00; // Default green
    public boolean crosshairRadarRgb = false;
    public float crosshairRadarRadius = 60.0f;
}
"""
code = re.sub(r'}\s*$', radar_config, code)

with open("src/main/java/com/example/glassmenu/GlassMenuConfigModel.java", "w") as f:
    f.write(code)
