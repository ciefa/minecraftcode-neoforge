package com.opencode.minecraft.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.opencode.minecraft.OpenCodeMod;
import com.opencode.minecraft.client.session.SessionInfo;
import com.opencode.minecraft.gui.markdown.FormattedLine;
import com.opencode.minecraft.gui.markdown.MarkdownParser;
import com.opencode.minecraft.gui.markdown.TextSegment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Terminal-style GUI screen for OpenCode chat interface
 * Inspired by classic command-line terminals
 */
public class OpenCodeGuiScreen extends Screen {

    // Autumn theme colors
    private static final int BACKGROUND_COLOR = 0xE01a1614; // Dark brown (semi-transparent)
    private static final int BORDER_COLOR = 0xFFe67700; // Orange border
    private static final int TEXT_COLOR = 0xFFf4e8d0; // Cream text
    private static final int INPUT_COLOR = 0xFFfaf0e6; // Bright cream input
    private static final int PROMPT_COLOR = 0xFFe67700; // Orange prompt

    private EditBox inputField;
    private List<FormattedLine> messageHistory;
    private int scrollOffset = 0;
    private static final int MAX_VISIBLE_LINES = 20;
    private StringBuilder currentAssistantMessage = new StringBuilder();
    private boolean receivingResponse = false;

    public OpenCodeGuiScreen() {
        super(Component.literal("OpenCode Terminal"));
        this.messageHistory = new ArrayList<>();
    }

    @Override
    protected void init() {
        super.init();

        // Load message history from current session
        if (messageHistory.isEmpty()) {
            loadMessageHistory();
        }

        // Register for real-time message updates
        OpenCodeMod.getClient().setGuiMessageListener(this::onMessageDelta);
        OpenCodeMod.getClient().setGuiResponseCompleteListener(this::onResponseComplete);

        // Calculate dimensions for terminal window
        int terminalWidth = this.width - 40;
        int terminalHeight = this.height - 40;
        int terminalX = 20;
        int terminalY = 20;

        // Input field at bottom
        int inputY = terminalY + terminalHeight - 30;
        int inputX = terminalX + 30; // Leave space for prompt ">"

        this.inputField = new EditBox(
            this.font,
            inputX,
            inputY,
            terminalWidth - 40,
            20,
            Component.literal("Input")
        );
        this.inputField.setMaxLength(1000);
        this.inputField.setBordered(false);
        this.inputField.setTextColor(INPUT_COLOR);
        this.inputField.setValue("");
        this.addRenderableWidget(this.inputField);
        this.setInitialFocus(this.inputField);
    }

    @Override
    public void removed() {
        super.removed();
        // Unregister listener when GUI is closed
        OpenCodeMod.getClient().clearGuiMessageListener();
    }

    /**
     * Called when a message delta arrives via SSE
     */
    private void onMessageDelta(String delta) {
        // This is called from the main thread via OpenCodeClient
        if (!receivingResponse) {
            // Start of a new response
            receivingResponse = true;
            currentAssistantMessage.setLength(0);
        }

        // Append delta to current message
        currentAssistantMessage.append(delta);

        // Update the last message line with the accumulated text
        updateLastMessage("[OPENCODE] " + currentAssistantMessage.toString());

        // Auto-scroll to bottom when receiving messages
        scrollOffset = 0;
    }

    /**
     * Called when a response completes (session goes to idle)
     */
    private void onResponseComplete() {
        if (receivingResponse) {
            // Add spacing after completed response
            addMessage("", 0xFF8b6f47);
            receivingResponse = false;
        }
    }

    /**
     * Updates the last message in the history (used for streaming updates)
     */
    private void updateLastMessage(String newText) {
        // Find where the last assistant message starts by looking backwards
        int lastMessageStart = messageHistory.size();

        // Look for the last [OPENCODE] message
        for (int i = messageHistory.size() - 1; i >= 0; i--) {
            FormattedLine line = messageHistory.get(i);
            String plainText = line.getPlainText();
            if (plainText.trim().startsWith("[OPENCODE]")) {
                lastMessageStart = i;
                break;
            }
            // Stop at empty lines or other message types
            if (plainText.trim().isEmpty() || plainText.trim().startsWith("[YOU]") || plainText.trim().startsWith("[SYSTEM]")) {
                break;
            }
        }

        // Remove old assistant message lines
        while (messageHistory.size() > lastMessageStart) {
            messageHistory.remove(messageHistory.size() - 1);
        }

        // Parse markdown and add the new text
        List<FormattedLine> parsedLines = MarkdownParser.parse(newText, 0xFFdaa520); // Goldenrod for responses
        messageHistory.addAll(parsedLines);
    }

