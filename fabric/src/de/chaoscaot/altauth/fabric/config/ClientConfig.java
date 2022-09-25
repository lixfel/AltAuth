// SPDX-License-Identifier: MIT

package de.chaoscaot.altauth.fabric.config;

import com.google.gson.JsonSyntaxException;
import de.chaoscaot.altauth.fabric.AltAuth;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class ClientConfig {

    private static final String CONFIG_FILE_NAME = "altauth.client.json";

    public static ClientConfig INSTANCE;

    static {
        if(!new File(FabricLoader.getInstance().getConfigDir().toFile(), CONFIG_FILE_NAME).exists()) {
            INSTANCE = new ClientConfig();
        } else {
            try {
                INSTANCE = AltAuth.GSON.fromJson(Files.readString(FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME)), ClientConfig.class);
            } catch (IOException | JsonSyntaxException e) {
                AltAuth.LOGGER.error("AltauthClient: AltAuthClientConfig: Error while loading config", e);
                AltAuth.LOGGER.info("Reset Config...");
                INSTANCE = new ClientConfig();
                INSTANCE.save();
            }
        }
    }

    public boolean enabled = true;
    public List<String> allowedServers = new ArrayList<>();
    public List<String> forcedMojang = new ArrayList<>();

    public void save() {
        try {
            Files.writeString(FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME), AltAuth.GSON.toJson(this));
        } catch (IOException e) {
            AltAuth.LOGGER.error("AltauthClient: AltAuthClientConfig: Error while saving config", e);
        }
    }
}
