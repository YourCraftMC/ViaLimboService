package com.loohp.vialimbo;

import cn.ycraft.limbo.config.ServerConfig;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.loohp.limbo.Limbo;
import com.loohp.limbo.events.Listener;
import com.loohp.limbo.plugins.LimboPlugin;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.additionalclassprovider.GuavaClassPathProvider;
import net.lenni0451.classtransform.mixinstranslator.MixinsTranslator;
import net.lenni0451.classtransform.utils.tree.IClassProvider;
import net.lenni0451.lambdaevents.EventHandler;
import net.lenni0451.optconfig.ConfigLoader;
import net.lenni0451.optconfig.provider.ConfigProvider;
import net.lenni0451.reflect.Agents;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.plugins.events.Proxy2ServerChannelInitializeEvent;
import net.raphimc.viaproxy.protocoltranslator.ProtocolTranslator;
import net.raphimc.viaproxy.protocoltranslator.viaproxy.ViaProxyConfig;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;
import net.raphimc.viaproxy.util.ClassLoaderPriorityUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.geysermc.mcprotocollib.network.server.NetworkServer;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftTypes;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.Set;

public class ViaLimbo extends LimboPlugin implements Listener {

    @Override
    public void onEnable() {
        try {
            String ip = ServerConfig.SERVER_IP.getNotNull();
            int port = ServerConfig.SERVER_PORT.getNotNull();
            boolean bungeecord = ServerConfig.BUNGEECORD.getNotNull() || ServerConfig.BUNGEE_GUARD.getNotNull();

            Limbo.getInstance().setBindIp("127.0.0.1");
            Limbo.getInstance().setBindPort(0);


            Limbo.getInstance().getScheduler().runTaskLater(this, () -> {
                String minecraftVersion = Limbo.getInstance().SERVER_IMPLEMENTATION_VERSION;
                NetworkServer server = Limbo.getInstance().getServerConnection().getServer();
                try {
                    Field channelField = NetworkServer.class.getDeclaredField("channel");
                    channelField.setAccessible(true);
                    io.netty.channel.Channel channel = (io.netty.channel.Channel) channelField.get(server);
                    int limboPort = ((InetSocketAddress) channel.localAddress()).getPort();
                    startViaProxy(ip, port, minecraftVersion, limboPort, bungeecord);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }, 2);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDisable() {
        stopViaProxy();
    }

    private void startViaProxy(String ip, int port, String minecraftVersion, int limboPort, boolean bungeecord) {
        try {
            Limbo.getInstance().getConsole().sendMessage("[ViaLimbo] Initializing ViaProxy " + ViaProxy.VERSION + " (" + ViaProxy.IMPL_VERSION + ")");

            Logger logger = LogManager.getRootLogger();
            AbstractFilter filter = new AbstractFilter() {
                @Override
                public Result filter(LogEvent event) {
                    String logger = event.getLoggerName();
                    if (!logger.contains("Via")) {
                        return Result.NEUTRAL;
                    }
                    if (!logger.equals("ViaProxy")) {
                        Limbo.getInstance().getConsole().sendMessage("[ViaLimbo] (" + logger + ") " + event.getMessage().getFormattedMessage());
                    }
                    return Result.DENY;
                }
            };
            Method addFilterMethod = logger.getClass().getMethod("addFilter", Filter.class);
            addFilterMethod.invoke(logger, filter);

            IClassProvider classProvider = new GuavaClassPathProvider();
            TransformerManager transformerManager = new TransformerManager(classProvider);
            transformerManager.addTransformerPreprocessor(new MixinsTranslator());
            transformerManager.addTransformer("net.raphimc.viaproxy.injection.mixins.**");
            transformerManager.hookInstrumentation(Agents.getInstrumentation());

            ConfigLoader<ViaProxyConfig> configLoader = new ConfigLoader<>(ViaProxyConfig.class);
            configLoader.getConfigOptions().setResetInvalidOptions(true).setRewriteConfig(true).setCommentSpacing(1);

            getDataFolder().mkdirs();
            Field cwdField = ViaProxy.class.getDeclaredField("CWD");
            cwdField.setAccessible(true);
            cwdField.set(null, getDataFolder());

            ViaProxyConfig config = configLoader.load(ConfigProvider.memory("", s -> {
            })).getConfigInstance();
            Field configField = ViaProxy.class.getDeclaredField("CONFIG");
            configField.setAccessible(true);
            configField.set(null, config);

            config.setBindAddress(new InetSocketAddress(ip, port));
            config.setTargetAddress(new InetSocketAddress("127.0.0.1", limboPort));
            config.setTargetVersion(ProtocolVersion.getClosest(minecraftVersion));
            config.setPassthroughBungeecordPlayerInfo(bungeecord);
            config.setAllowLegacyClientPassthrough(true);

            ViaProxy.EVENT_MANAGER.register(new ViaProxyListener());

            Method loadNettyMethod = ViaProxy.class.getDeclaredMethod("loadNetty");
            loadNettyMethod.setAccessible(true);
            loadNettyMethod.invoke(null);
            ClassLoaderPriorityUtil.loadOverridingJars();
            ProtocolTranslator.init();
            ViaProxy.startProxy();

            Limbo.getInstance().getConsole().sendMessage("[ViaLimbo] ViaProxy listening on /" + ip + ":" + port);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void stopViaProxy() {
        ViaProxy.stopProxy();
        Limbo.getInstance().getConsole().sendMessage("[ViaLimbo] ViaProxy Shutdown");
    }

    public static class ViaProxyListener {

        private final Set<io.netty.channel.Channel> initializedChannels;

        public ViaProxyListener() {
            Cache<io.netty.channel.Channel, Boolean> cache = CacheBuilder.newBuilder().weakKeys().build();
            this.initializedChannels = Collections.newSetFromMap(cache.asMap());
        }

        @EventHandler
        public void onProxy2ServerChannelInitialize(Proxy2ServerChannelInitializeEvent event) {
            io.netty.channel.Channel channel = event.getChannel();
            if (initializedChannels.add(channel)) {
                ProxyConnection proxyConnection = ProxyConnection.fromChannel(channel);
                SocketAddress socketAddress = proxyConnection.getC2P().remoteAddress();
                ByteBuf buf = channel.alloc().buffer();
                if (socketAddress instanceof InetSocketAddress) {
                    buf.writeBoolean(true);
                    MinecraftTypes.writeString(buf, ((InetSocketAddress) socketAddress).getAddress().getHostAddress());
                } else {
                    buf.writeBoolean(false);
                }
                channel.writeAndFlush(buf);
            }
        }
    }
}
