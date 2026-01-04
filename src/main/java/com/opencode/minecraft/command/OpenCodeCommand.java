package com.opencode.minecraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.opencode.minecraft.OpenCodeMod;
import com.opencode.minecraft.client.OpenCodeClient;
import com.opencode.minecraft.client.session.SessionInfo;
import com.opencode.minecraft.gui.OpenCodeGuiScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles all /oc commands for interacting with OpenCode.
 *
 * Commands:
 * - /oc                 - Show help
 * - /oc help            - Show help
 * - /oc gui             - Open ModernUI GUI interface
 * - /oc <prompt>        - Send a prompt to OpenCode
 * - /oc status          - Show connection and session status
 * - /oc session new     - Create a new session
 * - /oc session list    - List available sessions
 * - /oc session use <id> - Switch to an existing session
 * - /oc cancel          - Cancel current generation
 * - /oc config url <url> - Set server URL
 * - /oc config dir <path> - Set working directory
 * - /oc pause           - Toggle pause controller
 */
public class OpenCodeCommand {

    // Cache of sessions from last list command, indexed by number (1-based)
    private static List<SessionInfo> cachedSessions = new ArrayList<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("oc")
                // /oc help
                .then(Commands.literal("help")
                    .executes(OpenCodeCommand::executeHelp))

                // /oc gui
                .then(Commands.literal("gui")
                    .executes(OpenCodeCommand::executeGui))

                // /oc status
                .then(Commands.literal("status")
                    .executes(OpenCodeCommand::executeStatus))

                // /oc cancel
                .then(Commands.literal("cancel")
                    .executes(OpenCodeCommand::executeCancel))

                // /oc pause
                .then(Commands.literal("pause")
                    .executes(OpenCodeCommand::executePause))

                // /oc session ...
                .then(Commands.literal("session")
                    // /oc session new
                    .then(Commands.literal("new")
                        .executes(OpenCodeCommand::executeSessionNew))
                    // /oc session list
                    .then(Commands.literal("list")
                        .executes(OpenCodeCommand::executeSessionList))
                    // /oc session use <id>
                    .then(Commands.literal("use")
                        .then(Commands.argument("sessionId", StringArgumentType.string())
                            .executes(OpenCodeCommand::executeSessionUse))))

                // /oc config ...
                .then(Commands.literal("config")
                    // /oc config url <url>
                    .then(Commands.literal("url")
                        .then(Commands.argument("url", StringArgumentType.string())
                            .executes(OpenCodeCommand::executeConfigUrl)))
                    // /oc config dir <path>
                    .then(Commands.literal("dir")
                        .then(Commands.argument("path", StringArgumentType.greedyString())
                            .executes(OpenCodeCommand::executeConfigDir))))

                // /oc <prompt> - default: send prompt
                .then(Commands.argument("prompt", StringArgumentType.greedyString())
                    .executes(OpenCodeCommand::executePrompt))

