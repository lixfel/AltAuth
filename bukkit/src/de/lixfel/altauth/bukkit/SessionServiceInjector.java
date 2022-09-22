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

import de.lixfel.tinyprotocol.Reflection;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;

public class SessionServiceInjector {

    private static final Class<?> SESSION_SERVICE = Reflection.getClass("com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService");

    private static final Reflection.FieldAccessor<URL> JOIN_URL = Reflection.getField(SESSION_SERVICE, "JOIN_URL", URL.class);
    private static final Reflection.FieldAccessor<URL> CHECK_URL = Reflection.getField(SESSION_SERVICE, "CHECK_URL", URL.class);

    private final URL joinUrlBackup;
    private final URL checkUrlBackup;

    public SessionServiceInjector(String altAuthServer) {
        joinUrlBackup = JOIN_URL.get(null);
        checkUrlBackup = CHECK_URL.get(null);

        altAuthServer = "https://" + altAuthServer + "/session/minecraft/";
        try {
            JOIN_URL.set(null, new URL(altAuthServer + "join"));
            CHECK_URL.set(null, new URL(altAuthServer + "hasJoined"));
        } catch (MalformedURLException e) {
            AltAuthBukkit.getInstance().getLogger().log(Level.SEVERE, "Could not create AltAuth URLs", e);
        }
    }

    public void revert() {
        JOIN_URL.set(null, joinUrlBackup);
        CHECK_URL.set(null, checkUrlBackup);
    }
}
