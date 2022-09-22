// SPDX-License-Identifier: MIT

package de.lixfel.altauth.bukkit;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.plugin.java.JavaPlugin;


public class AltAuthBukkit extends JavaPlugin {

	@Getter
	@Setter
	private static JavaPlugin instance;

	private SessionServiceInjector serviceInjector;
	private EncryptionRequestInjector requestInjector;

	@Override
	public void onLoad() {
		setInstance(this);
	}
	
	@Override
	public void onEnable() {
		saveDefaultConfig();
		String altAuthServer = getConfig().getString("altauth-proxy");

		ProtocolInjector.init();
		serviceInjector = new SessionServiceInjector(altAuthServer);
		requestInjector = new EncryptionRequestInjector(altAuthServer);
	}

	@Override
	public void onDisable() {
		requestInjector.remove();
		serviceInjector.revert();
		ProtocolInjector.instance.close();
	}
}
