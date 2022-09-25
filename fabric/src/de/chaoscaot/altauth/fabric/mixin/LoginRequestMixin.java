// SPDX-License-Identifier: MIT

package de.chaoscaot.altauth.fabric.mixin;

import com.mojang.authlib.yggdrasil.YggdrasilEnvironment;
import de.chaoscaot.altauth.fabric.AltAuth;
import de.chaoscaot.altauth.fabric.ClientConfig;
import de.chaoscaot.altauth.fabric.TrustServerScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.network.packet.s2c.login.LoginHelloS2CPacket;
import net.minecraft.text.Text;
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
                if(ClientConfig.INSTANCE.allowedServers.contains(packet.getServerId()) || AltAuth.trustOnce) {
                    AltAuth.trustOnce = false;
                    URL url = new URL("https://" + packet.getServerId() + "/session/minecraft/join");
                    Reflect.on(MinecraftClient.getInstance().getSessionService()).set("joinUrl", url);
                } else {
                    if(MinecraftClient.getInstance().currentScreen instanceof ConnectScreen cs) {
                        cs.connectingCancelled = true;
                        if( cs.connection != null) {
                            cs.connection.disconnect(Text.translatable("connect.aborted"));
                        }

                        MinecraftClient.getInstance().setScreen(new TrustServerScreen(packet.getServerId(), cs.parent));
                    }
                }
            } else {
                LOGGER.info("AltauthClient: LoginRequestMixin: Server is running on mojang");
                URL url = new URL(YggdrasilEnvironment.PROD.getEnvironment().getSessionHost() + "/session/minecraft/join");
                Reflect.on(MinecraftClient.getInstance().getSessionService()).set("joinUrl", url);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if(MinecraftClient.getInstance().currentScreen instanceof ConnectScreen cs) {
                MinecraftClient.getInstance().setScreen(new DisconnectedScreen(cs.parent, Text.of("AltAuth error"), Text.of(e.getMessage())));
            }
        }
    }
}
