/**
 * Linux-Specific Media Controller.
 * Utilizes 'playerctl' via system calls to poll and control media players 
 * (Title, Artist, Playback state, Album Art) for the Dynamic Island.
 */
package com.example.glassmenu.media;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class LinuxMediaController {
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "MediaController-Polling");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });

    private static final ExecutorService IMAGE_LOADER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "MediaController-ImageLoader");
        t.setDaemon(true);
        return t;
    });

    public static class MediaState {
        public String title = "Unknown";
        public String artist = "Unknown";
        public boolean isPlaying = false;
        public float volume = 1.0f;
        public String artUrl = "";
        public Identifier artTexture = null;
        public int artWidth = 0;
        public int artHeight = 0;
    }

    private static final AtomicReference<MediaState> CURRENT_STATE = new AtomicReference<>(new MediaState());
    private static String lastArtUrl = "";
    private static final Identifier ART_TEXTURE_ID = Identifier.of("glassmenu", "current_album_art");

    public static void init() {
        RenderSystem.recordRenderCall(() -> {
            NativeImage placeholder = new NativeImage(1, 1, false);
            placeholder.setColor(0, 0, 0x00000000); // Transparent
            MinecraftClient.getInstance().getTextureManager().registerTexture(ART_TEXTURE_ID, new NativeImageBackedTexture(placeholder));
        });
        EXECUTOR.scheduleAtFixedRate(LinuxMediaController::poll, 0, 1000, TimeUnit.MILLISECONDS);
    }

    private static void poll() {
        try {
            String title = exec("playerctl metadata title");
            String artist = exec("playerctl metadata artist");
            String status = exec("playerctl status");
            boolean isPlaying = "Playing".equalsIgnoreCase(status);
            
            float volume = 1.0f;
            String volStr = exec("playerctl volume");
            try {
                volume = Float.parseFloat(volStr);
            } catch (Exception ignored) {}

            String artUrl = exec("playerctl metadata mpris:artUrl");
            
            MediaState oldState = CURRENT_STATE.get();
            MediaState newState = new MediaState();
            
            // Persist metadata if new is empty
            newState.title = title.isEmpty() ? oldState.title : title;
            newState.artist = artist.isEmpty() ? oldState.artist : artist;
            newState.isPlaying = isPlaying;
            newState.volume = volume;
            newState.artUrl = artUrl;
            
            // Inherit texture info
            newState.artTexture = oldState.artTexture;
            newState.artWidth = oldState.artWidth;
            newState.artHeight = oldState.artHeight;

            // Only clear if absolutely nothing is playing and we have no info
            if (title.isEmpty() && artist.isEmpty() && !isPlaying) {
                // We keep the old info for the "exit" animation but we could mark it as inactive
            }

            // Handle image loading if URL changed
            if (!artUrl.isEmpty() && !artUrl.equals(lastArtUrl)) {
                lastArtUrl = artUrl;
                loadArt(artUrl);
            }

            CURRENT_STATE.set(newState);
        } catch (Exception e) {
            // Keep previous state on error to avoid flicker
        }
    }

    private static void loadArt(String url) {
        IMAGE_LOADER.execute(() -> {
            try {
                String finalUrl = url;
                if (url.startsWith("file://")) {
                    finalUrl = url.substring(7);
                }
                
                try (InputStream is = finalUrl.startsWith("/") ? new java.io.FileInputStream(finalUrl) : new URL(finalUrl).openStream()) {
                    NativeImage image = NativeImage.read(is);
                    int width = image.getWidth();
                    int height = image.getHeight();

                    RenderSystem.recordRenderCall(() -> {
                        var textureManager = MinecraftClient.getInstance().getTextureManager();
                        // Close previous texture if it exists to prevent leaks
                        var oldTexture = textureManager.getTexture(ART_TEXTURE_ID);
                        if (oldTexture != null) {
                            oldTexture.close();
                        }
                        
                        textureManager.registerTexture(ART_TEXTURE_ID, new NativeImageBackedTexture(image));
                        
                        // ONLY update the state with the texture after it's registered
                        MediaState s = CURRENT_STATE.get();
                        MediaState newStateWithArt = new MediaState();
                        newStateWithArt.title = s.title;
                        newStateWithArt.artist = s.artist;
                        newStateWithArt.isPlaying = s.isPlaying;
                        newStateWithArt.volume = s.volume;
                        newStateWithArt.artUrl = s.artUrl;
                        newStateWithArt.artTexture = ART_TEXTURE_ID;
                        newStateWithArt.artWidth = width;
                        newStateWithArt.artHeight = height;
                        CURRENT_STATE.set(newStateWithArt);
                    });
                }
            } catch (Exception e) {
                System.err.println("Failed to load album art: " + e.getMessage());
            }
        });
    }

    public static MediaState getCurrentState() {
        return CURRENT_STATE.get();
    }

    public static void playPause() { 
        EXECUTOR.execute(() -> {
            exec("playerctl play-pause");
            poll(); // Force immediate poll after action
        }); 
    }
    public static void next() { EXECUTOR.execute(() -> exec("playerctl next")); }
    public static void previous() { EXECUTOR.execute(() -> exec("playerctl previous")); }
    public static void setVolume(float vol) { 
        EXECUTOR.execute(() -> {
            exec("playerctl volume " + String.format("%.2f", vol));
            com.example.glassmenu.GlassMenuClient.saveConfig(vol);
        }); 
    }

    private static String exec(String cmd) {
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(cmd.split(" "));
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (sb.length() == 0) {
                        sb.append(line.trim());
                    }
                }
            }
            try (InputStream err = p.getErrorStream()) {
                byte[] buffer = new byte[1024];
                while (err.read(buffer) != -1) {
                    // Drain error stream
                }
            }
            p.waitFor(100, TimeUnit.MILLISECONDS);
            return sb.toString();
        } catch (Exception e) {
            return "";
        } finally {
            if (p != null) {
                p.destroyForcibly();
            }
        }
    }
}
