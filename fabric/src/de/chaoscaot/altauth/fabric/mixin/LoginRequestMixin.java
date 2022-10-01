// SPDX-License-Identifier: MIT

package de.chaoscaot.altauth.fabric.mixin;

import com.mojang.authlib.yggdrasil.YggdrasilEnvironment;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import de.chaoscaot.altauth.fabric.AltAuth;
import de.chaoscaot.altauth.fabric.TrustServerScreen;
import de.chaoscaot.altauth.fabric.config.ClientConfig;
import de.lixfel.ReflectionUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.network.packet.s2c.login.LoginHelloS2CPacket;
import net.minecraft.text.Text;
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

    @Shadow @Final private MinecraftClient client;

    @Inject(at = @At("HEAD"), method = "onHello", cancellable = true)
    public void onHello(LoginHelloS2CPacket packet, CallbackInfo ci) {
        String server = packet.getServerId();
        try {
            if(server.contains(".") && !ClientConfig.INSTANCE.forcedMojang.contains(AltAuth.address.getAddress()) && ClientConfig.INSTANCE.enabled) {
                LOGGER.info("AltauthClient: LoginRequestMixin: Server is running on a custom: {}", server);
                if(ClientConfig.INSTANCE.allowedServers.contains(server) || AltAuth.trustOnce) {
                    AltAuth.trustOnce = false;
                    if(MinecraftClient.getInstance().currentScreen instanceof ConnectScreen cs) {
                        cs.status = Text.translatable("gui.altauth.connecting", server);
                    }
                    URL url = new URL("https://" + server + "/session/minecraft/join");
                    ReflectionUtil.getField(YggdrasilMinecraftSessionService.class, URL.class, 0).set(MinecraftClient.getInstance().getSessionService(), url);
                } else {
                    if(MinecraftClient.getInstance().currentScreen instanceof ConnectScreen cs) {
                        ci.cancel();
                        cs.connectingCancelled = true;
                        if( cs.connection != null) {
                            cs.connection.disconnect(Text.translatable("connect.aborted"));
                        }

                        MinecraftClient.getInstance().execute(() -> client.setScreen(new TrustServerScreen(server, cs.parent)));
                    }
                }
            } else {
                LOGGER.info("AltauthClient: LoginRequestMixin: Server is running on mojang");
                URL url = new URL(YggdrasilEnvironment.PROD.getEnvironment().getSessionHost() + "/session/minecraft/join");
                ReflectionUtil.getField(YggdrasilMinecraftSessionService.class, URL.class, 0).set(MinecraftClient.getInstance().getSessionService(), url);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if(MinecraftClient.getInstance().currentScreen instanceof ConnectScreen cs) {
                client.execute(() -> MinecraftClient.getInstance().setScreen(new DisconnectedScreen(cs.parent, Text.translatable("gui.altauth.error"), Text.of(e.getMessage()))));
            }
        }
    }
}
