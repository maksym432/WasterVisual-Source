/*
 * TitleScreenMixin - Architecture & Primary Responsibility:
 * Title Screen Mixin.
 * Intercepts TitleScreen rendering, mouse gestures, and inputs to render the
 * immersive iPad lock screen overlay and intercept clicks for playback controls.
 */
package com.example.glassmenu.mixin;

import com.example.glassmenu.widget.IpadLockScreenManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(at = @At("HEAD"), method = "init()V")
    private void glassmenu$init(CallbackInfo ci) {
        IpadLockScreenManager.init();
    }

    @Inject(at = @At("TAIL"), method = "render")
    private void glassmenu$render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        IpadLockScreenManager.render(context, this.width, this.height, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        IpadLockScreenManager.resetInactivity();
        if (com.example.glassmenu.widget.DynamicIslandWidget.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (IpadLockScreenManager.mouseClicked(mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        IpadLockScreenManager.resetInactivity();
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        IpadLockScreenManager.resetInactivity();
        if (!IpadLockScreenManager.dismissed) {
            IpadLockScreenManager.mouseDragged(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        IpadLockScreenManager.resetInactivity();
        if (!IpadLockScreenManager.dismissed) {
            IpadLockScreenManager.mouseReleased(this.height);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        IpadLockScreenManager.resetInactivity();
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
