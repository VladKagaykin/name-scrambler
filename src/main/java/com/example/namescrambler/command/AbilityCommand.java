package com.example.namescrambler.command;

import com.example.namescrambler.AbilitySystem;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Arrays;
import java.util.List;

public class AbilityCommand {
    
    private static final List<String> VALID_ABILITIES = Arrays.asList(
        "superfly", "20centuryboy", "none"
    );
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("ability")
            .requires(source -> source.hasPermissionLevel(2))
            
            // /ability give <игрок> <способность>
            .then(CommandManager.literal("give")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .then(CommandManager.argument("ability", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (String ability : VALID_ABILITIES) {
                                builder.suggest(ability);
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
                            String ability = StringArgumentType.getString(context, "ability");
                            
                            if ("none".equals(ability)) {
                                AbilitySystem.removePlayerAbility(target.getUuid());
                                context.getSource().sendMessage(Text.literal(
                                    "§aСпособность удалена у игрока " + target.getGameProfile().getName()
                                ));
                                target.sendMessage(Text.literal("§cВаша способность была отобрана оператором"), false);
                            } else if (VALID_ABILITIES.contains(ability)) {
                                AbilitySystem.setPlayerAbility(target.getUuid(), ability, target);
                                context.getSource().sendMessage(Text.literal(
                                    "§aИгроку " + target.getGameProfile().getName() + 
                                    " выдана способность: §e" + ability
                                ));
                                target.sendMessage(Text.literal(
                                    "§6Оператор выдал вам способность: §e" + ability
                                ), false);
                            } else {
                                context.getSource().sendMessage(Text.literal(
                                    "§cНеизвестная способность. Доступные: " + String.join(", ", VALID_ABILITIES)
                                ));
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                    )
                )
            )
            
            // /ability remove <игрок>
            .then(CommandManager.literal("remove")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .executes(context -> {
                        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
                        AbilitySystem.removePlayerAbility(target.getUuid());
                        
                        context.getSource().sendMessage(Text.literal(
                            "§aСпособность удалена у игрока " + target.getGameProfile().getName()
                        ));
                        target.sendMessage(Text.literal("§cВаша способность была отобрана"), false);
                        
                        return Command.SINGLE_SUCCESS;
                    })
                )
            )
            
            // /ability check <игрок>
            .then(CommandManager.literal("check")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .executes(context -> {
                        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
                        String ability = AbilitySystem.getPlayerAbility(target.getUuid());
                        
                        if (ability != null) {
                            context.getSource().sendMessage(Text.literal(
                                "§eИгрок " + target.getGameProfile().getName() + 
                                " имеет способность: §a" + ability
                            ));
                        } else {
                            context.getSource().sendMessage(Text.literal(
                                "§eИгрок " + target.getGameProfile().getName() + 
                                " не имеет способностей"
                            ));
                        }
                        return Command.SINGLE_SUCCESS;
                    })
                )
            )
            
            // /ability list
            .then(CommandManager.literal("list")
                .executes(context -> {
                    context.getSource().sendMessage(Text.literal("§6=== Доступные способности ==="));
                    context.getSource().sendMessage(Text.literal("§esuperfly §7- Ограничение по чанку точки возрождения"));
                    context.getSource().sendMessage(Text.literal("§d20centuryboy §7- Неподвижность при Shift+взгляд вниз"));
                    context.getSource().sendMessage(Text.literal("§7Используйте: §a/ability give <игрок> <способность>"));
                    return Command.SINGLE_SUCCESS;
                })
            )
            
            // /ability info (для игроков)
            .then(CommandManager.literal("info")
                .executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    if (player != null) {
                        String ability = AbilitySystem.getPlayerAbility(player.getUuid());
                        
                        if (ability != null) {
                            switch (ability) {
                                case "superfly":
                                    player.sendMessage(Text.literal("§6=== Способность: §eSUPERFLY §6==="), false);
                                    player.sendMessage(Text.literal("§7Вы можете находиться только в чанке точки возрождения"), false);
                                    player.sendMessage(Text.literal("§7Выход за границы вызывает негативные эффекты"), false);
                                    player.sendMessage(Text.literal("§7Действует только в §aВерхнем мире"), false);
                                    break;
                                case "20centuryboy":
                                    player.sendMessage(Text.literal("§6=== Способность: §d20th CENTURY BOY §6==="), false);
                                    player.sendMessage(Text.literal("§7Зажмите §eShift §7и смотрите §eпрямо вниз"), false);
                                    player.sendMessage(Text.literal("§7Вы становитесь неподвижным и неуязвимым"), false);
                                    player.sendMessage(Text.literal("§7Отпустите Shift или посмотрите в сторону"), false);
                                    break;
                            }
                        } else {
                            player.sendMessage(Text.literal("§7У вас нет активных способностей"), false);
                            player.sendMessage(Text.literal("§7Шанс получить: §e" + 
                                (int)(com.example.namescrambler.NameScramblerMod.ABILITY_CONFIG.arrowActivationChance * 100) + 
                                "% §7при попадании стрелы"), false);
                        }
                    }
                    return Command.SINGLE_SUCCESS;
                })
            )
            
            // /ability reload (перезагрузка конфига)
            .then(CommandManager.literal("reload")
                .executes(context -> {
                    com.example.namescrambler.NameScramblerMod.ABILITY_CONFIG.load();
                    context.getSource().sendMessage(Text.literal("§aКонфигурация способностей перезагружена"));
                    return Command.SINGLE_SUCCESS;
                })
            )
        );
    }
}