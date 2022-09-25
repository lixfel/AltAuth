// SPDX-License-Identifier: MIT

package de.chaoscaot.altauth.fabric.mixin;

import de.chaoscaot.altauth.fabric.ServerConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.login.LoginHelloS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Environment(EnvType.SERVER)
@Mixin(LoginHelloS2CPacket.class)
public class ServerIdMixin {

    @Redirect(method = "write", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/PacketByteBuf;writeString(Ljava/lang/String;)Lnet/minecraft/network/PacketByteBuf;"))
    public PacketByteBuf writeString(PacketByteBuf packetByteBuf, String string) {
        return packetByteBuf.writeString(ServerConfig.INSTANCE.serverUrl);
    }
}
