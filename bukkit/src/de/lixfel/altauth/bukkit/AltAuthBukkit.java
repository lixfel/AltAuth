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

package de.lixfel.altauth.bukkit;

import de.lixfel.tinyprotocol.TinyProtocol;
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

		TinyProtocol.init();
		serviceInjector = new SessionServiceInjector(altAuthServer);
		requestInjector = new EncryptionRequestInjector(altAuthServer);
	}

	@Override
	public void onDisable() {
		requestInjector.remove();
		serviceInjector.revert();
		TinyProtocol.instance.close();
	}
}
