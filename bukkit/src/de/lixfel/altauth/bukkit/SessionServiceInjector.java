// SPDX-License-Identifier: MIT

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
