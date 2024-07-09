import {CommandSupport} from "./CommandSupport";


export class CommandSupportProviders {

    private static supports: { [key: string]: CommandSupport } = {};


   static register(serviceId: string, support: CommandSupport) {
        this.supports[serviceId] = support;
    }


    static get(serviceId: string): CommandSupport {
        return this.supports[serviceId];
    }

}