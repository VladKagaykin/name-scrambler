package com.example.namescrambler;

import com.example.namescrambler.command.HideNamesCommand;
import com.example.namescrambler.command.ProximityChatCommand;
import com.example.namescrambler.command.AbilityCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NameScramblerMod implements ModInitializer {

    public static final Logger LOGGER = LogManager.getLogger("NameScrambler");
    public static final Config CONFIG = new Config();
    public static final AbilityConfig ABILITY_CONFIG = new AbilityConfig();
    public static final String HIDDEN_TEAM_NAME = "hidden_players";

    @Override
    public void onInitialize() {
        LOGGER.info("Name Scrambler + Proximity Chat + Abilities mod by AVCD initialized!");

        // Загружаем конфиги при запуске сервера
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            CONFIG.load();
            ABILITY_CONFIG.load();
            AbilitySystem.init(); // Инициализируем систему способностей
            LOGGER.info("Proximity chat radius: {} blocks", CONFIG.chatRadius);
            LOGGER.info("Arrow activation chance: {}%", ABILITY_CONFIG.arrowActivationChance * 100);
        });

        // Регистрируем команды
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ProximityChatCommand.register(dispatcher);
            HideNamesCommand.register(dispatcher);
            AbilityCommand.register(dispatcher);
        });

        // Создаем скрытую команду, когда сервер уже запущен
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);

        // Добавляем обработчик входа игрока
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            setupPlayer(player);
            AbilitySystem.onPlayerJoin(player); // Восстанавливаем способность
        });
        
        // Добавляем обработчик выхода игрока
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            AbilitySystem.onPlayerLeave(player); // Сохраняем данные
        });
    }

    private void onServerStarted(MinecraftServer server) {
        LOGGER.info("Setting up hidden team on server start...");
        setupHiddenTeam(server.getScoreboard());
    }

    private void setupHiddenTeam(ServerScoreboard scoreboard) {
        Team hiddenTeam = scoreboard.getTeam(HIDDEN_TEAM_NAME);
        
        if (hiddenTeam == null) {
            hiddenTeam = scoreboard.addTeam(HIDDEN_TEAM_NAME);
            hiddenTeam.setDisplayName(Text.literal("Hidden Players"));
            hiddenTeam.setColor(net.minecraft.util.Formatting.GRAY);
            LOGGER.info("Created hidden team: {}", HIDDEN_TEAM_NAME);
        }
    }

    private void setupPlayer(ServerPlayerEntity player) {
        try {
            ServerScoreboard scoreboard = player.getServer().getScoreboard();
            Team hiddenTeam = scoreboard.getTeam(HIDDEN_TEAM_NAME);
            
            if (hiddenTeam != null) {
                scoreboard.addPlayerToTeam(player.getGameProfile().getName(), hiddenTeam);
                LOGGER.info("Added player to hidden team: {}", player.getGameProfile().getName());
            } else {
                LOGGER.warn("Hidden team not found for player: {}", player.getGameProfile().getName());
            }

        } catch (Exception e) {
            LOGGER.error("Error setting up player: {}", e.getMessage());
        }
    }
}