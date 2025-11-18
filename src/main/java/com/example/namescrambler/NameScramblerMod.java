package com.example.namescrambler;

import com.example.namescrambler.command.ProximityChatCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NameScramblerMod implements ModInitializer {

    public static final Logger LOGGER = LogManager.getLogger("NameScrambler");
    public static final Config CONFIG = new Config();

    @Override
    public void onInitialize() {
        LOGGER.info("Name Scrambler + Proximity Chat mod by AVCD initialized!");

        // Загружаем конфиг при запуске сервера
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            CONFIG.load();
            LOGGER.info("Proximity chat radius: {} blocks", CONFIG.chatRadius);
        });

        // Регистрируем команду
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ProximityChatCommand.register(dispatcher);
        });
    }
}
