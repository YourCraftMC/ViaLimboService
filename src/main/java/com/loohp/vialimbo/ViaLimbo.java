package com.loohp.vialimbo;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.loohp.limbo.Limbo;
import com.loohp.limbo.events.Listener;
import com.loohp.limbo.events.connection.ConnectionEstablishedEvent;
import com.loohp.limbo.file.ServerProperties;
import com.loohp.limbo.network.Channel;
import com.loohp.limbo.network.ClientConnection;
import com.loohp.limbo.plugins.LimboPlugin;
import com.loohp.limbo.utils.DataTypeIO;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.additionalclassprovider.GuavaClassPathProvider;
import net.lenni0451.classtransform.mixinstranslator.MixinsTranslator;
import net.lenni0451.classtransform.utils.tree.IClassProvider;
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
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.filter.AbstractFilter;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;

public class ViaLimbo extends LimboPlugin implements Listener {

    @Override
    public void onEnable() {
        try {
            ServerProperties serverProperties = Limbo.getInstance().getServerProperties();
            String ip = serverProperties.getServerIp();
            int port = serverProperties.getServerPort();
            boolean bungeecord = serverProperties.isBungeecord() || serverProperties.isBungeeGuard();

            Field portField = ServerProperties.class.getDeclaredField("serverPort");
            portField.setAccessible(true);
            portField.setInt(serverProperties, 0);
            Field ipField = ServerProperties.class.getDeclaredField("serverIp");
            ipField.setAccessible(true);
            ipField.set(serverProperties, "127.0.0.1");

            Limbo.getInstance().getEventsManager().registerEvents(this, new ViaLimboListener());

            Limbo.getInstance().getScheduler().runTask(this, () -> {
                String minecraftVersion = Limbo.getInstance().SERVER_IMPLEMENTATION_VERSION;
                int limboPort = Limbo.getInstance().getServerConnection().getServerSocket().getLocalPort();
                startViaProxy(ip, port, minecraftVersion, limboPort, bungeecord);
            });
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

            org.apache.logging.log4j.Logger logger = LogManager.getRootLogger();
            AbstractFilter filter = new AbstractFilter() {
                @Override
                public Filter.Result filter(LogEvent event) {
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

            ViaProxyConfig config = configLoader.load(ConfigProvider.memory("", s -> {})).getConfigInstance();
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

        @net.lenni0451.lambdaevents.EventHandler
        public void onProxy2ServerChannelInitialize(Proxy2ServerChannelInitializeEvent event) {
            io.netty.channel.Channel channel = event.getChannel();
            if (initializedChannels.add(channel)) {
                ProxyConnection proxyConnection = ProxyConnection.fromChannel(channel);
                SocketAddress socketAddress = proxyConnection.getC2P().remoteAddress();
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
                try {
                    if (socketAddress instanceof InetSocketAddress) {
                        dataOutputStream.writeBoolean(true);
                        DataTypeIO.writeString(dataOutputStream, ((InetSocketAddress) socketAddress).getAddress().getHostAddress(), StandardCharsets.UTF_8);
                    } else {
                        dataOutputStream.writeBoolean(false);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                channel.unsafe().write(channel.alloc().buffer().writeBytes(byteArrayOutputStream.toByteArray()), channel.voidPromise());
            }
        }

    }

    public static class ViaLimboListener implements Listener {

        @com.loohp.limbo.events.EventHandler
        public void onConnectionEstablished(ConnectionEstablishedEvent event) {
            try {
                Field inputField = Channel.class.getDeclaredField("input");
                inputField.setAccessible(true);
                DataInputStream inputStream = (DataInputStream) inputField.get(event.getConnection().getChannel());
                if (inputStream.readBoolean()) {
                    String host = DataTypeIO.readString(inputStream, StandardCharsets.UTF_8);
                    Field addressField = ClientConnection.class.getDeclaredField("inetAddress");
                    addressField.setAccessible(true);
                    addressField.set(event.getConnection(), InetAddress.getByName(host));
                }
            } catch (NoSuchFieldException | IllegalAccessException | IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

}
