import re

# 1. Update GlassMenuConfigModel.java
with open("src/main/java/com/example/glassmenu/GlassMenuConfigModel.java", "r") as f:
    config = f.read()

config = config.replace("public AttackRangeMode attackRangeMode = AttackRangeMode.GLOW_OUTLINE;",
"""public AttackRangeMode attackRangeMode = AttackRangeMode.GLOW_OUTLINE;
    public boolean attackRangeGear = false;""")

with open("src/main/java/com/example/glassmenu/GlassMenuConfigModel.java", "w") as f:
    f.write(config)

# 2. Update AttackRangeRenderer.java
with open("src/main/java/com/example/glassmenu/render/AttackRangeRenderer.java", "r") as f:
    renderer = f.read()

# FILLED mode: add outline
filled_replace = """if (mode == GlassMenuConfigModel.AttackRangeMode.FILLED) {
                    RenderSystem.setShader(GameRenderer::getPositionColorProgram);
                    BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
                    buffer.vertex(posMatrix, 0, 0, 0).color(r, g, b, 0.45f); // Bright center
                    for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
                        float angle = (float) (i * Math.PI * 2.0 / CIRCLE_SEGMENTS);
                        float cx = MathHelper.cos(angle) * getRadius(angle, time, isGear);
                        float cz = MathHelper.sin(angle) * getRadius(angle, time, isGear);
                        buffer.vertex(posMatrix, cx, 0, cz).color(r, g, b, 0.0f); // Fades to edges
                    }
                    BufferRenderer.drawWithGlobalProgram(buffer.end());

                    // Add outline for FILLED mode as requested
                    RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);
                    RenderSystem.lineWidth(2.0f);
                    BufferBuilder outlineBuf = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
                    for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
                        float angle = (float) (i * Math.PI * 2.0 / CIRCLE_SEGMENTS);
                        float cx = MathHelper.cos(angle) * getRadius(angle, time, isGear);
                        float cz = MathHelper.sin(angle) * getRadius(angle, time, isGear);
                        outlineBuf.vertex(posMatrix, cx, 0, cz).color(r, g, b, 0.8f);
                    }
                    BufferRenderer.drawWithGlobalProgram(outlineBuf.end());
                }"""

# SOLID_OUTLINE mode: thicker line
solid_replace = """} else if (mode == GlassMenuConfigModel.AttackRangeMode.SOLID_OUTLINE) {
                    RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);
                    RenderSystem.lineWidth(5.0f); // Thicker outline as requested
                    BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
                    for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
                        float angle = (float) (i * Math.PI * 2.0 / CIRCLE_SEGMENTS);
                        float cx = MathHelper.cos(angle) * getRadius(angle, time, isGear);
                        float cz = MathHelper.sin(angle) * getRadius(angle, time, isGear);
                        buffer.vertex(posMatrix, cx, 0, cz).color(r, g, b, 0.9f);
                    }
                    BufferRenderer.drawWithGlobalProgram(buffer.end());
                }"""

# GLOW_OUTLINE mode: updated to use getRadius
glow_replace = """} else if (mode == GlassMenuConfigModel.AttackRangeMode.GLOW_OUTLINE) {
                    RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);
                    // Pass 1: Thick glow
                    RenderSystem.lineWidth(6.0f);
                    BufferBuilder buffer1 = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
                    for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
                        float angle = (float) (i * Math.PI * 2.0 / CIRCLE_SEGMENTS);
                        float cx = MathHelper.cos(angle) * getRadius(angle, time, isGear);
                        float cz = MathHelper.sin(angle) * getRadius(angle, time, isGear);
                        buffer1.vertex(posMatrix, cx, 0, cz).color(r, g, b, 0.25f);
                    }
                    BufferRenderer.drawWithGlobalProgram(buffer1.end());
                    
                    // Pass 2: Bright core line
                    RenderSystem.lineWidth(1.5f);
                    BufferBuilder buffer2 = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
                    for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
                        float angle = (float) (i * Math.PI * 2.0 / CIRCLE_SEGMENTS);
                        float cx = MathHelper.cos(angle) * getRadius(angle, time, isGear);
                        float cz = MathHelper.sin(angle) * getRadius(angle, time, isGear);
                        buffer2.vertex(posMatrix, cx, 0, cz).color(r, g, b, 0.95f);
                    }
                    BufferRenderer.drawWithGlobalProgram(buffer2.end());
                }"""

# We need to add the getRadius helper method and the time/isGear variables
helper_method = """
    private static float getRadius(float angle, float time, boolean isGear) {
        float currentRadius = ATTACK_RANGE_RADIUS;
        if (isGear) {
            // 12 bumps, rotating. Changing form via secondary sine wave.
            float wave1 = (float) Math.sin(angle * 12 - time * 2.5f);
            float wave2 = (float) Math.sin(angle * 6 + time * 1.5f);
            currentRadius += (wave1 * 0.15f) + (wave2 * 0.05f);
        }
        return currentRadius;
    }

    public static void render"""

renderer = renderer.replace("public static void render", helper_method)

# Inject variables
vars_inject = """        GlassMenuConfigModel.AttackRangeMode mode = GlassMenuClient.CONFIG.attackRangeMode();
        boolean isGear = GlassMenuClient.CONFIG.attackRangeGear();
        float time = (System.currentTimeMillis() % 100000L) / 1000f;"""
renderer = renderer.replace("GlassMenuConfigModel.AttackRangeMode mode = GlassMenuClient.CONFIG.attackRangeMode();", vars_inject)

# Replace the blocks
import re
# FILLED
renderer = re.sub(r'if \(mode == GlassMenuConfigModel\.AttackRangeMode\.FILLED\) \{.*?BufferRenderer\.drawWithGlobalProgram\(buffer\.end\(\)\);\s*\}', filled_replace, renderer, flags=re.DOTALL)
# SOLID
renderer = re.sub(r'\} else if \(mode == GlassMenuConfigModel\.AttackRangeMode\.SOLID_OUTLINE\) \{.*?BufferRenderer\.drawWithGlobalProgram\(buffer\.end\(\)\);\s*\}', solid_replace, renderer, flags=re.DOTALL)
# GLOW
renderer = re.sub(r'\} else if \(mode == GlassMenuConfigModel\.AttackRangeMode\.GLOW_OUTLINE\) \{.*?BufferRenderer\.drawWithGlobalProgram\(buffer2\.end\(\)\);\s*\}', glow_replace, renderer, flags=re.DOTALL)


with open("src/main/java/com/example/glassmenu/render/AttackRangeRenderer.java", "w") as f:
    f.write(renderer)

print("Renderer updated")
