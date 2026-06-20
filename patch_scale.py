import re

with open("src/main/java/com/example/glassmenu/screen/LiquidGlassScreen.java", "r") as f:
    code = f.read()

# 1. Add getMenuScale() method
scale_method = """
    private float getMenuScale() {
        if (currentTab == Tab.POSITION) return 1.0f;
        float maxW = this.width - 20f;
        float maxH = this.height - 20f;
        float scaleW = maxW / PANEL_W_EXPANDED;
        float scaleH = maxH / PANEL_H_EXPANDED;
        return Math.min(1.0f, Math.min(scaleW, scaleH));
    }
"""
code = code.replace("    public LiquidGlassScreen() {", scale_method + "\n    public LiquidGlassScreen() {")

# 2. Modify init() to use logical width/height for x and y
init_search = """        float x = (this.width - getPanelW()) / 2f;
        float y = (this.height - getPanelH()) / 2f;"""
init_replace = """        float scale = getMenuScale();
        float logicalWidth = this.width / scale;
        float logicalHeight = this.height / scale;
        float x = (logicalWidth - getPanelW()) / 2f;
        float y = (logicalHeight - getPanelH()) / 2f;"""
code = code.replace(init_search, init_replace)

# 3. Patch render() method
render_search = """    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {"""
render_replace = """    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float scale = getMenuScale();
        int logicalMouseX = (int)(mouseX / scale);
        int logicalMouseY = (int)(mouseY / scale);
        context.getMatrices().push();
        if (scale < 1.0f && currentTab != Tab.POSITION) {
            context.getMatrices().scale(scale, scale, 1.0f);
        }
        // Use logicalMouseX and logicalMouseY for everything in render
        int origMouseX = mouseX;
        int origMouseY = mouseY;
        mouseX = logicalMouseX;
        mouseY = logicalMouseY;"""

code = code.replace(render_search, render_replace)

# End of render: pop matrices and restore mouseX/Y before super.render?
# Actually, the original render ends with super.render(context, mouseX, mouseY, delta);
# We need to pop matrices!
render_end_search = """        else if (currentTab == Tab.POSITION) renderPositionTab(context, mouseX, mouseY, delta);

        super.render(context, mouseX, mouseY, delta);
    }"""
render_end_replace = """        else if (currentTab == Tab.POSITION) renderPositionTab(context, mouseX, mouseY, delta);

        super.render(context, mouseX, mouseY, delta);
        if (scale < 1.0f && currentTab != Tab.POSITION) {
            context.getMatrices().pop();
        }
    }"""
code = code.replace(render_end_search, render_end_replace)

with open("src/main/java/com/example/glassmenu/screen/LiquidGlassScreen.java", "w") as f:
    f.write(code)
