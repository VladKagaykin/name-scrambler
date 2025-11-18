package com.example.namescrambler.command;

import com.example.namescrambler.NameScramblerMod;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class ProximityChatCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("proximitychat")
            .requires(source -> source.hasPermissionLevel(4)) // Только для операторов (уровень 4)
            .then(CommandManager.literal("setradius")
                .then(CommandManager.argument("radius", DoubleArgumentType.doubleArg(1.0, 1000.0))
                    .executes(context -> {
                        double newRadius = DoubleArgumentType.getDouble(context, "radius");
                        ServerCommandSource source = context.getSource();

                        // Обновляем радиус в конфиге
                        NameScramblerMod.CONFIG.chatRadius = newRadius;
                        NameScramblerMod.CONFIG.save();

                        // Уведомляем оператора
                        source.sendMessage(Text.literal(
                            String.format("§aРадиус слышимости чата изменен на %.1f блоков", newRadius)
                        ));

                        NameScramblerMod.LOGGER.info("Operator {} set proximity chat radius to {}",
                            source.getName(), newRadius);

                        return Command.SINGLE_SUCCESS;
                    })
                )
            )
            .then(CommandManager.literal("getradius")
                .executes(context -> {
                    ServerCommandSource source = context.getSource();
                    double currentRadius = NameScramblerMod.CONFIG.chatRadius;
                    source.sendMessage(Text.literal(
                        String.format("§eТекущий радиус слышимости чата: %.1f блоков", currentRadius)
                    ));
                    return Command.SINGLE_SUCCESS;
                })
            )
            .then(CommandManager.literal("help")
                .executes(context -> {
                    ServerCommandSource source = context.getSource();
                    source.sendMessage(Text.literal("§6=== Proximity Chat Commands ==="));
                    source.sendMessage(Text.literal("§a/proximitychat setradius <число> §7- Установить радиус слышимости"));
                    source.sendMessage(Text.literal("§a/proximitychat getradius §7- Текущий радиус"));
                    source.sendMessage(Text.literal("§a/proximitychat help §7- Эта справка"));
                    return Command.SINGLE_SUCCESS;
                })
            )
        );
    }
}
