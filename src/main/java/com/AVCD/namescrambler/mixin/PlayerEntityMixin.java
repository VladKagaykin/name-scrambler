package com.example.namescrambler.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    
    @Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
    private void replaceDisplayName(CallbackInfoReturnable<Text> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        Text scrambledName = Text.literal("§k" + player.getGameProfile().getName());
        cir.setReturnValue(scrambledName);
    }
    
    @Inject(method = "getEntityName", at = @At("HEAD"), cancellable = true)
    private void replaceEntityName(CallbackInfoReturnable<String> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        String scrambledName = "§k" + player.getGameProfile().getName();
        cir.setReturnValue(scrambledName);
    }
}