package de.chaoscaot.altauth.mixin.client;

import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConnectScreen;
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

import java.lang.reflect.Field;
import java.net.URL;

@Environment(EnvType.CLIENT)
@Mixin(ClientLoginNetworkHandler.class)
public class LoginRequestMixin {
    @Shadow @Final private static Logger LOGGER;

    private static final Class<YggdrasilMinecraftSessionService> YGGDRASIL_MINECRAFT_SESSION_SERVICE_CLASS = YggdrasilMinecraftSessionService.class;
    private static final Field YGGDRASIL_MINECRAFT_SESSION_SERVICE_JOIN_URL;

    static {
        try {
            YGGDRASIL_MINECRAFT_SESSION_SERVICE_JOIN_URL = YGGDRASIL_MINECRAFT_SESSION_SERVICE_CLASS.getDeclaredField("joinUrl");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/login/LoginHelloS2CPacket;getServerId()Ljava/lang/String;"), method = "onHello")
    public void onHello(LoginHelloS2CPacket packet, CallbackInfo ci) {
        try {
            if(packet.getServerId().contains(".")) {
                LOGGER.info("AltauthClient: LoginRequestMixin: Server is running on a custom: {}", packet.getServerId());
                YGGDRASIL_MINECRAFT_SESSION_SERVICE_JOIN_URL.set(MinecraftClient.getInstance().getSessionService(), new URL("https://" + packet.getServerId() + "/session/minecraft/join"));
            } else {
                LOGGER.info("AltauthClient: LoginRequestMixin: Server is running on mojang");
                YGGDRASIL_MINECRAFT_SESSION_SERVICE_JOIN_URL.set(MinecraftClient.getInstance().getSessionService(), new URL("https://sessionserver.mojang.com/session/minecraft/join"));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
