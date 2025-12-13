package com.example.namescrambler.mixin;

import com.example.namescrambler.AbilitySystem;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArrowEntity.class)
public class ArrowEntityMixin {
    
    @Inject(method = "onHit", at = @At("HEAD"))
    private void onArrowHit(Entity target, CallbackInfo ci) {
        if (target instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) target;
            AbilitySystem.onArrowHit(player);
        }
    }
}