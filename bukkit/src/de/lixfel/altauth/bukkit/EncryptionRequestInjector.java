// SPDX-License-Identifier: MIT

package de.lixfel.altauth.bukkit;

import de.lixfel.tinyprotocol.Reflection;
import de.lixfel.tinyprotocol.TinyProtocol;
import org.bukkit.entity.Player;

public class EncryptionRequestInjector {

    private static final Class<?> EncryptionRequest = Reflection.getClass("{nms.network.protocol.login}.PacketLoginOutEncryptionBegin");
    private static final Reflection.FieldAccessor<String> ServerID = Reflection.getField(EncryptionRequest, String.class, 0);

    private final String altAuthServer;

    public EncryptionRequestInjector(String altAuthServer) {
        this.altAuthServer = altAuthServer;

        TinyProtocol.instance.addFilter(EncryptionRequest, this::handleEncryptionRequest);
    }

    public Object handleEncryptionRequest(Player player, Object packet) {
        ServerID.set(packet, altAuthServer);
        TinyProtocol.instance.getInterceptor(player).ifPresent(TinyProtocol.PacketInterceptor::close);
        return packet;
    }

    public void remove() {
        TinyProtocol.instance.removeFilter(EncryptionRequest, this::handleEncryptionRequest);
    }
}
