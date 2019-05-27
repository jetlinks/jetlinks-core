package org.jetlinks.core.support;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.Unpooled;
import lombok.AllArgsConstructor;
import org.jetlinks.core.message.*;
import org.jetlinks.core.message.codec.*;
import org.jetlinks.core.message.event.ChildDeviceOfflineMessage;
import org.jetlinks.core.message.event.ChildDeviceOnlineMessage;
import org.jetlinks.core.message.event.EventMessage;
import org.jetlinks.core.message.function.FunctionInvokeMessage;
import org.jetlinks.core.message.function.FunctionInvokeMessageReply;
import org.jetlinks.core.message.property.ReadPropertyMessage;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.jetlinks.core.message.codec.TransportDeviceMessageCodec;
import org.jetlinks.core.message.property.WritePropertyMessage;
import org.jetlinks.core.message.property.WritePropertyMessageReply;

/**
 * 基于jet links 的消息编解码器
 *
 * @author zhouhao
 * @since 1.0.0
 */
public class JetLinkMQTTDeviceMessageCodec implements TransportDeviceMessageCodec {
    @Override
    public Transport getSupportTransport() {
        return Transport.MQTT;
    }

    @AllArgsConstructor
    private class EncodeResult {
        private String     topic;
        private JSONObject data;
    }

    private class DecodeResult {
        public DecodeResult(String topic) {
            this.topic = topic;
        }

        private String                   topic;
        private CommonDeviceMessageReply message;
    }

    protected EncodeResult encode(DeviceMessage message) {
        if (message instanceof ReadPropertyMessage) {
            String topic = "/read-property";
            JSONObject mqttData = new JSONObject();
            mqttData.put("messageId", message.getMessageId());
            mqttData.put("properties", ((ReadPropertyMessage) message).getPropertyIds());
            return new EncodeResult(topic, mqttData);
        } else if (message instanceof WritePropertyMessage) {
            String topic = "/write-property";
            JSONObject mqttData = new JSONObject();
            mqttData.put("messageId", message.getMessageId());
            mqttData.put("properties", ((WritePropertyMessage) message).getProperties());
            return new EncodeResult(topic, mqttData);
        } else if (message instanceof FunctionInvokeMessage) {
            String topic = "/invoke-function";
            FunctionInvokeMessage invokeMessage = ((FunctionInvokeMessage) message);
            JSONObject mqttData = new JSONObject();
            mqttData.put("messageId", message.getMessageId());
            mqttData.put("function", invokeMessage.getFunctionId());
            mqttData.put("args", invokeMessage.getInputs());
            return new EncodeResult(topic, mqttData);
        } else if (message instanceof ChildDeviceMessage) {
            String topic = "/child-device-message";
            ChildDeviceMessage childDeviceMessage = ((ChildDeviceMessage) message);
            JSONObject mqttData = new JSONObject();
            EncodeResult result = encode(childDeviceMessage.getChildDeviceMessage());
            mqttData.put("messageId", message.getMessageId());
            mqttData.put("childDeviceId", childDeviceMessage.getChildDeviceId());
            result.data.put("clientId", childDeviceMessage.getChildDeviceId());
            mqttData.put("childMessage", result.data);
            mqttData.put("childTopic", result.topic);
            return new EncodeResult(topic, mqttData);
        }
        throw new UnsupportedOperationException("不支持的消息类型:" + message.getClass());
    }

    protected DecodeResult decode(String topic, JSONObject object) {
        DecodeResult result = new DecodeResult(topic);
        switch (topic) {
            case "/read-property-reply":
                result.message = object.toJavaObject(ReadPropertyMessageReply.class);
                break;
            case "/write-property-reply":
                result.message = object.toJavaObject(WritePropertyMessageReply.class);
                break;
            case "/child-device-connect":
                result.message = object.toJavaObject(ChildDeviceOnlineMessage.class);
                break;
            case "/child-device-disconnect":
                result.message = object.toJavaObject(ChildDeviceOfflineMessage.class);
                break;
            case "/invoke-function-reply":
                result.message = object.toJavaObject(FunctionInvokeMessageReply.class);
                break;
            case "/event":
                result.message = object.toJavaObject(EventMessage.class);
                break;
            case "/child-device-message":
                String childTopic = object.getString("topic");
                JSONObject message = object.getJSONObject("message");
                result.message = decode(childTopic, message).message;
                break;
            default:
                throw new UnsupportedOperationException("不支持的topic:" + topic);
        }
        return result;
    }

    @Override
    public EncodedMessage encode(MessageEncodeContext context) {
        DeviceMessage message = context.getMessage();
        //读取设备属性
        EncodeResult convertResult = encode(message);

        return EncodedMessage.mqtt(message.getDeviceId(),
                convertResult.topic,
                Unpooled.copiedBuffer(JSON.toJSONBytes(convertResult.data)));
    }


    @Override
    public DeviceMessage decode(MessageDecodeContext context) {
        MqttMessage message = (MqttMessage) context.getMessage();
        String topic = message.getTopic();
        CommonDeviceMessageReply reply = decode(topic, JSON.parseObject(message.getByteBuf().array(), JSONObject.class)).message;
        if (reply.getDeviceId() == null || reply.getDeviceId().isEmpty()) {
            reply.setDeviceId(message.getDeviceId());
        }
        return reply;
    }
}
