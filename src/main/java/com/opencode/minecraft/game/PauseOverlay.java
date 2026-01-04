package com.opencode.minecraft.game;

import com.opencode.minecraft.OpenCodeMod;
import com.opencode.minecraft.client.session.SessionStatus;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Renders an overlay when the game is paused by OpenCode.
 */
public class PauseOverlay {

    /**
     * Renders the pause overlay if the game is paused.
     * Called from HUD rendering.
     */
    public static void render(GuiGraphics context, float tickDelta) {
        PauseController pauseController = OpenCodeMod.getPauseController();
        if (pauseController == null || !pauseController.shouldGameBePaused()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        Font textRenderer = client.font;

        // Semi-transparent dark overlay
        int overlayColor = 0x88000000;
        context.fill(0, 0, screenWidth, screenHeight, overlayColor);

        // Main message
        String mainMessage = getMainMessage(pauseController);
        String subMessage = getSubMessage(pauseController);

        // Center the text
        int mainWidth = textRenderer.width(mainMessage);
        int subWidth = textRenderer.width(subMessage);

        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        // Draw main message with shadow
        context.drawString(
            textRenderer,
            Component.literal(mainMessage).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
            centerX - mainWidth / 2,
            centerY - 20,
            0xFFFFAA00
        );

        // Draw sub message
        context.drawString(
            textRenderer,
            Component.literal(subMessage).withStyle(ChatFormatting.GRAY),
            centerX - subWidth / 2,
            centerY + 5,
            0xFFAAAAAA
        );

        // Draw hint at bottom
        String hint = "Use /oc <prompt> to give OpenCode a task";
        int hintWidth = textRenderer.width(hint);
        context.drawString(
            textRenderer,
            Component.literal(hint).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC),
            centerX - hintWidth / 2,
            centerY + 30,
            0xFF666666
        );
    }

    private static String getMainMessage(PauseController controller) {
        SessionStatus status = controller.getStatus();

        if (controller.isUserTyping()) {
            return "GAME PAUSED - Typing...";
        }

        return switch (status) {
            case DISCONNECTED -> "GAME PAUSED - Not Connected";
            case IDLE -> "GAME PAUSED - Waiting for Task";
            case BUSY -> "Processing...";
            case GENERATING -> "OpenCode is working...";
            case RETRY -> "Retrying...";
        };
    }

    private static String getSubMessage(PauseController controller) {
        SessionStatus status = controller.getStatus();

        if (controller.isUserTyping()) {
            return "Game will resume when OpenCode starts generating";
        }

        return switch (status) {
            case DISCONNECTED -> "Start OpenCode with: opencode serve";
            case IDLE -> "You can only play while OpenCode is building";
            case BUSY -> "Waiting for response...";
            case GENERATING -> "Game resumed - LLM is generating tokens";
            case RETRY -> "Connection issue, retrying...";
        };
    }
}
