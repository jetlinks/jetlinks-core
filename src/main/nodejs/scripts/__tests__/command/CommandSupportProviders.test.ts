import {CommandSupportProviders} from "../../command/CommandSupportProviders";
import {SimpleCommandSupport} from "../../command/CommandSupport";
import {FunctionMetadata} from "../../metadata/FunctionMetadata";
import {Command} from "../../command/Command";
import {DataTypeId, ObjectType} from "../../metadata/DataType";
import {Observable} from 'rxjs'


test('Test CommandSupportProviders', async () => {

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
                    execute(inputs: { [key: string]: any }): Observable<any> {

                        let name = inputs['name'];

                        return new Observable(ob => {
                            ob.next(name.toUpperCase());
                            ob.complete();
                        });

                    },

                }
            ]));

    let result = await CommandSupportProviders
        .execute<string>(
            "testService",
            "test",
            {"name": "test"})
        .toPromise();


    expect(result).toEqual("TEST")
})