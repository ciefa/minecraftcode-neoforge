package com.opencode.minecraft.mixin;

import com.opencode.minecraft.game.PauseOverlay;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to render the pause overlay on the HUD.
 */
@Mixin(Gui.class)
public abstract class GuiMixin {

    /**
     * Render the pause overlay after the normal HUD renders.
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void opencode$onRender(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        PauseOverlay.render(context, tickCounter.getGameTimeDeltaTicks());
    }
}
