/*
 * IpadLockScreenManager - Architecture & Primary Responsibility:
 * iPad-Style Lock Screen Manager.
 * Handles the inactivity timer, swipe-up gesture, wallpaper rendering (with
 * aspect ratio / Center Crop correction), and clock display for the immersive
 * lock screen experience.
 */
package com.example.glassmenu.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class IpadLockScreenManager {
    // Correct resource path for standard Minecraft resource loading
    public static final Identifier WALLPAPER_RES = Identifier.of("glassmenu", "textures/gui/wallpaper.png");
    public static final Identifier WALLPAPER_DYNAMIC_ID = Identifier.of("glassmenu", "ipad_wallpaper_dynamic");
    
    public static boolean dismissed = false;
    public static float currentYOffset = 0;
    public static float targetYOffset = 0;
    
    private static boolean textureLoaded = false;
    private static int imgW = 0, imgH = 0;

    private static boolean isDragging = false;
    private static double dragStartY = 0;

    private static boolean firstShowDone = false;
    private static long lastActivityTime = System.currentTimeMillis();
    private static final long AFK_THRESHOLD_MS = 10 * 60 * 1000; // 10 minutes

    public static void init() {
        if (!firstShowDone) {
            dismissed = false;
            currentYOffset = 0;
            targetYOffset = 0;
            firstShowDone = true;
        }
        
        resetInactivity();

        if (textureLoaded) return;
        
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            // Standard approach to load resource from assets
            Optional<Resource> resource = client.getResourceManager().getResource(WALLPAPER_RES);
            
            if (resource.isPresent()) {
                try (InputStream is = resource.get().getInputStream()) {
                    NativeImage image = NativeImage.read(is);
                    imgW = image.getWidth();
                    imgH = image.getHeight();
                    
                    // Explicitly register as a dynamic texture
                    client.getTextureManager().registerTexture(
                        WALLPAPER_DYNAMIC_ID, 
                        new NativeImageBackedTexture(image)
                    );
                    textureLoaded = true;
                    System.out.println("GlassMenu: Wallpaper loaded successfully (" + imgW + "x" + imgH + ")");
                }
            } else {
                System.err.println("GlassMenu: Wallpaper resource not found: " + WALLPAPER_RES);
            }
        } catch (Exception e) {
            System.err.println("GlassMenu: Failed to load internal iPad wallpaper: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static long lastFrameTime = -1;

    public static void resetInactivity() {
        lastActivityTime = System.currentTimeMillis();
    }

    public static void render(DrawContext context, int width, int height, int mouseX, int mouseY, float delta) {
        if (dismissed && currentYOffset <= -height) return;

        long currentTime = System.currentTimeMillis();

        // AFK Logic
        if (dismissed && (currentTime - lastActivityTime) > AFK_THRESHOLD_MS) {
            dismissed = false;
            targetYOffset = 0;
        }

        if (dismissed && currentYOffset <= -height) {
            lastFrameTime = currentTime;
            return;
        }

        // Smoother interpolation
        float lerpFactor = 0.15f * delta; 
        currentYOffset = currentYOffset + (targetYOffset - currentYOffset) * Math.min(lerpFactor, 1.0f);
        lastFrameTime = currentTime;

        // Final dismissal logic
        if (dismissed && currentYOffset < -height + 1) {
            currentYOffset = -height - 100;
            return;
        }

        // --- Start Rendering ---
        context.getMatrices().push();
        context.getMatrices().translate(0, currentYOffset, 500);

        // 1. Wallpaper (Background)
        if (textureLoaded) {
            renderCroppedWallpaper(context, width, height);
        } else {
            context.fill(0, 0, width, height, 0xFF111111);
        }

        // 2. Clock & UI
        LocalTime now = LocalTime.now();
        String timeStr = now.format(DateTimeFormatter.ofPattern("HH:mm"));
        
        context.getMatrices().push();
        context.getMatrices().translate(width / 2f, height * 0.15f, 50);
        context.getMatrices().scale(4.0f, 4.0f, 1.0f);
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, timeStr, 0, 0, 0xFFFFFFFF);
        context.getMatrices().pop();

        context.drawCenteredTextWithShadow(
            MinecraftClient.getInstance().textRenderer,
            "Swipe up to unlock", width / 2, (int)(height * 0.9f), 0xAAFFFFFF
        );

        // 3. Dynamic Island (Child of Lockscreen, but on top)
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 100);
        DynamicIslandWidget.render(context, width, height, mouseX, (int)(mouseY - currentYOffset), delta);
        context.getMatrices().pop();

        context.getMatrices().pop();
    }

    private static void renderCroppedWallpaper(DrawContext context, int width, int height) {
        float screenRatio = (float) width / height;
        float imageRatio = (float) imgW / imgH;
        float u1 = 0, v1 = 0, u2 = 1, v2 = 1;

        if (screenRatio > imageRatio) {
            float visibleHeight = imageRatio / screenRatio;
            v1 = (1f - visibleHeight) / 2f;
            v2 = 1f - v1;
        } else {
            float visibleWidth = screenRatio / imageRatio;
            u1 = (1f - visibleWidth) / 2f;
            u2 = 1f - u1;
        }

        RenderSystem.setShaderTexture(0, WALLPAPER_DYNAMIC_ID);
        context.drawTexture(WALLPAPER_DYNAMIC_ID, 0, 0, width, height, u1 * imgW, v1 * imgH, (int)((u2 - u1) * imgW), (int)((v2 - v1) * imgH), imgW, imgH);
    }

    public static boolean mouseClicked(double mouseY) {
        if (dismissed) return false;
        isDragging = true;
        dragStartY = mouseY;
        return true;
    }

    public static void mouseDragged(double mouseY) {
        if (!isDragging || dismissed) return;
        float diff = (float) (mouseY - dragStartY);
        if (diff < 0) targetYOffset = diff;
    }

    public static void mouseReleased(int height) {
        if (!isDragging || dismissed) return;
        isDragging = false;
        if (currentYOffset < -height * 0.20f) {
            targetYOffset = -height - 100;
            dismissed = true;
        } else {
            targetYOffset = 0;
        }
    }
}