    private void loadMessageHistory() {
        // Add header
        addMessage("[SYSTEM] OpenCode Terminal v1.0", 0xFFe67700); // Orange
        addMessage("", 0xFF8b6f47);

        // Get current session and load messages
        SessionInfo session = OpenCodeMod.getClient().getCurrentSession();
        if (session != null) {
            addMessage("[SYSTEM] Loading session: " + session.getId(), 0xFF8b6f47);
            addMessage("", 0xFF8b6f47);

            // Fetch messages asynchronously
            OpenCodeMod.getClient().getCurrentSession();
            // Access the HTTP client through reflection or add a getter
            // For now, we'll need to add a method to OpenCodeClient to expose this
            loadSessionMessages(session.getId());
        } else {
            addMessage("[SYSTEM] No active session", 0xFF8b6f47);
            addMessage("[SYSTEM] Run '/oc session new' to create a session", 0xFF8b6f47);
            addMessage("", 0xFF8b6f47);
        }
    }

    private void loadSessionMessages(String sessionId) {
        OpenCodeMod.getClient().getSessionMessages(sessionId)
                .thenAccept(messages -> {
                    // Process on main thread
                    net.minecraft.client.Minecraft.getInstance().execute(() -> {
                        if (messages.size() == 0) {
                            addMessage("[SYSTEM] No messages in session yet", 0xFF8b6f47);
                            addMessage("[SYSTEM] Type your prompt below to start", 0xFF8b6f47);
                            addMessage("", 0xFF8b6f47);
                        } else {
                            addMessage("[SYSTEM] Loaded " + messages.size() + " messages", 0xFF8b6f47);
                            addMessage("", 0xFF8b6f47);

                            // Parse and display each message
                            for (JsonElement msgElement : messages) {
                                parseAndDisplayMessage(msgElement.getAsJsonObject());
                            }
                        }
                    });
                })
                .exceptionally(e -> {
                    net.minecraft.client.Minecraft.getInstance().execute(() -> {
                        addMessage("[ERROR] Failed to load messages: " + e.getMessage(), 0xFFff0000);
                        addMessage("", 0xFF8b6f47);
                    });
                    return null;
                });
    }

    private void parseAndDisplayMessage(JsonObject message) {
        // Get message info
        JsonObject info = message.has("info") ? message.getAsJsonObject("info") : null;
        if (info == null) return;

        String role = info.has("role") ? info.get("role").getAsString() : "unknown";

        // Get message parts
        JsonArray parts = message.has("parts") ? message.getAsJsonArray("parts") : new JsonArray();

        // Display based on role
        for (JsonElement partElement : parts) {
            JsonObject part = partElement.getAsJsonObject();
            String type = part.has("type") ? part.get("type").getAsString() : "";

            // Only display text parts
            if ("text".equals(type) && part.has("text")) {
                String text = part.get("text").getAsString();

                if ("user".equals(role)) {
                    addMessage("[YOU] " + text, 0xFFf4a261); // Light orange/peach
                } else if ("assistant".equals(role)) {
                    addMessage("[OPENCODE] " + text, 0xFFdaa520); // Goldenrod
                }
            }
        }

        // Add spacing after each message exchange
        addMessage("", 0xFF8b6f47);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render dark background to prevent blur
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        // Calculate dimensions
        int terminalWidth = this.width - 40;
        int terminalHeight = this.height - 40;
        int terminalX = 20;
        int terminalY = 20;

        // Draw terminal background (autumn dark brown)
        guiGraphics.fill(0, 0, this.width, this.height, 0xFF1a1614); // Full dark brown background
        guiGraphics.fill(terminalX, terminalY, terminalX + terminalWidth, terminalY + terminalHeight, BACKGROUND_COLOR);

        // Draw border (terminal-style double line)
        drawTerminalBorder(guiGraphics, terminalX, terminalY, terminalWidth, terminalHeight);

        // Draw title bar (with more spacing from top border)
        String title = "┤ OpenCode Terminal ├";
        int titleX = terminalX + (terminalWidth - this.font.width(title)) / 2;
        guiGraphics.drawString(this.font, title, titleX, terminalY + 11, BORDER_COLOR, false);

        // Draw message history (adjusted for title spacing)
        int messageY = terminalY + 24;
        int maxY = terminalY + terminalHeight - 40;
        int lineHeight = this.font.lineHeight + 2;

        int startIndex = Math.max(0, messageHistory.size() - MAX_VISIBLE_LINES - scrollOffset);
        int endIndex = Math.min(messageHistory.size(), startIndex + MAX_VISIBLE_LINES);

        for (int i = startIndex; i < endIndex; i++) {
            if (messageY >= maxY) break;

            FormattedLine line = messageHistory.get(i);
            renderFormattedLine(guiGraphics, line, terminalX + 10, messageY);
            messageY += lineHeight;
        }

        // Draw input prompt
        int inputY = terminalY + terminalHeight - 30;
        guiGraphics.drawString(this.font, ">", terminalX + 10, inputY + 6, PROMPT_COLOR, false);

        // Draw input field border (darker brown)
        int inputBoxX = terminalX + 25;
        int inputBoxY = inputY + 2;
        int inputBoxWidth = terminalWidth - 35;
        guiGraphics.fill(inputBoxX, inputBoxY, inputBoxX + inputBoxWidth, inputBoxY + 18, 0xFF3e342e);

        // Render widgets (input field)
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Draw scroll indicator if needed (tan/brown color)
        if (messageHistory.size() > MAX_VISIBLE_LINES) {
            String scrollInfo = String.format("[↑↓ to scroll | %d/%d]",
                Math.max(0, messageHistory.size() - MAX_VISIBLE_LINES - scrollOffset),
                messageHistory.size() - MAX_VISIBLE_LINES);
            guiGraphics.drawString(this.font, scrollInfo, terminalX + terminalWidth - this.font.width(scrollInfo) - 10,
                terminalY + terminalHeight - 10, 0xFF8b6f47, false);
        }
    }

