package com.example.namescrambler.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
    
    @Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
    private void replaceEntityDisplayName(CallbackInfoReturnable<Text> cir) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) entity;
            Text scrambledName = Text.literal("§k" + player.getGameProfile().getName());
            cir.setReturnValue(scrambledName);
        }
    }
    
    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private void replaceEntityName(CallbackInfoReturnable<Text> cir) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) entity;
            Text scrambledName = Text.literal("§k" + player.getGameProfile().getName());
            cir.setReturnValue(scrambledName);
        }
    }
}