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
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;

public class GlassMenuClient implements ClientModInitializer {

    private static KeyBinding openMenuKey;
    private static KeyBinding cssTestKey;
    public static final com.example.glassmenu.GlassMenuConfig CONFIG = com.example.glassmenu.GlassMenuConfig.createAndLoad();
    private static final File CONFIG_FILE = new File("config/glassmenu_settings.json");

    @Override
    public void onInitializeClient() {
        ModShaders.init();
        LinuxMediaController.init();

        // Register Target ESP, Jump Rings, and Player Card tick
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            com.example.glassmenu.render.TargetESPManager.tick();
            com.example.glassmenu.render.JumpRingsManager.tick();
            com.example.glassmenu.render.PlayerCardRenderer.tick();
            com.example.glassmenu.render.CustomHitParticleManager.tick();
            com.example.glassmenu.render.GhostTrailManager.tick();
        });

        // SAFE World Rendering via Fabric API (Compatible with Sodium/Iris)
        WorldRenderEvents.LAST.register(context -> {
            com.example.glassmenu.render.TargetESPManager.render(context);
            
            // Bridge Box Effect (Rendering)
            com.example.glassmenu.render.BridgeBoxRenderer.render(context);

            // BedWars ESP (Rendering)
            com.example.glassmenu.render.BedWarsESPManager.render(context);

            // Custom Hit Particles (Rendering)
            com.example.glassmenu.render.CustomHitParticleManager.render(context);

            // Ghost Trail / Afterimage (Rendering)
            com.example.glassmenu.render.GhostTrailManager.render(context);
        });

        // Cancel vanilla outline if Vortex is enabled
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.BLOCK_OUTLINE.register((context, hitResult) -> {
            return !CONFIG.enableBridgeVortex();
        });
        
        com.example.glassmenu.render.JumpRingsManager.init();
        com.example.glassmenu.widget.IslandManager.init();

        // Render Dynamic Island, Inventory HUD and Player Card on in-game HUD (when no screen is open)
        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.currentScreen == null) {
                if (CONFIG.enableDynamicIsland()) {
                    com.example.glassmenu.widget.IslandManager.render(
                        context,
                        client.getWindow().getScaledWidth(),
                        client.getWindow().getScaledHeight(),
                        -1, -1, 0.0f
                    );
                }
                com.example.glassmenu.render.InventoryHudRenderer.render(
                    context,
                    client.getWindow().getScaledWidth(),
                    client.getWindow().getScaledHeight()
                );
                com.example.glassmenu.render.PlayerCardRenderer.render(
                    context,
                    client.getWindow().getScaledWidth(),
                    client.getWindow().getScaledHeight()
                );
                com.example.glassmenu.render.UserIndicatorRenderer.render(
                    context,
                    client.getWindow().getScaledWidth(),
                    client.getWindow().getScaledHeight()
                );
                com.example.glassmenu.render.ArmorHudRenderer.render(
                    context,
                    client.getWindow().getScaledWidth(),
                    client.getWindow().getScaledHeight()
                );
                com.example.glassmenu.render.FastItemRenderer.render(
                    context,
                    client.getWindow().getScaledWidth(),
                    client.getWindow().getScaledHeight()
                );
                com.example.glassmenu.render.GlassHotbarRenderer.render(
                    context,
                    client.getWindow().getScaledWidth(),
                    client.getWindow().getScaledHeight()
                );
                com.example.glassmenu.render.UserHudRenderer.render(
                    context,
                    client.getWindow().getScaledWidth(),
                    client.getWindow().getScaledHeight()
                );
                com.example.glassmenu.render.EffectsHudRenderer.render(
                    context,
                    client.getWindow().getScaledWidth(),
                    client.getWindow().getScaledHeight()
                );
                com.example.glassmenu.render.LeftHandItemRenderer.render(
                    context,
                    client.getWindow().getScaledWidth(),
                    client.getWindow().getScaledHeight()
                );
            }
        });

        // Intercept Dynamic Island clicks on all screens natively using Fabric API
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ScreenMouseEvents.allowMouseClick(screen).register((screenInstance, mouseX, mouseY, button) -> {
                // TitleScreen clicks are handled in TitleScreenMixin to coordinate with lock screen drag
                if (screenInstance instanceof net.minecraft.client.gui.screen.TitleScreen) {
                    return true;
                }
                if (CONFIG.enableDynamicIsland() && com.example.glassmenu.widget.IslandManager.mouseClicked(mouseX, mouseY, button)) {
                    return false; // Consume click event
                }
                return true;
            });
        });

        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.glassmenu.open",          // translation key (see en_us.json)
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,               // default: K
                "category.glassmenu"           // category in Controls screen
        ));

        cssTestKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.glassmenu.css_test",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,               // default: H
                "category.glassmenu"
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
            while (cssTestKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new com.example.glassmenu.screen.CssGlassTestScreen());
                }
            }
        });
    }

    public static void saveConfig(float volume) {
        // Volume handled by media controller
    }
}
