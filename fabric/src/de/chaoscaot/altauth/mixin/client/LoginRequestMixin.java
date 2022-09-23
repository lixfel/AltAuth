package de.chaoscaot.altauth.mixin.client;

import com.mojang.authlib.yggdrasil.YggdrasilEnvironment;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.network.packet.s2c.login.LoginHelloS2CPacket;
import org.joor.Reflect;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.URL;

@Environment(EnvType.CLIENT)
@Mixin(ClientLoginNetworkHandler.class)
public class LoginRequestMixin {
    @Shadow @Final private static Logger LOGGER;

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/login/LoginHelloS2CPacket;getServerId()Ljava/lang/String;"), method = "onHello")
    public void onHello(LoginHelloS2CPacket packet, CallbackInfo ci) {
        try {
            if(packet.getServerId().contains(".")) {
                LOGGER.info("AltauthClient: LoginRequestMixin: Server is running on a custom: {}", packet.getServerId());
                URL url = new URL("https://" + packet.getServerId() + "/session/minecraft/join");
                Reflect.on(MinecraftClient.getInstance().getSessionService()).set("joinUrl", url);
            } else {
                LOGGER.info("AltauthClient: LoginRequestMixin: Server is running on mojang");
                URL url = new URL(YggdrasilEnvironment.PROD.getEnvironment().getSessionHost() + "/session/minecraft/join");
                Reflect.on(MinecraftClient.getInstance().getSessionService()).set("joinUrl", url);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
