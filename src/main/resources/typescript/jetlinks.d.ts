// @ts-ignore
import {java} from "java";
// @ts-ignore
import {io} from "netty";

declare module org.jetlinks.core.message {

    enum MessageType {
        /**
         * 设备上报属性
         */
        REPORT_PROPERTY,
        /**
         * 读取属性
         */
        READ_PROPERTY,
        /**
         * 修改属性
         */
        WRITE_PROPERTY,
        /**
         * 读取属性回复
         */
        READ_PROPERTY_REPLY,
        /**
         * 修改属性回复
         */
        WRITE_PROPERTY_REPLY,
        /**
         * 调用功能
         */
        INVOKE_FUNCTION,
        /**
         * 调用功能回复
         */
        INVOKE_FUNCTION_REPLY,
        /**
         * 事件上报
         */
        EVENT

    }

    interface ThingMessage<T extends ThingMessage<T>> {
        getMessageType(): MessageType;

        getThingType(): string;

        getThingId(): string;

        // @ts-ignore
        getTimestamp(): long;

        getHeaders(): java.util.Map<string, any>;

        getHeader(key: string): java.util.Optional<any>;

        addHeader(key: string, value: string): T;

        getMessageId(): string;

        // @ts-ignore
        timestamp(utcTimestamp: long): T;

        messageId(msgId: string): T;
    }

    interface ThingMessageReply<T extends ThingMessageReply<T>> extends ThingMessage<T> {

        isSuccess(): boolean;

        //业务码,具体由设备定义
        getCode(): string;

        //错误消息
        getMessage(): string;

        //设置失败
        error(errorCode: string, message: string);

        //设置失败
        error(err: Error): T;

        //设置物类型和物ID
        thingId(type: string, thingId: string): T;

        //设置成功
        success(): T;

        success(success: boolean): T;

        //设置业务码
        code(code: string): T;

        //设置消息
        message(message: string): T;


        //设置消息ID
        messageId(messageId: string): T;

        //设置时间戳
        timestamp(timestamp: number): T;


    }

    interface DeviceMessage<T extends DeviceMessage<T>> extends ThingMessage<T> {
        /**
         * 获取设备ID
         */
        getDeviceId(): string;

    }

    interface DeviceMessageReply<T extends DeviceMessageReply<T>> extends ThingMessageReply<T> {
        /**
         * 获取设备ID
         */
        getDeviceId(): string;

    }

    interface PropertyMessage {

        getProperties(): java.util.Map<string, any>;

        properties(properties: java.util.Map<string, any>): void;

        // @ts-ignore
        propertySourceTimes(properties: java.util.Map<string, long>): void;

        propertyStates(properties: java.util.Map<string, string>): void;

        getProperty(propertyId: string): java.util.Optional<any>;

        getPropertySourceTime(propertyId: string): java.util.Optional<number>;

        getPropertyState(propertyId: string): java.util.Optional<string>;

    }

    interface ReportPropertyMessage extends DeviceMessage<ReportPropertyMessage>, PropertyMessage {

    }

    interface ReadPropertyMessage extends DeviceMessage<ReadPropertyMessage> {
        getProperties(): java.util.List<string>;

        newReply(): ReadPropertyMessageReply;
    }

    interface ReadPropertyMessageReply extends DeviceMessageReply<ReadPropertyMessageReply>, PropertyMessage {

    }

    interface WritePropertyMessage extends DeviceMessage<ReadPropertyMessage> {
        getProperties(): java.util.Map<string, object>;

        newReply(): WritePropertyMessageReply;
    }

    interface WritePropertyMessageReply extends DeviceMessageReply<WritePropertyMessageReply>, PropertyMessage {

    }

    /**
     * 物模型功能调用消息
     */
    interface FunctionInvokeMessage extends DeviceMessage<FunctionInvokeMessage> {
        /**
         * 物模型功能ID
         */
        getFunctionId(): string;

        /**
         * 获取输入参数值,如:
         * let val = message.getInput('val').orElse(null);
         * @param argKey 参数id
         */
        getInput(argKey: string): java.util.Optional<object>;

        /**
         * 将输入参数转为Map
         */
        inputsToMap(): java.util.Map<string, Object>;

