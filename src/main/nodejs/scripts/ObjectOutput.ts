declare interface ObjectOutput {

    write(buffer: Uint8Array): ObjectOutput;

    writeBoolean(bool: boolean): ObjectOutput;

    writeByte(b: number): ObjectOutput;

    writeShort(d: number): ObjectOutput;

    writeInt(d: number): ObjectOutput;

    writeLong(d: number): ObjectOutput;

    writeFloat(d: number): ObjectOutput;

    writeDouble(d: number): ObjectOutput;

    writeUTF(str: String): ObjectOutput;


}

export {ObjectOutput}