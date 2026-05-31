/*
 * CssParser - Architecture & Primary Responsibility:
 * Lightweight CSS Stylesheet Parser.
 * Parses CSS selector blocks (e.g. .panel, .button) and maps properties
 * (background-color, border-color, border-width, border-radius, blur-radius, scale, color)
 * to design tokens used by OpenGL SDF shaders.
 */
package com.example.glassmenu.render;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.*;

public class CssParser {
    public static class StyleRule {
        public int backgroundColor = 0x00000000;
        public int borderColor = 0x00000000;
        public float borderWidth = 0.0f;
        public float borderRadius = 0.0f;
        public float blurRadius = 0.0f;
        public int textColor = 0xFFFFFFFF;
        public float scale = 1.0f;
    }

    private final Map<String, StyleRule> rules = new HashMap<>();

    public void load(File file) {
        rules.clear();
        if (!file.exists()) return;
        try {
            String content = Files.readString(file.toPath());
            parse(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void parse(String css) {
        // Remove comments
        css = css.replaceAll("/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/", "");
        
        // Match rules like selector { decls }
        Pattern pattern = Pattern.compile("([\\w.:-]+)\\s*\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(css);
        while (matcher.find()) {
            String selector = matcher.group(1).trim().toLowerCase();
            String body = matcher.group(2).trim();
            
            StyleRule rule = new StyleRule();
            parseBody(body, rule);
            rules.put(selector, rule);
        }
    }

    private void parseBody(String body, StyleRule rule) {
        String[] declarations = body.split(";");
        for (String decl : declarations) {
            String[] parts = decl.split(":", 2);
            if (parts.length < 2) continue;
            String prop = parts[0].trim().toLowerCase();
            String val = parts[1].trim().toLowerCase();
            
            switch (prop) {
                case "background-color":
                    rule.backgroundColor = parseColor(val);
                    break;
                case "border-color":
                    rule.borderColor = parseColor(val);
                    break;
                case "border-width":
                    rule.borderWidth = parseSize(val);
                    break;
                case "border-radius":
                    rule.borderRadius = parseSize(val);
                    break;
                case "blur-radius":
                    rule.blurRadius = parseSize(val);
                    break;
                case "color":
                    rule.textColor = parseColor(val);
                    break;
                case "scale":
                    try {
                        rule.scale = Float.parseFloat(val);
                    } catch (NumberFormatException ignored) {}
                    break;
            }
        }
    }

    private int parseColor(String val) {
        val = val.trim();
        // Parse hex color like #ffffff or #ffffffff
        if (val.startsWith("#")) {
            String hex = val.substring(1);
            if (hex.length() == 6) {
                return 0xFF000000 | Integer.parseInt(hex, 16);
            } else if (hex.length() == 8) {
                long rgba = Long.parseLong(hex, 16);
                int r = (int) ((rgba >> 24) & 0xFF);
                int g = (int) ((rgba >> 16) & 0xFF);
                int b = (int) ((rgba >> 8) & 0xFF);
                int a = (int) (rgba & 0xFF);
                return (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
        // Parse rgba like rgba(28, 28, 36, 0.25)
        if (val.startsWith("rgba")) {
            Pattern p = Pattern.compile("rgba\\s*\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*([\\d.]+)\\s*\\)");
            Matcher m = p.matcher(val);
            if (m.find()) {
                int r = Integer.parseInt(m.group(1));
                int g = Integer.parseInt(m.group(2));
                int b = Integer.parseInt(m.group(3));
                float a = Float.parseFloat(m.group(4));
                int alphaVal = (int) (a * 255);
                return (alphaVal << 24) | (r << 16) | (g << 8) | b;
            }
        }
        // Parse rgb like rgb(255, 255, 255)
        if (val.startsWith("rgb")) {
            Pattern p = Pattern.compile("rgb\\s*\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)");
            Matcher m = p.matcher(val);
            if (m.find()) {
                int r = Integer.parseInt(m.group(1));
                int g = Integer.parseInt(m.group(2));
                int b = Integer.parseInt(m.group(3));
                return 0xFF000000 | (r << 16) | (g << 8) | b;
            }
        }
        return 0xFFFFFFFF; // Default fallback
    }

    private float parseSize(String val) {
        val = val.trim();
        if (val.endsWith("px")) {
            val = val.substring(0, val.length() - 2).trim();
        }
        try {
            return Float.parseFloat(val);
        } catch (NumberFormatException e) {
            return 0.0f;
        }
    }

    public StyleRule getRule(String selector) {
        return rules.getOrDefault(selector.toLowerCase(), new StyleRule());
    }
}
