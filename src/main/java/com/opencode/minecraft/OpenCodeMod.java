package com.opencode.minecraft;

import com.opencode.minecraft.client.OpenCodeClient;
import com.opencode.minecraft.command.OpenCodeCommand;
import com.opencode.minecraft.config.ConfigManager;
import com.opencode.minecraft.game.PauseController;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(OpenCodeMod.MOD_ID)
public class OpenCodeMod {
    public static final String MOD_ID = "opencode";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static OpenCodeClient client;
    private static PauseController pauseController;
    private static ConfigManager configManager;

    public OpenCodeMod(IEventBus modEventBus) {
        LOGGER.info("Initializing OpenCode Minecraft client");

        // Initialize configuration
        configManager = new ConfigManager();
        configManager.load();

        // Initialize pause controller
        pauseController = new PauseController();
        pauseController.setEnabled(configManager.getConfig().pauseEnabled);

        // Initialize OpenCode client
        client = new OpenCodeClient(configManager.getConfig(), pauseController);

        // Register event handlers
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);

        LOGGER.info("OpenCode Minecraft client initialized");
    }

    private void onClientTick(ClientTickEvent.Post event) {
        pauseController.tick();
    }

    private void onRegisterCommands(RegisterClientCommandsEvent event) {
        OpenCodeCommand.register(event.getDispatcher());
    }

    public static OpenCodeClient getClient() {
        return client;
    }

    public static PauseController getPauseController() {
        return pauseController;
    }

    public static ConfigManager getConfigManager() {
        return configManager;
    }
}
