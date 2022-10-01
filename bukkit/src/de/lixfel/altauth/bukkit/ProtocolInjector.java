// SPDX-License-Identifier: MIT

package de.lixfel.altauth.bukkit;

import de.lixfel.ReflectionUtil;
import de.lixfel.ReflectionUtil.FieldWrapper;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.function.BiFunction;
import java.util.logging.Level;

// ProtocolInjector heavily inspired by TinyProtocol
public class ProtocolInjector implements Listener {

	private static final Class<?> craftServer = ReflectionUtil.getClass("org.bukkit.craftbukkit.CraftServer");
	private static final Class<?> dedicatedPlayerList = ReflectionUtil.getClass("net.minecraft.server.dedicated.DedicatedPlayerList");
	private static final FieldWrapper<?> getPlayerList = ReflectionUtil.getField(craftServer, dedicatedPlayerList, 0);
	private static final Class<?> playerList = ReflectionUtil.getClass("net.minecraft.server.players.PlayerList");
	public static final Class<?> minecraftServer = ReflectionUtil.getClass("net.minecraft.server.MinecraftServer");
	private static final FieldWrapper<?> getMinecraftServer = ReflectionUtil.getField(playerList, minecraftServer, 0);
	private static final Class<?> serverConnection = ReflectionUtil.getClass("net.minecraft.server.network.ServerConnection");
	private static final FieldWrapper<?> getServerConnection = ReflectionUtil.getField(minecraftServer, serverConnection, 0);
	private static final Class<?> networkManager = ReflectionUtil.getClass("net.minecraft.network.NetworkManager");
	private static final FieldWrapper<List> getConnections = ReflectionUtil.getField(serverConnection, List.class, 0, networkManager);

	public static final ProtocolInjector instance = new ProtocolInjector(AltAuthBukkit.getInstance());

	public static void init() {
		//enforce init
	}

	private final Plugin plugin;
	private final String handlerName;
	private final Object minecraftServerInstance;
	private final List<?> connections;
	private boolean closed;

	private final Map<Class<?>, List<BiFunction<Player, Object, Object>>> packetFilters = new HashMap<>();
	private final Map<Player, PacketInterceptor> playerInterceptors = new HashMap<>();

	private ProtocolInjector(final Plugin plugin) {
		this.plugin = plugin;
		this.handlerName = "altauth";
		this.minecraftServerInstance = getMinecraftServer.get(getPlayerList.get(plugin.getServer()));
		this.connections = getConnections.get(getServerConnection.get(minecraftServerInstance));

		plugin.getServer().getPluginManager().registerEvents(this, plugin);

		for (Player player : plugin.getServer().getOnlinePlayers()) {
			new PacketInterceptor(player);
		}
	}

	public Object minecraftServer() {
		return minecraftServerInstance;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerLogin(PlayerLoginEvent e) {
		if(closed)
			return;
		new PacketInterceptor(e.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerDisconnect(PlayerQuitEvent e) {
		getInterceptor(e.getPlayer()).ifPresent(PacketInterceptor::close);
	}

	@EventHandler
	public void onPluginDisable(PluginDisableEvent e) {
		if (e.getPlugin().equals(plugin)) {
			close();
		}
	}

	public void addFilter(Class<?> packetType, BiFunction<Player, Object, Object> filter) {
		packetFilters.computeIfAbsent(packetType, c -> new ArrayList<>(1)).add(filter);
	}

	public void removeFilter(Class<?> packetType, BiFunction<Player, Object, Object> filter) {
		packetFilters.getOrDefault(packetType, Collections.emptyList()).remove(filter);
	}

	public final void close() {
		if(closed)
			return;
		closed = true;

		HandlerList.unregisterAll(this);

		for (Player player : plugin.getServer().getOnlinePlayers()) {
			getInterceptor(player).ifPresent(PacketInterceptor::close);
		}
	}

	public Optional<PacketInterceptor> getInterceptor(Player player) {
		synchronized (playerInterceptors) {
			return Optional.ofNullable(playerInterceptors.get(player));
		}
	}

	private static final FieldWrapper<Channel> getChannel = ReflectionUtil.getField(networkManager, Channel.class, 0);
	private static final FieldWrapper<UUID> getUUID = ReflectionUtil.getField(networkManager, UUID.class, 0);

	public final class PacketInterceptor extends ChannelDuplexHandler {
		private final Player player;
		private final Channel channel;

		private PacketInterceptor(Player player) {
			this.player = player;

			channel = getChannel.get(connections.stream().filter(connection -> player.getUniqueId().equals(getUUID.get(connection))).findAny().orElseThrow(() -> new SecurityException("Could not find channel for player " + player.getName())));

			synchronized (playerInterceptors) {
				playerInterceptors.put(player, this);
			}

			channel.pipeline().addBefore("packet_handler", handlerName, this);
		}

		private void sendPacket(Object packet) {
			channel.pipeline().writeAndFlush(packet);
		}

		private void receivePacket(Object packet) {
			channel.pipeline().context("encoder").fireChannelRead(packet);
		}

		public void close() {
			if(channel.isActive()) {
				channel.eventLoop().execute(() -> {
					try {
						channel.pipeline().remove(handlerName);
					} catch (NoSuchElementException e) {
						// ignore
					}
				});
			}

			synchronized (playerInterceptors) {
				playerInterceptors.remove(player, this);
			}
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			try {
				msg = filterPacket(player, msg);
			} catch (Exception e) {
				plugin.getLogger().log(Level.SEVERE, "Error during incoming packet processing", e);
			}

			if (msg != null) {
				super.channelRead(ctx, msg);
			}
		}

		@Override
		public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
			try {
				msg = filterPacket(player, msg);
			} catch (Exception e) {
				plugin.getLogger().log(Level.SEVERE, "Error during outgoing packet processing", e);
			}

			if (msg != null) {
				super.write(ctx, msg, promise);
			}
		}

		private Object filterPacket(Player player, Object packet) {
			List<BiFunction<Player, Object, Object>> filters = packetFilters.getOrDefault(packet.getClass(), Collections.emptyList());

			for(BiFunction<Player, Object, Object> filter : filters) {
				packet = filter.apply(player, packet);

				if(packet == null)
					break;
			}

			return packet;
		}
	}
}
