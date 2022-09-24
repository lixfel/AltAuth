package de.chaoscaot.altauth.config.server;

import de.chaoscaot.altauth.AltAuth;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class AltAuthServerConfig {

    private static final String CONFIG_FILE_NAME = "altauth.json";

    public static AltAuthServerConfig INSTANCE;

    static {
        if(!new File(FabricLoader.getInstance().getConfigDir().toFile(), CONFIG_FILE_NAME).exists()) {
            INSTANCE = new AltAuthServerConfig();
        } else {
            try {
                INSTANCE = AltAuth.GSON.fromJson(Files.readString(FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME)), AltAuthServerConfig.class);
                if(INSTANCE.serverUrl.length() > 20) {
                    AltAuth.LOGGER.error("AltauthServer: AltAuthServerConfig: ServerUrl is too long. Max length is 20 characters");
                    INSTANCE.serverUrl = "";
                }
            } catch (IOException e) {
                AltAuth.LOGGER.error("AltauthServer: AltAuthServerConfig: Error while loading config", e);
                AltAuth.LOGGER.info("Reset Config...");
                INSTANCE = new AltAuthServerConfig();
                INSTANCE.save();
            }
        }
    }

    public String serverUrl = "EnterNameHere";

    public void save() {
        try {
            Files.writeString(FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME), AltAuth.GSON.toJson(this));
        } catch (IOException e) {
            AltAuth.LOGGER.error("AltauthServer: AltAuthServerConfig: Error while saving config", e);
        }
    }
}
