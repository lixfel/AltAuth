// SPDX-License-Identifier: MIT

package de.lixfel.altauth.bukkit;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.plugin.java.JavaPlugin;


public class AltAuthBukkit extends JavaPlugin {

	@Getter
	@Setter
	private static JavaPlugin instance;

	private ServerIdInjector serverIdInjector;
	private AltAuthSessionService serviceInjector;

	@Override
	public void onLoad() {
		setInstance(this);
	}
	
	@Override
	public void onEnable() {
		saveDefaultConfig();
		String altAuthServer = getConfig().getString("altauth-proxy");

		serverIdInjector = new ServerIdInjector(this, altAuthServer);
		serviceInjector = new AltAuthSessionService(serverIdInjector, altAuthServer);
	}

	@Override
	public void onDisable() {
		serviceInjector.revert();
		serverIdInjector.close();
	}
}
