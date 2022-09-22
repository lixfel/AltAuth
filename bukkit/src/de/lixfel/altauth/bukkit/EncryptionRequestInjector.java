/*
 *    This file is part of AltAuth.
 *
 *    Copyright (C) 2022  Lixfel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *    GNU Affero General Public License for more details.
 *
 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
