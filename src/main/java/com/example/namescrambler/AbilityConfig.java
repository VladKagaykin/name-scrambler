package com.example.namescrambler;

import com.example.namescrambler.NameScramblerMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class AbilityConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File("config/name-scrambler-abilities.json");
    
    // Настройки стрелы
    public double arrowActivationChance = 0.01;
    public int immunityDuration = 200;
    public int blindnessDuration = 200;
    public int glowingDuration = 200;
    
    // Настройки superfly
    public int superflyPoisonDuration = 100;
    public int superflyNauseaDuration = 100;
    public int superflyBlindnessDuration = 100;
    public int superflyMiningFatigueDuration = 100;
    public int superflySlownessDuration = 100;
    public int superflyParticleRange = 50;
    public int superflyGlowingDuration = 100;
    
    // Настройки 20centuryboy
    public int boyImmunityDuration = 100;
    public int boyRegenerationDuration = 100;
    public int boyParticleRange = 30;
    public int boyGlowingDuration = 100;
    public double boyPositionTolerance = 0.1;
    
    // Настройки whitealbum
    public float whitealbumRed = 1.0f;
    public float whitealbumGreen = 1.0f;
    public float whitealbumBlue = 1.0f;
    public int whitealbumFreezeDuration = 200;
    public int whitealbumFreezeRadius = 3;
    public int whitealbumParticleRange = 30;
    public float whitealbumMinSpeed = 0.21f; // Минимальная скорость для активации
    public boolean whitealbumFreezeOnStand = false;
    public int whitealbumIceRadius = 3; // Радиус заморозки льда
    public boolean whitealbumFreezeFlowingWater = false; // Замораживать текущую воду
    public int whitealbumFreezeTickInterval = 2; // Интервал проверки

    // Настройки heyya
    public float heyyaRed = 0.0f;
    public float heyyaGreen = 1.0f;
    public float heyyaBlue = 0.0f;
    public int heyyaStructureRadius = 96;
    public int heyyaOreRadius = 8;
    public int heyyaMobRadius = 16;
    public int heyyaParticleRange = 50;
    // Добавьте эти настройки:
    public int whitealbumFreezeHeight = 1; // Высота замерзания
    public float heyyaViewAngle = 45.0f; // Угол обзора для скрытия стрелок (градусы)
    public boolean heyyaShowForward = true; // Показывать цели прямо вперед
    
    // Цвета частиц для старых способностей
    public float superflyRed = 0.3f;
    public float superflyGreen = 0.3f;
    public float superflyBlue = 0.3f;
    
    public float boyRed = 0.6f;
    public float boyGreen = 0.0f;
    public float boyBlue = 0.8f;
    
    public void load() {
        try {
            if (CONFIG_FILE.exists()) {
                AbilityConfig config = GSON.fromJson(new FileReader(CONFIG_FILE), AbilityConfig.class);
                this.arrowActivationChance = config.arrowActivationChance;
                this.immunityDuration = config.immunityDuration;
                this.blindnessDuration = config.blindnessDuration;
                this.glowingDuration = config.glowingDuration;
                this.superflyPoisonDuration = config.superflyPoisonDuration;
                this.superflyNauseaDuration = config.superflyNauseaDuration;
                this.superflyBlindnessDuration = config.superflyBlindnessDuration;
                this.superflyMiningFatigueDuration = config.superflyMiningFatigueDuration;
                this.superflySlownessDuration = config.superflySlownessDuration;
                this.superflyParticleRange = config.superflyParticleRange;
                this.superflyGlowingDuration = config.superflyGlowingDuration;
                this.superflyRed = config.superflyRed;
                this.superflyGreen = config.superflyGreen;
                this.superflyBlue = config.superflyBlue;
                this.boyImmunityDuration = config.boyImmunityDuration;
                this.boyRegenerationDuration = config.boyRegenerationDuration;
                this.boyParticleRange = config.boyParticleRange;
                this.boyGlowingDuration = config.boyGlowingDuration;
                this.boyPositionTolerance = config.boyPositionTolerance;
                this.boyRed = config.boyRed;
                this.boyGreen = config.boyGreen;
                this.boyBlue = config.boyBlue;
                this.whitealbumRed = config.whitealbumRed;
                this.whitealbumGreen = config.whitealbumGreen;
                this.whitealbumBlue = config.whitealbumBlue;
                this.whitealbumFreezeDuration = config.whitealbumFreezeDuration;
                this.whitealbumFreezeRadius = config.whitealbumFreezeRadius;
                this.whitealbumParticleRange = config.whitealbumParticleRange;
                this.heyyaRed = config.heyyaRed;
                this.heyyaGreen = config.heyyaGreen;
                this.heyyaBlue = config.heyyaBlue;
                this.heyyaStructureRadius = config.heyyaStructureRadius;
                this.heyyaOreRadius = config.heyyaOreRadius;
                this.heyyaMobRadius = config.heyyaMobRadius;
                this.heyyaParticleRange = config.heyyaParticleRange;
            } else {
                save();
            }
        } catch (Exception e) {
            NameScramblerMod.LOGGER.error("Failed to load ability config", e);
        }
    }
    
    public void save() {
        try {
            if (!CONFIG_FILE.getParentFile().exists()) {
                CONFIG_FILE.getParentFile().mkdirs();
            }
            FileWriter writer = new FileWriter(CONFIG_FILE);
            GSON.toJson(this, writer);
            writer.close();
        } catch (IOException e) {
            NameScramblerMod.LOGGER.error("Failed to save ability config", e);
        }
    }
}