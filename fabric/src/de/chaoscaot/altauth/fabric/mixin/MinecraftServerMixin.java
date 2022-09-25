package de.chaoscaot.altauth.fabric.mixin;

import de.chaoscaot.altauth.fabric.AltAuth;
import de.chaoscaot.altauth.fabric.config.ServerConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ApiServices;
import org.joor.Reflect;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.MalformedURLException;
import java.net.URL;

@Environment(EnvType.SERVER)
@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

    @Shadow
    protected ApiServices apiServices;

    @Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/server/MinecraftServer;apiServices:Lnet/minecraft/util/ApiServices;", opcode = Opcodes.PUTFIELD))
    private void apiServiceMixin(MinecraftServer instance, ApiServices value) {
        apiServices = value;

        try {
            URL url = new URL("https://" + ServerConfig.INSTANCE.serverUrl + "/session/minecraft/join");
            Reflect.on(MinecraftClient.getInstance().getSessionService()).set("joinUrl", url);
        } catch (MalformedURLException e) {
            AltAuth.LOGGER.error("Malformed URL: {}", e.getMessage());
        }
    }
}
