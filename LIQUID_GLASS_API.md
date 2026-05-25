# Liquid Glass UI Architecture (Fabric 1.21.1)

This document summarizes the current high-end UI system built for the `glassmenu` mod.

## 🎨 Core Principles
1. **SDF Rendering**: All shapes (rounded rects, circles, ovals) are rendered via a Signed Distance Field fragment shader. This ensures perfect anti-aliasing and eliminates overlapping artifacts.
2. **Modular Components**: Each UI element is a self-contained class inheriting from `ClickableWidget` or a custom base.
3. **Delta-Time Animations**: Animations (swell, sliding, color interpolation) are calculated using frame delta-time for maximum smoothness (FPS-independent).
4. **Clean Refraction**: Background effects are handled by `LiquidGlassEffectView`, applying a static lens refraction and soft vignette.

## 🛠️ API Reference

### RenderUtils
- `drawSdfRoundedRect(MatrixStack matrices, float x, float y, float w, float h, float radius, int color, float swell)`
  - Renders a perfectly smooth shape with an optional "swell" scaling effect.
- `drawLine(MatrixStack matrices, float x, float y, float x2, float y2, float thickness, int color)`
  - Renders a precise line for separators.

### ModShaders
- `getSdfRoundedRect()`: Returns the ShaderProgram for SDF shapes.
- `getGlassRefraction()`: Returns the ShaderProgram for background effects.

### Components
- `LiquidGlassEffectView`: Handles background blur and refraction.
- `LiquidLensView`: The main glass panel with decorative separators.
- `LiquidGlassSwitch`: Circular thumb, anti-aliased, swell on hover.
- `LiquidGlassSlider`: Oval knob, lerp-based movement, swell on hover/drag.

## 🚀 How to Add New Effects
1. **New Shaders**: Place `.vsh`, `.fsh`, and `.json` in `assets/glassmenu/shaders/core/`. Register them in `ModShaders.java`.
2. **Tab Switching**: Use a `int currentTab` state in the Screen class. Render different children based on this state.
3. **Scrolling**: Implement a `scrollOffset` float. Offset the `y` coordinate of all rendered children in the list by this value. Use `MathHelper.lerp` to make the scrolling "liquid".
