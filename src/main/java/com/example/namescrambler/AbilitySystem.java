package com.example.namescrambler;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FrostedIceBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.io.*;
import java.util.*;

public class AbilitySystem {
    
    private static final Map<UUID, PlayerAbilityData> playerAbilities = new HashMap<>();
    private static final Map<String, UUID> abilityAssignments = new HashMap<>();
    private static final List<String> ALL_ABILITIES = Arrays.asList(
        "superfly", "20centuryboy", "whitealbum"
    );
    private static final Random random = new Random();
    
    private static final File SAVE_FILE = new File("config/name-scrambler-abilities-save.json");
    
    public static void init() {
        loadSavedData();
        NameScramblerMod.LOGGER.info("Ability system initialized, loaded {} saved abilities", playerAbilities.size());
    }
    
    public static class PlayerAbilityData implements Serializable {
        private static final long serialVersionUID = 1L;
        
        public String currentAbility = null;
        public transient BlockPos respawnChunkPos = null;
        public transient ChunkPos respawnChunk = null;
        public boolean isUsingBoyAbility = false;
        public long abilityActivationTime = 0;
        public transient Vec3d boyActivationPos = null;
        
        public int spawnX = 0;
        public int spawnY = 0;
        public int spawnZ = 0;
        
        public PlayerAbilityData() {}
        
        public PlayerAbilityData(ServerPlayerEntity player) {
            this.respawnChunkPos = player.getSpawnPointPosition();
            if (this.respawnChunkPos != null) {
                this.respawnChunk = new ChunkPos(this.respawnChunkPos);
                this.spawnX = respawnChunkPos.getX();
                this.spawnY = respawnChunkPos.getY();
                this.spawnZ = respawnChunkPos.getZ();
            }
        }
        
        public void restoreTransientFields(ServerPlayerEntity player) {
            if (spawnX != 0 || spawnY != 0 || spawnZ != 0) {
                this.respawnChunkPos = new BlockPos(spawnX, spawnY, spawnZ);
                this.respawnChunk = new ChunkPos(this.respawnChunkPos);
            }
        }
    }
    
