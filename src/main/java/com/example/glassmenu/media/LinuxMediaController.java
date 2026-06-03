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
    public static class PlayerInfo {
        public String name = "";
        public String title = "Unknown";
        public String artist = "Unknown";
        public boolean isPlaying = false;
        public double position = 0;
        public double length = 0;
        public String artUrl = "";
        public Identifier artTexture = null;
        public int artWidth = 0;
        public int artHeight = 0;
    }

    public static final java.util.List<PlayerInfo> ALL_PLAYERS_INFO = new java.util.concurrent.CopyOnWriteArrayList<>();
    public static String selectedPlayer = "";
    
    public static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    public static class BrowserCommand {
        public String command;
        public int tabId;
        public BrowserCommand(String command, int tabId) {
            this.command = command;
            this.tabId = tabId;
        }
    }
    public static class BrowserTabInfo {
        public int id;
        public String title = "";
        public String url = "";
        public String favIconUrl = "";
        public boolean audible = false;
        public boolean active = false;
    }
    public static final java.util.Queue<BrowserCommand> PENDING_COMMANDS = new java.util.concurrent.ConcurrentLinkedQueue<>();
    public static final java.util.List<BrowserTabInfo> BROWSER_TABS = new java.util.concurrent.CopyOnWriteArrayList<>();
    private static com.sun.net.httpserver.HttpServer httpServer = null;
    
    private static final java.util.Map<String, String> LOADED_ART_URLS = new java.util.concurrent.ConcurrentHashMap<>();
    public static final java.util.Map<String, Identifier> PLAYER_ART_TEXTURES = new java.util.concurrent.ConcurrentHashMap<>();
    public static final java.util.Map<String, Integer> PLAYER_ART_WIDTHS = new java.util.concurrent.ConcurrentHashMap<>();
    public static final java.util.Map<String, Integer> PLAYER_ART_HEIGHTS = new java.util.concurrent.ConcurrentHashMap<>();

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
        if (IS_WINDOWS) {
            writeWindowsScript();
        }
        startHttpServer();
        EXECUTOR.scheduleAtFixedRate(LinuxMediaController::poll, 0, 1000, TimeUnit.MILLISECONDS);
    }

    public static void poll() {
        try {
            if (IS_WINDOWS) {
                pollWindows();
                return;
            }

            // 1. Query running players list
            String listOutput = exec("playerctl -l");
            java.util.List<String> players = new java.util.ArrayList<>();
            if (!listOutput.isEmpty()) {
                for (String p : listOutput.split("\n")) {
                    p = p.trim();
                    if (!p.isEmpty()) {
                        players.add(p);
                    }
                }
            }

            boolean foundSelected = false;
            for (String p : players) {
                if (p.equals(selectedPlayer)) { foundSelected = true; break; }
            }
            for (BrowserTabInfo tab : BROWSER_TABS) {
                if (("browser_tab_" + tab.id).equals(selectedPlayer)) { foundSelected = true; break; }
            }
            if (!selectedPlayer.isEmpty() && !foundSelected) {
                selectedPlayer = "";
            }

            java.util.List<PlayerInfo> playerInfos = new java.util.ArrayList<>();
            for (String pName : players) {
                PlayerInfo pi = new PlayerInfo();
                pi.name = pName;
                String metaOut = exec("playerctl -p " + pName + " metadata --format {{title}};;;{{artist}};;;{{status}};;;{{position}};;;{{mpris:length}};;;{{mpris:artUrl}}");
                if (!metaOut.isEmpty() && metaOut.contains(";;;")) {
                    String[] parts = metaOut.split(";;;", -1);
                    if (parts.length >= 3) {
                        pi.title = parts[0].trim();
                        pi.artist = parts[1].trim();
                        pi.isPlaying = "Playing".equalsIgnoreCase(parts[2].trim());
                        if (parts.length >= 5) {
                            try {
                                pi.position = Double.parseDouble(parts[3].trim()) / 1_000_000.0;
                            } catch (Exception ignored) {}
                            try {
                                pi.length = Double.parseDouble(parts[4].trim()) / 1_000_000.0;
                            } catch (Exception ignored) {}
                        }
                        if (parts.length >= 6) {
                            String artUrl = parts[5].trim();
                            pi.artUrl = artUrl;
                            if (!artUrl.isEmpty()) {
                                String lastUrlForPlayer = LOADED_ART_URLS.get(pName);
                                if (!artUrl.equals(lastUrlForPlayer)) {
                                    LOADED_ART_URLS.put(pName, artUrl);
                                    loadArtForPlayer(pName, artUrl);
                                }
                                pi.artTexture = PLAYER_ART_TEXTURES.get(pName);
                                pi.artWidth = PLAYER_ART_WIDTHS.getOrDefault(pName, 0);
                                pi.artHeight = PLAYER_ART_HEIGHTS.getOrDefault(pName, 0);
                            }
                        }
                    }
                }
                playerInfos.add(pi);
            }
            addBrowserTabsToPlayerInfos(playerInfos);

            ALL_PLAYERS_INFO.clear();
            ALL_PLAYERS_INFO.addAll(playerInfos);

            // 2. Poll metadata of the default/selected player
            String output = exec("playerctl metadata --format {{title}};;;{{artist}};;;{{status}};;;{{volume}};;;{{mpris:artUrl}};;;{{position}};;;{{mpris:length}}");
            
            MediaState oldState = CURRENT_STATE.get();
            MediaState newState = new MediaState();
            
            PlayerInfo active = null;
            if (!selectedPlayer.isEmpty()) {
                for (PlayerInfo p : playerInfos) {
                    if (p.name.equals(selectedPlayer)) {
                        active = p;
                        break;
                    }
                }
            }

            if (handleActiveBrowserTab(active, newState, oldState)) {
                // Handled!
            } else if (!output.isEmpty() && output.contains(";;;")) {
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
                    if (finalUrl.startsWith("/") && finalUrl.length() > 2 && finalUrl.charAt(2) == ':') {
                        finalUrl = finalUrl.substring(1);
                    }
                }
                
                boolean isLocal = finalUrl.startsWith("/") || finalUrl.contains(":\\") || finalUrl.contains(":/") || new java.io.File(finalUrl).exists();
                try (InputStream is = isLocal ? new java.io.FileInputStream(finalUrl) : new URL(finalUrl).openStream()) {
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

    private static void loadArtForPlayer(String pName, String url) {
        IMAGE_LOADER.execute(() -> {
            try {
                String finalUrl = url;
                if (url.startsWith("file://")) {
                    finalUrl = url.substring(7);
                    if (finalUrl.startsWith("/") && finalUrl.length() > 2 && finalUrl.charAt(2) == ':') {
                        finalUrl = finalUrl.substring(1);
                    }
                }
                boolean isLocal = finalUrl.startsWith("/") || finalUrl.contains(":\\") || finalUrl.contains(":/") || new java.io.File(finalUrl).exists();
                try (InputStream is = isLocal ? new java.io.FileInputStream(finalUrl) : new URL(finalUrl).openStream()) {
                    NativeImage image = NativeImage.read(is);
                    int width = image.getWidth();
                    int height = image.getHeight();
                    
                    Identifier textureId = Identifier.of("glassmenu", "art_" + pName.toLowerCase().replaceAll("[^a-z0-9_]", "_"));
                    
                    RenderSystem.recordRenderCall(() -> {
                        MinecraftClient.getInstance().getTextureManager().registerTexture(textureId, new NativeImageBackedTexture(image));
                        PLAYER_ART_TEXTURES.put(pName, textureId);
                        PLAYER_ART_WIDTHS.put(pName, width);
                        PLAYER_ART_HEIGHTS.put(pName, height);
                    });
                }
            } catch (Exception e) {
                System.err.println("Failed to load player art for " + pName + ": " + e.getMessage());
            }
        });
    }

    public static MediaState getCurrentState() {
        return CURRENT_STATE.get();
    }

    public static void playPause() { 
        if (selectedPlayer.startsWith("browser_tab_")) {
            try {
                int tabId = Integer.parseInt(selectedPlayer.substring(12));
                PENDING_COMMANDS.add(new BrowserCommand("playPause", tabId));
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
            } catch (Exception ignored) {}
            return;
        }
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
            if (IS_WINDOWS) {
                execWin("playPause", selectedPlayer, "");
            } else {
                exec("playerctl play-pause");
            }
            for (int i = 0; i < 4; i++) {
                try { Thread.sleep(150); } catch (InterruptedException ignored) {}
                poll();
            }
        }); 
    }
    public static void next() { 
        if (selectedPlayer.startsWith("browser_tab_")) {
            try {
                int tabId = Integer.parseInt(selectedPlayer.substring(12));
                PENDING_COMMANDS.add(new BrowserCommand("next", tabId));
            } catch (Exception ignored) {}
            return;
        }
        EXECUTOR.execute(() -> {
            if (IS_WINDOWS) {
                execWin("next", selectedPlayer, "");
            } else {
                exec("playerctl next");
            }
            for (int i = 0; i < 4; i++) {
                try { Thread.sleep(150); } catch (InterruptedException ignored) {}
                poll();
            }
        }); 
    }
    public static void previous() { 
        if (selectedPlayer.startsWith("browser_tab_")) {
            try {
                int tabId = Integer.parseInt(selectedPlayer.substring(12));
                PENDING_COMMANDS.add(new BrowserCommand("previous", tabId));
            } catch (Exception ignored) {}
            return;
        }
        EXECUTOR.execute(() -> {
            if (IS_WINDOWS) {
                execWin("previous", selectedPlayer, "");
            } else {
                exec("playerctl previous");
            }
            for (int i = 0; i < 4; i++) {
                try { Thread.sleep(150); } catch (InterruptedException ignored) {}
                poll();
            }
        }); 
    }
    public static void setVolume(float vol) { 
        EXECUTOR.execute(() -> {
            if (!IS_WINDOWS) {
                exec("playerctl volume " + String.format("%.2f", vol));
            }
            com.example.glassmenu.GlassMenuClient.saveConfig(vol);
        }); 
    }
    public static void seek(double seconds) { 
        if (selectedPlayer.startsWith("browser_tab_")) {
            return;
        }
        EXECUTOR.execute(() -> {
            if (IS_WINDOWS) {
                execWin("seek", selectedPlayer, String.format(java.util.Locale.US, "%.2f", seconds));
            } else {
                exec("playerctl position " + String.format(java.util.Locale.US, "%.2f", seconds));
            }
            poll();
        }); 
    }

    public static void selectAndPlayPlayer(String newPlayerName) {
        String oldPlayer = selectedPlayer;
        if (oldPlayer != null && !oldPlayer.isEmpty() && !oldPlayer.equals(newPlayerName)) {
            // Pause the old player
            EXECUTOR.execute(() -> {
                if (oldPlayer.startsWith("browser_tab_")) {
                    try {
                        int tabId = Integer.parseInt(oldPlayer.substring(12));
                        PENDING_COMMANDS.add(new BrowserCommand("pause", tabId));
                    } catch (Exception ignored) {}
                } else {
                    if (IS_WINDOWS) {
                        execWin("pause", oldPlayer, "");
                    } else {
                        exec("playerctl -p " + oldPlayer + " pause");
                    }
                }
            });
        }
        
        selectedPlayer = newPlayerName;
        
        // Play the new player
        EXECUTOR.execute(() -> {
            try { Thread.sleep(150); } catch (InterruptedException ignored) {}
            if (newPlayerName.startsWith("browser_tab_")) {
                try {
                    int tabId = Integer.parseInt(newPlayerName.substring(12));
                    PENDING_COMMANDS.add(new BrowserCommand("play", tabId));
                } catch (Exception ignored) {}
            } else {
                if (IS_WINDOWS) {
                    execWin("play", newPlayerName, "");
                } else {
                    exec("playerctl -p " + newPlayerName + " play");
                }
            }
            for (int i = 0; i < 4; i++) {
                try { Thread.sleep(150); } catch (InterruptedException ignored) {}
                poll();
            }
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
        if (cmd.startsWith("playerctl") && !selectedPlayer.isEmpty() 
            && !cmd.contains("-l") && !cmd.contains("--list-all") 
            && !cmd.contains("-p") && !cmd.contains("--player")) {
            
            String[] parts = cmd.split(" ", 2);
            if (parts.length == 2) {
                cmd = parts[0] + " -p " + selectedPlayer + " " + parts[1];
            } else {
                cmd = cmd + " -p " + selectedPlayer;
            }
        }

        Process p = null;
        try {
            p = Runtime.getRuntime().exec(cmd.split(" "));
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (sb.length() > 0) {
                        sb.append("\n");
                    }
                    sb.append(line.trim());
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

    public static void pollWindows() {
        try {
            String output = execWin("poll", "", "");
            if (output.isEmpty()) {
                ALL_PLAYERS_INFO.clear();
                MediaState newState = new MediaState();
                newState.title = "";
                newState.artist = "";
                newState.isPlaying = false;
                newState.volume = 1.0f;
                newState.artUrl = "";
                CURRENT_STATE.set(newState);
                return;
            }

            java.util.List<PlayerInfo> playerInfos = new java.util.ArrayList<>();
            PlayerInfo currentDefault = null;
            PlayerInfo firstPlaying = null;
            PlayerInfo firstAny = null;

            String[] lines = output.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(";;;", -1);
                if (parts.length >= 8) {
                    PlayerInfo pi = new PlayerInfo();
                    pi.name = parts[0].trim();
                    pi.title = parts[1].trim();
                    pi.artist = parts[2].trim();
                    pi.isPlaying = "Playing".equalsIgnoreCase(parts[3].trim());
                    try {
                        pi.position = Double.parseDouble(parts[4].trim());
                    } catch (Exception ignored) {}
                    try {
                        pi.length = Double.parseDouble(parts[5].trim());
                    } catch (Exception ignored) {}
                    
                    String artPath = parts[6].trim();
                    pi.artUrl = artPath;
                    if (!artPath.isEmpty()) {
                        String lastUrlForPlayer = LOADED_ART_URLS.get(pi.name);
                        if (!artPath.equals(lastUrlForPlayer)) {
                            LOADED_ART_URLS.put(pi.name, artPath);
                            loadArtForPlayer(pi.name, artPath);
                        }
                        pi.artTexture = PLAYER_ART_TEXTURES.get(pi.name);
                        pi.artWidth = PLAYER_ART_WIDTHS.getOrDefault(pi.name, 0);
                        pi.artHeight = PLAYER_ART_HEIGHTS.getOrDefault(pi.name, 0);
                    }

                    playerInfos.add(pi);

                    boolean isCurrent = "True".equalsIgnoreCase(parts[7].trim());
                    if (isCurrent) {
                        currentDefault = pi;
                    }
                    if (pi.isPlaying && firstPlaying == null) {
                        firstPlaying = pi;
                    }
                    if (firstAny == null) {
                        firstAny = pi;
                    }
                }
            }

            addBrowserTabsToPlayerInfos(playerInfos);

            if (!selectedPlayer.isEmpty()) {
                boolean foundSelected = false;
                for (PlayerInfo p : playerInfos) {
                    if (p.name.equals(selectedPlayer)) {
                        foundSelected = true;
                        break;
                    }
                }
                if (!foundSelected) {
                    selectedPlayer = "";
                }
            }

            ALL_PLAYERS_INFO.clear();
            ALL_PLAYERS_INFO.addAll(playerInfos);

            PlayerInfo active = null;
            if (!selectedPlayer.isEmpty()) {
                for (PlayerInfo p : playerInfos) {
                    if (p.name.equals(selectedPlayer)) {
                        active = p;
                        break;
                    }
                }
            }
            if (active == null) {
                active = currentDefault;
            }
            if (active == null) {
                active = firstPlaying;
            }
            if (active == null) {
                active = firstAny;
            }

            MediaState oldState = CURRENT_STATE.get();
            MediaState newState = new MediaState();

            if (handleActiveBrowserTab(active, newState, oldState)) {
                // Handled!
            } else if (active != null) {
                newState.title = active.title;
                newState.artist = active.artist;
                newState.isPlaying = active.isPlaying;
                if (System.currentTimeMillis() < isPlayingLockUntil) {
                    newState.isPlaying = lockedIsPlaying;
                }
                newState.volume = oldState.volume;
                newState.artUrl = active.artUrl;
                newState.position = active.position;
                newState.length = active.length;
                newState.shuffle = oldState.shuffle;
                newState.loopStatus = oldState.loopStatus;

                if (!active.artUrl.isEmpty()) {
                    if (!active.artUrl.equals(lastArtUrl)) {
                        lastArtUrl = active.artUrl;
                        loadArt(active.artUrl);
                    }
                    newState.artTexture = oldState.artTexture;
                    newState.artWidth = oldState.artWidth;
                    newState.artHeight = oldState.artHeight;
                    newState.avgColor = oldState.avgColor;
                    newState.visualizerGradients = oldState.visualizerGradients;
                } else {
                    lastArtUrl = "";
                    newState.artTexture = null;
                }
            } else {
                newState.title = "";
                newState.artist = "";
                newState.isPlaying = false;
                newState.volume = 1.0f;
                newState.artUrl = "";
            }

            CURRENT_STATE.set(newState);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String execWin(String action, String target, String value) {
        Process p = null;
        try {
            java.io.File scriptFile = new java.io.File(System.getProperty("java.io.tmpdir"), "glassmenu_smtc.ps1");
            if (!scriptFile.exists()) {
                writeWindowsScript();
            }
            
            String[] cmd = new String[]{
                "powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File",
                scriptFile.getAbsolutePath(), action, target, value
            };
            
            p = Runtime.getRuntime().exec(cmd);
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (sb.length() > 0) {
                        sb.append("\n");
                    }
                    sb.append(line.trim());
                }
            }
            try (InputStream err = p.getErrorStream()) {
                byte[] buffer = new byte[1024];
                while (err.read(buffer) != -1) {
                    // Drain error stream
                }
            }
            p.waitFor(400, TimeUnit.MILLISECONDS);
            return sb.toString();
        } catch (Exception e) {
            return "";
        } finally {
            if (p != null) {
                p.destroyForcibly();
            }
        }
    }

    private static void writeWindowsScript() {
        try {
            java.io.File scriptFile = new java.io.File(System.getProperty("java.io.tmpdir"), "glassmenu_smtc.ps1");
            String script = "Add-Type -AssemblyName System.Runtime.WindowsRuntime\n" +
                "$asTaskGeneric = ([System.WindowsRuntimeSystemExtensions].GetMethods() | ? { \n" +
                "    $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 \n" +
                "})[0]\n" +
                "function Await($WinRtTask, $ResultType) {\n" +
                "    $asTask = $asTaskGeneric.MakeGenericMethod($ResultType)\n" +
                "    $netTask = $asTask.Invoke($null, @($WinRtTask))\n" +
                "    $netTask.Wait(-1) | Out-Null\n" +
                "    return $netTask.Result\n" +
                "}\n" +
                "$mgr = Await ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])\n" +
                "if (-not $mgr) { exit }\n" +
                "$action = $args[0]\n" +
                "$target = $args[1]\n" +
                "$value = $args[2]\n" +
                "if ($action -and $action -ne 'poll') {\n" +
                "    $sessions = $mgr.GetSessions()\n" +
                "    foreach ($session in $sessions) {\n" +
                "        $name = $session.SourceAppUserModelId\n" +
                "        if ($name.ToLower().Contains($target.ToLower())) {\n" +
                "            if ($action -eq 'play') {\n" +
                "                Await ($session.TryPlayAsync()) ([bool]) | Out-Null\n" +
                "            } elseif ($action -eq 'pause') {\n" +
                "                Await ($session.TryPauseAsync()) ([bool]) | Out-Null\n" +
                "            } elseif ($action -eq 'playPause') {\n" +
                "                $playback = $session.GetPlaybackInfo()\n" +
                "                if ($playback.PlaybackStatus -eq 4) {\n" +
                "                    Await ($session.TryPauseAsync()) ([bool]) | Out-Null\n" +
                "                } else {\n" +
                "                    Await ($session.TryPlayAsync()) ([bool]) | Out-Null\n" +
                "                }\n" +
                "            } elseif ($action -eq 'next') {\n" +
                "                Await ($session.TrySkipNextAsync()) ([bool]) | Out-Null\n" +
                "            } elseif ($action -eq 'previous') {\n" +
                "                Await ($session.TrySkipPreviousAsync()) ([bool]) | Out-Null\n" +
                "            } elseif ($action -eq 'seek') {\n" +
                "                $ticks = [Int64]([double]$value * 10000000)\n" +
                "                Await ($session.TryChangePlaybackPositionAsync($ticks)) ([bool]) | Out-Null\n" +
                "            }\n" +
                "            break\n" +
                "        }\n" +
                "    }\n" +
                "    exit\n" +
                "}\n" +
                "$sessions = $mgr.GetSessions()\n" +
                "$currentSession = $mgr.GetCurrentSession()\n" +
                "$currentName = ''\n" +
                "if ($currentSession) { $currentName = $currentSession.SourceAppUserModelId }\n" +
                "$output = @()\n" +
                "foreach ($session in $sessions) {\n" +
                "    $name = $session.SourceAppUserModelId\n" +
                "    $playback = $session.GetPlaybackInfo()\n" +
                "    $status = 'Paused'\n" +
                "    if ($playback.PlaybackStatus -eq 4) { $status = 'Playing' }\n" +
                "    $props = Await ($session.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])\n" +
                "    $title = $props.Title\n" +
                "    $artist = $props.Artist\n" +
                "    $timeline = $session.GetTimelineProperties()\n" +
                "    $position = 0\n" +
                "    $length = 0\n" +
                "    if ($timeline) {\n" +
                "        $position = $timeline.Position.TotalSeconds\n" +
                "        $length = $timeline.EndTime.TotalSeconds\n" +
                "    }\n" +
                "    $artPath = ''\n" +
                "    if ($props.Thumbnail) {\n" +
                "        try {\n" +
                "            $stream = Await ($props.Thumbnail.OpenReadAsync()) ([Windows.Storage.Streams.IRandomAccessStreamWithContentType])\n" +
                "            if ($stream) {\n" +
                "                $hash = [Math]::Abs(($title + $artist).GetHashCode())\n" +
                "                $tempPath = [System.IO.Path]::Combine([System.IO.Path]::GetTempPath(), \"glassmenu_art_$hash.png\")\n" +
                "                if (-not (Test-Path $tempPath)) {\n" +
                "                    $fileStream = [System.IO.File]::Create($tempPath)\n" +
                "                    $netStream = [System.IO.WindowsRuntimeStreamExtensions]::AsStream($stream)\n" +
                "                    $netStream.CopyTo($fileStream)\n" +
                "                    $fileStream.Dispose()\n" +
                "                }\n" +
                "                $artPath = $tempPath\n" +
                "                $stream.Dispose()\n" +
                "                if ((Get-Item $tempPath).Length -eq 0) { Remove-Item $tempPath; $artPath = '' }\n" +
                "            }\n" +
                "        } catch {}\n" +
                "    }\n" +
                "    $isCurrent = ($name -eq $currentName) ? 'True' : 'False'\n" +
                "    $output += \"$name;;;$title;;;$artist;;;$status;;;$position;;;$length;;;$artPath;;;$isCurrent\"\n" +
                "}\n" +
                "$output -join \"`n\"";
            
            java.nio.file.Files.write(scriptFile.toPath(), script.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void startHttpServer() {
        try {
            httpServer = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress(31252), 0);
            httpServer.createContext("/api/media/update", exchange -> {
                try {
                    if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
                        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                        exchange.sendResponseHeaders(204, -1);
                        return;
                    }
                    
                    if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                        java.io.InputStream is = exchange.getRequestBody();
                        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = is.read(buffer)) != -1) {
                            bos.write(buffer, 0, len);
                        }
                        String json = bos.toString("UTF-8");
                        
                        updateBrowserTabsFromJson(json);
                        
                        String responseJson = "{\"status\":\"ok\"}";
                        BrowserCommand cmd = PENDING_COMMANDS.poll();
                        if (cmd != null) {
                            responseJson = String.format("{\"command\":\"%s\",\"tabId\":%d}", cmd.command, cmd.tabId);
                        }
                        
                        byte[] respBytes = responseJson.getBytes("UTF-8");
                        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                        exchange.getResponseHeaders().add("Content-Type", "application/json");
                        exchange.sendResponseHeaders(200, respBytes.length);
                        exchange.getResponseBody().write(respBytes);
                    } else {
                        exchange.sendResponseHeaders(405, -1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    try { exchange.sendResponseHeaders(500, -1); } catch (Exception ignored) {}
                } finally {
                    exchange.close();
                }
            });
            httpServer.setExecutor(null);
            httpServer.start();
        } catch (Exception e) {
            System.err.println("Failed to start media controller HTTP server: " + e.getMessage());
        }
    }

    private static void updateBrowserTabsFromJson(String json) {
        java.util.List<BrowserTabInfo> newTabs = new java.util.ArrayList<>();
        try {
            int index = 0;
            while ((index = json.indexOf("{", index)) != -1) {
                int endIndex = json.indexOf("}", index);
                if (endIndex == -1) break;
                String obj = json.substring(index, endIndex + 1);
                index = endIndex + 1;
                
                BrowserTabInfo tab = new BrowserTabInfo();
                try {
                    tab.id = Integer.parseInt(extractJsonValue(obj, "id"));
                    tab.title = decodeJsonString(extractJsonValue(obj, "title"));
                    tab.url = decodeJsonString(extractJsonValue(obj, "url"));
                    tab.favIconUrl = decodeJsonString(extractJsonValue(obj, "favIconUrl"));
                    tab.audible = Boolean.parseBoolean(extractJsonValue(obj, "audible"));
                    tab.active = Boolean.parseBoolean(extractJsonValue(obj, "active"));
                    newTabs.add(tab);
                } catch (Exception ignored) {}
            }
            BROWSER_TABS.clear();
            BROWSER_TABS.addAll(newTabs);
        } catch (Exception e) {
            // Ignore parse errors
        }
    }

    private static String extractJsonValue(String obj, String key) {
        String searchKey = "\"" + key + "\":";
        int keyIndex = obj.indexOf(searchKey);
        if (keyIndex == -1) return "";
        int valStart = keyIndex + searchKey.length();
        char firstChar = obj.charAt(valStart);
        if (firstChar == '"') {
            int end = valStart + 1;
            while (end < obj.length()) {
                if (obj.charAt(end) == '"' && obj.charAt(end - 1) != '\\') {
                    break;
                }
                end++;
            }
            return obj.substring(valStart + 1, end);
        } else {
            int end = valStart;
            while (end < obj.length() && obj.charAt(end) != ',' && obj.charAt(end) != '}') {
                end++;
            }
            return obj.substring(valStart, end).trim();
        }
    }
    
    private static String decodeJsonString(String val) {
        if (val == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < val.length(); i++) {
            char c = val.charAt(i);
            if (c == '\\' && i + 1 < val.length()) {
                char next = val.charAt(i + 1);
                if (next == 'u' && i + 5 < val.length()) {
                    try {
                        int code = Integer.parseInt(val.substring(i + 2, i + 6), 16);
                        sb.append((char) code);
                        i += 5;
                    } catch (Exception e) {
                        sb.append(c);
                    }
                } else if (next == 'n') {
                    sb.append('\n');
                    i++;
                } else if (next == 'r') {
                    sb.append('\n');
                    i++;
                } else if (next == 't') {
                    sb.append('\t');
                    i++;
                } else {
                    sb.append(next);
                    i++;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static void addBrowserTabsToPlayerInfos(java.util.List<PlayerInfo> playerInfos) {
        for (BrowserTabInfo tab : BROWSER_TABS) {
            PlayerInfo pi = new PlayerInfo();
            pi.name = "browser_tab_" + tab.id;
            pi.title = tab.title;
            
            String domain = "Web";
            if (!tab.url.isEmpty()) {
                try {
                    String host = new java.net.URI(tab.url).getHost();
                    if (host != null) {
                        if (host.startsWith("www.")) host = host.substring(4);
                        if (host.contains("youtube.com")) domain = "YouTube";
                        else if (host.contains("spotify.com")) domain = "Spotify Web";
                        else if (host.contains("soundcloud.com")) domain = "SoundCloud";
                        else {
                            String[] parts = host.split("\\.");
                            if (parts.length >= 2) {
                                domain = parts[parts.length - 2];
                                domain = domain.substring(0, 1).toUpperCase() + domain.substring(1);
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
            pi.artist = domain;
            pi.isPlaying = tab.audible;
            pi.position = 0;
            pi.length = 0;
            
            if (tab.favIconUrl != null && !tab.favIconUrl.isEmpty()) {
                pi.artUrl = tab.favIconUrl;
                String lastUrlForPlayer = LOADED_ART_URLS.get(pi.name);
                if (!tab.favIconUrl.equals(lastUrlForPlayer)) {
                    LOADED_ART_URLS.put(pi.name, tab.favIconUrl);
                    loadArtForPlayer(pi.name, tab.favIconUrl);
                }
                pi.artTexture = PLAYER_ART_TEXTURES.get(pi.name);
                pi.artWidth = PLAYER_ART_WIDTHS.getOrDefault(pi.name, 0);
                pi.artHeight = PLAYER_ART_HEIGHTS.getOrDefault(pi.name, 0);
            }
            
            playerInfos.add(pi);
        }
    }

    private static boolean handleActiveBrowserTab(PlayerInfo active, MediaState newState, MediaState oldState) {
        if (active != null && active.name.startsWith("browser_tab_")) {
            newState.title = active.title;
            newState.artist = active.artist;
            newState.isPlaying = active.isPlaying;
            if (System.currentTimeMillis() < isPlayingLockUntil) {
                newState.isPlaying = lockedIsPlaying;
            }
            newState.volume = oldState.volume;
            newState.artUrl = active.artUrl;
            newState.position = active.position;
            newState.length = active.length;
            newState.shuffle = oldState.shuffle;
            newState.loopStatus = oldState.loopStatus;
            
            if (active.artTexture != null) {
                newState.artTexture = active.artTexture;
                newState.artWidth = active.artWidth;
                newState.artHeight = active.artHeight;
                newState.avgColor = oldState.avgColor;
                newState.visualizerGradients = oldState.visualizerGradients;
            } else {
                newState.artTexture = null;
            }
            return true;
        }
        return false;
    }
}
