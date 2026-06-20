with open("src/main/java/com/example/glassmenu/render/AttackRangeRenderer.java", "r") as f:
    code = f.read()

# Change the alpha values for FILLED mode
# Center vertex
code = code.replace("buffer.vertex(posMatrix, 0, 0, 0).color(r, g, b, 0.45f); // Bright center", "buffer.vertex(posMatrix, 0, 0, 0).color(r, g, b, 0.3f); // Uniform transparency")

# Edge vertices
code = code.replace("buffer.vertex(posMatrix, cx, 0, cz).color(r, g, b, 0.0f); // Fades to edges", "buffer.vertex(posMatrix, cx, 0, cz).color(r, g, b, 0.3f); // Solid transparency up to the contour")

with open("src/main/java/com/example/glassmenu/render/AttackRangeRenderer.java", "w") as f:
    f.write(code)

print("Patch applied for uniform fill")
