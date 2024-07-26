package org.jetlinks.core.server.session;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.jetlinks.core.device.DeviceConfigKey;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.codec.Transport;
import reactor.core.publisher.Mono;

import java.time.Duration;

class KeepOnlineDeviceSessionProvider implements DeviceSessionProvider {

    static final String ID = "keep_online";

    static final KeepOnlineDeviceSessionProvider INSTANCE = new KeepOnlineDeviceSessionProvider();

    static {
        DeviceSessionProviders.register(INSTANCE);
    }

    public static void load() {

    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Mono<PersistentSession> deserialize(byte[] sessionData, DeviceRegistry registry) {
        JSONObject data = JSON.parseObject(sessionData, JSONObject.class);
        String deviceId = data.getString("deviceId");
        return registry
            .getDevice(deviceId)
            .flatMap(device -> {
                String id = data.getString("id");
                String transport = data.getString("transport");
                long timeout = data.getLongValue("timeout");
                long lstTime = data.getLongValue("lstTime");
                //子设备
                boolean children = data.getBooleanValue("children");

                Mono<DeviceSession> sessionMono;

                String parentProvider = data.getString("parentProvider");
                byte[] parent = data.getBytes("parent");
                //尝试反序列化父设备会话
                if (parentProvider != null && parent != null) {
                    sessionMono = Mono
                        .justOrEmpty(DeviceSessionProvider.lookup(parentProvider))
                        .flatMap(provider -> provider.deserialize(parent, registry));
                }
                //其他会话则认为已丢失
                else {
                    sessionMono = Mono.empty();
                }

                //子设备会话
                if (children) {
                    sessionMono = sessionMono
                        .flatMap(session -> device
                            .getSelfConfig(DeviceConfigKey.parentGatewayId)
                            .flatMap(registry::getDevice)
                            .map(parentDevice -> new ChildrenDeviceSession(id, session, parentDevice)));
                }

                return sessionMono
                    .switchIfEmpty(Mono.fromSupplier(() -> new LostDeviceSession(id, device, Transport.of(transport))))
                    .map(_parent -> {
                        KeepOnlineSession session = new KeepOnlineSession(_parent, Duration.ofMillis(timeout));
                        session.setLastKeepAliveTime(lstTime);
                        session.setIgnoreParent(data.getBooleanValue("ignoreParent"));
                        return session;
                    });
            });
    }

    @Override
    public Mono<byte[]> serialize(PersistentSession session, DeviceRegistry registry) {
        KeepOnlineSession keepOnlineSession = session.unwrap(KeepOnlineSession.class);
        JSONObject data = new JSONObject();
        data.put("id", session.getId());
        data.put("deviceId", session.getDeviceId());
        data.put("timeout", session.getKeepAliveTimeout().toMillis());
        data.put("lstTime", session.lastPingTime());
        data.put("transport", session.getTransport().getId());
        data.put("ignoreParent", keepOnlineSession.isIgnoreParent());
        data.put("children", session.isWrapFrom(ChildrenDeviceSession.class));
        DeviceSession parent = keepOnlineSession.getParent();

        //父设备会话也是可序列化的
        if (parent.isWrapFrom(PersistentSession.class)) {
            PersistentSession persistentSession = parent.unwrap(PersistentSession.class);
            return Mono
                .justOrEmpty(DeviceSessionProvider.lookup(persistentSession.getProvider()))
                .flatMap(provider -> provider
                    .serialize(persistentSession, registry)
                    .doOnNext(bytes -> {
                        data.put("parentProvider", provider.getId());
                        data.put("parent", bytes);
                    }))
                .then(Mono.fromCallable(() -> JSON.toJSONBytes(data)));
        }

        return Mono.just(JSON.toJSONBytes(data));
    }
}
