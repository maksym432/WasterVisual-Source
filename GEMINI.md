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

## Configuration
- This project uses `owo-lib` for its configuration system and Mod Menu integration.
- The config model is located in `GlassMenuConfigModel.java`.
