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
        public int avgColor = 0xFF34C759;
        public double position = 0;
        public double length = 0;
        public boolean shuffle = false;
        public String loopStatus = "None";
        public int[] visualizerGradients = null;
    }

    private static final AtomicReference<MediaState> CURRENT_STATE = new AtomicReference<>(new MediaState());
    private static String lastArtUrl = "";
    private static final Identifier ART_TEXTURE_ID = Identifier.of("glassmenu", "current_album_art");
    private static long isPlayingLockUntil = 0;
    private static boolean lockedIsPlaying = false;

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
            String output = exec("playerctl metadata --format {{title}};;;{{artist}};;;{{status}};;;{{volume}};;;{{mpris:artUrl}};;;{{position}};;;{{mpris:length}}");
            
            MediaState oldState = CURRENT_STATE.get();
            MediaState newState = new MediaState();
            
            if (!output.isEmpty() && output.contains(";;;")) {
                String[] parts = output.split(";;;", -1);
                if (parts.length >= 5) {
                    String title = parts[0].trim();
                    String artist = parts[1].trim();
                    String status = parts[2].trim();
                    
                    float volume = 1.0f;
                    try {
                        volume = Float.parseFloat(parts[3].trim());
                    } catch (Exception ignored) {}
                    boolean isPlaying = "Playing".equalsIgnoreCase(status);
                    if (System.currentTimeMillis() < isPlayingLockUntil) {
                        isPlaying = lockedIsPlaying;
                    }
                    
                    String artUrl = parts[4].trim();
                    
                    newState.title = title.isEmpty() ? oldState.title : title;
                    newState.artist = artist.isEmpty() ? oldState.artist : artist;
                    newState.isPlaying = isPlaying;
                    newState.volume = volume;
                    newState.artUrl = artUrl;
                    
                    // Inherit texture info, average color and gradients
                    newState.artTexture = oldState.artTexture;
                    newState.artWidth = oldState.artWidth;
                    newState.artHeight = oldState.artHeight;
                    newState.avgColor = oldState.avgColor;
                    newState.visualizerGradients = oldState.visualizerGradients;
                    
                    // Parse position and length (microseconds to seconds)
                    if (parts.length >= 7) {
                        try {
                            newState.position = Double.parseDouble(parts[5].trim()) / 1_000_000.0;
                        } catch (Exception ignored) {}
                        try {
                            newState.length = Double.parseDouble(parts[6].trim()) / 1_000_000.0;
                        } catch (Exception ignored) {}
                    }
                    
                    // Query shuffle and loop status
                    String shuffleOut = exec("playerctl shuffle");
                    newState.shuffle = "On".equalsIgnoreCase(shuffleOut.trim());

                    String loopOut = exec("playerctl loop");
                    newState.loopStatus = loopOut.trim().isEmpty() ? "None" : loopOut.trim();

                    // Handle image loading if URL changed
                    if (!artUrl.isEmpty() && !artUrl.equals(lastArtUrl)) {
                        lastArtUrl = artUrl;
                        loadArt(artUrl);
                    }
                }
            } else {
                // No media players active or playerctl returned empty
                newState.title = "";
                newState.artist = "";
                newState.isPlaying = false;
                newState.volume = 1.0f;
                newState.artUrl = "";
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

                    // Calculate average color
                    long sumR = 0, sumG = 0, sumB = 0;
                    int count = 0;
                    int stepX = Math.max(1, width / 20);
                    int stepY = Math.max(1, height / 20);
                    for (int px = 0; px < width; px += stepX) {
                        for (int py = 0; py < height; py += stepY) {
                            int abgr = image.getColor(px, py);
                            int r = abgr & 0xFF;
                            int g = (abgr >> 8) & 0xFF;
                            int b = (abgr >> 16) & 0xFF;
                            int a = (abgr >> 24) & 0xFF;
                            if (a > 50) {
                                sumR += r;
                                sumG += g;
                                sumB += b;
                                count++;
                            }
                        }
                    }
                    int avgColor = 0xFF34C759; // Default green
                    if (count > 0) {
                        int r = (int)(sumR / count);
                        int g = (int)(sumG / count);
                        int b = (int)(sumB / count);
                        avgColor = 0xFF000000 | (r << 16) | (g << 8) | b;
                    }
                    final int finalAvgColor = avgColor;

                    // Extract 6x3 visualizer gradient colors (6 bars, 3 vertical colors each)
                    int[] gradients = new int[18];
                    for (int col = 0; col < 6; col++) {
                        for (int row = 0; row < 3; row++) {
                            int px = (int) net.minecraft.util.math.MathHelper.clamp((col + 0.5f) * width / 6f, 0, width - 1);
                            int py = (int) net.minecraft.util.math.MathHelper.clamp((row + 0.5f) * height / 3f, 0, height - 1);
                            int abgr = image.getColor(px, py);
                            int r = abgr & 0xFF;
                            int g = (abgr >> 8) & 0xFF;
                            int b = (abgr >> 16) & 0xFF;
                            gradients[col * 3 + row] = 0xFF000000 | (r << 16) | (g << 8) | b;
                        }
                    }

                    RenderSystem.recordRenderCall(() -> {
                        var textureManager = MinecraftClient.getInstance().getTextureManager();
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
                        newStateWithArt.avgColor = finalAvgColor;
                        newStateWithArt.position = s.position;
                        newStateWithArt.length = s.length;
                        newStateWithArt.shuffle = s.shuffle;
                        newStateWithArt.loopStatus = s.loopStatus;
                        newStateWithArt.visualizerGradients = gradients;
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
        MediaState state = CURRENT_STATE.get();
        if (state != null) {
            MediaState newState = new MediaState();
            newState.title = state.title;
            newState.artist = state.artist;
            newState.isPlaying = !state.isPlaying;
            newState.volume = state.volume;
            newState.artUrl = state.artUrl;
            newState.artTexture = state.artTexture;
            newState.artWidth = state.artWidth;
            newState.artHeight = state.artHeight;
            newState.avgColor = state.avgColor;
            newState.position = state.position;
            newState.length = state.length;
            newState.shuffle = state.shuffle;
            newState.loopStatus = state.loopStatus;
            newState.visualizerGradients = state.visualizerGradients;
            
            isPlayingLockUntil = System.currentTimeMillis() + 800;
            lockedIsPlaying = newState.isPlaying;
            CURRENT_STATE.set(newState);
        }
        EXECUTOR.execute(() -> {
            exec("playerctl play-pause");
            for (int i = 0; i < 4; i++) {
                try { Thread.sleep(150); } catch (InterruptedException ignored) {}
                poll();
            }
        }); 
    }
    public static void next() { 
        EXECUTOR.execute(() -> {
            exec("playerctl next");
            for (int i = 0; i < 4; i++) {
                try { Thread.sleep(150); } catch (InterruptedException ignored) {}
                poll();
            }
        }); 
    }
    public static void previous() { 
        EXECUTOR.execute(() -> {
            exec("playerctl previous");
            for (int i = 0; i < 4; i++) {
                try { Thread.sleep(150); } catch (InterruptedException ignored) {}
                poll();
            }
        }); 
    }
    public static void setVolume(float vol) { 
        EXECUTOR.execute(() -> {
            exec("playerctl volume " + String.format("%.2f", vol));
            com.example.glassmenu.GlassMenuClient.saveConfig(vol);
        }); 
    }
    public static void seek(double seconds) { 
        EXECUTOR.execute(() -> {
            exec("playerctl position " + String.format(java.util.Locale.US, "%.2f", seconds));
            poll();
        }); 
    }

    public static void toggleShuffle() {
        EXECUTOR.execute(() -> {
            exec("playerctl shuffle Toggle");
            poll();
        });
    }

    public static void toggleLoop() {
        EXECUTOR.execute(() -> {
            String current = exec("playerctl loop").trim();
            String next = "None";
            if ("None".equalsIgnoreCase(current)) {
                next = "Track";
            } else if ("Track".equalsIgnoreCase(current)) {
                next = "Playlist";
            }
            exec("playerctl loop " + next);
            poll();
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
