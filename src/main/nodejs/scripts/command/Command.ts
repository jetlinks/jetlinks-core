import {Observable} from "rxjs";

export class Command<Response> {

    id: string;

    inputs: { [key: string]: any };

    stream?: Observable<any>
}
