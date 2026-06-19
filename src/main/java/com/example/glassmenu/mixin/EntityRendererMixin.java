package com.example.glassmenu.mixin;

import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.example.glassmenu.GlassMenuClient;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {

    @ModifyVariable(method = "renderLabelIfPresent", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Text glassmenu$modifyNametagText(Text text, T entity) {
        if (!GlassMenuClient.CONFIG.enableCustomNametags()) {
            return text;
        }

        // Strip existing formatting and apply custom RGB color
        int customColor = GlassMenuClient.CONFIG.customNametagColor() & 0xFFFFFF;
        
        return Text.literal(text.getString()).setStyle(Style.EMPTY.withColor(customColor));
    }
}
