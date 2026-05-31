# Project Instructions: WasterVisual (wastervisual)

## Workflow Conventions
- **Versioning**: Always increment the `mod_version` in `gradle.properties` before finalizing a set of changes or bug fixes. This ensures that the version reflected in Mod Menu and build artifacts is always up-to-date.
- **UI Rendering**: Prioritize SDF-based rendering for rounded shapes to maintain high-fidelity anti-aliasing.
- **Image Assets**: When rendering PNG textures, ensure aspect ratio correction (Center Crop) is handled, preferably at the shader level.

## Architectural Standards
- **File Documentation**: EVERY file MUST begin with a descriptive header comment explaining its primary responsibility and architecture role.
- **Rendering Hooks**: DO NOT use Mixins for world rendering (e.g., `WorldRenderer`, `GameRenderer`) to avoid conflicts with optimization mods like Sodium or Iris.
- **Fabric API Events**: Always use `WorldRenderEvents` and `HudRenderCallback` from the Fabric API for adding graphics to the world or HUD.
- **Background Effects**: UI background effects (like dark overlays) should be implemented in the `render` method of the `Screen` or `Widget` using `RenderSystem` state resets to prevent texture atlas leakage.
- **OpenGL/Shader State Cleanup**: Any custom rendering code using Fabric API rendering hooks (like `WorldRenderEvents.LAST`) must completely clean up its modified states to prevent leaks that can corrupt subsequent rendering pipelines.
  - Always restore the original shader program using `RenderSystem.getShader()` and `RenderSystem.setShader(() -> previousShader)` if you changed it.
  - Always restore the active texture bound to slot 0 (query it via `GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)` before changing texture bindings, and restore it via `RenderSystem.setShaderTexture(0, previousTexture)`). This is strictly handled in `GlassRefractionEngine.java` and `RenderUtils.java` (in `drawSdfRoundedRect`/`drawSdfRoundedOutline`) to prevent desynchronizing Minecraft's texture bindings.
  - DO NOT bind any custom textures to slot 0 when rendering with non-textured programs (like `getPositionColorProgram`) in world space, as this overrides the active texture bindings (like block/entity atlases) and breaks all textures and skins rendered afterwards. Only bind textures in GUI space or when using textured shaders.
  - Always restore blending states (e.g. call `RenderSystem.disableBlend()` if blending was enabled temporarily).
  - Always flush any buffered geometry in the immediate provider (`getEntityVertexConsumers().draw()`) at the end of the callback to prevent delayed flushes under a dirty state.

## Configuration
- This project uses `owo-lib` for its configuration system and Mod Menu integration.
- The config model is located in `GlassMenuConfigModel.java`.

## Recent Implementations & Shader Conventions
- **BedWars ESP Teammate Hearts**:
  - Teammate hearts are rendered using a flat vector heart shape made of **6 triangles** (`VertexFormat.DrawMode.TRIANGLES`) in camera-billboarded space.
  - Position: height offset `+1.4f` blocks above the player's head, scale `0.9f`.
  - Condition: rendered if `player.isTeammate(client.player)` and config setting `enableBedWarsHearts` is enabled.
  - White Team Exclusion: If a player's team color is white (`0xFFFFFFFF`), the ESP (hitbox, exclamation, or heart icon) is completely skipped.
- **Hitbox Line Thickness**:
  - The default hitbox ESP uses `RenderSystem.lineWidth(6.0f)` for thick, highly visible lines, which is restored to `1.0f` immediately after rendering.
- **Post-Processing Shaders**:
  - For post-processing shaders loaded via vanilla Minecraft (e.g. `assets/minecraft/shaders/post/glass_blur.json`), program names must NOT contain custom namespaces with colons (like `"glassmenu:glass_blur"`) inside the program JSON. Vanilla's path validator parses the whole string and treats the colon as an illegal character in the path, crashing shader compilation.
  - Always use the `minecraft` namespace prefix (e.g., `"minecraft:glass_blur"`) for files placed under `assets/minecraft/shaders/program/`.
- **User Indicator Widget**:
  - Displays real-time FPS, latency (Ping in ms), and mouse clicks per second (CPS) for Left/Right buttons in a single inline horizontal row.
  - Interactive mouse clicks are registered via GLFW mouse button press action listener in `MouseMixin`.
  - Configurable options: toggle visibility (`enableUserIndicator`), toggle transparency (`transparentUserIndicator`), and adjust color values using sliders.
  - Resizable and draggable in the Position tab: customizable width and height clamped to safe ranges (width: 100-350, height: 15-60) and draggable layout positioning.
  - Transparent Background: uses `GlassRefractionEngine.drawRefractedPanel` with `0x22FFFFFF` overlay color and a subtle white border outline.
  - Non-Transparent Background: uses solid black (`0xFF000000`) color inside the main container.
  - Separate Rounded Squares: stats (FPS, Ping, CPS) are drawn inside individual rounded rectangles with slot fill colors. The slot border outlines are disabled (invisible) when transparency is OFF to avoid white frames around the boxes.
  - Text Coloring: text is drawn in black (`0xFF000000`) without shadow when transparency is OFF, and in white (`0xFFFFFFFF`) with shadow when transparency is ON.
  - Dynamic Positioning Scale: background, indicators, and text elements scale uniformly when resized via matrix multiplication transformation matrix inside the renderer.

## Saved Release Checkpoints
- **Release v2.23.0** (Tag: `v2.23.0`): Features the Armor HUD overlay (configurable color, horizontal/vertical orientation, transparency, and Position scaling/dragging) and a redesigned vertical category settings menu list with mouse wheel scrolling and scissor clipping. Revert or checkout using `git checkout v2.23.0`.
