# Changelog

## [2.24.79] - 2026-06-19

### Changed
- **Custom Crosshair Shader**: Added dedicated `custom_crosshair` shader.
- Added RGB Rainbow effect with toggle and speed controls.
- Improved `4-Corners` mode to render smooth L-shapes for a proper square outline.
- Fixed panel sizing issue when switching to the Custom Crosshair menu tab.## [2.24.78] - 2026-06-19

### Added
- **Custom Crosshair**: Added highly customizable SDF-based crosshair module to the Visuals menu.
  - Toggles on/off, automatically replacing the vanilla crosshair.
  - Support for 4 modes: Rounded Cross, Dot, Circle, and 4-Corners.
  - All modes use hardware-accelerated SDF rendering for perfectly rounded shapes with zero pixelation.
  - Fully customizable size, thickness, gap, and RGB color.

### Files Modified
- [GlassMenuConfigModel.java](file:///home/waster/vibecoding/minecraft/wastervisual/src/main/java/com/example/glassmenu/GlassMenuConfigModel.java)
- [LiquidGlassScreen.java](file:///home/waster/vibecoding/minecraft/wastervisual/src/main/java/com/example/glassmenu/screen/LiquidGlassScreen.java)
- [InGameHudMixin.java](file:///home/waster/vibecoding/minecraft/wastervisual/src/main/java/com/example/glassmenu/mixin/InGameHudMixin.java)
- [CustomCrosshairEngine.java](file:///home/waster/vibecoding/minecraft/wastervisual/src/main/java/com/example/glassmenu/render/CustomCrosshairEngine.java)
## [2.24.77] - 2026-06-19

### Added
- **Global Color Grading**: Added "Color Grading" configuration to the Visuals menu.
  - Apply global saturation, contrast, and color tint filters to the entire game (world + UI).
  - Configurable via 5 sliders: Saturation, Contrast, Tint Red, Tint Green, and Tint Blue.
  - High-performance, robust post-processing implemented via framebuffer texture copying and a custom core shader (`color_grading.fsh`), injected at the end of `GameRenderer#render`.

### Files Modified
- [GlassMenuConfigModel.java](file:///home/waster/vibecoding/minecraft/wastervisual/src/main/java/com/example/glassmenu/GlassMenuConfigModel.java)
- [LiquidGlassScreen.java](file:///home/waster/vibecoding/minecraft/wastervisual/src/main/java/com/example/glassmenu/screen/LiquidGlassScreen.java)
- [ColorGradingEngine.java](file:///home/waster/vibecoding/minecraft/wastervisual/src/main/java/com/example/glassmenu/render/ColorGradingEngine.java)
- [ModShaders.java](file:///home/waster/vibecoding/minecraft/wastervisual/src/main/java/com/example/glassmenu/shader/ModShaders.java)
- `color_grading` shader files
- [GameRendererMixin.java](file:///home/waster/vibecoding/minecraft/wastervisual/src/main/java/com/example/glassmenu/mixin/GameRendererMixin.java)
## [2.24.76] - 2026-06-19

### Added
- **Stretch Resolution**: Added "Stretch" configuration to the Visuals menu.
  - Allows players to stretch the 3D world horizontally and vertically, simulating a 4:3 stretched aspect ratio.
  - Features an enable/disable toggle, horizontal/vertical scaling sliders, and a reset button.
  - Implemented via a Mixin into `GameRenderer#getBasicProjectionMatrix`, scaling the `Matrix4f` projection matrix mathematically.

### Files Modified
- [GlassMenuConfigModel.java](file:///home/waster/vibecoding/minecraft/wastervisual/src/main/java/com/example/glassmenu/GlassMenuConfigModel.java)
- [LiquidGlassScreen.java](file:///home/waster/vibecoding/minecraft/wastervisual/src/main/java/com/example/glassmenu/screen/LiquidGlassScreen.java)
- [GameRendererMixin.java](file:///home/waster/vibecoding/minecraft/wastervisual/src/main/java/com/example/glassmenu/mixin/GameRendererMixin.java)
- [glassmenu.mixins.json](file:///home/waster/vibecoding/minecraft/wastervisual/src/main/resources/glassmenu.mixins.json)
- [gradle.properties](file:///home/waster/vibecoding/minecraft/wastervisual/gradle.properties)
## [2.24.70] - 2026-06-12

### Added
- **Item HUD Classic Layout & Orientation Toggle:** Classic HUD mode now supports horizontal and vertical layouts (controlled via `itemHudVertical` toggle in the settings screen).
- **Hotkey Numbers Support:** Added toggleable rendering of hotkey numbers (1–9) positioned cleanly below the slot for horizontal layout, and to the left of the slot for vertical layout. They dynamically center when disabled to ensure perfect symmetry.
- **Enhanced Slot Visibility:** Adjusted the slot outline and slot background fill colors to make them clearly visible on both transparent/glass and solid colored HUD layouts.
- **Aspect-Ratio Resizing Constraints:** Enforced square aspect-ratio resizing in CIRCLE mode and correct orientation-dependent aspect ratio bounds in CLASSIC mode.
- **Scaled Previews and Mock Items:** Implemented preview support for the CLASSIC HUD configuration, including scaling math for slots, numbers, and active/mock item icons.

### Files Modified
- [ItemHudRenderer.java](file:///home/waster/vibecoding/minecraft/wastervisual/src/main/java/com/example/glassmenu/render/ItemHudRenderer.java)
- [LiquidGlassScreen.java](file:///home/waster/vibecoding/minecraft/wastervisual/src/main/java/com/example/glassmenu/screen/LiquidGlassScreen.java)
- [gradle.properties](file:///home/waster/vibecoding/minecraft/wastervisual/gradle.properties)

## [2.24.69] - 2026-06-12

### Added
- **Vanilla Glass Hotbar Styling:** Added a feature to render the vanilla hotbar background and offhand slot backgrounds with a beautiful glass-refraction style (with a subtle border outline) when the circular Item HUD (Circle Hotbar) is disabled.
- **Glass Hotbar Configuration Toggle:** Added a configuration button `"Glass Hotbar"` in the Item HUD settings tab to toggle this style on/off.

### Changed
- Incremented `mod_version` to `2.24.69` in `gradle.properties`.

### Files Modified
- [InGameHudMixin.java](file:///home/waster/vibecoding/minecraft/wastervisual/src/main/java/com/example/glassmenu/mixin/InGameHudMixin.java)
- [LiquidGlassScreen.java](file:///home/waster/vibecoding/minecraft/wastervisual/src/main/java/com/example/glassmenu/screen/LiquidGlassScreen.java)
- [GlassMenuConfigModel.java](file:///home/waster/vibecoding/minecraft/wastervisual/src/main/java/com/example/glassmenu/GlassMenuConfigModel.java)
- [gradle.properties](file:///home/waster/vibecoding/minecraft/wastervisual/gradle.properties)

## [2.24.68] - 2026-06-12

### Fixed
- **Item HUD (Fast Item Wheel) Glass Refraction Effect:** Fixed glass shader effect on the Fast Item circular overlay by drawing it outside the pushed/scaled matrix stack, avoiding layout/rounding distortion and matching `InventoryHudRenderer` behaviour.

### Changed
- Renamed GUI configuration menu option "Fast Item Wheel" to "Item HUD" (and titles "Fast Item" to "Item HUD") for cleaner nomenclature.
- Incremented `mod_version` to `2.24.68` in `gradle.properties`.

### Files Modified
- [FastItemRenderer.java](file:///home/waster/vibecoding/minecraft/wastervisual/src/main/java/com/example/glassmenu/render/FastItemRenderer.java)
- [LiquidGlassScreen.java](file:///home/waster/vibecoding/minecraft/wastervisual/src/main/java/com/example/glassmenu/screen/LiquidGlassScreen.java)
- [gradle.properties](file:///home/waster/vibecoding/minecraft/wastervisual/gradle.properties)

## [2.24.67] - 2026-06-12

### Optimized
- **Rendering Performance on Attacks:** Fixed critical FPS drops that occurred when hitting entities.
  - Batched custom hit particles rendering in `CustomHitParticleManager.java` from 3 draw calls per particle to 1 draw call per frame total using `VertexFormat.DrawMode.TRIANGLES`.
  - Batched Target ESP coil droplet rendering in `TargetESPManager.java` from 3 draw calls per droplet (405 draw calls total) to 1 draw call per frame total.
  - Added precomputed trigonometric (sin/cos) lookup tables for stars and circles to avoid expensive runtime math calculations.

### Changed
- Incremented `mod_version` to `2.24.67` in `gradle.properties`.

### Files Modified
- [CustomHitParticleManager.java](file:///home/waster/vibecoding/minecraft/wastervisual/src/main/java/com/example/glassmenu/render/CustomHitParticleManager.java)
- [TargetESPManager.java](file:///home/waster/vibecoding/minecraft/wastervisual/src/main/java/com/example/glassmenu/render/TargetESPManager.java)
- [gradle.properties](file:///home/waster/vibecoding/minecraft/wastervisual/gradle.properties)

### Current Status
- Both rendering pipelines are optimized and batched.
- Build succeeded.
- The new mod jar was deployed to the user launcher's mods folder.

### Next Steps
- Verify the FPS performance in-game during PVP/attacks.
