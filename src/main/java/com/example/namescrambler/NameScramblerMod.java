package com.example.namescrambler;

import com.example.namescrambler.command.ProximityChatCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
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
        
        // Добавляем обработчик входа игрока
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            setupPlayer(player);
        });
    }
    
    private void setupPlayer(ServerPlayerEntity player) {
        try {
            // Используем команды для создания команды с префиксом §k
            String teamName = "namescrambler_" + player.getUuid().toString().substring(0, 8);
            
            // Создаем команду
            player.getServer().getCommandManager().executeWithPrefix(
                player.getServer().getCommandSource().withSilent(),
                "team add " + teamName
            );
            
            // Устанавливаем префикс §k для команды
            player.getServer().getCommandManager().executeWithPrefix(
                player.getServer().getCommandSource().withSilent(),
                "team modify " + teamName + " prefix \"§k\""
            );
            
            // Добавляем игрока в команду
            player.getServer().getCommandManager().executeWithPrefix(
                player.getServer().getCommandSource().withSilent(),
                "team join " + teamName + " " + player.getGameProfile().getName()
            );
            
            LOGGER.info("Applied name hiding for: {}", player.getGameProfile().getName());
            
        } catch (Exception e) {
            LOGGER.error("Error setting up player: {}", e.getMessage());
        }
    }
}