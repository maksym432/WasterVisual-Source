/*
 * TitleScreenMixin - Architecture & Primary Responsibility:
 * Title Screen Mixin.
 * Intercepts TitleScreen rendering and mouse inputs to render the
 * Dynamic Island and handle playback controls.
 */
package com.example.glassmenu.mixin;

import com.example.glassmenu.widget.IslandManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(at = @At("HEAD"), method = "init()V")
    private void glassmenu$init(CallbackInfo ci) {
        IslandManager.init();
    }

    @Inject(at = @At("TAIL"), method = "render")
    private void glassmenu$render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        IslandManager.render(context, this.width, this.height, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (IslandManager.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
