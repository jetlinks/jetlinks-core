import {CommandSupport} from "./CommandSupport";
import {Command} from "./Command";
import {Observable} from "rxjs";


export class CommandSupportProviders {

    private static supports: { [key: string]: CommandSupport } = {};


    static register(serviceId: string, support: CommandSupport) {
        this.supports[serviceId] = support;
    }


    static get(serviceId: string): CommandSupport {
        return this.supports[serviceId];
    }

    static execute<T>(serviceId: string, commandId: string, inputs: { [key: string]: any }): Observable<T> {
        const support = this.get(serviceId);
        if (!support) {
            return new Observable(observer => {
                observer.error(new Error(`unsupported service :${serviceId}`));
                observer.complete();
            });
        }

        return support
            .execute({
                id: commandId,
                inputs: inputs
            });
    }

    static executeStream<T>(serviceId: string,
                            commandId: string,
                            inputs: { [key: string]: any },
                            stream: Observable<any>): Observable<T> {
        const support = this.get(serviceId);
        if (!support) {
            return new Observable(observer => {
                observer.error(new Error(`unsupported service :${serviceId}`));
                observer.complete();
            });
        }
        return support
            .execute({
                id: commandId,
                inputs: inputs,
                stream: stream
            });
    }

}