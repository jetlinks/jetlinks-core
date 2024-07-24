import {Command} from "./Command";
import {FunctionMetadata} from "../metadata/FunctionMetadata";
import {Observable} from 'rxjs'

export interface CommandSupport {

    execute<R>(command: Command<R>): Observable<R>;

    getAllCommandMetadata(): Observable<FunctionMetadata>;

    getCommandMetadata(id: string): Observable<FunctionMetadata>;

}

export class SimpleCommandSupport implements CommandSupport {

    private readonly handlers: { [key: string]: CommandHandler<any, any> };

    constructor(handlers: CommandHandler<any, any>[]) {
        this.handlers = {};
        handlers.forEach(handler => this.handlers[handler.metadata().id] = handler);
    }


    execute<R>(command: Command<R>): Observable<R> {
        const handler = this.handlers[command.id];

        if (!handler || !handler.execute) {
            return new Observable(observer => {
                observer.error(new Error(`unsupported command :${command.id}`));
                observer.complete();
            });
        }
        if(command.stream){
            return handler.execute({...command.inputs,stream:command.stream})
        }
        return handler.execute(command.inputs);
    }

    public registerHandler<R, C extends Command<R>>(handler: CommandHandler<R, C>): this {
        this.handlers[handler.metadata().id] = handler;
        return this;
    }

    public getCommandMetadata(id: string): Observable<FunctionMetadata> {
        const handler = this.handlers[id];
        return new Observable(sub => {
            if (handler) {
                sub.next(handler.metadata());
            }
            sub.complete();
        })
    }

    public getAllCommandMetadata(): Observable<FunctionMetadata> {
        return new Observable(sub => {
            for (let handlersKey in this.handlers) {
                sub.next(this.handlers[handlersKey].metadata());
            }
            sub.complete();
        })
    }

}


export interface CommandHandler<R, C extends Command<R>> {

    metadata(): FunctionMetadata;

    execute(command: { [key: string]: any }): Observable<R>;

}