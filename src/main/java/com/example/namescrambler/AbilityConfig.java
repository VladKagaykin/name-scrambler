package com.example.namescrambler;

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
    public double arrowActivationChance = 0.01; // 1% шанс
    public int immunityDuration = 200; // 10 секунд (20 тиков/сек)
    public int blindnessDuration = 200;
    
    // Настройки superfly
    public int superflyPoisonDuration = 100;
    public int superflyNauseaDuration = 100;
    public int superflyBlindnessDuration = 100;
    public int superflyMiningFatigueDuration = 100;
    public int superflySlownessDuration = 100;
    public int superflyParticleRange = 50;
    
    // Настройки 20centuryboy
    public int boyImmunityDuration = 100;
    public int boyParticleRange = 30;
    
    public void load() {
        try {
            if (CONFIG_FILE.exists()) {
                AbilityConfig config = GSON.fromJson(new FileReader(CONFIG_FILE), AbilityConfig.class);
                this.arrowActivationChance = config.arrowActivationChance;
                this.immunityDuration = config.immunityDuration;
                this.blindnessDuration = config.blindnessDuration;
                this.superflyPoisonDuration = config.superflyPoisonDuration;
                this.superflyNauseaDuration = config.superflyNauseaDuration;
                this.superflyBlindnessDuration = config.superflyBlindnessDuration;
                this.superflyMiningFatigueDuration = config.superflyMiningFatigueDuration;
                this.superflySlownessDuration = config.superflySlownessDuration;
                this.superflyParticleRange = config.superflyParticleRange;
                this.boyImmunityDuration = config.boyImmunityDuration;
                this.boyParticleRange = config.boyParticleRange;
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