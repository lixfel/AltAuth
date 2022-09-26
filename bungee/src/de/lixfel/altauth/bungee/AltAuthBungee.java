// SPDX-License-Identifier: MIT

package de.lixfel.altauth.bungee;

import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AltAuthBungee extends Plugin implements Listener {

    private String altAuthUrl;

    @Override
    public void onEnable() {
        File dataFolder = getDataFolder();
        File config = new File(dataFolder, "config.yml");
        ConfigurationProvider provider = ConfigurationProvider.getProvider(YamlConfiguration.class);

        if(!config.exists()) {
            dataFolder.mkdir();

            try (InputStream in = getResourceAsStream("config.yml")) {
                try (FileOutputStream out = new FileOutputStream(config)) {
                    int read;
                    byte[] buffer = new byte[4096];
                    while ((read = in.read(buffer)) >= 0)
                        out.write(buffer, 0, read);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Could not create config", e);
            }
        }

        try {
            Configuration configuration = provider.load(config);
            altAuthUrl = configuration.getString("altauth-proxy");
        } catch (IOException e) {
            throw new IllegalStateException("Could not load config", e);
        }

        getProxy().getPluginManager().registerListener(this, this);
    }

    @EventHandler
    public void onPreLogin(PreLoginEvent e) {
        new AltAuthHandler(getProxy(), e.getConnection(), altAuthUrl);
    }
}
