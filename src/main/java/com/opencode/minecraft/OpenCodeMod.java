package com.opencode.minecraft;

import com.opencode.minecraft.client.OpenCodeClient;
import com.opencode.minecraft.command.OpenCodeCommand;
import com.opencode.minecraft.config.ConfigManager;
import com.opencode.minecraft.game.PauseController;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(OpenCodeMod.MOD_ID)
public class OpenCodeMod {
    public static final String MOD_ID = "opencode";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static OpenCodeClient client;
    private static PauseController pauseController;
    private static ConfigManager configManager;

    public OpenCodeMod() {
        LOGGER.info("Initializing OpenCode Minecraft client");

        // Initialize configuration
        configManager = new ConfigManager();
        configManager.load();

        // Initialize pause controller
        pauseController = new PauseController();
        pauseController.setEnabled(configManager.getConfig().pauseEnabled);

        // Initialize OpenCode client
        client = new OpenCodeClient(configManager.getConfig(), pauseController);

        // Register event handlers on the Forge event bus
        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("OpenCode Minecraft client initialized");
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        // Only process at the end of the tick
        if (event.phase == TickEvent.Phase.END) {
            pauseController.tick();
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterClientCommandsEvent event) {
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
