//@ts-ignore
import {java} from "java";

declare module io.netty.buffer {

    class Unpooled {

        static buffer(): ByteBuf;
        // @ts-ignore
        static wrappedBuffer(byteArray: byte[]): ByteBuf;
    }

    class ByteBuf {

        /**
         * 当前读到的索引
         */
        // @ts-ignore
        readerIndex(): int;


        /**
         * 当前写入的索引
         */
        // @ts-ignore
        writerIndex(): int;

        /**
         * 可读取的字节数
         */
        // @ts-ignore
        readableBytes(): int;

        /**
         * 可写入的字节数
         */
        // @ts-ignore
        writableBytes(): int;

        /**
         * 写入另外一个ByteBuf
         * @param buf ByteBuf
         */
        // @ts-ignore
        writeBytes(buf: ByteBuf | byte[]): ByteBuf;

        /**
         * 读取单字节数据
         * @return byte
         */
        // @ts-ignore
        readByte(): byte;

        /**
         * 获取指定索引的无符号单字节数据
         * @return short
         */
        // @ts-ignore
        readUnsignedByte(): short;

        /**
         * 读取2字节数字,大端模式
         * @return short
         */
        // @ts-ignore
        readShort(): short;

        /**
         * 读取2字节数字,小端模式
         * @return short
         */
        // @ts-ignore
        readShortLE(): short;

        /**
         * 读取无符号2字节数字,大端模式
         * @return int
         */
        // @ts-ignore
        readUnsignedShort(): int;

        /**
         * 读取2字节数字,小端模式
         * @return int
         */
        // @ts-ignore
        readUnsignedShortLE(): int;


        /**
         * 读取3字节数字,大端模式
         * @return int
         */
        // @ts-ignore
        readMedium(): int;

        /**
         * 读取3字节数字,小端模式
         * @return int
         */
        // @ts-ignore
        readMediumLE(): int;

        /**
         * 读取3字节数字,大端模式
         * @return int
         */
        // @ts-ignore
        readUnsignedMedium(): int;

        /**
         * 读取无符号3字节数字,小端模式
         * @return int
         */
        // @ts-ignore
        readUnsignedMediumLE(): int;

        /**
         * 读取4字节数字,大端模式
         * @return int
         */
        // @ts-ignore
        readInt(): int;

        /**
         * 读取4字节数字,小端模式
         * @return int
         */
        // @ts-ignore
        readIntLE(): int;

        /**
         * 读取4字节数字,大端模式
         * @return long
         */
        // @ts-ignore
        readUnsignedInt(): long;

        /**
         * 读取无符号4字节数字,小端模式
         * @return long
         */
        // @ts-ignore
        readUnsignedIntLE(): long;

        /**
         * 获取指定索引的8字节数字,大端模式
         * @return long
         */
        // @ts-ignore
        readLong(): long;

        /**
         * 读取8字节数字,小端模式
         * @return long
         */
        // @ts-ignore
        readLongLE(): long;

        /**
         * 读取4字节浮点数,IEEE754编码,大端模式
         * @return float
         */
        // @ts-ignore
        readFloat(): float;

        /**
         * 读取4字节浮点数,IEEE754编码,小端模式
         * @return float
         */
        // @ts-ignore
        readFloatLE(): float;

        /**
         * 读取8字节浮点数,IEEE754编码,大端模式
         * @return double
         */
        // @ts-ignore
        readDouble(): double;

        /**
         * 读取8字节浮点数,IEEE754编码,小端模式
         * @return double
         */
        // @ts-ignore
        readDoubleLE(): double;


        /**
         * 获取指定索引的单字节数据
         */
        // @ts-ignore
        getByte(index: int): byte;

        /**
         * 获取指定索引的无符号单字节数据
         */
        // @ts-ignore
        getUnsignedByte(index: int): short;

        /**
         * 获取指定索引的2字节数字(short)大端模式
         */
        // @ts-ignore
        getShort(index: int): short;

        /**
         * 获取指定索引的2字节数字(short)小端模式
         * @param index 索引,从0开始
         * @return short
         */
        // @ts-ignore
        getShortLE(index: int): short;

        /**
         * 获取指定索引的无符号2字节数字(int)大端模式
         * @param index 索引,从0开始
         * @return int
         */
        // @ts-ignore
        getUnsignedShort(index: int): int;

        /**
         * 获取指定索引的无符号2字节数字,小端模式
         * @param index 索引,从0开始
         * @return int
         */
        // @ts-ignore
        getUnsignedShortLE(index: int): int;

        /**
         * 获取指定索引的3字节数字值,大端
         * @param index 索引,从0开始
         */
        // @ts-ignore
        getMedium(index: int): int;

