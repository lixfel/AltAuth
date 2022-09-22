// SPDX-License-Identifier: MIT

package de.lixfel.altauth.bukkit;

import de.lixfel.ReflectionUtil;
import org.bukkit.entity.Player;

public class EncryptionRequestInjector {

    private static final Class<?> EncryptionRequest = ReflectionUtil.getClass("net.minecraft.network.protocol.login.PacketLoginOutEncryptionBegin");
    private static final ReflectionUtil.FieldWrapper<String> ServerID = ReflectionUtil.getField(EncryptionRequest, String.class, 0);

    private final String altAuthServer;

    public EncryptionRequestInjector(String altAuthServer) {
        this.altAuthServer = altAuthServer;

        ProtocolInjector.instance.addFilter(EncryptionRequest, this::handleEncryptionRequest);
    }

    public Object handleEncryptionRequest(Player player, Object packet) {
        ServerID.set(packet, altAuthServer);
        ProtocolInjector.instance.getInterceptor(player).ifPresent(ProtocolInjector.PacketInterceptor::close);
        return packet;
    }

    public void remove() {
        ProtocolInjector.instance.removeFilter(EncryptionRequest, this::handleEncryptionRequest);
    }
}
