/*
 *    This file is part of AltAuth.
 *
 *    Copyright (C) 2022  Lixfel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *    GNU Affero General Public License for more details.
 *
 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
        File config = new File(getDataFolder(), "config.yml");
        ConfigurationProvider provider = ConfigurationProvider.getProvider(YamlConfiguration.class);

        if(!config.exists()) {
            try (InputStream in = getResourceAsStream("config.yml")) {
                try (FileOutputStream out = new FileOutputStream(config)) {
                    int read;
                    byte[] buffer = new byte[4096];
                    while ((read = in.read(buffer)) >= 0)
                        out.write(buffer, 0, read);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Could not create config");
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
