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
                    boolean children = data.getBooleanValue("children");

                    Mono<DeviceSession> sessionMono = Mono.just(new LostDeviceSession(
                            id,
                            device,
                            Transport.of(transport)));
                    //子设备会话
                    if (children) {
                        sessionMono = device
                                .getSelfConfig(DeviceConfigKey.parentGatewayId)
                                .flatMap(registry::getDevice)
                                .map(parentDevice -> new ChildrenDeviceSession(
                                        id,
                                        new LostDeviceSession(id,
                                                              parentDevice,
                                                              Transport.of(transport)),
                                        parentDevice));
                    }

                    return sessionMono
                            .map(parent -> {
                                KeepOnlineSession session = new KeepOnlineSession(parent, Duration.ofMillis(timeout));
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
        return Mono.just(JSON.toJSONBytes(data));
    }
}
