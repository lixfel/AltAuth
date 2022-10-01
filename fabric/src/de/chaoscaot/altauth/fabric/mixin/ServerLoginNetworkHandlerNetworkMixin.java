package de.chaoscaot.altauth.fabric.mixin;

import de.chaoscaot.altauth.fabric.config.ServerConfig;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ServerLoginNetworkHandler.class)
public class ServerLoginNetworkHandlerNetworkMixin {

    @ModifyArg(method = "onKey", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/encryption/NetworkEncryptionUtils;computeServerId(Ljava/lang/String;Ljava/security/PublicKey;Ljavax/crypto/SecretKey;)[B"), index =  0)
    private String onKey(String serverId) {
        return ServerConfig.INSTANCE.serverUrl;
    }
}
