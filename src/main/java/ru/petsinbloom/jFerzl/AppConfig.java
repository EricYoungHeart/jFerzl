package ru.petsinbloom.jFerzl;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/// Switched from lazy initialization" pattern to enum for thread-safety
/// Enums are inherently thread-safe and serialization-safe.
/// No need for synchronized or manual double-checked locking.
/// Joshua Bloch recommends this in Effective Java.

/// Low-level configuration storage
public enum AppConfig {
    INSTANCE;

    // private static AppConfig instance;
    private final Properties properties;
    // private Properties properties;

    AppConfig() {
        properties = new Properties();
        loadConfig();
    }

//    public static AppConfig getInstance() {
//        if (instance == null) {
//            instance = new AppConfig();
//        }
//        return instance;
//    }

    private void loadConfig() {
        // Load from a file with UTF-8 encoding
        // Версия, если не нужно сохранять кириллицу в UTF-8
        //try (InputStream input = new FileInputStream("config.properties")) {
        //    properties.load(input);
        try (Reader reader = new InputStreamReader(new FileInputStream("config.properties"), StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException ex) {
            // Initialize with default values if the file doesn't exist
            properties.setProperty("workingFolder", "BASE");
            properties.setProperty("commonFolder", "COMMON");
            properties.setProperty("aisomsFolder", "D:\\AISOMS");

            properties.setProperty("REQUEST_WORKBOOK_FILENAME", "request.xlsx");
            properties.setProperty("F003", "F003.xml");

            properties.setProperty("IsStateMachine", Boolean.toString(false));

            saveConfig(); // Save the default configuration to file
        }
    }

    public void saveConfig() {
        // Save to a file with UTF-8 encoding
        // Версия, если не нужно читить кириллицу в UTF-8
        //try (OutputStream output = new FileOutputStream("config.properties")) {
        //    properties.store(output, null);
        try (Writer writer = new OutputStreamWriter(new FileOutputStream("config.properties"), StandardCharsets.UTF_8)) {
            properties.store(writer, null);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    public String getConfig(String key) {
        return properties.getProperty(key);
    }

    public void setConfig(String key, String value) {
        properties.setProperty(key, value);
    }

    public boolean isStateMachine() {
        return Boolean.parseBoolean(getConfig("IsStateMachine"));
    }

}
