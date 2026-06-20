import re

with open("src/main/java/com/example/glassmenu/screen/LiquidGlassScreen.java", "r") as f:
    code = f.read()

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

# Patch render
render_search = """    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {"""
render_replace = """    @Override
    public void render(DrawContext context, int origMouseX, int origMouseY, float delta) {
        float scale = getMenuScale();
        int mouseX = origMouseX;
        int mouseY = origMouseY;
        if (scale < 1.0f && currentTab != Tab.POSITION) {
            mouseX = (int)((origMouseX - this.width / 2f) / scale + this.width / 2f);
            mouseY = (int)((origMouseY - this.height / 2f) / scale + this.height / 2f);
            context.getMatrices().push();
            context.getMatrices().translate(this.width / 2f, this.height / 2f, 0);
            context.getMatrices().scale(scale, scale, 1.0f);
            context.getMatrices().translate(-this.width / 2f, -this.height / 2f, 0);
        }"""
code = code.replace(render_search, render_replace)

# Pop matrices in render
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

# Patch mouseClicked
mc_search = """    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {"""
mc_replace = """    @Override public boolean mouseClicked(double origMouseX, double origMouseY, int button) {
        double mouseX = origMouseX; double mouseY = origMouseY;
        float scale = getMenuScale();
        if (scale < 1.0f && currentTab != Tab.POSITION) {
            mouseX = (origMouseX - this.width / 2f) / scale + this.width / 2f;
            mouseY = (origMouseY - this.height / 2f) / scale + this.height / 2f;
        }"""
code = code.replace(mc_search, mc_replace)

# Patch mouseReleased
mr_search = """    @Override public boolean mouseReleased(double mX, double mY, int b) {"""
mr_replace = """    @Override public boolean mouseReleased(double origMX, double origMY, int b) {
        double mX = origMX; double mY = origMY;
        float scale = getMenuScale();
        if (scale < 1.0f && currentTab != Tab.POSITION) {
            mX = (origMX - this.width / 2f) / scale + this.width / 2f;
            mY = (origMY - this.height / 2f) / scale + this.height / 2f;
        }"""
code = code.replace(mr_search, mr_replace)

# Patch mouseDragged
md_search = """    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {"""
md_replace = """    @Override
    public boolean mouseDragged(double origMouseX, double origMouseY, int button, double origDeltaX, double origDeltaY) {
        double mouseX = origMouseX; double mouseY = origMouseY; double deltaX = origDeltaX; double deltaY = origDeltaY;
        float scale = getMenuScale();
        if (scale < 1.0f && currentTab != Tab.POSITION) {
            mouseX = (origMouseX - this.width / 2f) / scale + this.width / 2f;
            mouseY = (origMouseY - this.height / 2f) / scale + this.height / 2f;
            deltaX = origDeltaX / scale;
            deltaY = origDeltaY / scale;
        }"""
code = code.replace(md_search, md_replace)

# Patch mouseScrolled
ms_search = """    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {"""
ms_replace = """    @Override public boolean mouseScrolled(double origMouseX, double origMouseY, double horizontalAmount, double verticalAmount) {
        double mouseX = origMouseX; double mouseY = origMouseY;
        float scale = getMenuScale();
        if (scale < 1.0f && currentTab != Tab.POSITION) {
            mouseX = (origMouseX - this.width / 2f) / scale + this.width / 2f;
            mouseY = (origMouseY - this.height / 2f) / scale + this.height / 2f;
        }"""
code = code.replace(ms_search, ms_replace)

with open("src/main/java/com/example/glassmenu/screen/LiquidGlassScreen.java", "w") as f:
    f.write(code)
