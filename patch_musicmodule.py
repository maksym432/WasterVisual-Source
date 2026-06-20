import re

with open("src/main/java/com/example/glassmenu/widget/impl/MusicModule.java", "r") as f:
    code = f.read()

old_draw = """        if (shader == null) {
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
            context.drawTexture(texture, (int) x, (int) y, 0, 0, (int) size, (int) size, (int) size, (int) size);"""

new_draw = """        if (shader == null) {
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
            context.drawTexture(texture, (int) x, (int) y, (int) size, (int) size, 0.0f, 0.0f, tw, th, tw, th);"""

code = code.replace(old_draw, new_draw)

with open("src/main/java/com/example/glassmenu/widget/impl/MusicModule.java", "w") as f:
    f.write(code)
