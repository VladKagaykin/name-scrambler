package com.example.namescrambler.mixin;

import com.example.namescrambler.NameScramblerMod;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.stream.Collectors;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {

    @Shadow public ServerPlayerEntity player;

    @Inject(
        method = "handleDecoratedMessage",
        at = @At("HEAD"),
        cancellable = true
    )
    private void handleProximityChat(SignedMessage signedMessage, CallbackInfo ci) {
        try {
            // Получаем содержимое сообщения из SignedMessage
            String content = signedMessage.getContent().getString();

            // Пропускаем команды
            if (content.startsWith("/")) {
                return;
            }

            ServerPlayerEntity sender = this.player;
            MinecraftServer server = sender.getServer();
            double radius = NameScramblerMod.CONFIG.chatRadius;

            // Находим игроков в радиусе
            List<ServerPlayerEntity> playersInRange = server.getPlayerManager().getPlayerList().stream()
                .filter(player -> {
                    if (player == sender) return true; // Отправитель всегда слышит себя
                    if (player.getWorld() != sender.getWorld()) return false; // Разные миры
                    double distance = player.getPos().distanceTo(sender.getPos());
                    return distance <= radius;
                })
                .collect(Collectors.toList());

            // Если в радиусе только отправитель
            if (playersInRange.size() <= 1) {
                sender.sendMessage(Text.literal("§cВас никто не услышал"));
                ci.cancel();
                return;
            }

            // Создаем сообщение со скрытым именем отправителя
            // §k - случайные символы, §r - сброс форматирования
            Text chatMessage = Text.literal("§k" + sender.getGameProfile().getName() + "§r: " + content);

            // Отправляем сообщение только игрокам в радиусе
            for (ServerPlayerEntity receiver : playersInRange) {
                receiver.sendMessage(chatMessage);
            }

            // Логируем в консоль
            NameScramblerMod.LOGGER.info("[Proximity] {}: {}", sender.getGameProfile().getName(), content);

            // Отменяем стандартную обработку сообщения
            ci.cancel();

        } catch (Exception e) {
            NameScramblerMod.LOGGER.error("Error in proximity chat", e);
        }
    }
}
