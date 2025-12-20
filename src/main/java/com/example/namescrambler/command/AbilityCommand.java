package com.example.namescrambler.command;

import com.example.namescrambler.AbilitySystem;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.server.MinecraftServer;

import java.util.Arrays;
import java.util.List;

public class AbilityCommand {
    
    private static final List<String> VALID_ABILITIES = Arrays.asList(
        "superfly", "20centuryboy", "whitealbum", "none"
    );
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("ability")
            .requires(source -> source.hasPermissionLevel(2)) // Только операторы
            .then(CommandManager.literal("info")
                .executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    if (player != null) {
                        String ability = AbilitySystem.getPlayerAbility(player.getUuid());
                        
                        if (ability != null) {
                            switch (ability) {
                                case "superfly":
                                    player.sendMessage(Text.literal("§6Способность: §eSUPERFLY"), false);
                                    player.sendMessage(Text.literal("§7Вы можете находиться только в чанке точки возрождения"), false);
                                    player.sendMessage(Text.literal("§7Эта способность сохранена навсегда"), false);
                                    break;
                                case "20centuryboy":
                                    player.sendMessage(Text.literal("§6Способность: §d20th CENTURY BOY"), false);
                                    player.sendMessage(Text.literal("§7Shift+взгляд вниз = неподвижность и неуязвимость"), false);
                                    player.sendMessage(Text.literal("§7Эта способность сохранена навсегда"), false);
                                    break;
                                case "whitealbum":
                                    player.sendMessage(Text.literal("§6Способность: §fWHITE ALBUM"), false);
                                    player.sendMessage(Text.literal("§7Вода замерзает рядом с вами"), false);
                                    player.sendMessage(Text.literal("§7Удары наносят эффект заморозки"), false);
                                    player.sendMessage(Text.literal("§7Эта способность сохранена навсегда"), false);
                                    break;
                            }
                        } else {
                            player.sendMessage(Text.literal("§7У вас нет активных способностей"), false);
                        }
                    }
                    return Command.SINGLE_SUCCESS;
                })
            )
            
            .then(CommandManager.literal("setchance")
                .then(CommandManager.argument("chance", DoubleArgumentType.doubleArg(0.0, 1.0))
                    .executes(context -> {
                        double newChance = DoubleArgumentType.getDouble(context, "chance");
                        AbilitySystem.setArrowChance(newChance);
                        
                        context.getSource().sendMessage(Text.literal(
                            String.format("§aШанс активации способности от стрелы установлен на: §e%.2f%%", newChance * 100)
                        ));
                        return Command.SINGLE_SUCCESS;
                    })
                )
            )
            
            .then(CommandManager.literal("getchance")
                .executes(context -> {
                    double currentChance = AbilitySystem.getArrowChance();
                    context.getSource().sendMessage(Text.literal(
                        String.format("§eТекущий шанс активации от стрелы: §a%.2f%%", currentChance * 100)
                    ));
                    return Command.SINGLE_SUCCESS;
                })
            )
            
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
                                target.sendMessage(Text.literal("§cВаша способность была удалена администратором"), false);
                            } else if (VALID_ABILITIES.contains(ability)) {
                                if (!AbilitySystem.isAbilityTaken(ability) || 
                                    ability.equals(AbilitySystem.getPlayerAbility(target.getUuid()))) {
                                    AbilitySystem.setPlayerAbility(target.getUuid(), ability, target);
                                    context.getSource().sendMessage(Text.literal(
                                        "§aИгроку " + target.getGameProfile().getName() + 
                                        " выдана способность: §e" + ability + " §7(сохранено навсегда)"
                                    ));
                                    target.sendMessage(Text.literal("§aВам выдана способность: §e" + ability), false);
                                } else {
                                    context.getSource().sendMessage(Text.literal(
                                        "§cЭта способность уже занята другим игроком!"
                                    ));
                                }
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
            
            .then(CommandManager.literal("remove")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .executes(context -> {
                        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
                        AbilitySystem.removePlayerAbility(target.getUuid());
                        
                        context.getSource().sendMessage(Text.literal(
                            "§aСпособность удалена у игрока " + target.getGameProfile().getName()
                        ));
                        target.sendMessage(Text.literal("§cВаша способность была удалена администратором"), false);
                        
                        return Command.SINGLE_SUCCESS;
                    })
                )
            )
            
            .then(CommandManager.literal("check")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .executes(context -> {
                        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
                        String ability = AbilitySystem.getPlayerAbility(target.getUuid());
                        
                        if (ability != null) {
                            context.getSource().sendMessage(Text.literal(
                                "§eИгрок " + target.getGameProfile().getName() + 
                                " имеет способность: §a" + ability + " §7(сохранено навсегда)"
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
            
            .then(CommandManager.literal("list")
                .executes(context -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("§6=== Доступные способности ===\n");
                    sb.append("§esuperfly §7- Ограничение по чанку точки возрождения\n");
                    sb.append("§d20centuryboy §7- Shift+взгляд вниз = фиксация позиции\n");
                    sb.append("§fwhitealbum §7- Замораживание воды и эффект заморозки\n");
                    sb.append("\n§6=== Занятые способности ===\n");
                    
                    MinecraftServer server = context.getSource().getServer();
                    boolean hasAbilities = false;
                    
                    for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                        String ability = AbilitySystem.getPlayerAbility(player.getUuid());
                        if (ability != null) {
                            sb.append("§e").append(player.getGameProfile().getName())
                            .append("§7: §a").append(ability).append("\n");
                            hasAbilities = true;
                        }
                    }
                    
                    if (!hasAbilities) {
                        sb.append("§cНет активных способностей\n");
                    }
                    
                    context.getSource().sendMessage(Text.literal(sb.toString()));
                    return Command.SINGLE_SUCCESS;
                })
            )
            
            .then(CommandManager.literal("reload")
                .executes(context -> {
                    com.example.namescrambler.NameScramblerMod.ABILITY_CONFIG.load();
                    double currentChance = AbilitySystem.getArrowChance();
                    context.getSource().sendMessage(Text.literal("§aКонфигурация способностей перезагружена"));
                    context.getSource().sendMessage(Text.literal(
                        String.format("§eТекущий шанс активации: §a%.2f%%", currentChance * 100)
                    ));
                    return Command.SINGLE_SUCCESS;
                })
            )
            
            .then(CommandManager.literal("save")
                .executes(context -> {
                    AbilitySystem.saveData();
                    context.getSource().sendMessage(Text.literal("§aДанные способностей сохранены в файл"));
                    return Command.SINGLE_SUCCESS;
                })
            )
            
            .then(CommandManager.literal("help")
                .executes(context -> {
                    context.getSource().sendMessage(Text.literal("§6=== Система способностей ==="));
                    context.getSource().sendMessage(Text.literal("§7• Способности сохраняются навсегда"));
                    context.getSource().sendMessage(Text.literal("§7• Каждая способность уникальна для одного игрока"));
                    context.getSource().sendMessage(Text.literal("§7• Стрела с шансом дает случайную свободную способность"));
                    context.getSource().sendMessage(Text.literal(""));
                    context.getSource().sendMessage(Text.literal("§6=== Команды оператора ==="));
                    context.getSource().sendMessage(Text.literal("§a/ability info §7- Информация о вашей способности"));
                    context.getSource().sendMessage(Text.literal("§a/ability list §7- Все сохранённые способности"));
                    context.getSource().sendMessage(Text.literal("§a/ability give <игрок> <способность> §7- Выдать способность"));
                    context.getSource().sendMessage(Text.literal("§a/ability remove <игрок> §7- Удалить способность"));
                    context.getSource().sendMessage(Text.literal("§a/ability check <игрок> §7- Проверить способность"));
                    context.getSource().sendMessage(Text.literal("§a/ability setchance <0.0-1.0> §7- Установить шанс активации"));
                    context.getSource().sendMessage(Text.literal("§a/ability getchance §7- Текущий шанс активации"));
                    context.getSource().sendMessage(Text.literal("§a/ability save §7- Принудительно сохранить данные"));
                    context.getSource().sendMessage(Text.literal("§a/ability reload §7- Перезагрузить конфигурацию"));
                    return Command.SINGLE_SUCCESS;
                })
            )
        );
    }
}