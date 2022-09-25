package de.chaoscaot.altauth.fabric.mixin;

import de.chaoscaot.altauth.fabric.AltAuth;
import de.chaoscaot.altauth.fabric.TrustServerScreen;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ConnectScreen.class)
public class ConnectScreenMixin {

    @ModifyArg(method = "connect(Lnet/minecraft/client/gui/screen/Screen;Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/network/ServerAddress;Lnet/minecraft/client/network/ServerInfo;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ConnectScreen;connect(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/network/ServerAddress;)V"), index = 1)
    private static ServerAddress copyServerAddress(ServerAddress address) {
        AltAuth.address = address;
        return address;
    }
}
