// SPDX-License-Identifier: MIT

package de.lixfel.altauth.bukkit;

import de.lixfel.ReflectionUtil;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;

public class SessionServiceInjector {

    private static final Class<?> MINECRAFT_SESSION_SERVICE = ReflectionUtil.getClass("com.mojang.authlib.minecraft.MinecraftSessionService");
    private static final Class<?> YGGDRASIL_SESSION_SERVICE = ReflectionUtil.getClass("com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService");

    private static final ReflectionUtil.FieldWrapper<URL> JOIN_URL = ReflectionUtil.getField(YGGDRASIL_SESSION_SERVICE, URL.class, 0);
    private static final ReflectionUtil.FieldWrapper<URL> CHECK_URL = ReflectionUtil.getField(YGGDRASIL_SESSION_SERVICE, URL.class, 1);

    private final Object sessionService;
    private final URL joinUrlBackup;
    private final URL checkUrlBackup;

    public SessionServiceInjector(String altAuthServer) {
        altAuthServer = "https://" + altAuthServer + "/session/minecraft/";

        if(ReflectionUtil.MINECRAFT_VERSION < 17) { //TODO maybe 16 (untested)
            sessionService = null;
        } else if (ReflectionUtil.MINECRAFT_VERSION < 19) {
            sessionService = ReflectionUtil.getField(ProtocolInjector.minecraftServer, MINECRAFT_SESSION_SERVICE, 0).get(
                    ProtocolInjector.instance.minecraftServer()
            );
        } else {
            Class<?> services = ReflectionUtil.getClass("net.minecraft.server.Services");
            sessionService = ReflectionUtil.getField(services, MINECRAFT_SESSION_SERVICE, 0).get(
                    ReflectionUtil.getField(ProtocolInjector.minecraftServer, services, 0).get(
                            ProtocolInjector.instance.minecraftServer()
                    )
            );
        }

        joinUrlBackup = JOIN_URL.get(sessionService);
        checkUrlBackup = CHECK_URL.get(sessionService);

        try {
            JOIN_URL.set(sessionService, new URL(altAuthServer + "join"));
            CHECK_URL.set(sessionService, new URL(altAuthServer + "hasJoined"));
        } catch (MalformedURLException e) {
            AltAuthBukkit.getInstance().getLogger().log(Level.SEVERE, "Could not create AltAuth URLs", e);
        }
    }

    public void revert() {
        JOIN_URL.set(sessionService, joinUrlBackup);
        CHECK_URL.set(sessionService, checkUrlBackup);
    }
}
