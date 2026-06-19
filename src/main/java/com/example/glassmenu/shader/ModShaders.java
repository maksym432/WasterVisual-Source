/**
 * Shader Registry and Loader.
 * Manages the registration and lifecycle of core shaders used for 
 * SDF rendering, frosted glass effects, and solid color overlays.
 */
package com.example.glassmenu.shader;

import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

public class ModShaders {
    private static ShaderProgram sdfRoundedRect;
    private static ShaderProgram glassRefraction;
    private static ShaderProgram frostedGlass;
    private static ShaderProgram solidColor;
    private static ShaderProgram invGlass;
    private static ShaderProgram jumpGlow;
    private static ShaderProgram hitStar;
    private static ShaderProgram colorGrading;

    public static void init() {
        CoreShaderRegistrationCallback.EVENT.register(context -> {
            try {
                context.register(Identifier.of("glassmenu", "sdf_rounded_rect"), VertexFormats.POSITION_TEXTURE, shader -> {
                    sdfRoundedRect = shader;
                });
                context.register(Identifier.of("glassmenu", "glass_refraction"), VertexFormats.POSITION_TEXTURE, shader -> {
                    glassRefraction = shader;
                });
                context.register(Identifier.of("glassmenu", "frosted_glass"), VertexFormats.POSITION_TEXTURE, shader -> {
                    frostedGlass = shader;
                });
                context.register(Identifier.of("glassmenu", "solid_color"), VertexFormats.POSITION, shader -> {
                    solidColor = shader;
                });
                context.register(Identifier.of("glassmenu", "inv_glass"), VertexFormats.POSITION_TEXTURE, shader -> {
                    invGlass = shader;
                });
                context.register(Identifier.of("glassmenu", "jump_glow"), VertexFormats.POSITION_TEXTURE_COLOR, shader -> {
                    jumpGlow = shader;
                });
                context.register(Identifier.of("glassmenu", "hit_star"), VertexFormats.POSITION_TEXTURE_COLOR, shader -> {
                    hitStar = shader;
                });
                context.register(Identifier.of("glassmenu", "color_grading"), VertexFormats.POSITION_TEXTURE, shader -> {
                    colorGrading = shader;
                });
            } catch (Exception e) {
                System.err.println("Failed to register glassmenu shaders!");
            }
        });
    }

    public static ShaderProgram getSdfRoundedRect() {
        return sdfRoundedRect;
    }

    public static ShaderProgram getGlassRefraction() {
        return glassRefraction;
    }

    public static ShaderProgram getFrostedGlass() {
        return frostedGlass;
    }

    public static ShaderProgram getSolidColor() {
        return solidColor;
    }

    public static ShaderProgram getInvGlass() {
        return invGlass;
    }

    public static ShaderProgram getJumpGlow() {
        return jumpGlow;
    }

    public static ShaderProgram getHitStar() {
        return hitStar;
    }

    public static ShaderProgram getColorGrading() {
        return colorGrading;
    }
}
