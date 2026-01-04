package com.opencode.minecraft.mixin;

import com.opencode.minecraft.OpenCodeMod;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to block player movement input when OpenCode is paused.
 */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {

    /**
     * After tick processes input, zero out all movement if paused.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void opencode$onTick(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
        if (OpenCodeMod.getPauseController() != null &&
            OpenCodeMod.getPauseController().shouldGameBePaused()) {

            // Zero out all movement
            this.up = false;
            this.down = false;
            this.left = false;
            this.right = false;
            this.jumping = false;
            this.shiftKeyDown = false;
            this.forwardImpulse = 0.0f;
            this.leftImpulse = 0.0f;
        }
    }
}
