/*
 * GameMenuScreenMixin - Architecture & Primary Responsibility:
 * Game Menu Screen Mixin.
 * Intercepts GameMenuScreen (escape menu) rendering and input events to display
 * the Dynamic Island media player at the top of the menu.
 */
package com.example.glassmenu.mixin;

import com.example.glassmenu.widget.DynamicIslandWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {

    protected GameMenuScreenMixin(Text title) {
        super(title);
    }

    @Inject(at = @At("TAIL"), method = "render")
    private void wastervisual$renderIsland(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        DynamicIslandWidget.render(context, this.width, this.height, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (DynamicIslandWidget.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