    private void drawTerminalBorder(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        int borderCharWidth = this.font.width("─");
        int sideCharWidth = this.font.width("│");

        // Top border
        guiGraphics.drawString(this.font, "┌" + "─".repeat((width - 20) / borderCharWidth) + "┐",
            x, y, BORDER_COLOR, false);

        // Bottom border
        guiGraphics.drawString(this.font, "└" + "─".repeat((width - 20) / borderCharWidth) + "┘",
            x, y + height - 10, BORDER_COLOR, false);

        // Side borders (adjusted right side for proper alignment)
        for (int i = 10; i < height - 10; i += this.font.lineHeight) {
            guiGraphics.drawString(this.font, "│", x, y + i, BORDER_COLOR, false);
            guiGraphics.drawString(this.font, "│", x + width - 9, y + i, BORDER_COLOR, false);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            handleSubmit();
            return true;
        }

        // Scroll with arrow keys
        if (keyCode == GLFW.GLFW_KEY_UP) {
            scrollOffset = Math.min(scrollOffset + 1, Math.max(0, messageHistory.size() - MAX_VISIBLE_LINES));
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            scrollOffset = Math.max(0, scrollOffset - 1);
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }

        return this.inputField.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        // Scroll with mouse wheel
        if (deltaY > 0) {
            scrollOffset = Math.min(scrollOffset + 1, Math.max(0, messageHistory.size() - MAX_VISIBLE_LINES));
        } else if (deltaY < 0) {
            scrollOffset = Math.max(0, scrollOffset - 1);
        }
        return true;
    }

    private void handleSubmit() {
        String text = this.inputField.getValue().trim();
        if (!text.isEmpty()) {
            // Check if we have an active session
            SessionInfo session = OpenCodeMod.getClient().getCurrentSession();
            if (session == null) {
                addMessage("[ERROR] No active session. Run '/oc session new' first", 0xFFff0000);
                this.inputField.setValue("");
                return;
            }

            addMessage("[YOU] " + text, 0xFFf4a261); // Light orange/peach for user input
            addMessage("", 0xFF8b6f47); // Empty line for spacing
            this.inputField.setValue("");

            // Reset response tracking
            receivingResponse = false;
            currentAssistantMessage.setLength(0);

            // Send to OpenCode server
            OpenCodeMod.getClient().sendPrompt(text)
                    .exceptionally(e -> {
                        net.minecraft.client.Minecraft.getInstance().execute(() -> {
                            addMessage("[ERROR] Failed to send message: " + e.getMessage(), 0xFFff0000);
                        });
                        return null;
                    });

            // Auto-scroll to bottom
            scrollOffset = 0;
        }
    }

    public void addMessage(String message, int color) {
        // Parse markdown and add formatted lines
        List<FormattedLine> parsedLines = MarkdownParser.parse(message, color);
        messageHistory.addAll(parsedLines);

        // Keep scroll at bottom for new messages
        if (scrollOffset == 0) {
            // Already at bottom, stay there
        }
    }

    /**
     * Renders a formatted line with multiple colored segments
     */
    private void renderFormattedLine(GuiGraphics guiGraphics, FormattedLine line, int x, int y) {
        int currentX = x;

        // Add indentation for code blocks and lists
        for (int i = 0; i < line.getIndentLevel(); i++) {
            currentX += this.font.width("  ");
        }

        // Draw each segment
        for (TextSegment segment : line.getSegments()) {
            String text = segment.getText();
            int color = segment.getEffectiveColor();

            guiGraphics.drawString(this.font, text, currentX, y, color, false);
            currentX += this.font.width(text);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false; // Don't pause the game
    }
}
