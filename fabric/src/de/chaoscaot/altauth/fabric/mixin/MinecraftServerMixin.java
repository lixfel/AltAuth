// SPDX-License-Identifier: MIT

package de.chaoscaot.altauth.fabric.mixin;

import com.mojang.datafixers.DataFixer;
import de.chaoscaot.altauth.fabric.AltAuth;
import de.chaoscaot.altauth.fabric.config.ServerConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.SaveLoader;
import net.minecraft.server.WorldGenerationProgressListenerFactory;
import net.minecraft.util.ApiServices;
import net.minecraft.world.level.storage.LevelStorage;
import org.joor.Reflect;
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
            Reflect.on(apiServices.sessionService()).set("checkUrl", url);
            URL joinUrl = new URL("https://" + ServerConfig.INSTANCE.serverUrl + "/session/minecraft/join");
            Reflect.on(apiServices.sessionService()).set("joinUrl", joinUrl);
        } catch (MalformedURLException e) {
            AltAuth.LOGGER.error("Malformed URL: {}", e.getMessage());
        }
        ServerConfig.INSTANCE.save();
    }
}
