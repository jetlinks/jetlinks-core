import {CommandSupportProviders} from "../../src/command/CommandSupportProviders";
import {SimpleCommandSupport} from "../../src/command/CommandSupport";
import {FunctionMetadata} from "../../src/metadata/FunctionMetadata";
import {Command} from "../../src/command/Command";
import {DataTypeId, ObjectType} from "../../src/metadata/DataType";


test('Test CommandSupportProviders', () => {

    CommandSupportProviders
        .register("testService",
            new SimpleCommandSupport([
                {
                    metadata(): FunctionMetadata {
                        return {
                            id: "test",
                            name: "test",
                            description: "test",
                            output: new ObjectType([]),
                            inputs: []
                        }
                    },
                    execute(command: Command<any>): any {

                        let name = command.inputs['name'];

                        return name?.toUpperCase();

                    },

                }
            ]));

    let support = CommandSupportProviders.get("testService");

    let result = support.execute({
        "id": "test",
        inputs: {
            "name": "test"
        }
    });

    console.log(JSON.stringify(support.getCommandMetadata("test")))

    expect(result).toEqual("TEST")
})