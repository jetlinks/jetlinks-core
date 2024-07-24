export interface Monitor {

    logger(): Logger;

    metrics(): Metrics;

    tracer(): Tracer;
}

export interface Logger {

    debug(message: string, ...args: any[]): void;

    info(message: string, ...args: any[]): void;

    warn(message: string, ...args: any[]): void;

    error(message: string, ...args: any[]): void;

    trace(message: string, ...args: any[]): void;

}

export interface Metrics {

    count(operation: string, inc: number): void;

    value(operation: string, inc: number): void;

    error(operation: string, error: Error): void;
}

export interface Tracer {

    trace<T>(operation: string, callback: () => T): T;

}