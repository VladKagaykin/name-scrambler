package com.example.namescrambler.mixin;

import com.example.namescrambler.AbilitySystem;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin_Abilities {
    
    @Inject(method = "tick", at = @At("HEAD"))
    private void onPlayerTick(CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        String ability = AbilitySystem.getPlayerAbility(player.getUuid());
        
        if (ability != null) {
            switch (ability) {
                case "superfly":
                    AbilitySystem.updateSuperflyAbility(player);
                    break;
                case "20centuryboy":
                    AbilitySystem.update20CenturyBoyAbility(player);
                    break;
            }
        }
    }
}