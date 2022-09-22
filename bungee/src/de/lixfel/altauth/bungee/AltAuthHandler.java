// SPDX-License-Identifier: MIT

package de.lixfel.altauth.bungee;

import de.lixfel.tinyprotocol.Reflection;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.EncryptionUtil;
import net.md_5.bungee.Util;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.connection.LoginResult;
import net.md_5.bungee.http.HttpClient;
import net.md_5.bungee.jni.cipher.BungeeCipher;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.PipelineUtils;
import net.md_5.bungee.netty.cipher.CipherDecoder;
import net.md_5.bungee.netty.cipher.CipherEncoder;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.PacketWrapper;
import net.md_5.bungee.protocol.packet.EncryptionRequest;
import net.md_5.bungee.protocol.packet.EncryptionResponse;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class AltAuthHandler extends MessageToMessageCodec<PacketWrapper, PacketWrapper> {

    private static final Class<?> InitialHandler = Reflection.getClass("net.md_5.bungee.connection.InitialHandler");
    private static final Reflection.FieldAccessor<ChannelWrapper> CH = Reflection.getField(InitialHandler, ChannelWrapper.class, 0);
    private static final Reflection.FieldAccessor<LoginResult> LoginProfile = Reflection.getField(InitialHandler, "loginProfile", LoginResult.class);
    private static final Reflection.FieldAccessor<String> Name = Reflection.getField(InitialHandler, "name", String.class);
    private static final Reflection.FieldAccessor<UUID> UniqueId = Reflection.getField(InitialHandler, "uniqueId", UUID.class);
    private static final Reflection.MethodInvoker Finish = Reflection.getMethod(InitialHandler, "finish");

    private final ProxyServer bungee;
    private final String altAuthUrl;
    private final PendingConnection connection;

    public AltAuthHandler(ProxyServer bungee, PendingConnection connection, String altAuthUrl) {
        this.bungee = bungee;
        this.connection = connection;
        this.altAuthUrl = altAuthUrl;

        ChannelWrapper ch = CH.get(connection);
        ch.addBefore(PipelineUtils.BOSS_HANDLER, "altauth", this);
        //ch.addBefore(PipelineUtils.PACKET_ENCODER, "altauth", this);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, PacketWrapper packetWrapper, List<Object> out) {
        DefinedPacket packet = packetWrapper.packet;

        if(packet instanceof EncryptionRequest)
            ((EncryptionRequest)packet).setServerId(altAuthUrl);

        out.add(packetWrapper);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void decode(ChannelHandlerContext ctx, PacketWrapper packetWrapper, List<Object> out) throws Exception {
        DefinedPacket packet = packetWrapper.packet;
        if(!(packet instanceof EncryptionResponse)) {
            out.add(packetWrapper);
            return;
        }

        ChannelWrapper ch = CH.get(connection);
        ch.getHandle().pipeline().remove(this);

        SecretKey sharedKey = EncryptionUtil.getSecret((EncryptionResponse) packet, null);
        if (sharedKey instanceof SecretKeySpec && sharedKey.getEncoded().length != 16) {
            ch.close();
            return;
        }

        BungeeCipher decrypt = EncryptionUtil.getCipher(false, sharedKey);
        ch.addBefore("frame-decoder", "decrypt", new CipherDecoder(decrypt));
        BungeeCipher encrypt = EncryptionUtil.getCipher(true, sharedKey);
        ch.addBefore("frame-prepender", "encrypt", new CipherEncoder(encrypt));
        String encName = URLEncoder.encode(connection.getName(), "UTF-8");
        MessageDigest sha = MessageDigest.getInstance("SHA-1");

        sha.update(altAuthUrl.getBytes(StandardCharsets.ISO_8859_1));
        sha.update(sharedKey.getEncoded());
        sha.update(EncryptionUtil.keys.getPublic().getEncoded());

        String encodedHash = URLEncoder.encode(new BigInteger(sha.digest()).toString(16), "UTF-8");
        String preventProxy = BungeeCord.getInstance().config.isPreventProxyConnections() && connection.getSocketAddress() instanceof InetSocketAddress ? "&ip=" + URLEncoder.encode(connection.getAddress().getAddress().getHostAddress(), "UTF-8") : "";
        String authURL = String.format("https://%s/session/minecraft/hasJoined?username=%s&serverId=%s%s", altAuthUrl, encName, encodedHash, preventProxy);
        Callback<String> handler = (result, error) -> {
            if (error != null) {
                connection.disconnect(bungee.getTranslation("mojang_fail"));
                bungee.getLogger().log(Level.SEVERE, error, () -> "Error authenticating " + connection.getName() + " with minecraft.net");
                return;
            }

            LoginResult loginResult = BungeeCord.getInstance().gson.fromJson(result, LoginResult.class);
            if (loginResult == null || loginResult.getId() == null) {
                connection.disconnect(bungee.getTranslation("offline_mode_player"));
                return;
            }

            LoginProfile.set(connection, loginResult);
            Name.set(connection, loginResult.getName());
            UniqueId.set(connection, Util.getUUID(loginResult.getId()));
            Finish.invoke(connection);
        };
        HttpClient.get(authURL, ch.getHandle().eventLoop(), handler);
    }
}
