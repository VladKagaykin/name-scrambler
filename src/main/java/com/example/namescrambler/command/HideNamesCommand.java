package com.example.namescrambler.command;

import com.example.namescrambler.NameScramblerMod;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import net.minecraft.server.MinecraftServer;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.util.Formatting;

import java.util.Arrays;
import java.util.List;

public class HideNamesCommand {

    private static final String HIDDEN_TEAM_NAME = "hidden_players";

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("hidenames")
            .requires(source -> source.hasPermissionLevel(2))
            .executes(context -> {
                ServerCommandSource source = context.getSource();
                MinecraftServer server = source.getServer();
                ServerScoreboard scoreboard = server.getScoreboard();
                
                // Получаем или создаем команду
                Team hiddenTeam = scoreboard.getTeam(HIDDEN_TEAM_NAME);
                if (hiddenTeam == null) {
                    hiddenTeam = scoreboard.addTeam(HIDDEN_TEAM_NAME);
                    hiddenTeam.setDisplayName(Text.literal("Hidden Players"));
                    hiddenTeam.setColor(Formatting.GRAY);
                    source.sendMessage(Text.literal("§aКоманда 'hidden_players' создана!"));
                }

                // Настраиваем команду для скрытия имен
                hiddenTeam.setNameTagVisibilityRule(AbstractTeam.VisibilityRule.NEVER);
                hiddenTeam.setCollisionRule(AbstractTeam.CollisionRule.ALWAYS);
                
                // Получаем список имен из вайтлиста (массив преобразуем в список)
                String[] whitelistedArray = server.getPlayerManager().getWhitelistedNames();
                List<String> whitelistedPlayers = Arrays.asList(whitelistedArray);
                int added = 0;

                // Добавляем каждого игрока из вайтлиста в команду
                for (String playerName : whitelistedPlayers) {
                    if (scoreboard.getPlayerTeam(playerName) != hiddenTeam) {
                        scoreboard.addPlayerToTeam(playerName, hiddenTeam);
                        added++;
                    }
                }

                // Также добавляем всех онлайн игроков на всякий случай
                for (String playerName : server.getPlayerManager().getPlayerNames()) {
                    if (scoreboard.getPlayerTeam(playerName) != hiddenTeam) {
                        scoreboard.addPlayerToTeam(playerName, hiddenTeam);
                        added++;
                    }
                }

                source.sendMessage(Text.literal("§aДобавлено игроков в скрытую команду: " + added));
                NameScramblerMod.LOGGER.info("Added {} players to hidden team via command", added);
                
                return Command.SINGLE_SUCCESS;
            })
            .then(CommandManager.literal("clear")
                .executes(context -> {
                    ServerCommandSource source = context.getSource();
                    MinecraftServer server = source.getServer();
                    ServerScoreboard scoreboard = server.getScoreboard();
                    
                    Team hiddenTeam = scoreboard.getTeam(HIDDEN_TEAM_NAME);
                    if (hiddenTeam != null) {
                        // Удаляем всех игроков из команды
                        for (String playerName : hiddenTeam.getPlayerList()) {
                            scoreboard.removePlayerFromTeam(playerName, hiddenTeam);
                        }
                        source.sendMessage(Text.literal("§cВсе игроки удалены из скрытой команды"));
                    } else {
                        source.sendMessage(Text.literal("§cСкрытая команда не найдена"));
                    }
                    return Command.SINGLE_SUCCESS;
                })
            )
            .then(CommandManager.literal("status")
                .executes(context -> {
                    ServerCommandSource source = context.getSource();
                    MinecraftServer server = source.getServer();
                    ServerScoreboard scoreboard = server.getScoreboard();
                    
                    Team hiddenTeam = scoreboard.getTeam(HIDDEN_TEAM_NAME);
                    if (hiddenTeam != null) {
                        int memberCount = hiddenTeam.getPlayerList().size();
                        source.sendMessage(Text.literal("§eВ скрытой команде: " + memberCount + " игроков"));
                    } else {
                        source.sendMessage(Text.literal("§cСкрытая команда не найдена"));
                    }
                    return Command.SINGLE_SUCCESS;
                })
            )
        );
    }
}