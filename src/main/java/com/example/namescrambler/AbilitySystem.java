package com.example.namescrambler;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.*;

public class AbilitySystem {
    
    private static final Map<UUID, PlayerAbilityData> playerAbilities = new HashMap<>();
    private static final Random random = new Random();
    
    public static void init() {
        NameScramblerMod.LOGGER.info("Ability system initialized");
    }
    
    public static class PlayerAbilityData {
        public String currentAbility = null;
        public BlockPos respawnChunkPos = null;
        public ChunkPos respawnChunk = null;
        public boolean isUsingBoyAbility = false;
        public long abilityActivationTime = 0;
        
        public PlayerAbilityData(ServerPlayerEntity player) {
            this.respawnChunkPos = player.getSpawnPointPosition();
            if (this.respawnChunkPos != null) {
                this.respawnChunk = new ChunkPos(this.respawnChunkPos);
            }
        }
    }
    
    public static void onArrowHit(ServerPlayerEntity player) {
        double chance = NameScramblerMod.ABILITY_CONFIG.arrowActivationChance;
        
        if (random.nextDouble() < chance) {
            activateRandomAbility(player);
        }
    }
    
    private static void activateRandomAbility(ServerPlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(
            StatusEffects.RESISTANCE, 
            NameScramblerMod.ABILITY_CONFIG.immunityDuration, 
            255,
            false, false, true
        ));
        
        player.addStatusEffect(new StatusEffectInstance(
            StatusEffects.BLINDNESS, 
            NameScramblerMod.ABILITY_CONFIG.blindnessDuration, 
            0, false, false, true
        ));
        
        String[] abilities = {"superfly", "20centuryboy"};
        String ability = abilities[random.nextInt(abilities.length)];
        
        PlayerAbilityData data = playerAbilities.computeIfAbsent(player.getUuid(), 
            k -> new PlayerAbilityData(player));
        data.currentAbility = ability;
        data.abilityActivationTime = player.getWorld().getTime();
        
        player.sendMessage(net.minecraft.text.Text.literal(
            "§6Вы получили способность: §e" + ability + "§6! Используйте §a/ability info§6 для информации."
        ), false);
        
        NameScramblerMod.LOGGER.info("Player {} got ability: {}", player.getGameProfile().getName(), ability);
    }
    
    public static PlayerAbilityData getPlayerData(UUID playerId) {
        return playerAbilities.get(playerId);
    }
    
    public static void setPlayerAbility(UUID playerId, String ability, ServerPlayerEntity player) {
        PlayerAbilityData data = playerAbilities.computeIfAbsent(playerId, 
            k -> new PlayerAbilityData(player));
        data.currentAbility = ability;
        data.abilityActivationTime = player.getWorld().getTime();
    }
    
    public static void removePlayerAbility(UUID playerId) {
        PlayerAbilityData data = playerAbilities.get(playerId);
        if (data != null) {
            data.currentAbility = null;
            data.isUsingBoyAbility = false;
        }
    }
    
    public static String getPlayerAbility(UUID playerId) {
        PlayerAbilityData data = playerAbilities.get(playerId);
        return data != null ? data.currentAbility : null;
    }
    
    public static void updateSuperflyAbility(ServerPlayerEntity player) {
        PlayerAbilityData data = getPlayerData(player.getUuid());
        if (data == null || !"superfly".equals(data.currentAbility)) return;
        
        if (data.respawnChunk == null) {
            data.respawnChunkPos = player.getSpawnPointPosition();
            if (data.respawnChunkPos != null) {
                data.respawnChunk = new ChunkPos(data.respawnChunkPos);
            }
            return;
        }
        
        ChunkPos currentChunk = new ChunkPos(player.getBlockPos());
        boolean isInOverworld = player.getWorld().getRegistryKey() == World.OVERWORLD;
        
        if (isInOverworld && !currentChunk.equals(data.respawnChunk)) {
            player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.POISON, 
                NameScramblerMod.ABILITY_CONFIG.superflyPoisonDuration, 
                0, false, true, true
            ));
            
            player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.NAUSEA, 
                NameScramblerMod.ABILITY_CONFIG.superflyNauseaDuration, 
                0, false, true, true
            ));
            
            player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.BLINDNESS, 
                NameScramblerMod.ABILITY_CONFIG.superflyBlindnessDuration, 
                0, false, true, true
            ));
            
            player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.MINING_FATIGUE, 
                NameScramblerMod.ABILITY_CONFIG.superflyMiningFatigueDuration, 
                2, false, true, true
            ));
            
            player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.SLOWNESS, 
                NameScramblerMod.ABILITY_CONFIG.superflySlownessDuration, 
                2, false, true, true
            ));
            
            spawnGrayParticles(player);
        }
    }
    
    public static void update20CenturyBoyAbility(ServerPlayerEntity player) {
        PlayerAbilityData data = getPlayerData(player.getUuid());
        if (data == null || !"20centuryboy".equals(data.currentAbility)) return;
        
        boolean isSneaking = player.isSneaking();
        Vec3d lookVec = player.getRotationVec(1.0F);
        
        boolean isLookingDown = lookVec.y < -0.9;
        
        if (isSneaking && isLookingDown) {
            data.isUsingBoyAbility = true;
            
            player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.RESISTANCE, 
                NameScramblerMod.ABILITY_CONFIG.boyImmunityDuration, 
                255, false, false, true
            ));
            
            player.setVelocity(Vec3d.ZERO);
            player.velocityDirty = true;
            player.velocityModified = true;
            
            spawnPurpleParticles(player);
            
        } else {
            data.isUsingBoyAbility = false;
        }
    }
    
    private static void spawnGrayParticles(ServerPlayerEntity player) {
        if (player.getWorld() instanceof ServerWorld) {
            ServerWorld serverWorld = (ServerWorld) player.getWorld();
            BlockPos pos = player.getBlockPos();
            int range = NameScramblerMod.ABILITY_CONFIG.superflyParticleRange;
            
            for (int i = 0; i < 5; i++) {
                double x = pos.getX() + (random.nextDouble() - 0.5) * range;
                double y = pos.getY() + random.nextDouble() * 2;
                double z = pos.getZ() + (random.nextDouble() - 0.5) * range;
                
                serverWorld.spawnParticles(
                    net.minecraft.particle.DustParticleEffect.DEFAULT,
                    x, y, z, 1, 0, 0, 0, 0
                );
            }
        }
    }
    
    private static void spawnPurpleParticles(ServerPlayerEntity player) {
        if (player.getWorld() instanceof ServerWorld) {
            ServerWorld serverWorld = (ServerWorld) player.getWorld();
            BlockPos pos = player.getBlockPos();
            int range = NameScramblerMod.ABILITY_CONFIG.boyParticleRange;
            
            Vector3f color = new Vector3f(0.5f, 0f, 0.5f);
            
            for (int i = 0; i < 3; i++) {
                double x = pos.getX() + (random.nextDouble() - 0.5) * range;
                double y = pos.getY() + random.nextDouble() * 2;
                double z = pos.getZ() + (random.nextDouble() - 0.5) * range;
                
                serverWorld.spawnParticles(
                    new net.minecraft.particle.DustParticleEffect(color, 1.0f),
                    x, y, z, 1, 0, 0, 0, 0
                );
            }
        }
    }
    
    public static void tickAllPlayers(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            String ability = getPlayerAbility(player.getUuid());
            if (ability != null) {
                switch (ability) {
                    case "superfly":
                        updateSuperflyAbility(player);
                        break;
                    case "20centuryboy":
                        update20CenturyBoyAbility(player);
                        break;
                }
            }
        }
    }
}