declare interface ObjectInput {
    read(buffer: Uint8Array): void;

    readBoolean(): boolean;

    readByte(): number;

    readShort(): number;

    readInt(): number;

    readLong(): number;

    readFloat(): number;

    readDouble(): number;

    readUTF(): string;

    get(offset: number, buffer: Uint8Array): Uint8Array;

    getByte(offset: number): number;

    getShort(offset: number): number;

    getInt(offset: number): number;

    getLong(offset: number): number;

    getFloat(offset: number): number;

    getDouble(offset: number): number;

    getUTF(offset: number): string;
}

export {ObjectInput}