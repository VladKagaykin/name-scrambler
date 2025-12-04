package com.example.namescrambler.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CommandManager.class)
public class CommandManagerMixin {

    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    private void blockTellAndMsgCommands(CommandContext<ServerCommandSource> context, CallbackInfoReturnable<Integer> cir) {
        try {
            // Получаем команду, которую пытаются выполнить
            String input = context.getInput();
            
            if (input.startsWith("/tell ") || input.startsWith("/msg ") || 
                input.startsWith("/w ") || input.startsWith("/whisper ")) {
                
                ServerCommandSource source = context.getSource();
                source.sendMessage(Text.literal("§cКоманды /tell и /msg запрещены на этом сервере!"));
                source.sendMessage(Text.literal("§7Используйте обычный чат или proximity чат."));
                
                // Отменяем выполнение команды
                cir.setReturnValue(0);
                cir.cancel();
            }
        } catch (Exception e) {
            // Игнорируем ошибки, чтобы не сломать другие команды
        }
    }
}