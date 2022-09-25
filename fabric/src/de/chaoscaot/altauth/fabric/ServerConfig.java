// SPDX-License-Identifier: MIT

package de.chaoscaot.altauth.fabric;

import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ServerConfig {

    private static final String CONFIG_FILE_NAME = "altauth.json";

    public static ServerConfig INSTANCE;

    static {
        if(!new File(FabricLoader.getInstance().getConfigDir().toFile(), CONFIG_FILE_NAME).exists()) {
            INSTANCE = new ServerConfig();
        } else {
            try {
                INSTANCE = AltAuth.GSON.fromJson(Files.readString(FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME)), ServerConfig.class);
                if(INSTANCE.serverUrl.length() > 20) {
                    AltAuth.LOGGER.error("AltauthServer: AltAuthServerConfig: ServerUrl is too long. Max length is 20 characters");
                    INSTANCE.serverUrl = "";
                }
            } catch (IOException e) {
                AltAuth.LOGGER.error("AltauthServer: AltAuthServerConfig: Error while loading config", e);
                AltAuth.LOGGER.info("Reset Config...");
                INSTANCE = new ServerConfig();
                INSTANCE.save();
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
                System.exit(1);
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
