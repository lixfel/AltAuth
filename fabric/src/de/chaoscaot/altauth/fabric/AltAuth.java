// SPDX-License-Identifier: MIT

package de.chaoscaot.altauth.fabric;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.network.ServerAddress;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AltAuth {

    private AltAuth() {}

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Logger LOGGER = LogManager.getLogger();

    public static ServerAddress address;
}
