package org.jetlinks.core.server.session;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
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
                .map(device -> {
                    String id = data.getString("id");
                    String transport = data.getString("transport");
                    long timeout = data.getLongValue("timeout");
                    long lstTime = data.getLongValue("lstTime");
                    KeepOnlineSession session = new KeepOnlineSession(
                            new LostDeviceSession(id,
                                                  device,
                                                  Transport.of(transport)),
                            Duration.ofMillis(timeout));
                    session.setLastKeepAliveTime(lstTime);
                    return session;
                });
    }

    @Override
    public Mono<byte[]> serialize(PersistentSession session, DeviceRegistry registry) {
        JSONObject data = new JSONObject();
        data.put("id", session.getId());
        data.put("deviceId", session.getDeviceId());
        data.put("timeout", session.getKeepAliveTimeout().toMillis());
        data.put("lstTime", session.lastPingTime());
        data.put("transport", session.getTransport().getId());
        return Mono.just(JSON.toJSONBytes(data));
    }
}
