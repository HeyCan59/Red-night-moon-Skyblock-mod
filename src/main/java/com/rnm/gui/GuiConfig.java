package com.rnm.gui;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class GuiConfig {

    public static boolean chatTimestamp = true;
    public static boolean enabled = true;
    public static boolean verticalScrolling = true;
    public static boolean MetalDetector = true;

    private static final Path CONFIG_FILE =
        FabricLoader.getInstance()
            .getConfigDir()
            .resolve("rnm-config.properties");

    // 遊戲載入設定時，自動讀取設定檔
    static {
        load();
    }

    // 讀取設定
    public static void load() {
        if (!Files.exists(CONFIG_FILE)) {
            return;
        }

        Properties properties = new Properties();

        try (InputStream input = Files.newInputStream(CONFIG_FILE)) {
            properties.load(input);

            chatTimestamp = Boolean.parseBoolean(
                properties.getProperty("chatTimestamp", "false")
            );

            enabled = Boolean.parseBoolean(
                properties.getProperty("enabled", "false")
            );

            verticalScrolling = Boolean.parseBoolean(
                properties.getProperty("verticalScrolling", "false")
            );

            MetalDetector = Boolean.parseBoolean(
                properties.getProperty("MetalDetector", "true")
            );

        } catch (IOException e) {
            System.err.println("[RNM] 載入設定失敗：" + e.getMessage());
        }
    }

    // 儲存設定
    public static void save() {
        Properties properties = new Properties();

        properties.setProperty("chatTimestamp", String.valueOf(chatTimestamp));
        properties.setProperty("enabled", String.valueOf(enabled));
        properties.setProperty("verticalScrolling", String.valueOf(verticalScrolling));
        properties.setProperty("MetalDetector", String.valueOf(MetalDetector));

        try {
            Files.createDirectories(CONFIG_FILE.getParent());

            try (OutputStream output = Files.newOutputStream(CONFIG_FILE)) {
                properties.store(output, "RNM Mod Configuration");
            }

        } catch (IOException e) {
            System.err.println("[RNM] 儲存設定失敗：" + e.getMessage());
        }
    }
}