        /**
         * 根据此消息创建回复消息
         */
        newReply(): WritePropertyMessageReply;
    }

    /**
     * 功能调用回复,用于回复由平台下发的功能调用消息
     */
    interface FunctionInvokeMessageReply extends DeviceMessageReply<FunctionInvokeMessageReply> {
        /**
         * 获取输出数据
         */
        getOutput(): object;

        /**
         * 设置输出数据
         * @param output 输出数据
         */
        setOutput(output: object): void;
    }

    /**
     * 透传设备消息
     */
    interface DirectMessage extends DeviceMessage<DirectMessage> {
        /**
         * 获取透传原始数据,类型为字节数组byte[]
         */
        getData(): number[]

        /**
         * 将原始数据转换为netty ByteBuf
         */
        asByteBuf(): io.netty.buffer.ByteBuf;
    }
}

declare module org.jetlinks.metadata {

    class DataType {
        getType(): string;
    }

}
declare module org.jetlinks.metadata.types {

    class GeoPoint extends DataType {
        /**
         * 经度
         */
            //@ts-ignore
        lat: float;

        /**
         * 纬度
         */
            //@ts-ignore
        lon: float;

    }
}

declare module org.jetlinks.core.things.rpc {

    /**
     * 提供链式 API 以配置消息头、消息ID、是否异步等，并执行调用。
     */
    interface ThingRpcSpec<Self, Message, Reply> {
        /** 设置超时时间（毫秒）。 */
        timeout(timeoutMs: number): Self;

        /** 指定自定义消息ID。 */
        messageId(messageId: string): Self;

        /** 是否采用异步执行策略。 */
        async(async: boolean): Self;

        /** 设置Header */
        header(header: string, value: any): Self;

        /** 批量设置头部键值。 */
        headers(headers: java.util.Map<string, any>): Self;

        /** 将执行错误映射为 Reply（而非抛出异常）。 */
        mapErrorToReply(mapErrorToReply: boolean): Self;

        /** 对底层消息进行自定义处理（直接修改 Message）。请勿修改设备id等信息 */
        custom(handler: (message: Message) => void): Self;

        /**
         * 响应式方式执行
         */
        // @ts-ignore
        execute(): reactor.core.publisher.Mono<Reply>;

        /**
         * 阻塞方式执行
         */
        executeBlocking(): Reply;
    }

    /**
     * 读取物属性的 RPC 规范。
     * 在通用能力上提供了便捷的属性选择方法。
     */
    interface ReadPropertyRpcSpec<Message extends org.jetlinks.core.message.ReadPropertyMessage, Reply extends org.jetlinks.core.message.ReadPropertyMessageReply>
        extends ThingRpcSpec<ReadPropertyRpcSpec<Message, Reply>, Message, Reply> {

        /** 指定要读取的属性ID列表。 */
        properties(...properties: string[]): ReadPropertyRpcSpec<Message, Reply>;
    }

    /**
     * 调用物功能的 RPC 规范。
     * 提供功能入参的便捷设置方法。
     */
    interface InvokeFunctionRpcSpec<Message extends org.jetlinks.core.message.FunctionInvokeMessage, Reply extends org.jetlinks.core.message.FunctionInvokeMessageReply>
        extends ThingRpcSpec<InvokeFunctionRpcSpec<Message, Reply>, Message, Reply> {

        /** 设置单个功能入参。 */
        input(arg: string, value: any): InvokeFunctionRpcSpec<Message, Reply>;

        /** 批量设置功能入参。 */
        inputs(maps: java.util.Map<string, any>): InvokeFunctionRpcSpec<Message, Reply>;
    }

    /**
     * 修改物属性的 RPC 规范。
     * 提供单个/批量属性设置方法。
     */
    interface WritePropertyRpcSpec<Message extends org.jetlinks.core.message.WritePropertyMessage, Reply extends org.jetlinks.core.message.WritePropertyMessageReply>
        extends ThingRpcSpec<WritePropertyRpcSpec<Message, Reply>, Message, Reply> {

        /** 设置单个属性值。 */
        property(property: string, value: any): WritePropertyRpcSpec<Message, Reply>;

        /** 批量设置属性值。 */
        properties(properties: java.util.Map<string, any>): WritePropertyRpcSpec<Message, Reply>;
    }

}