// SPDX-License-Identifier: MIT

package de.lixfel.altauth.bukkit;

import de.lixfel.ReflectionUtil;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;

public class SessionServiceInjector {

    private static final Class<?> SESSION_SERVICE = ReflectionUtil.getClass("com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService");

    private static final ReflectionUtil.FieldWrapper<URL> JOIN_URL = ReflectionUtil.getField(SESSION_SERVICE, URL.class, "JOIN_URL");
    private static final ReflectionUtil.FieldWrapper<URL> CHECK_URL = ReflectionUtil.getField(SESSION_SERVICE, URL.class, "CHECK_URL");

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
