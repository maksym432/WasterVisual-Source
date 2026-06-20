import re

with open("src/main/java/com/example/glassmenu/GlassMenuClient.java", "r") as f:
    code = f.read()

search = """        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.currentScreen == null) {"""

replace = """        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            
            // Render Crosshair Radar over everything when in game
            if (client.currentScreen == null) {
                com.example.glassmenu.render.CrosshairRadarRenderer.render(context, tickCounter.getTickDelta(true));
            }

            if (client.currentScreen == null) {"""

code = code.replace(search, replace)

with open("src/main/java/com/example/glassmenu/GlassMenuClient.java", "w") as f:
    f.write(code)
