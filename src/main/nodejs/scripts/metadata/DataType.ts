import {PropertyMetadata} from "./PropertyMetadata";

export enum DataTypeId {
    int = "int",
    long = "long",
    float = "float",
    double = "double",
    string = "string",
    boolean = "boolean",
    date = "date",
    file = "file",
    object = "object",
    array = "array"
}

export interface DataType {
    id: DataTypeId;

    expands?: { [key: string]: any };
}

export class ObjectType implements DataType {

    readonly id: DataTypeId = DataTypeId.object;

    expands?: { [key: string]: any } = {};

    properties: PropertyMetadata[];

    constructor(properties: PropertyMetadata[]) {
        this.properties = properties;
    }

    withExpands(expands: { [key: string]: any }): this {
        this.expands = expands;
        return this;
    }

}

