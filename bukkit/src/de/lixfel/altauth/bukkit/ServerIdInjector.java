// SPDX-License-Identifier: MIT

package de.lixfel.altauth.bukkit;

import com.mojang.authlib.GameProfile;
import de.lixfel.ReflectionUtil;
import de.lixfel.ReflectionUtil.FieldWrapper;
import io.netty.channel.*;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// ServerIdInjector heavily inspired by TinyProtocol
public class ServerIdInjector {

	private static final Class<?> craftServer = ReflectionUtil.getClass("org.bukkit.craftbukkit.CraftServer");
	private static final Class<?> dedicatedPlayerList = ReflectionUtil.getClass("net.minecraft.server.dedicated.DedicatedPlayerList");
	private static final FieldWrapper<?> getPlayerList = ReflectionUtil.getField(craftServer, dedicatedPlayerList, 0);
	private static final Class<?> playerList = ReflectionUtil.getClass("net.minecraft.server.players.PlayerList");
	public static final Class<?> minecraftServer = ReflectionUtil.getClass("net.minecraft.server.MinecraftServer");
	private static final FieldWrapper<?> getMinecraftServer = ReflectionUtil.getField(playerList, minecraftServer, 0);
	private static final Class<?> serverConnection = ReflectionUtil.getClass("net.minecraft.server.network.ServerConnection");
	private static final FieldWrapper<?> getServerConnection = ReflectionUtil.getField(minecraftServer, serverConnection, 0);
	private static final FieldWrapper<List> getChannelFutures = ReflectionUtil.getField(serverConnection, List.class, 0, ChannelFuture.class);

	private static final Class networkManager = ReflectionUtil.getClass("net.minecraft.network.NetworkManager");
	private static final Class<?> packetListener = ReflectionUtil.getClass("net.minecraft.network.PacketListener");
	private static final FieldWrapper<?> getPacketListener = ReflectionUtil.getField(networkManager, packetListener, 0);

	public static final Class<?> loginListener = ReflectionUtil.getClass("net.minecraft.server.network.LoginListener");
	private static final FieldWrapper<GameProfile> getGameProfile = ReflectionUtil.getField(loginListener, GameProfile.class, 0);

	private static final Class<?> packetLoginInEncryptionBegin = ReflectionUtil.getClass("net.minecraft.network.protocol.login.PacketLoginInEncryptionBegin");
	private static final FieldWrapper<byte[]> getEncryptedSecret = ReflectionUtil.getField(packetLoginInEncryptionBegin, byte[].class, 0);
	private static final Class<?> packetLoginOutEncryptionBegin = ReflectionUtil.getClass("net.minecraft.network.protocol.login.PacketLoginOutEncryptionBegin");
	private static final FieldWrapper<String> getPacketServerID = ReflectionUtil.getField(packetLoginOutEncryptionBegin, String.class, 0);

	private final String altAuthServer;
	private final String handlerName;
	private final String initializerName;
	private final Object minecraftServerInstance;
	private final List<ChannelFuture> channelFutures;

	private final Map<String, byte[]> encryptedSecrets = new ConcurrentHashMap<>();

	private boolean closed;

	private final ChannelInitializer<Channel> initializer = new ChannelInitializer<Channel>() {
		@Override
		protected void initChannel(Channel channel) {
			if(!closed)
				new PacketInterceptor(channel);
		}
	};

	public ServerIdInjector(Plugin plugin, String altAuthServer) {
		this.altAuthServer = altAuthServer;
		this.handlerName = "altauth";
		this.initializerName = "altauth-init";
		this.minecraftServerInstance = getMinecraftServer.get(getPlayerList.get(plugin.getServer()));
		this.channelFutures = getChannelFutures.get(getServerConnection.get(minecraftServerInstance));

		for(ChannelFuture serverChannel : channelFutures) {
			serverChannel.channel().pipeline().addFirst(initializerName, new ChannelInboundHandlerAdapter() {
				@Override
				public void channelRead(ChannelHandlerContext ctx, Object o) throws Exception {
					super.channelRead(ctx, o);

					Channel channel = (Channel) o;
					channel.pipeline().addLast(initializerName, initializer);
				}
			});
		}
	}

	public Object minecraftServerInstance() {
		return minecraftServerInstance;
	}

	public byte[] getEncryptedSecret(String name) {
		return encryptedSecrets.remove(name);
	}

	public final void close() {
		if(closed)
			return;
		closed = true;

		for(ChannelFuture serverChannel : channelFutures) {
			serverChannel.channel().pipeline().remove(initializerName);
		}
	}

	public final class PacketInterceptor extends ChannelDuplexHandler {
		private final Channel channel;

		private PacketInterceptor(Channel channel) {
			this.channel = channel;
			channel.pipeline().addBefore("packet_handler", handlerName, this);
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object packet) throws Exception {
			if(packetLoginInEncryptionBegin.isInstance(packet)) {
				encryptedSecrets.put(
						getGameProfile.get(getPacketListener.get(channel.pipeline().get(networkManager))).getName(),
						getEncryptedSecret.get(packet)
				);
				channel.pipeline().remove(this);
			}

			super.channelRead(ctx, packet);
		}

		@Override
		public void write(ChannelHandlerContext ctx, Object packet, ChannelPromise promise) throws Exception {
			if(packetLoginOutEncryptionBegin.isInstance(packet)) {
				getPacketServerID.set(packet, altAuthServer);
			}

			super.write(ctx, packet, promise);
		}
	}
}
