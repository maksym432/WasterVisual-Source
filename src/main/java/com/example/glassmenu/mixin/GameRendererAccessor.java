/*
 * GameRendererAccessor - Architecture & Primary Responsibility:
 * Mixin Accessor Interface.
 * Exposes internal Minecraft GameRenderer post-processing methods (loadPostProcessor,
 * disablePostProcessor) to allow screen managers to trigger full-screen shader blurs.
 */
package com.example.glassmenu.mixin;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    @Invoker("loadPostProcessor")
    void glassmenu$loadPostProcessor(Identifier id);

    @Invoker("disablePostProcessor")
    void glassmenu$disablePostProcessor();
}
