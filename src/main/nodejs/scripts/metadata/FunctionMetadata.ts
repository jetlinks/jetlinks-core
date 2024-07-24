import {PropertyMetadata} from "./PropertyMetadata";
import {DataType, DataTypeId} from "./DataType";

export class FunctionMetadata {

    id: string;
    name: string;
    description?: string;
    inputs: PropertyMetadata[];
    output: DataType | DataTypeId;
}
