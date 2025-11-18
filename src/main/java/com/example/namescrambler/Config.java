package com.example.namescrambler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File("config/name-scrambler.json");

    public double chatRadius = 20.0;

    public void load() {
        try {
            if (CONFIG_FILE.exists()) {
                Config config = GSON.fromJson(new FileReader(CONFIG_FILE), Config.class);
                this.chatRadius = config.chatRadius;
                NameScramblerMod.LOGGER.info("AVCD Name Scrambler: Config loaded successfully");
            } else {
                // Создаем default config
                save();
                NameScramblerMod.LOGGER.info("AVCD Name Scrambler: Default config created");
            }
        } catch (Exception e) {
            NameScramblerMod.LOGGER.error("AVCD Name Scrambler: Failed to load config", e);
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
            NameScramblerMod.LOGGER.error("AVCD Name Scrambler: Failed to save config", e);
        }
    }
}
