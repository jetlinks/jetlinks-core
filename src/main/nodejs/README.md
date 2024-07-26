##

```ts

import {CommandSupportProviders} from './CommandSupportProviders';
import {SimpleCommandSupport} from 'CommandSupport';


CommandSupportProviders
    .register("testService", new SimpleCommandSupport()
        .registerHandler({
            execute:"query",

        }));


```