                // /oc - show help
                .executes(OpenCodeCommand::executeHelp)
        );
    }

    private static int executeHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        source.sendSystemMessage(Component.literal("=== OpenCode Commands ===").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        source.sendSystemMessage(Component.literal("/oc <prompt>").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(" - Send a prompt").withStyle(ChatFormatting.GRAY)));
        source.sendSystemMessage(Component.literal("/oc gui").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(" - Open GUI interface").withStyle(ChatFormatting.GRAY)));
        source.sendSystemMessage(Component.literal("/oc status").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(" - Show status").withStyle(ChatFormatting.GRAY)));
        source.sendSystemMessage(Component.literal("/oc session new").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(" - Create new session").withStyle(ChatFormatting.GRAY)));
        source.sendSystemMessage(Component.literal("/oc session list").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(" - List sessions").withStyle(ChatFormatting.GRAY)));
        source.sendSystemMessage(Component.literal("/oc session use <#>").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(" - Switch session by number").withStyle(ChatFormatting.GRAY)));
        source.sendSystemMessage(Component.literal("/oc cancel").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(" - Cancel generation").withStyle(ChatFormatting.GRAY)));
        source.sendSystemMessage(Component.literal("/oc pause").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(" - Toggle pause control").withStyle(ChatFormatting.GRAY)));
        source.sendSystemMessage(Component.literal("/oc help").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(" - Show this help").withStyle(ChatFormatting.GRAY)));

        return 1;
    }

    private static int executeGui(CommandContext<CommandSourceStack> context) {
        // Open terminal GUI on client thread
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().setScreen(new OpenCodeGuiScreen());
        });
        return 1;
    }

    private static int executeStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        OpenCodeClient client = OpenCodeMod.getClient();

        source.sendSystemMessage(Component.literal("=== OpenCode Status ===").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));

        // Connection status
        boolean connected = client.isReady();
        source.sendSystemMessage(Component.literal("Connection: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(connected ? "Connected" : "Disconnected")
                        .withStyle(connected ? ChatFormatting.GREEN : ChatFormatting.RED)));

        // Session status
        SessionInfo session = client.getCurrentSession();
        if (session != null) {
            source.sendSystemMessage(Component.literal("Session: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(session.getId()).withStyle(ChatFormatting.YELLOW)));
            source.sendSystemMessage(Component.literal("Title: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(session.getTitle()).withStyle(ChatFormatting.WHITE)));
        } else {
            source.sendSystemMessage(Component.literal("Session: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("None (use /oc session new)").withStyle(ChatFormatting.YELLOW)));
        }

        // Pause status
        String pauseStatus = OpenCodeMod.getPauseController().getStatusText();
        source.sendSystemMessage(Component.literal("Status: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(pauseStatus).withStyle(ChatFormatting.GOLD)));

        return 1;
    }

    private static int executePrompt(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        OpenCodeClient client = OpenCodeMod.getClient();
        String prompt = StringArgumentType.getString(context, "prompt");

        if (!client.isReady()) {
            source.sendFailure(Component.literal("Not connected to OpenCode server"));
            return 0;
        }

        if (client.getCurrentSession() == null) {
            source.sendFailure(Component.literal("No active session. Use /oc session new"));
            return 0;
        }

        // Mark as typing to pause the game while submitting
        OpenCodeMod.getPauseController().setUserTyping(true);

        client.sendPrompt(prompt)
                .exceptionally(e -> {
                    OpenCodeMod.LOGGER.error("Failed to send prompt", e);
                    source.sendFailure(Component.literal("Failed: " + e.getMessage()));
                    OpenCodeMod.getPauseController().setUserTyping(false);
                    return null;
                });

        return 1;
    }

    private static int executeCancel(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        OpenCodeClient client = OpenCodeMod.getClient();

        client.cancel()
                .thenRun(() -> {
                    source.sendSystemMessage(Component.literal("Cancelled").withStyle(ChatFormatting.YELLOW));
                })
                .exceptionally(e -> {
                    source.sendFailure(Component.literal("Failed to cancel: " + e.getMessage()));
                    return null;
                });

        return 1;
    }

    private static int executePause(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        var pauseController = OpenCodeMod.getPauseController();

        boolean newState = !pauseController.isEnabled();
        pauseController.setEnabled(newState);

        // Save the setting to config so it persists across restarts
        OpenCodeMod.getConfigManager().setPauseEnabled(newState);

        source.sendSystemMessage(Component.literal("Pause control: ")
                .append(Component.literal(newState ? "Enabled" : "Disabled")
                        .withStyle(newState ? ChatFormatting.GREEN : ChatFormatting.RED)));

        return 1;
    }

    private static int executeSessionNew(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        OpenCodeClient client = OpenCodeMod.getClient();

        if (!client.isReady()) {
            source.sendFailure(Component.literal("Not connected to OpenCode server"));
            return 0;
        }

        source.sendSystemMessage(Component.literal("Creating new session...").withStyle(ChatFormatting.GRAY));

        client.createSession()
                .thenAccept(session -> {
                    source.sendSystemMessage(Component.literal("Created session: ")
                            .append(Component.literal(session.getId()).withStyle(ChatFormatting.GREEN)));
                })
                .exceptionally(e -> {
                    source.sendFailure(Component.literal("Failed: " + e.getMessage()));
                    return null;
                });

        return 1;
    }

    private static int executeSessionList(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        OpenCodeClient client = OpenCodeMod.getClient();

        if (!client.isReady()) {
            source.sendFailure(Component.literal("Not connected to OpenCode server"));
            return 0;
        }

        client.listSessions()
                .thenAccept(sessions -> {
                    if (sessions.isEmpty()) {
                        cachedSessions.clear();
                        source.sendSystemMessage(Component.literal("No sessions found").withStyle(ChatFormatting.YELLOW));
                        return;
                    }

                    // Cache sessions for use with /oc session use <number>
                    cachedSessions = new ArrayList<>(sessions);

                    source.sendSystemMessage(Component.literal("=== Sessions ===").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
                    source.sendSystemMessage(Component.literal("Use /oc session use <number> to switch").withStyle(ChatFormatting.GRAY));

                    for (int i = 0; i < sessions.size(); i++) {
                        SessionInfo session = sessions.get(i);
                        String current = client.getCurrentSession() != null &&
                                client.getCurrentSession().getId().equals(session.getId()) ? " (current)" : "";
                        source.sendSystemMessage(Component.literal((i + 1) + ". ").withStyle(ChatFormatting.GREEN)
                                .append(Component.literal(session.getTitle() + current).withStyle(ChatFormatting.WHITE)));
                    }
                })
                .exceptionally(e -> {
                    source.sendFailure(Component.literal("Failed: " + e.getMessage()));
                    return null;
                });

        return 1;
    }

    private static int executeSessionUse(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        OpenCodeClient client = OpenCodeMod.getClient();
        String sessionIdOrNumber = StringArgumentType.getString(context, "sessionId");

        if (!client.isReady()) {
            source.sendFailure(Component.literal("Not connected to OpenCode server"));
            return 0;
        }

        // Check if input is a number (reference to cached session list)
        // If it's a valid number in range, use cached session; otherwise treat as session ID
        String sessionId = sessionIdOrNumber;
        try {
            int index = Integer.parseInt(sessionIdOrNumber);
            if (!cachedSessions.isEmpty() && index >= 1 && index <= cachedSessions.size()) {
                sessionId = cachedSessions.get(index - 1).getId();
            }
            // If cache is empty or out of range, fall through and use as-is (let server validate)
        } catch (NumberFormatException e) {
            // Not a number, treat as session ID
        }

        final String finalSessionId = sessionId;
        client.useSession(finalSessionId)
                .thenAccept(session -> {
                    source.sendSystemMessage(Component.literal("Switched to session: ")
                            .append(Component.literal(session.getTitle()).withStyle(ChatFormatting.GREEN)));
                })
                .exceptionally(e -> {
                    source.sendFailure(Component.literal("Failed: " + e.getMessage()));
                    return null;
                });

        return 1;
    }

    private static int executeConfigUrl(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String url = StringArgumentType.getString(context, "url");

        OpenCodeMod.getConfigManager().setServerUrl(url);
        source.sendSystemMessage(Component.literal("Server URL set to: ")
                .append(Component.literal(url).withStyle(ChatFormatting.GREEN)));
        source.sendSystemMessage(Component.literal("Restart the game to apply changes").withStyle(ChatFormatting.YELLOW));

        return 1;
    }

    private static int executeConfigDir(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String path = StringArgumentType.getString(context, "path");

        OpenCodeMod.getConfigManager().setWorkingDirectory(path);
        source.sendSystemMessage(Component.literal("Working directory set to: ")
                .append(Component.literal(path).withStyle(ChatFormatting.GREEN)));
        source.sendSystemMessage(Component.literal("Restart the game to apply changes").withStyle(ChatFormatting.YELLOW));

        return 1;
    }
}
