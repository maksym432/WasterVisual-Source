/**
 * WasterVisual Main Client Entrypoint.
 * Responsible for initializing shaders, media controllers, keybindings, 
 * and registering global tick/render events (like Target ESP).
 */
package com.example.glassmenu;

import com.example.glassmenu.screen.LiquidGlassScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Client-side mod initializer.
 *
 * <p>Registers a keybind (default: K) that opens {@link LiquidGlassScreen}.
 * The keybind appears in Options → Controls → WasterVisual.
 */
import com.example.glassmenu.media.LinuxMediaController;
import com.example.glassmenu.shader.ModShaders;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import java.io.File;
import java.nio.file.Files;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class GlassMenuClient implements ClientModInitializer {

    private static KeyBinding openMenuKey;
    public static final com.example.glassmenu.GlassMenuConfig CONFIG = com.example.glassmenu.GlassMenuConfig.createAndLoad();
    private static final File CONFIG_FILE = new File("config/glassmenu_settings.json");

    @Override
    public void onInitializeClient() {
        ModShaders.init();
        LinuxMediaController.init();

        // Register Target ESP and Jump Rings tick
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            com.example.glassmenu.render.TargetESPManager.tick();
            com.example.glassmenu.render.JumpRingsManager.tick();
        });

        // SAFE World Rendering via Fabric API (Compatible with Sodium/Iris)
        WorldRenderEvents.LAST.register(context -> {
            com.example.glassmenu.render.TargetESPManager.render(context);
            
            // Bridge Box Effect (Rendering)
            com.example.glassmenu.render.BridgeBoxRenderer.render(context);
        });

        // Cancel vanilla outline if Vortex is enabled
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.BLOCK_OUTLINE.register((context, hitResult) -> {
            return !CONFIG.enableBridgeVortex();
        });
        
        com.example.glassmenu.render.JumpRingsManager.init();

        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.glassmenu.open",          // translation key (see en_us.json)
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,               // default: K
                "category.glassmenu"           // category in Controls screen
        ));

        // ── Poll keybind every client tick ───────────────────────────────────
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // wasPressed() consumes the press so it fires once per key-down
            while (openMenuKey.wasPressed()) {
                if (client.currentScreen == null) {
                    // Open the menu only when no other screen is open
                    client.setScreen(new LiquidGlassScreen());
                }
            }
        });
    }

    public static void saveConfig(float volume) {
        // Volume handled by media controller
    }
}
