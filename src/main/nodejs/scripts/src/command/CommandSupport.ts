import {Command} from "./Command";
import {FunctionMetadata} from "../metadata/FunctionMetadata";

export interface CommandSupport {

    execute<R>(command: Command<R>): R;

    getAllCommandMetadata(): FunctionMetadata[];

    getCommandMetadata(id: string): FunctionMetadata;

}

export class SimpleCommandSupport implements CommandSupport {

    private readonly handlers: { [key: string]: CommandHandler<any, any> };

    constructor(handlers: CommandHandler<any, any>[]) {
        this.handlers = {};
        handlers.forEach(handler=> this.handlers[handler.metadata().id] = handler);
    }


    public execute<R>(command: Command<R>): R {

        const handler = this.handlers[command.id];

        if (handler) {
            return handler.execute(command);
        }

        throw new Error(`unsupported command :${command.id}`);
    }

    public registerHandler<R, C extends Command<R>>(handler: CommandHandler<R, C>): this {
        this.handlers[handler.metadata().id] = handler;
        return this;
    }

    public getCommandMetadata(id: string): FunctionMetadata {
        const handler = this.handlers[id];
        if (handler) {
            return handler.metadata();
        }
        throw new Error(`unsupported command :${id}`);
    }

    public getAllCommandMetadata(): FunctionMetadata[] {
        let arr = [];

        for (let handlersKey in this.handlers) {
            arr[handlersKey] = this.handlers[handlersKey].metadata();
        }

        return arr;
    }

}


export interface CommandHandler<R, C extends Command<R>> {

    metadata(): FunctionMetadata;

    execute(command: C): R;

}