    private static void loadSavedData() {
        try {
            if (SAVE_FILE.exists()) {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(SAVE_FILE));
                @SuppressWarnings("unchecked")
                Map<UUID, PlayerAbilityData> loaded = (Map<UUID, PlayerAbilityData>) ois.readObject();
                ois.close();
                
                playerAbilities.clear();
                abilityAssignments.clear();
                playerAbilities.putAll(loaded);
                
                for (Map.Entry<UUID, PlayerAbilityData> entry : playerAbilities.entrySet()) {
                    if (entry.getValue().currentAbility != null) {
                        abilityAssignments.put(entry.getValue().currentAbility, entry.getKey());
                    }
                }
            }
        } catch (Exception e) {
            NameScramblerMod.LOGGER.error("Failed to load saved abilities", e);
        }
    }
    
    public static void saveData() {
        try {
            if (!SAVE_FILE.getParentFile().exists()) {
                SAVE_FILE.getParentFile().mkdirs();
            }
            
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SAVE_FILE));
            oos.writeObject(playerAbilities);
            oos.close();
        } catch (IOException e) {
            NameScramblerMod.LOGGER.error("Failed to save abilities", e);
        }
    }
    
    public static void onPlayerJoin(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        PlayerAbilityData data = playerAbilities.get(playerId);
        
        if (data != null && data.currentAbility != null) {
            data.restoreTransientFields(player);
            // Не отправляем сообщение игроку при входе
            NameScramblerMod.LOGGER.info("Ability restored for {}: {}", 
                player.getGameProfile().getName(), data.currentAbility);
        }
    }
    
    public static void onPlayerLeave(ServerPlayerEntity player) {
        saveData();
    }
    
    public static void onArrowHit(ServerPlayerEntity player) {
        double chance = NameScramblerMod.ABILITY_CONFIG.arrowActivationChance;
        if (random.nextDouble() < chance) {
            String ability = getAvailableRandomAbility();
            if (ability != null) {
                activateAbility(player, ability);
            } else {
                // Все способности заняты - ничего не делаем
                NameScramblerMod.LOGGER.info("All abilities are taken, arrow hit gave no ability to {}", 
                    player.getGameProfile().getName());
            }
        }
    }
    
    public static boolean isAbilityTaken(String ability) {
        return abilityAssignments.containsKey(ability) && 
               playerAbilities.containsKey(abilityAssignments.get(ability));
    }
    
    public static String getAvailableRandomAbility() {
        List<String> available = new ArrayList<>();
        for (String ability : ALL_ABILITIES) {
            if (!isAbilityTaken(ability)) {
                available.add(ability);
            }
        }
        return available.isEmpty() ? null : available.get(random.nextInt(available.size()));
    }
    
    public static void activateAbility(ServerPlayerEntity player, String ability) {
        // Если способность "none", удаляем способность у игрока
        if ("none".equals(ability)) {
            removePlayerAbility(player.getUuid());
            player.sendMessage(Text.literal("§cВаша способность была удалена"), false);
            return;
        }
        
        // Проверяем, занята ли способность другим игроком
        if (isAbilityTaken(ability)) {
            UUID currentOwner = abilityAssignments.get(ability);
            // Если способность уже принадлежит этому игроку, ничего не делаем
            if (currentOwner.equals(player.getUuid())) {
                player.sendMessage(Text.literal("§eУ вас уже есть эта способность: " + ability), false);
                return;
            }
            
            // Ищем другую свободную способность
            String newAbility = getAvailableRandomAbility();
            if (newAbility == null) {
                // Все способности заняты - ничего не делаем
                NameScramblerMod.LOGGER.info("All abilities are taken, cannot give ability to {}", 
                    player.getGameProfile().getName());
                return;
            }
            
            // Активируем другую свободную способность
            activateAbilityInternal(player, newAbility);
            player.sendMessage(Text.literal("§eИзначальная способность была занята, вы получили другую: §a" + newAbility), false);
            return;
        }
        
        // Если способность свободна - активируем её
        activateAbilityInternal(player, ability);
    }
    
    // Внутренний метод активации способности без дополнительных проверок
    private static void activateAbilityInternal(ServerPlayerEntity player, String ability) {
        String oldAbility = getPlayerAbility(player.getUuid());
        if (oldAbility != null) {
            abilityAssignments.remove(oldAbility);
        }
        
        // Даём эффекты при активации
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 
            NameScramblerMod.ABILITY_CONFIG.immunityDuration, 255, false, false, true));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 
            NameScramblerMod.ABILITY_CONFIG.blindnessDuration, 0, false, false, true));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING,
            NameScramblerMod.ABILITY_CONFIG.immunityDuration, 0, false, false, true));
        
        // Сохраняем данные
        PlayerAbilityData data = playerAbilities.computeIfAbsent(player.getUuid(), 
            k -> new PlayerAbilityData(player));
        data.currentAbility = ability;
        data.abilityActivationTime = player.getWorld().getTime();
        abilityAssignments.put(ability, player.getUuid());
        saveData();
        
        // Отправляем сообщение
        player.sendMessage(Text.literal("§eВы получили способность: " + ability), false);
    }
    
    public static PlayerAbilityData getPlayerData(UUID playerId) {
        return playerAbilities.get(playerId);
    }
    
    public static void setPlayerAbility(UUID playerId, String ability, ServerPlayerEntity player) {
        // Если ability == "none", удаляем способность
        if ("none".equals(ability)) {
            removePlayerAbility(playerId);
            return;
        }
        
        // Проверяем, занята ли способность
        if (isAbilityTaken(ability)) {
            UUID currentOwner = abilityAssignments.get(ability);
            // Если пытаемся дать ту же способность тому же игроку
            if (currentOwner.equals(playerId)) {
                return; // У игрока уже есть эта способность
            }
            
            // Ищем другую свободную способность
            String newAbility = getAvailableRandomAbility();
            if (newAbility != null) {
                activateAbilityInternal(player, newAbility);
                NameScramblerMod.LOGGER.info("Ability {} was taken, gave {} instead to {}", 
                    ability, newAbility, player.getGameProfile().getName());
            } else {
                NameScramblerMod.LOGGER.info("All abilities are taken, cannot give ability to {}", 
                    player.getGameProfile().getName());
            }
            return;
        }
        
        // Если способность свободна
        activateAbilityInternal(player, ability);
    }
    
    public static UUID getAbilityOwner(String ability) {
        return abilityAssignments.get(ability);
    }
    
    public static void removePlayerAbility(UUID playerId) {
        PlayerAbilityData data = playerAbilities.get(playerId);
        if (data != null && data.currentAbility != null) {
            abilityAssignments.remove(data.currentAbility);
            data.currentAbility = null;
            data.isUsingBoyAbility = false;
            data.boyActivationPos = null;
            saveData();
        }
    }
    
    public static String getPlayerAbility(UUID playerId) {
        PlayerAbilityData data = playerAbilities.get(playerId);
        return data != null ? data.currentAbility : null;
    }
    
    public static void setArrowChance(double chance) {
        NameScramblerMod.ABILITY_CONFIG.arrowActivationChance = chance;
        NameScramblerMod.ABILITY_CONFIG.save();
    }
    
    public static double getArrowChance() {
        return NameScramblerMod.ABILITY_CONFIG.arrowActivationChance;
    }
    
    public static void updateSuperflyAbility(ServerPlayerEntity player) {
        PlayerAbilityData data = getPlayerData(player.getUuid());
        if (data == null || !"superfly".equals(data.currentAbility)) return;
        
        if (data.respawnChunk == null || player.getSpawnPointPosition() != data.respawnChunkPos) {
            data.respawnChunkPos = player.getSpawnPointPosition();
            if (data.respawnChunkPos != null) {
                data.respawnChunk = new ChunkPos(data.respawnChunkPos);
                data.spawnX = data.respawnChunkPos.getX();
                data.spawnY = data.respawnChunkPos.getY();
                data.spawnZ = data.respawnChunkPos.getZ();
            }
            return;
        }
        
        ChunkPos currentChunk = new ChunkPos(player.getBlockPos());
        boolean isInOverworld = player.getWorld().getRegistryKey() == World.OVERWORLD;
        
        if (isInOverworld && !currentChunk.equals(data.respawnChunk)) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 
                NameScramblerMod.ABILITY_CONFIG.superflyPoisonDuration, 0, false, true, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 
                NameScramblerMod.ABILITY_CONFIG.superflyNauseaDuration, 0, false, true, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 
                NameScramblerMod.ABILITY_CONFIG.superflyBlindnessDuration, 0, false, true, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 
                NameScramblerMod.ABILITY_CONFIG.superflyMiningFatigueDuration, 2, false, true, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 
                NameScramblerMod.ABILITY_CONFIG.superflySlownessDuration, 2, false, true, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 100, 0, false, true, true));
            spawnGrayParticles(player);
        } else {
            player.removeStatusEffect(StatusEffects.GLOWING);
        }
    }
    
    public static void update20CenturyBoyAbility(ServerPlayerEntity player) {
        PlayerAbilityData data = getPlayerData(player.getUuid());
        if (data == null || !"20centuryboy".equals(data.currentAbility)) return;
        
        boolean isSneaking = player.isSneaking();
        Vec3d lookVec = player.getRotationVec(1.0F);
        boolean isLookingDown = lookVec.y < -0.9;
        
        if (isSneaking && isLookingDown) {
            if (data.boyActivationPos == null) {
                data.boyActivationPos = player.getPos();
            }
            
            data.isUsingBoyAbility = true;
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 
                NameScramblerMod.ABILITY_CONFIG.boyImmunityDuration, 255, false, false, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION,
                NameScramblerMod.ABILITY_CONFIG.boyImmunityDuration, 1, false, false, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 100, 0, false, true, true));
            
            if (data.boyActivationPos != null) {
                Vec3d currentPos = player.getPos();
                double tolerance = NameScramblerMod.ABILITY_CONFIG.boyPositionTolerance;
                
                if (Math.abs(currentPos.x - data.boyActivationPos.x) > tolerance ||
                    Math.abs(currentPos.z - data.boyActivationPos.z) > tolerance) {
                    
                    player.requestTeleport(data.boyActivationPos.x, currentPos.y, data.boyActivationPos.z);
                    player.setVelocity(Vec3d.ZERO);
                }
            }
            
            player.setVelocity(Vec3d.ZERO);
            spawnPurpleParticles(player);
        } else {
            data.isUsingBoyAbility = false;
            data.boyActivationPos = null;
            player.removeStatusEffect(StatusEffects.GLOWING);
        }
    }
    
    public static void updateWhiteAlbumAbility(ServerPlayerEntity player) {
        PlayerAbilityData data = getPlayerData(player.getUuid());
        if (data == null || !"whitealbum".equals(data.currentAbility)) return;
        
        World world = player.getWorld();
        BlockPos playerPos = player.getBlockPos();
        int radius = NameScramblerMod.ABILITY_CONFIG.whitealbumFreezeRadius;
        
        // Уменьшаем порог скорости - работает даже при медленной ходьбе
        Vec3d velocity = player.getVelocity();
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        boolean isMoving = horizontalSpeed > 0.02; // Очень низкий порог
        
        // Для ServerPlayerEntity нет прямого доступа к input, используем только скорость
        // Если игрок не движется, выходим
        if (!isMoving) return;
        
        // Проверяем, что игрок НЕ в воде и не плавает
        boolean playerInWater = player.isTouchingWater() || player.isSubmergedInWater();
        if (playerInWater) return;
        
        boolean waterFrozen = false;
        
        // Работаем как Frost Walker - замораживаем воду на уровне ног
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                // Создаём круг радиусом radius
                double distance = Math.sqrt(x * x + z * z);
                if (distance > radius) continue;
                
                // Основная позиция для заморозки (на уровне земли под игроком)
                BlockPos groundPos = playerPos.add(x, -1, z);
                
                // Проверяем блок на уровне земли
                BlockState groundState = world.getBlockState(groundPos);
                
                // Замораживаем ТОЛЬКО воду (не текущую реку, только стоячую воду)
                if (groundState.getBlock() == Blocks.WATER) {
                    // Проверяем, что блок над водой - воздух или безопасная для ходьбы поверхность
                    BlockPos abovePos = groundPos.up();
                    BlockState aboveState = world.getBlockState(abovePos);
                    
                    if (aboveState.isAir() || aboveState.getMaterial().isReplaceable()) {
                        waterFrozen = true;
                        // Создаём морозный лёд (Frosted Ice), который тает как в Frost Walker
                        world.setBlockState(groundPos, Blocks.FROSTED_ICE.getDefaultState()
                            .with(FrostedIceBlock.AGE, random.nextInt(4)));
                    }
                }
            }
        }
        
        // Вторая проверка - на уровне ног (y = 0) для воды на том же уровне
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double distance = Math.sqrt(x * x + z * z);
                if (distance > radius) continue;
                
                BlockPos sameLevelPos = playerPos.add(x, 0, z);
                BlockState sameLevelState = world.getBlockState(sameLevelPos);
                
                // Если на уровне ног вода, замораживаем её и поднимаем игрока на лёд
                if (sameLevelState.getBlock() == Blocks.WATER) {
                    BlockPos belowPos = sameLevelPos.down();
                    BlockState belowState = world.getBlockState(belowPos);
                    
                    // Проверяем, что под водой есть твёрдая поверхность
                    if (belowState.isSolidBlock(world, belowPos)) {
                        waterFrozen = true;
                        world.setBlockState(sameLevelPos, Blocks.FROSTED_ICE.getDefaultState()
                            .with(FrostedIceBlock.AGE, random.nextInt(4)));
                    }
                }
            }
        }
        
        // Третья проверка - вода вокруг на том же уровне для создания "ледяного моста"
        if (isMoving && horizontalSpeed > 0.05) { // Только при движении
            Vec3d lookVec = player.getRotationVec(1.0F).multiply(2.0); // Смотрим на 2 блока вперед
            BlockPos lookPos = playerPos.add((int)lookVec.x, 0, (int)lookVec.z);
            
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos forwardPos = lookPos.add(x, 0, z);
                    BlockState forwardState = world.getBlockState(forwardPos);
                    
                    if (forwardState.getBlock() == Blocks.WATER) {
                        waterFrozen = true;
                        world.setBlockState(forwardPos, Blocks.FROSTED_ICE.getDefaultState()
                            .with(FrostedIceBlock.AGE, random.nextInt(4)));
                    }
                }
            }
        }
        
        // Визуальные эффекты
        if (waterFrozen) {
            // Частицы льда при заморозке
            if (world.getTime() % 3 == 0) {
                spawnWhiteParticles(player);
            }
            
            // Звук заморозки (тихий, как при Frost Walker)
            if (world instanceof ServerWorld) {
                ServerWorld serverWorld = (ServerWorld) world;
                serverWorld.playSound(null, playerPos, 
                    net.minecraft.sound.SoundEvents.BLOCK_GLASS_BREAK, 
                    net.minecraft.sound.SoundCategory.PLAYERS, 0.3f, 1.5f);
                
                // Дополнительный звук для эффекта
                if (random.nextInt(5) == 0) {
                    serverWorld.playSound(null, playerPos, 
                        net.minecraft.sound.SoundEvents.BLOCK_SNOW_STEP, 
                        net.minecraft.sound.SoundCategory.PLAYERS, 0.2f, 1.2f);
                }
            }
            
            // Эффект на игроке - маленький глитч-эффект при создании льда
            if (world.getTime() % 20 == 0) {
                player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.GLOWING, 10, 0, false, false, false));
            }
        }
    }
    
    public static void onEntityHit(ServerPlayerEntity attacker, LivingEntity target) {
        String ability = getPlayerAbility(attacker.getUuid());
        if ("whitealbum".equals(ability)) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 
                NameScramblerMod.ABILITY_CONFIG.whitealbumFreezeDuration, 2, false, true, true));
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE,
                NameScramblerMod.ABILITY_CONFIG.whitealbumFreezeDuration, 2, false, true, true));
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS,
                NameScramblerMod.ABILITY_CONFIG.whitealbumFreezeDuration, 0, false, true, true));
            
            // Добавляем визуальный эффект заморозки
            if (target.getWorld() instanceof ServerWorld) {
                ServerWorld world = (ServerWorld) target.getWorld();
                BlockPos pos = target.getBlockPos();
                for (int i = 0; i < 30; i++) {
                    double x = pos.getX() + (random.nextDouble() - 0.5) * 2;
                    double y = pos.getY() + random.nextDouble() * 2;
                    double z = pos.getZ() + (random.nextDouble() - 0.5) * 2;
                    world.spawnParticles(net.minecraft.particle.ParticleTypes.SNOWFLAKE, x, y, z, 1, 0, 0, 0, 0);
                }
                
                // Звук заморозки
                world.playSound(null, pos, 
                    net.minecraft.sound.SoundEvents.BLOCK_GLASS_BREAK, 
                    net.minecraft.sound.SoundCategory.PLAYERS, 0.7f, 1.2f);
            }
        }
    }
    
    private static void spawnGrayParticles(ServerPlayerEntity player) {
        if (player.getWorld() instanceof ServerWorld) {
            ServerWorld world = (ServerWorld) player.getWorld();
            BlockPos pos = player.getBlockPos();
            int range = NameScramblerMod.ABILITY_CONFIG.superflyParticleRange;
            Vector3f color = new Vector3f(
                NameScramblerMod.ABILITY_CONFIG.superflyRed,
                NameScramblerMod.ABILITY_CONFIG.superflyGreen,
                NameScramblerMod.ABILITY_CONFIG.superflyBlue
            );
            for (int i = 0; i < 5; i++) {
                double x = pos.getX() + (random.nextDouble() - 0.5) * range;
                double y = pos.getY() + random.nextDouble() * 2;
                double z = pos.getZ() + (random.nextDouble() - 0.5) * range;
                world.spawnParticles(new net.minecraft.particle.DustParticleEffect(color, 1.0f), x, y, z, 1, 0, 0, 0, 0);
            }
        }
    }
    
    private static void spawnPurpleParticles(ServerPlayerEntity player) {
        if (player.getWorld() instanceof ServerWorld) {
            ServerWorld world = (ServerWorld) player.getWorld();
            BlockPos pos = player.getBlockPos();
            int range = NameScramblerMod.ABILITY_CONFIG.boyParticleRange;
            Vector3f color = new Vector3f(
                NameScramblerMod.ABILITY_CONFIG.boyRed,
                NameScramblerMod.ABILITY_CONFIG.boyGreen,
                NameScramblerMod.ABILITY_CONFIG.boyBlue
            );
            for (int i = 0; i < 3; i++) {
                double x = pos.getX() + (random.nextDouble() - 0.5) * range;
                double y = pos.getY() + random.nextDouble() * 2;
                double z = pos.getZ() + (random.nextDouble() - 0.5) * range;
                world.spawnParticles(new net.minecraft.particle.DustParticleEffect(color, 1.0f), x, y, z, 1, 0, 0, 0, 0);
            }
        }
    }
    
    private static void spawnWhiteParticles(ServerPlayerEntity player) {
        if (player.getWorld() instanceof ServerWorld) {
            ServerWorld world = (ServerWorld) player.getWorld();
            BlockPos pos = player.getBlockPos();
            int range = NameScramblerMod.ABILITY_CONFIG.whitealbumParticleRange;
            Vector3f color = new Vector3f(1.0f, 1.0f, 1.0f);
            
            // Снежинки
            for (int i = 0; i < 15; i++) {
                double x = pos.getX() + (random.nextDouble() - 0.5) * range;
                double y = pos.getY() + random.nextDouble() * 1.5;
                double z = pos.getZ() + (random.nextDouble() - 0.5) * range;
                world.spawnParticles(net.minecraft.particle.ParticleTypes.SNOWFLAKE, 
                    x, y, z, 1, 0, 0, 0, 0.05);
            }
            
            // Пар от замерзания воды
            for (int i = 0; i < 10; i++) {
                double x = pos.getX() + (random.nextDouble() - 0.5) * range;
                double y = pos.getY() + 0.1;
                double z = pos.getZ() + (random.nextDouble() - 0.5) * range;
                world.spawnParticles(net.minecraft.particle.ParticleTypes.CLOUD, 
                    x, y, z, 1, 0, 0.1, 0, 0.01);
            }
        }
    }
    
    public static void tickAllPlayers(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            String ability = getPlayerAbility(player.getUuid());
            if (ability != null) {
                switch (ability) {
                    case "superfly": updateSuperflyAbility(player); break;
                    case "20centuryboy": update20CenturyBoyAbility(player); break;
                    case "whitealbum": updateWhiteAlbumAbility(player); break;
                }
            }
        }
    }
}