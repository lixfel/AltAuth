// SPDX-License-Identifier: MIT

package de.lixfel.altauth.bukkit;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import de.lixfel.ReflectionUtil;

import javax.crypto.SecretKey;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;

public class AltAuthSessionService implements MinecraftSessionService {

    private static final ReflectionUtil.FieldWrapper<URL> CHECK_URL = ReflectionUtil.getField(YggdrasilMinecraftSessionService.class, URL.class, 1);
    private static final boolean URL_STATIC = ReflectionUtil.MINECRAFT_VERSION < 17; //TODO maybe 16 (untested)

    private static final Function<AltAuthSessionService, YggdrasilMinecraftSessionService> swapService;
    private static final Consumer<AltAuthSessionService> revertService;

    static {
        if (ReflectionUtil.MINECRAFT_VERSION < 19) {
            ReflectionUtil.FieldWrapper<MinecraftSessionService> getService = ReflectionUtil.getField(ServerIdInjector.minecraftServer, MinecraftSessionService.class, 0);

            swapService = altAuthService -> {
                Object minecraftServer = altAuthService.serverIdInjector.minecraftServerInstance();
                YggdrasilMinecraftSessionService service = (YggdrasilMinecraftSessionService) getService.get(minecraftServer);
                getService.set(minecraftServer, altAuthService);
                return service;
            };
            revertService = altAuthService -> getService.set(altAuthService.serverIdInjector.minecraftServerInstance(), altAuthService.service);
        } else {
            Class<?> services = ReflectionUtil.getClass("net.minecraft.server.Services");
            ReflectionUtil.FieldWrapper<?> getServices = ReflectionUtil.getField(ServerIdInjector.minecraftServer, services, 0);
            ReflectionUtil.FieldWrapper<MinecraftSessionService> getSessionService = ReflectionUtil.getField(services, MinecraftSessionService.class, 0);
            Class<?> signatureValidator = ReflectionUtil.getClass("net.minecraft.util.SignatureValidator");
            ReflectionUtil.FieldWrapper<?> getSignatureValidator = ReflectionUtil.getField(services, signatureValidator, 0);
            ReflectionUtil.FieldWrapper<GameProfileRepository> getGameProfileRepository = ReflectionUtil.getField(services, GameProfileRepository.class, 0);
            Class<?> userCache = ReflectionUtil.getClass("net.minecraft.server.players.UserCache");
            ReflectionUtil.FieldWrapper<?> getUserCache = ReflectionUtil.getField(services, userCache, 0);

            Constructor<?> constructor;
            try {
                constructor = services.getConstructor(MinecraftSessionService.class, signatureValidator, GameProfileRepository.class, userCache);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }

            swapService = altAuthService -> {
                Object servicesInstance = getServices.get(altAuthService.serverIdInjector.minecraftServerInstance());

                try {
                    getServices.set(
                        altAuthService.serverIdInjector.minecraftServerInstance(),
                        constructor.newInstance(altAuthService, getSignatureValidator.get(servicesInstance), getGameProfileRepository.get(servicesInstance), getUserCache.get(servicesInstance))
                    );
                } catch (InstantiationException | IllegalAccessException| InvocationTargetException e) {
                    throw new IllegalStateException(e);
                }

                return (YggdrasilMinecraftSessionService) getSessionService.get(servicesInstance);
            };
            revertService = altAuthService -> {
                Object servicesInstance = getServices.get(altAuthService.serverIdInjector.minecraftServerInstance());

                try {
                    getServices.set(
                            altAuthService.serverIdInjector.minecraftServerInstance(),
                            constructor.newInstance(altAuthService.service, getSignatureValidator.get(servicesInstance), getGameProfileRepository.get(servicesInstance), getUserCache.get(servicesInstance))
                    );
                } catch (InstantiationException | IllegalAccessException| InvocationTargetException e) {
                    throw new IllegalStateException(e);
                }
            };
        }
    }

    private static final Class<?> minecraftEncryption = ReflectionUtil.getClass("net.minecraft.util.MinecraftEncryption");
    private static final ReflectionUtil.MethodWrapper getServerHash = ReflectionUtil.getMethod(minecraftEncryption, byte[].class, null, String.class, PublicKey.class, SecretKey.class);
    private static final ReflectionUtil.MethodWrapper getSecretKey = ReflectionUtil.getMethod(minecraftEncryption, null, PrivateKey.class, byte[].class);

    private static final ReflectionUtil.FieldWrapper<KeyPair> getKeyPair = ReflectionUtil.getField(ServerIdInjector.minecraftServer, KeyPair.class, 0);

    private final YggdrasilMinecraftSessionService service;
    private final URL checkUrlBackup;

    private final KeyPair keyPair;
    private final ServerIdInjector serverIdInjector;
    private final String altAuthServer;

    public AltAuthSessionService(ServerIdInjector serverIdInjector, String altAuthServer) {
        this.serverIdInjector = serverIdInjector;
        this.altAuthServer = altAuthServer;
        this.keyPair = getKeyPair.get(serverIdInjector.minecraftServerInstance());

        service = swapService.apply(this);
        checkUrlBackup = CHECK_URL.get(URL_STATIC ? null : service);

        try {
            CHECK_URL.set(URL_STATIC ? null : service, new URL("https://" + altAuthServer + "/session/minecraft/hasJoined"));
        } catch (MalformedURLException e) {
            AltAuthBukkit.getInstance().getLogger().log(Level.SEVERE, "Could not create AltAuth URLs", e);
        }
    }

    public void revert() {
        CHECK_URL.set(URL_STATIC ? null : service, checkUrlBackup);
        revertService.accept(this);
    }

    @Override
    public void joinServer(GameProfile gameProfile, String s, String s1) throws AuthenticationException {
        service.joinServer(gameProfile, s, s1);
    }

    @Override
    public GameProfile hasJoinedServer(GameProfile gameProfile, String serverId, InetAddress inetAddress) throws AuthenticationUnavailableException {
        return service.hasJoinedServer(gameProfile, new BigInteger((byte[]) getServerHash.invoke(null, altAuthServer, keyPair.getPublic(), getSecretKey.invoke(null, keyPair.getPrivate(), serverIdInjector.getEncryptedSecret(gameProfile.getName())))).toString(16), inetAddress);
    }

    @Override
    public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTextures(GameProfile gameProfile, boolean b) {
        return service.getTextures(gameProfile, b);
    }

    @Override
    public GameProfile fillProfileProperties(GameProfile gameProfile, boolean b) {
        return service.fillProfileProperties(gameProfile, b);
    }
}
