# Changelog

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