        /**
         * 获取指定索引的3字节数字值,小端
         * @param index 索引,从0开始
         */
        // @ts-ignore
        getMediumLE(index: int): int;

        /**
         * 获取指定索引的无符号3字节数字值,大端
         * @param index 索引,从0开始
         */
        // @ts-ignore
        getUnsignedMedium(index: int): int;

        /**
         * 获取指定索引的无符号3字节数字值,小端
         * @param index 索引,从0开始
         */
        // @ts-ignore
        getUnsignedMediumLE(index: int): int;


        /**
         * 获取指定索引的无符号4字节数字,大端模式
         * @param index 索引,从0开始
         * @return int
         */
        // @ts-ignore
        getInt(index: int): int;

        /**
         * 获取指定索引的4字节数字,小端模式
         * @param index 索引,从0开始
         * @return int
         */
        // @ts-ignore
        getIntLE(index: int): int;

        /**
         * 获取指定索引的无符号4字节数字大端模式
         * @param index 索引,从0开始
         * @return long
         */
        // @ts-ignore
        getUnsignedInt(index: int): long;

        /**
         * 获取指定索引的无符号4字节数字小端模式
         * @param index 索引,从0开始
         * @return long
         *
         */
        // @ts-ignore
        getUnsignedIntLE(index: int): long;

        /**
         * 获取指定索引的8字节数字,大端模式
         * @param index 索引,从0开始
         * @return long
         */
        // @ts-ignore
        getLong(index: int): long;

        /**
         * 获取指定索引的8字节数字,小端模式
         * @param index 索引,从0开始
         * @return long
         */
        // @ts-ignore
        getLongLE(index: int): long;

        /**
         * 获取指定索引的4字节浮点数,IEEE754编码,大端模式
         * @param index 索引,从0开始
         * @return float
         */
        // @ts-ignore
        getFloat(index: int): float;

        /**
         * 获取指定索引的4字节浮点数,IEEE754编码,小端模式
         * @param index 索引,从0开始
         * @return float
         */
        // @ts-ignore
        getFloatLE(index: int): float;

        /**
         * 获取指定索引的8字节浮点数,IEEE754编码,大端模式
         * @param index 索引,从0开始
         * @return double
         */
        // @ts-ignore
        getDouble(index: int): double;

        /**
         * 获取指定索引的8字节浮点数,IEEE754编码,小端模式
         * @param index 索引,从0开始
         * @return double
         */
        // @ts-ignore
        getDoubleLE(index: int): double;


        ////

        /**
         * 写入单字节数据
         */
        // @ts-ignore
        writeByte(value: byte): ByteBuf;

        /**
         * 写入2字节数字(short)大端模式
         */
        // @ts-ignore
        writeShort(value: short): ByteBuf;

        /**
         * 写入2字节数字(short)小端模式
         * @param value 值
         */
        // @ts-ignore
        writeShortLE(value: short): ByteBuf;

        /**
         * 写入3字节数字值,大端
         * @param value 值
         */
        // @ts-ignore
        writeMedium(value: int): ByteBuf;

        /**
         * 写入3字节数字值,小端
         * @param value 值
         */
        // @ts-ignore
        writeMediumLE(value: int): ByteBuf;

        /**
         * 写入无符号4字节数字,大端模式
         * @param value 值
         */
        // @ts-ignore
        writeInt(value: int): ByteBuf;

        /**
         * 写入4字节数字,小端模式
         * @param value 值
         */
        // @ts-ignore
        writeIntLE(value: int): ByteBuf;


        /**
         * 写入8字节数字,大端模式
         * @param value 值
         */
        // @ts-ignore
        writeLong(value: long): ByteBuf;

        /**
         * 写入8字节数字,小端模式
         * @param value 值
         */
        // @ts-ignore
        writeLongLE(value: long): ByteBuf;

        /**
         * 写入4字节浮点数,IEEE754编码,大端模式
         * @param value 值
         */
        // @ts-ignore
        writeFloat(value: float): ByteBuf;

        /**
         * 写入4字节浮点数,IEEE754编码,小端模式
         * @param value 值
         */
        // @ts-ignore
        writeFloatLE(value: float): ByteBuf;

        /**
         * 写入8字节浮点数,IEEE754编码,大端模式
         * @param value 值
         */
        // @ts-ignore
        writeDouble(value: double): ByteBuf;

        /**
         * 写入8字节浮点数,IEEE754编码,小端模式
         * @param value 值
         */
        // @ts-ignore
        writeDoubleLE(value: double): ByteBuf;
    }

}
