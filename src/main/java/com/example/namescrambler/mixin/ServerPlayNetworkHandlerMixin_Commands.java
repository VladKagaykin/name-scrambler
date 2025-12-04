package com.example.namescrambler.mixin;

import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin_Commands {
    
    @Shadow public ServerPlayerEntity player;
    
    @Inject(
        method = "handleDecoratedMessage",
        at = @At("HEAD"),
        cancellable = true
    )
    private void blockPrivateMessages(SignedMessage signedMessage, CallbackInfo ci) {
        try {
            String content = signedMessage.getContent().getString();
            
            // Блокируем команды приватных сообщений
            if (content.startsWith("/tell ") || content.startsWith("/msg ") || 
                content.startsWith("/w ") || content.startsWith("/whisper ") ||
                content.startsWith("/me ") || content.startsWith("/m ")) {
                
                this.player.sendMessage(Text.literal("§cКоманды приватных сообщений запрещены!"));
                ci.cancel();
            }
        } catch (Exception e) {
            // Игнорируем ошибки
        }
    }
}