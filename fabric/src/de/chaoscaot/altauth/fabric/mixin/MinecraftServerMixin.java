// SPDX-License-Identifier: MIT

package de.chaoscaot.altauth.fabric.mixin;

import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import com.mojang.datafixers.DataFixer;
import de.chaoscaot.altauth.fabric.AltAuth;
import de.chaoscaot.altauth.fabric.config.ServerConfig;
import de.lixfel.ReflectionUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.SaveLoader;
import net.minecraft.server.WorldGenerationProgressListenerFactory;
import net.minecraft.util.ApiServices;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;

@Environment(EnvType.SERVER)
@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

    @Inject(method = "<init>", at = @At(value = "TAIL"))
    private void apiServiceMixin(Thread serverThread, LevelStorage.Session session, ResourcePackManager dataPackManager, SaveLoader saveLoader, Proxy proxy, DataFixer dataFixer, ApiServices apiServices, WorldGenerationProgressListenerFactory worldGenerationProgressListenerFactory, CallbackInfo ci) {
        try {
            URL url = new URL("https://" + ServerConfig.INSTANCE.serverUrl + "/session/minecraft/hasJoined");
            ReflectionUtil.getField(YggdrasilMinecraftSessionService.class, URL.class, 1).set(MinecraftClient.getInstance().getSessionService(), url);
        } catch (MalformedURLException e) {
            AltAuth.LOGGER.error("Malformed URL: {}", e.getMessage());
        }
        ServerConfig.INSTANCE.save();
    }
}
