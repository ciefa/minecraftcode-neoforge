package com.opencode.minecraft.game;

import com.opencode.minecraft.util.MarkdownToMinecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Renders OpenCode messages in Minecraft chat.
 */
public class MessageRenderer {
    private StringBuilder currentMessage = new StringBuilder();
    private boolean messageInProgress = false;
    private long lastUpdateTime = 0;

    // Flush accumulated text every 100ms for smoother output
    private static final long FLUSH_INTERVAL_MS = 100;

    /**
     * Starts a new assistant message
     */
    public void startNewMessage() {
        flushCurrentMessage();
        currentMessage = new StringBuilder();
        messageInProgress = true;
    }

    /**
     * Appends a delta (token) to the current message
     */
    public void appendDelta(String delta) {
        currentMessage.append(delta);

        long now = System.currentTimeMillis();
        if (now - lastUpdateTime > FLUSH_INTERVAL_MS) {
            flushCurrentMessage();
            lastUpdateTime = now;
        }
    }

    /**
     * Flushes the current message buffer to chat
     */
    public void flushCurrentMessage() {
        if (currentMessage.length() > 0) {
            String text = currentMessage.toString();
            currentMessage = new StringBuilder();

            // Split into lines and render each
            String[] lines = text.split("\n");
            for (String line : lines) {
                if (!line.isEmpty()) {
                    sendAssistantMessage(line);
                }
            }
        }
        messageInProgress = false;
    }

    /**
     * Adds a user message to chat
     */
    public void addUserMessage(String text) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        MutableComponent message = Component.literal("")
                .append(Component.literal("[You] ").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(text).withStyle(ChatFormatting.WHITE));

        client.gui.getChat().addMessage(message);
    }

    /**
     * Sends an assistant message line to chat (public)
     */
    public void sendAssistantLine(String text) {
        sendAssistantMessage(text);
    }

    /**
     * Sends an assistant message to chat
     */
    private void sendAssistantMessage(String text) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        // Convert markdown to Minecraft formatting
        Component formatted = MarkdownToMinecraft.convert(text);

        MutableComponent message = Component.literal("")
                .append(Component.literal("[OpenCode] ").withStyle(ChatFormatting.AQUA))
                .append(formatted);

        client.gui.getChat().addMessage(message);
    }

    /**
     * Sends a system message (status, errors, etc.)
     */
    public void sendSystemMessage(String text) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        MutableComponent message = Component.literal("")
                .append(Component.literal("[OpenCode] ").withStyle(ChatFormatting.GOLD))
                .append(Component.literal(text).withStyle(ChatFormatting.YELLOW));

        client.gui.getChat().addMessage(message);
    }

    /**
     * Sends an error message
     */
    public void sendErrorMessage(String text) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        MutableComponent message = Component.literal("")
                .append(Component.literal("[OpenCode Error] ").withStyle(ChatFormatting.RED))
                .append(Component.literal(text).withStyle(ChatFormatting.RED));

        client.gui.getChat().addMessage(message);
    }

    /**
     * Sends a tool execution message
     */
    public void sendToolMessage(String toolName, String status) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        ChatFormatting statusColor = switch (status) {
            case "running" -> ChatFormatting.YELLOW;
            case "completed" -> ChatFormatting.GREEN;
            case "failed" -> ChatFormatting.RED;
            default -> ChatFormatting.GRAY;
        };

        MutableComponent message = Component.literal("")
                .append(Component.literal("[Tool] ").withStyle(ChatFormatting.DARK_PURPLE))
                .append(Component.literal(toolName + ": ").withStyle(ChatFormatting.LIGHT_PURPLE))
                .append(Component.literal(status).withStyle(statusColor));

        client.gui.getChat().addMessage(message);
    }
}
