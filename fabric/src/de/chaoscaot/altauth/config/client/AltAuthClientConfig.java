package de.chaoscaot.altauth.config.client;

import de.chaoscaot.altauth.AltAuth;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class AltAuthClientConfig {

    private static final String CONFIG_FILE_NAME = "altauth.client.json";

    public static AltAuthClientConfig INSTANCE;

    static {
        if(!new File(FabricLoader.getInstance().getConfigDir().toFile(), CONFIG_FILE_NAME).exists()) {
            INSTANCE = new AltAuthClientConfig();
        } else {
            try {
                INSTANCE = AltAuth.GSON.fromJson(Files.readString(FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME)), AltAuthClientConfig.class);
            } catch (IOException e) {
                AltAuth.LOGGER.error("AltauthClient: AltAuthClientConfig: Error while loading config", e);
                AltAuth.LOGGER.info("Reset Config...");
                INSTANCE = new AltAuthClientConfig();
                INSTANCE.save();
            }
        }
    }

    public boolean enabled = true;
    public List<String> allowedServers = new ArrayList<>();

    public void save() {
        try {
            Files.writeString(FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME), AltAuth.GSON.toJson(this));
        } catch (IOException e) {
            AltAuth.LOGGER.error("AltauthClient: AltAuthClientConfig: Error while saving config", e);
        }
    }
}
